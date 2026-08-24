package io.github.exco9.questlogenvelope.mail;

import io.github.exco9.questlogenvelope.quest.QuestMailUnlocker;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/** Metadata and execution for quest actions triggered when an Envelope seal is broken. */
public final class QuestMagicSeal {
    private static final String ROOT_KEY = "questlog_envelope";
    private static final String MAGIC_KEY = "magic_seal";
    private static final String QUEST_KEY = "quest";
    private static final String OWNER_KEY = "owner";
    private static final String ACTION_ID_KEY = "action_id";

    private QuestMagicSeal() {
    }

    public static ItemStack attach(ItemStack stack, ServerPlayer recipient, ResourceLocation questId) {
        CompoundTag customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag addonData = customData.contains(ROOT_KEY, Tag.TAG_COMPOUND)
                ? customData.getCompound(ROOT_KEY)
                : new CompoundTag();

        CompoundTag magic = new CompoundTag();
        magic.putString(QUEST_KEY, questId.toString());
        magic.putUUID(OWNER_KEY, recipient.getUUID());
        magic.putUUID(ACTION_ID_KEY, UUID.randomUUID());
        addonData.put(MAGIC_KEY, magic);
        customData.put(ROOT_KEY, addonData);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
        return stack;
    }

    /** Executes once when the physical Envelope seal has finished breaking. */
    public static void activate(ItemStack sealedStack, Level level, @Nullable LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player) || level.isClientSide()) {
            return;
        }

        Optional<Action> action = read(sealedStack);
        if (action.isEmpty()) {
            return;
        }

        Action value = action.get();
        if (!player.getUUID().equals(value.owner())) {
            return;
        }

        if (!MagicSealSavedData.get(player.serverLevel()).claim(value.actionId())) {
            return;
        }

        // Reuse the same pending/unlock bridge as delivered quest mail. This keeps
        // the action safe even if Questlog player data is temporarily unavailable.
        PendingQuestMailSavedData.get(player.serverLevel())
                .record(player.getScoreboardName(), value.questId());
        QuestMailUnlocker.applyPending(player, value.questId());
        player.displayClientMessage(
                Component.translatable("questlog_envelope.magic_seal.accepted"),
                true
        );
    }

    /** Removes only magic-action metadata; normal quest/delivery markers remain untouched. */
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
        addonData.remove(MAGIC_KEY);
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
        if (!addonData.contains(MAGIC_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }

        CompoundTag magic = addonData.getCompound(MAGIC_KEY);
        if (!magic.contains(QUEST_KEY, Tag.TAG_STRING)
                || !magic.hasUUID(OWNER_KEY)
                || !magic.hasUUID(ACTION_ID_KEY)) {
            return Optional.empty();
        }

        ResourceLocation quest = ResourceLocation.tryParse(magic.getString(QUEST_KEY));
        if (quest == null) {
            return Optional.empty();
        }

        return Optional.of(new Action(
                quest,
                magic.getUUID(OWNER_KEY),
                magic.getUUID(ACTION_ID_KEY)
        ));
    }

    private record Action(ResourceLocation questId, UUID owner, UUID actionId) {
    }
}
