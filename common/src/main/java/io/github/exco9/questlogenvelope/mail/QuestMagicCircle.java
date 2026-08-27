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

    public static final int WRITABLE_WIDTH = 142;
    public static final int WRITABLE_HEIGHT = 144;
    public static final int DEFAULT_SIZE = 36;
    public static final int MIN_SIZE = 16;
    public static final int MAX_SIZE = 96;

    private static final int DEFAULT_MARGIN = 3;

    private static final String ROOT_KEY = "questlog_envelope";
    private static final String CIRCLE_KEY = "magic_circle";
    private static final String ACTION_ID_KEY = "action_id";
    private static final String COLOR_KEY = "color";
    private static final String X_KEY = "x";
    private static final String Y_KEY = "y";
    private static final String SIZE_KEY = "size";

    private QuestMagicCircle() {
    }

    /**
     * Attaches only the action id and visual settings to the physical item. The
     * executable action itself is stored server-side in MagicCircleSavedData.
     */
    public static ItemStack attach(
            ItemStack stack,
            ServerPlayer recipient,
            @Nullable ResourceLocation grantsQuestId,
            @Nullable String command,
            int color,
            int x,
            int y,
            int size
    ) {
        UUID actionId = UUID.randomUUID();
        MagicCircleSavedData.get(recipient.serverLevel()).register(
                actionId,
                recipient.getUUID(),
                grantsQuestId,
                normalizeCommand(command)
        );

        int safeSize = clampSize(size);
        int safeX = clampX(x, safeSize);
        int safeY = clampY(y, safeSize);

        CompoundTag customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag addonData = customData.contains(ROOT_KEY, Tag.TAG_COMPOUND)
                ? customData.getCompound(ROOT_KEY)
                : new CompoundTag();

        CompoundTag circle = new CompoundTag();
        circle.putUUID(ACTION_ID_KEY, actionId);
        circle.putInt(COLOR_KEY, color & 0xFFFFFF);
        circle.putInt(X_KEY, safeX);
        circle.putInt(Y_KEY, safeY);
        circle.putInt(SIZE_KEY, safeSize);
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

    public static int getXOffset(ItemStack stack) {
        return read(stack).map(CircleData::x).orElse(defaultX(DEFAULT_SIZE));
    }

    public static int getYOffset(ItemStack stack) {
        return read(stack).map(CircleData::y).orElse(defaultY(DEFAULT_SIZE));
    }

    public static int getSize(ItemStack stack) {
        return read(stack).map(CircleData::size).orElse(DEFAULT_SIZE);
    }

    public static int defaultX(int size) {
        int safeSize = clampSize(size);
        return clampX(WRITABLE_WIDTH - safeSize - DEFAULT_MARGIN, safeSize);
    }

    public static int defaultY(int size) {
        int safeSize = clampSize(size);
        return clampY(WRITABLE_HEIGHT - safeSize - DEFAULT_MARGIN, safeSize);
    }

    public static int clampSize(int size) {
        return Math.max(MIN_SIZE, Math.min(MAX_SIZE, size));
    }

    public static int clampX(int x, int size) {
        int safeSize = clampSize(size);
        return Math.max(0, Math.min(WRITABLE_WIDTH - safeSize, x));
    }

    public static int clampY(int y, int size) {
        int safeSize = clampSize(size);
        return Math.max(0, Math.min(WRITABLE_HEIGHT - safeSize, y));
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
        int size = circle.contains(SIZE_KEY, Tag.TAG_INT)
                ? clampSize(circle.getInt(SIZE_KEY))
                : DEFAULT_SIZE;
        int x = circle.contains(X_KEY, Tag.TAG_INT)
                ? clampX(circle.getInt(X_KEY), size)
                : defaultX(size);
        int y = circle.contains(Y_KEY, Tag.TAG_INT)
                ? clampY(circle.getInt(Y_KEY), size)
                : defaultY(size);

        return Optional.of(new CircleData(circle.getUUID(ACTION_ID_KEY), color, x, y, size));
    }

    public enum ActivationResult {
        ACTIVATED,
        ALREADY_USED,
        NOT_OWNER,
        INVALID_ACTION,
        NOT_THIS_ITEM
    }

    private record CircleData(UUID actionId, int color, int x, int y, int size) {
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
