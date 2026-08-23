package io.github.exco9.questlogenvelope.mail;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;

/**
 * Stores Questlog/Envelope integration markers directly on an Envelope ItemStack
 * using vanilla CUSTOM_DATA.
 */
public final class QuestMailMarker {
    private static final String ROOT_KEY = "questlog_envelope";
    private static final String QUEST_ID_KEY = "quest_id";
    private static final String DIRECT_PLAYER_DROP_KEY = "direct_player_drop";

    private QuestMailMarker() {
    }

    public static ItemStack set(ItemStack stack, ResourceLocation questId) {
        CompoundTag customData = getCustomData(stack);
        CompoundTag addonData = getAddonData(customData);

        addonData.putString(QUEST_ID_KEY, questId.toString());
        customData.put(ROOT_KEY, addonData);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
        return stack;
    }

    public static Optional<ResourceLocation> get(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return Optional.empty();
        }

        CompoundTag customData = data.copyTag();
        if (!customData.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }

        CompoundTag addonData = customData.getCompound(ROOT_KEY);
        if (!addonData.contains(QUEST_ID_KEY, Tag.TAG_STRING)) {
            return Optional.empty();
        }

        try {
            return Optional.of(ResourceLocation.parse(addonData.getString(QUEST_ID_KEY)));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public static ItemStack markDirectPlayerDrop(ItemStack stack) {
        CompoundTag customData = getCustomData(stack);
        CompoundTag addonData = getAddonData(customData);
        addonData.putBoolean(DIRECT_PLAYER_DROP_KEY, true);
        customData.put(ROOT_KEY, addonData);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
        return stack;
    }

    public static boolean isDirectPlayerDrop(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return false;
        }

        CompoundTag customData = data.copyTag();
        if (!customData.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return false;
        }

        return customData.getCompound(ROOT_KEY).getBoolean(DIRECT_PLAYER_DROP_KEY);
    }

    public static ItemStack clearDirectPlayerDrop(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return stack;
        }

        CompoundTag customData = data.copyTag();
        if (!customData.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return stack;
        }

        CompoundTag addonData = customData.getCompound(ROOT_KEY);
        addonData.remove(DIRECT_PLAYER_DROP_KEY);

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

    private static CompoundTag getCustomData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static CompoundTag getAddonData(CompoundTag customData) {
        return customData.contains(ROOT_KEY, Tag.TAG_COMPOUND)
                ? customData.getCompound(ROOT_KEY)
                : new CompoundTag();
    }
}
