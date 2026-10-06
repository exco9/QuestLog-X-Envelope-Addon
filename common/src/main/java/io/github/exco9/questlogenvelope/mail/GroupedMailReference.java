package io.github.exco9.questlogenvelope.mail;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;
import java.util.UUID;

/** Small server-side cargo reference on an otherwise ordinary Envelope mail item. */
public record GroupedMailReference(UUID batch, UUID attempt) {
    private static final String KEY = "questlog_envelope_batch";

    public static ItemStack attach(ItemStack original, UUID batch, UUID attempt) {
        var carrier = original.copyWithCount(1);
        var data = carrier.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var reference = new CompoundTag();
        reference.putUUID("batch", batch);
        reference.putUUID("attempt", attempt);
        data.put(KEY, reference);
        carrier.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return carrier;
    }

    public static boolean isCarrier(ItemStack mail) {
        return mail.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .contains(KEY, Tag.TAG_COMPOUND);
    }

    public static Optional<GroupedMailReference> read(ItemStack mail) {
        var reference = mail.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(KEY);
        return reference.hasUUID("batch") && reference.hasUUID("attempt")
                ? Optional.of(new GroupedMailReference(reference.getUUID("batch"), reference.getUUID("attempt")))
                : Optional.empty();
    }
}
