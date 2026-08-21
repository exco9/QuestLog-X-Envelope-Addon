package io.github.exco9.questlogenvelope.quest;

import com.google.gson.JsonObject;
import io.github.exco9.questlogenvelope.mail.QuestMailMarker;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.type.PlayerAddress;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import io.github.mortuusars.envelope.world.mail.service.ServiceAddressDefinition;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.infernalstudios.questlog.core.quests.rewards.Reward;
import org.jetbrains.annotations.Nullable;

public final class LetterReward extends Reward {
    @Nullable private final ResourceLocation senderId;
    @Nullable private final ResourceLocation grantsQuestId;
    private final String title;
    private final String text;

    public LetterReward(JsonObject definition) {
        super(definition);
        senderId = definition.has("sender") ? ResourceLocation.parse(definition.get("sender").getAsString()) : null;
        grantsQuestId = definition.has("grants_quest") ? ResourceLocation.parse(definition.get("grants_quest").getAsString()) : null;
        title = definition.has("title") ? definition.get("title").getAsString() : "Letter";
        text = definition.has("text") ? definition.get("text").getAsString() : "";
    }

    @Override
    public void applyReward(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        MailService service = MailService.of(level);
        Address sender = senderId == null ? service.getAddress() : resolveSender(level, senderId);

        ItemStack letter = Mail.createLetter(Component.literal(text))
                .set(DataComponents.ITEM_NAME, Component.literal(title))
                .get();

        if (grantsQuestId != null) QuestMailMarker.set(letter, grantsQuestId);

        service.getDeliveryManager().startService(Delivery.draft()
                .deliver(letter)
                .from(sender)
                .to(new PlayerAddress(player)));

        super.applyReward(player);
    }

    private static ServiceAddress resolveSender(ServerLevel level, ResourceLocation id) {
        ResourceKey<ServiceAddressDefinition> key = ResourceKey.create(Envelope.Registries.SERVICE_ADDRESS_DEFINITION, id);
        return ServiceAddress.getOrThrow(level.registryAccess(), key);
    }
}
