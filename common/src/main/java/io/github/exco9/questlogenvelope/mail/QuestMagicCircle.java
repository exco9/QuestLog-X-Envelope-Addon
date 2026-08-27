package io.github.exco9.questlogenvelope.mail;

import io.github.mortuusars.envelope.Envelope;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/** Metadata and server-side execution for the magic circle drawn on quest letters. */
public final class QuestMagicCircle {
    public static final int DEFAULT_COLOR = 0x55AAFF;

    private static final String ROOT_KEY = "questlog_envelope";
    private static final String CIRCLE_KEY = "magic_circle";
    private static final String ACTION_ID_KEY = "action_id";
    private static final String COLOR_KEY = "color";

    private QuestMagicCircle() {
    }

    /**
     * Attaches only the action id and visual color to the physical item. The
     * executable action itself is stored server-side in MagicCircleSavedData.
     */
    public static ItemStack attach(
            ItemStack stack,
            ServerPlayer recipient,
            @Nullable ResourceLocation grantsQuestId,
            @Nullable String command,
            int color
    ) {
        UUID actionId = UUID.randomUUID();
        MagicCircleSavedData.get(recipient.serverLevel()).register(
                actionId,
                recipient.getUUID(),
                grantsQuestId,
                normalizeCommand(command)
        );

        CompoundTag customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag addonData = customData.contains(ROOT_KEY, Tag.TAG_COMPOUND)
                ? customData.getCompound(ROOT_KEY)
                : new CompoundTag();

        CompoundTag circle = new CompoundTag();
        circle.putUUID(ACTION_ID_KEY, actionId);
        circle.putInt(COLOR_KEY, color & 0xFFFFFF);
        addonData.put(CIRCLE_KEY, circle);
        customData.put(ROOT_KEY, addonData);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
        return stack;
    }

    public static boolean has(ItemStack stack) {
        return read(stack).isPresent();
    }

    public static Optional<UUID> getActionId(ItemStack stack) {
        return read(stack).map(CircleData::actionId);
    }

    public static int getColor(ItemStack stack) {
        return read(stack).map(CircleData::color).orElse(DEFAULT_COLOR);
    }

    public static ActivationResult activate(ItemStack stack, ServerPlayer player, UUID requestedActionId) {
        Optional<CircleData> optionalCircle = read(stack);
        if (optionalCircle.isEmpty() || !optionalCircle.get().actionId().equals(requestedActionId)) {
            return ActivationResult.NOT_THIS_ITEM;
        }

        CircleData circle = optionalCircle.get();
        MagicCircleSavedData data = MagicCircleSavedData.get(player.serverLevel());
        Optional<MagicCircleSavedData.ActionDefinition> optionalAction = data.getAction(circle.actionId());

        if (optionalAction.isEmpty()) {
            if (data.isConsumed(circle.actionId())) {
                clear(stack);
                player.displayClientMessage(
                        Component.translatable("questlog_envelope.magic_circle.already_used"),
                        true
                );
                return ActivationResult.ALREADY_USED;
            }
            return ActivationResult.INVALID_ACTION;
        }

        MagicCircleSavedData.ActionDefinition action = optionalAction.get();
        if (!player.getUUID().equals(action.owner())) {
            return ActivationResult.NOT_OWNER;
        }

        if (!data.claim(circle.actionId())) {
            return ActivationResult.ALREADY_USED;
        }

        if (action.questId() != null) {
            PendingQuestMailSavedData.get(player.getServer().overworld())
                    .record(player.getScoreboardName(), action.questId());
            QuestMailUnlockerBridge.apply(player, action.questId());
        }

        executeCommand(player, action.command());

        clear(stack);
        player.displayClientMessage(
                Component.translatable("questlog_envelope.magic_circle.activated"),
                true
        );
        return ActivationResult.ACTIVATED;
    }

    private static void executeCommand(ServerPlayer player, @Nullable String configuredCommand) {
        String command = normalizeCommand(configuredCommand);
        if (command == null) {
            return;
        }

        try {
            // The command was registered from server-authored quest data and is
            // looked up from SavedData, never accepted from the client. Keeping
            // the player as command source makes selectors such as @s intuitive.
            CommandSourceStack source = player.createCommandSourceStack()
                    .withPermission(4)
                    .withSuppressedOutput();
            player.getServer().getCommands().performPrefixedCommand(source, command);
        } catch (RuntimeException exception) {
            Envelope.LOGGER.error(
                    "Failed to execute magic-circle command '{}' for {}.",
                    command,
                    player.getScoreboardName(),
                    exception
            );
        }
    }

    private static @Nullable String normalizeCommand(@Nullable String command) {
        if (command == null) {
            return null;
        }

        String value = command.trim();
        while (value.startsWith("/")) {
            value = value.substring(1).trim();
        }
        return value.isEmpty() ? null : value;
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

    private static Optional<CircleData> read(ItemStack stack) {
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
        if (!circle.hasUUID(ACTION_ID_KEY)) {
            return Optional.empty();
        }

        int color = circle.contains(COLOR_KEY, Tag.TAG_INT)
                ? circle.getInt(COLOR_KEY) & 0xFFFFFF
                : DEFAULT_COLOR;
        return Optional.of(new CircleData(circle.getUUID(ACTION_ID_KEY), color));
    }

    public enum ActivationResult {
        ACTIVATED,
        ALREADY_USED,
        NOT_OWNER,
        INVALID_ACTION,
        NOT_THIS_ITEM
    }

    private record CircleData(UUID actionId, int color) {
    }

    /** Keeps Questlog-specific progression out of the action storage class. */
    private static final class QuestMailUnlockerBridge {
        private QuestMailUnlockerBridge() {
        }

        private static void apply(ServerPlayer player, ResourceLocation questId) {
            io.github.exco9.questlogenvelope.quest.QuestMailUnlocker.applyPending(player, questId);
        }
    }
}
