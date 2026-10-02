package io.github.exco9.questlogenvelope.mail;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LetterSignatureSigningTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void signingPersistsInItemDataAndCannotRepeat() {
        ItemStack letter = new ItemStack(Items.PAPER);
        var signature = new LetterSignature("Arwen", 0x315C82, 12, 10, 100, true);
        signature.attach(letter);
        assertFalse(LetterSignature.isSigned(letter));
        assertTrue(LetterSignature.sign(letter, signature));
        assertTrue(LetterSignature.isSigned(letter));
        assertFalse(LetterSignature.sign(letter, signature));
        ItemStack restored = new ItemStack(Items.PAPER);
        restored.set(DataComponents.CUSTOM_DATA, CustomData.of(letter.get(DataComponents.CUSTOM_DATA).copyTag()));
        assertTrue(LetterSignature.isSigned(restored));
        assertFalse(LetterSignature.sign(restored, signature));
        assertTrue(LetterSignature.isSigned(letter.copy()));
        assertEquals(signature, LetterSignature.read(restored));
    }

    @Test
    void preservesCircleAndUnrelatedMetadataAndRejectsChangedLetters() {
        ItemStack letter = new ItemStack(Items.PAPER);
        CompoundTag custom = new CompoundTag();
        CompoundTag addon = new CompoundTag();
        addon.putString("quest_marker", "example:expedition");
        CompoundTag circle = new CompoundTag();
        circle.putBoolean("activated", true);
        addon.put("magic_circle", circle);
        custom.put("questlog_envelope", addon);
        custom.putString("unrelated", "retained");
        letter.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        var signature = new LetterSignature("Arwen", 0, 12, 0, 0, false);
        signature.attach(letter);
        var changed = new LetterSignature("Another name", 0, 12, 0, 0, false);
        assertFalse(LetterSignature.sign(letter, changed));
        assertFalse(LetterSignature.isSigned(letter));
        assertTrue(LetterSignature.sign(letter, signature));
        var saved = letter.get(DataComponents.CUSTOM_DATA).copyTag();
        assertEquals("retained", saved.getString("unrelated"));
        assertEquals("example:expedition", saved.getCompound("questlog_envelope").getString("quest_marker"));
        assertTrue(saved.getCompound("questlog_envelope").getCompound("magic_circle").getBoolean("activated"));
    }
}
