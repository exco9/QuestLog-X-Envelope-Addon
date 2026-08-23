package io.github.exco9.questlogenvelope.quest;

import com.google.gson.JsonObject;
import io.github.exco9.questlogenvelope.mail.QuestMailDelivery;
import io.github.exco9.questlogenvelope.mail.QuestMailMarker;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
        senderId = getOptionalId(definition, "sender");
        grantsQuestId = getOptionalId(definition, "grants_quest");
        title = definition.has("title") ? definition.get("title").getAsString() : "Letter";
        text = definition.has("text") ? definition.get("text").getAsString() : "";
    }

    @Override
    public void applyReward(ServerPlayer player) {
        ItemStack letter = Mail.createLetter(Component.literal(text))
                .set(DataComponents.ITEM_NAME, Component.literal(title))
                .get();

        if (grantsQuestId != null) {
            QuestMailMarker.set(letter, grantsQuestId);
        }

        try {
            QuestMailDelivery.dispatch(player, letter, senderId);
        } catch (RuntimeException exception) {
            Envelope.LOGGER.error(
                    "Failed to dispatch Questlog letter reward for {}. Dropping the letter directly instead.",
                    player.getScoreboardName(),
                    exception
            );
            try {
                QuestMailDelivery.dropImmediately(player, letter);
            } catch (RuntimeException fallbackException) {
                Envelope.LOGGER.error("Failed to drop Questlog letter reward fallback.", fallbackException);
            }
        } finally {
            // Questlog marks a reward as collected here. Never let an Envelope
            // routing/service error permanently block the Collect Reward button.
            super.applyReward(player);
        }
    }

    private static @Nullable ResourceLocation getOptionalId(JsonObject definition, String key) {
        if (!definition.has(key) || !definition.get(key).isJsonPrimitive()) {
            return null;
        }
        return ResourceLocation.tryParse(definition.get(key).getAsString());
    }
}
