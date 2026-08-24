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
    private static final String FONT_SIZE_KEY = "font_size";

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
        writeAddonData(stack, customData, addonData);
        return stack;
    }

    /**
     * Stores a global visual size preset for quest letters. Envelope itself has
     * no font-size formatting, so this is deliberately addon metadata while the
     * actual text stays normal Envelope formatted text.
     */
    public static ItemStack setFontSize(ItemStack stack, String fontSize) {
        String normalized = normalizeFontSize(fontSize);
        CompoundTag customData = getCustomData(stack);
        CompoundTag addonData = getAddonData(customData);

        if ("normal".equals(normalized)) {
            addonData.remove(FONT_SIZE_KEY);
        } else {
            addonData.putString(FONT_SIZE_KEY, normalized);
        }

        writeAddonData(stack, customData, addonData);
        return stack;
    }

    public static String getFontSize(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return "normal";
        }

        CompoundTag customData = data.copyTag();
        if (!customData.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return "normal";
        }

        CompoundTag addonData = customData.getCompound(ROOT_KEY);
        if (!addonData.contains(FONT_SIZE_KEY, Tag.TAG_STRING)) {
            return "normal";
        }
        return normalizeFontSize(addonData.getString(FONT_SIZE_KEY));
    }

    public static float getFontScale(ItemStack stack) {
        return switch (getFontSize(stack)) {
            case "small" -> 0.80F;
            case "large" -> 1.25F;
            default -> 1.0F;
        };
    }

    public static String normalizeFontSize(String value) {
        if (value == null) {
            return "normal";
        }
        return switch (value.toLowerCase()) {
            case "small", "large" -> value.toLowerCase();
            default -> "normal";
        };
    }

    private static CompoundTag getCustomData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static CompoundTag getAddonData(CompoundTag customData) {
        return customData.contains(ROOT_KEY, Tag.TAG_COMPOUND)
                ? customData.getCompound(ROOT_KEY)
                : new CompoundTag();
    }

    private static void writeAddonData(ItemStack stack, CompoundTag customData, CompoundTag addonData) {
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
    }
}
