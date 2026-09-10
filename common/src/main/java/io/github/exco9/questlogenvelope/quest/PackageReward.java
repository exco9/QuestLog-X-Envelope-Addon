package io.github.exco9.questlogenvelope.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.exco9.questlogenvelope.mail.QuestMailDelivery;
import io.github.exco9.questlogenvelope.mail.QuestMailMarker;
import io.github.exco9.questlogenvelope.mail.QuestMailSeal;
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
    @Nullable private final ResourceLocation sealSymbolId;
    private final String title;
    private final JsonArray packageDefinitions;
    private final JsonArray legacyItemDefinitions;

    public PackageReward(JsonObject definition) {
        super(definition);
        senderId = getOptionalId(definition, "sender");
        grantsQuestId = getOptionalId(definition, "grants_quest");
        sealSymbolId = getOptionalId(definition, "seal");
        title = definition.has("title") ? definition.get("title").getAsString() : "Package";

        packageDefinitions = definition.has("packages") && definition.get("packages").isJsonArray()
                ? definition.getAsJsonArray("packages").deepCopy()
                : new JsonArray();
        legacyItemDefinitions = definition.has("items") && definition.get("items").isJsonArray()
                ? definition.getAsJsonArray("items").deepCopy()
                : new JsonArray();
    }

    @Override
    public void applyReward(ServerPlayer player) {
        try {
            List<ItemStack> packages = buildPackages(player);
            if (packages.isEmpty()) {
                packages = List.of(Mail.createPackage(PackageContents.EMPTY)
                        .set(DataComponents.ITEM_NAME, Component.literal(title))
                        .get());
            }

            for (int index = 0; index < packages.size(); index++) {
                ItemStack packageStack = QuestMailSeal.apply(player, packages.get(index), sealSymbolId);

                // Wax seals are purely Envelope seals. Quest progression on package
                // delivery remains the normal mail marker behavior.
                if (index == 0 && grantsQuestId != null) {
                    QuestMailMarker.set(packageStack, grantsQuestId);
                }

                try {
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

    private List<ItemStack> buildPackages(ServerPlayer player) {
        if (packageDefinitions.size() > 0) {
            List<ItemStack> packages = new ArrayList<>();

            for (JsonElement pageElement : packageDefinitions) {
                if (!pageElement.isJsonArray()) {
                    Envelope.LOGGER.warn("Skipping invalid Questlog package page: {}", pageElement);
                    continue;
                }

                JsonArray page = pageElement.getAsJsonArray();
                List<ItemStack> pageItems = new ArrayList<>();
                int slots = Math.min(page.size(), PackageContents.SLOTS);

                for (int slot = 0; slot < slots; slot++) {
                    JsonElement definition = page.get(slot);
                    ItemStack stack = parseStack(player, definition);
                    pageItems.add(stack);
                }

                while (pageItems.size() < PackageContents.SLOTS) {
                    pageItems.add(ItemStack.EMPTY);
                }

                packages.add(Mail.createPackage(new PackageContents(pageItems))
                        .set(DataComponents.ITEM_NAME, Component.literal(title))
                        .get());
            }

            return packages;
        }

        List<ItemStack> contents = new ArrayList<>();
        for (JsonElement definition : legacyItemDefinitions) {
            ItemStack stack = parseStack(player, definition);
            if (!stack.isEmpty()) {
                contents.add(stack);
            }
        }

        if (contents.isEmpty()) {
            return List.of();
        }

        return Mail.createPackages(contents, builder ->
                builder.set(DataComponents.ITEM_NAME, Component.literal(title)));
    }

    private ItemStack parseStack(ServerPlayer player, JsonElement definition) {
        if (definition == null || definition.isJsonNull()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = ItemReward.parseItemStack(definition, player.level().registryAccess());
        if (stack.isEmpty()) {
            Envelope.LOGGER.warn("Skipping invalid Questlog package item: {}", definition);
            return ItemStack.EMPTY;
        }

        if (definition.isJsonObject()) {
            JsonObject object = definition.getAsJsonObject();
            if (object.has("count") && object.get("count").isJsonPrimitive()) {
                try {
                    stack.setCount(Math.max(1, object.get("count").getAsInt()));
                } catch (RuntimeException ignored) {
                }
            }
        }

        return stack;
    }

    private static @Nullable ResourceLocation getOptionalId(JsonObject definition, String key) {
        if (!definition.has(key) || !definition.get(key).isJsonPrimitive()) {
            return null;
        }
        return ResourceLocation.tryParse(definition.get(key).getAsString());
    }
}
