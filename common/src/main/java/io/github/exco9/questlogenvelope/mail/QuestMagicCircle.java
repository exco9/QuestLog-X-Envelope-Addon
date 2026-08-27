package io.github.exco9.questlogenvelope.mail;

import io.github.exco9.questlogenvelope.quest.QuestMailUnlocker;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Metadata and server-side execution for the magic circle drawn on quest letters.
 *
 * The transport/action model is deliberately independent from Envelope wax seals.
 * At the moment a circle may optionally execute the existing grants_quest action;
 * additional action types (commands, custom events, etc.) can be added without
 * changing the letter UI or one-shot validation format.
 */
public final class QuestMagicCircle {
    private static final String ROOT_KEY = "questlog_envelope";
    private static final String CIRCLE_KEY = "magic_circle";
    private static final String OWNER_KEY = "owner";
    private static final String ACTION_ID_KEY = "action_id";
    private static final String ACTION_TYPE_KEY = "action_type";
    private static final String QUEST_KEY = "quest";

    private static final String ACTION_NONE = "none";
    private static final String ACTION_QUEST = "quest";

    private QuestMagicCircle() {
    }

    public static ItemStack attach(
            ItemStack stack,
            ServerPlayer recipient,
            @Nullable ResourceLocation grantsQuestId
    ) {
        CompoundTag customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag addonData = customData.contains(ROOT_KEY, Tag.TAG_COMPOUND)
                ? customData.getCompound(ROOT_KEY)
                : new CompoundTag();

        CompoundTag circle = new CompoundTag();
        circle.putUUID(OWNER_KEY, recipient.getUUID());
        circle.putUUID(ACTION_ID_KEY, UUID.randomUUID());
        if (grantsQuestId == null) {
            circle.putString(ACTION_TYPE_KEY, ACTION_NONE);
        } else {
            circle.putString(ACTION_TYPE_KEY, ACTION_QUEST);
            circle.putString(QUEST_KEY, grantsQuestId.toString());
        }

        addonData.put(CIRCLE_KEY, circle);
        customData.put(ROOT_KEY, addonData);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
        return stack;
    }

    public static boolean has(ItemStack stack) {
        return read(stack).isPresent();
    }

    public static Optional<UUID> getActionId(ItemStack stack) {
        return read(stack).map(Action::actionId);
    }

    public static ActivationResult activate(ItemStack stack, ServerPlayer player, UUID requestedActionId) {
        Optional<Action> optionalAction = read(stack);
        if (optionalAction.isEmpty() || !optionalAction.get().actionId().equals(requestedActionId)) {
            return ActivationResult.NOT_THIS_ITEM;
        }

        Action action = optionalAction.get();
        if (!player.getUUID().equals(action.owner())) {
            return ActivationResult.NOT_OWNER;
        }

        if (!MagicCircleSavedData.get(player.serverLevel()).claim(action.actionId())) {
            clear(stack);
            player.displayClientMessage(
                    Component.translatable("questlog_envelope.magic_circle.already_used"),
                    true
            );
            return ActivationResult.ALREADY_USED;
        }

        if (ACTION_QUEST.equals(action.actionType()) && action.questId() != null) {
            ServerLevel mailDataLevel = player.getServer().overworld();
            PendingQuestMailSavedData.get(mailDataLevel)
                    .record(player.getScoreboardName(), action.questId());
            QuestMailUnlocker.applyPending(player, action.questId());
        }

        clear(stack);
        player.displayClientMessage(
                Component.translatable("questlog_envelope.magic_circle.activated"),
                true
        );
        return ActivationResult.ACTIVATED;
    }

    /** Removes only magic-circle metadata; normal mail and quest markers are untouched. */
    public static ItemStack clear(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return stack;
        }

        CompoundTag customData = data.copyTag();
        if (!customData.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return stack;
        }

        CompoundTag addonData = customData.getCompound(ROOT_KEY);
        addonData.remove(CIRCLE_KEY);
        if (addonData.isEmpty()) {
            customData.remove(ROOT_KEY);
        } else {
            customData.put(ROOT_KEY, addonData);
        }

        if (customData.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
        }
        return stack;
    }

    private static Optional<Action> read(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return Optional.empty();
        }

        CompoundTag customData = data.copyTag();
        if (!customData.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }

        CompoundTag addonData = customData.getCompound(ROOT_KEY);
        if (!addonData.contains(CIRCLE_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }

        CompoundTag circle = addonData.getCompound(CIRCLE_KEY);
        if (!circle.hasUUID(OWNER_KEY) || !circle.hasUUID(ACTION_ID_KEY)) {
            return Optional.empty();
        }

        String actionType = circle.contains(ACTION_TYPE_KEY, Tag.TAG_STRING)
                ? circle.getString(ACTION_TYPE_KEY)
                : ACTION_NONE;

        @Nullable ResourceLocation questId = null;
        if (ACTION_QUEST.equals(actionType) && circle.contains(QUEST_KEY, Tag.TAG_STRING)) {
            questId = ResourceLocation.tryParse(circle.getString(QUEST_KEY));
            if (questId == null) {
                return Optional.empty();
            }
        }

        return Optional.of(new Action(
                circle.getUUID(OWNER_KEY),
                circle.getUUID(ACTION_ID_KEY),
                actionType,
                questId
        ));
    }

    public enum ActivationResult {
        ACTIVATED,
        ALREADY_USED,
        NOT_OWNER,
        NOT_THIS_ITEM
    }

    private record Action(
            UUID owner,
            UUID actionId,
            String actionType,
            @Nullable ResourceLocation questId
    ) {
    }
}
