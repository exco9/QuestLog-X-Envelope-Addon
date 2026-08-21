package io.github.exco9.questlogenvelope.mail;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;

/**
 * Stores the quest-unlock marker directly on an Envelope ItemStack using vanilla CUSTOM_DATA.
 *
 * Keeping this data in a vanilla component avoids a loader-specific custom component registry
 * for the first implementation and lets Envelope transport the marker with the mail stack.
 */
public final class QuestMailMarker {
    private static final String ROOT_KEY = "questlog_envelope";
    private static final String QUEST_ID_KEY = "quest_id";

    private QuestMailMarker() {
    }

    public static ItemStack set(ItemStack stack, ResourceLocation questId) {
        CompoundTag customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag addonData = customData.contains(ROOT_KEY, Tag.TAG_COMPOUND)
                ? customData.getCompound(ROOT_KEY)
                : new CompoundTag();

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
}
