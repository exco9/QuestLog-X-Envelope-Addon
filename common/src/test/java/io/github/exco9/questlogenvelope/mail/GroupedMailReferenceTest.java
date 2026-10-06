package io.github.exco9.questlogenvelope.mail;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GroupedMailReferenceTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void carrierIsAnIndependentCopyWithOnlyTwoReferenceIds() {
        var original = new ItemStack(Items.PAPER);
        var signature = new LetterSignature("Alex", 0x123456, 12, 1, 2, true);
        signature.attach(original);
        var batch = UUID.randomUUID();
        var attempt = UUID.randomUUID();
        var carrier = GroupedMailReference.attach(original, batch, attempt);
        assertFalse(GroupedMailReference.isCarrier(original));
        assertTrue(GroupedMailReference.isCarrier(carrier));
        assertEquals(batch, GroupedMailReference.read(carrier).orElseThrow().batch());
        assertEquals(attempt, GroupedMailReference.read(carrier.copy()).orElseThrow().attempt());
        assertEquals(signature, LetterSignature.read(carrier));
        var reference = carrier.get(DataComponents.CUSTOM_DATA).copyTag().getCompound("questlog_envelope_batch");
        assertEquals(2, reference.size());
    }

    @Test void ordinaryMailAndMalformedReferenceCannotActAsCargo() {
        var normal = new ItemStack(Items.PAPER);
        assertTrue(GroupedMailReference.read(normal).isEmpty());
        var tag = new net.minecraft.nbt.CompoundTag();
        var reference = new net.minecraft.nbt.CompoundTag();
        reference.putString("batch", "not-a-uuid");
        tag.put("questlog_envelope_batch", reference);
        normal.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        assertTrue(GroupedMailReference.isCarrier(normal));
        assertTrue(GroupedMailReference.read(normal).isEmpty());
    }
}
