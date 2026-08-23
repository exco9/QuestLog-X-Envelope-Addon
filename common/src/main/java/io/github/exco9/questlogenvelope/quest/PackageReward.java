package io.github.exco9.questlogenvelope.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.exco9.questlogenvelope.mail.QuestMailDelivery;
import io.github.exco9.questlogenvelope.mail.QuestMailMarker;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.infernalstudios.questlog.core.quests.rewards.ItemReward;
import org.infernalstudios.questlog.core.quests.rewards.Reward;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Questlog reward that sends one or more real Envelope packages. */
public final class PackageReward extends Reward {
    @Nullable private final ResourceLocation senderId;
    @Nullable private final ResourceLocation grantsQuestId;
    private final String title;
    private final JsonArray itemDefinitions;

    public PackageReward(JsonObject definition) {
        super(definition);
        senderId = getOptionalId(definition, "sender");
        grantsQuestId = getOptionalId(definition, "grants_quest");
        title = definition.has("title") ? definition.get("title").getAsString() : "Package";
        itemDefinitions = definition.has("items") && definition.get("items").isJsonArray()
                ? definition.getAsJsonArray("items").deepCopy()
                : new JsonArray();
    }

    @Override
    public void applyReward(ServerPlayer player) {
        try {
            List<ItemStack> contents = parseContents(player);
            List<ItemStack> packages;

            if (contents.isEmpty()) {
                packages = List.of(Mail.createPackage(PackageContents.EMPTY)
                        .set(DataComponents.ITEM_NAME, Component.literal(title))
                        .get());
            } else {
                packages = Mail.createPackages(contents, builder ->
                        builder.set(DataComponents.ITEM_NAME, Component.literal(title)));
            }

            for (int index = 0; index < packages.size(); index++) {
                ItemStack packageStack = packages.get(index);

                // One quest reward should progress mail_received once, even when
                // Envelope had to split the contents across several packages.
                if (index == 0 && grantsQuestId != null) {
                    QuestMailMarker.set(packageStack, grantsQuestId);
                }

                try {
                    // Quest reward packages are deliberately express: when a mailbox
                    // exists, a service pigeon starts near it instead of spending the
                    // full background travel time crossing the world.
                    QuestMailDelivery.dispatch(player, packageStack, senderId, true);
                } catch (RuntimeException exception) {
                    Envelope.LOGGER.error(
                            "Failed to dispatch Questlog package reward for {}. Dropping this package directly instead.",
                            player.getScoreboardName(),
                            exception
                    );
                    try {
                        QuestMailDelivery.dropImmediately(player, packageStack);
                    } catch (RuntimeException fallbackException) {
                        Envelope.LOGGER.error("Failed to drop Questlog package reward fallback.", fallbackException);
                    }
                }
            }
        } catch (RuntimeException exception) {
            Envelope.LOGGER.error(
                    "Failed to build Questlog package reward for {}.",
                    player.getScoreboardName(),
                    exception
            );
        } finally {
            super.applyReward(player);
        }
    }

    private List<ItemStack> parseContents(ServerPlayer player) {
        List<ItemStack> items = new ArrayList<>();

        for (JsonElement definition : itemDefinitions) {
            ItemStack stack = ItemReward.parseItemStack(definition, player.level().registryAccess());
            if (stack.isEmpty()) {
                Envelope.LOGGER.warn("Skipping invalid Questlog package item: {}", definition);
                continue;
            }

            if (definition.isJsonObject()) {
                JsonObject object = definition.getAsJsonObject();
                if (object.has("count") && object.get("count").isJsonPrimitive()) {
                    try {
                        stack.setCount(Math.max(1, object.get("count").getAsInt()));
                    } catch (RuntimeException ignored) {
                        // Keep the count parsed by ItemStack/Questlog.
                    }
                }
            }

            items.add(stack);
        }

        return items;
    }

    private static @Nullable ResourceLocation getOptionalId(JsonObject definition, String key) {
        if (!definition.has(key) || !definition.get(key).isJsonPrimitive()) {
            return null;
        }
        return ResourceLocation.tryParse(definition.get(key).getAsString());
    }
}
