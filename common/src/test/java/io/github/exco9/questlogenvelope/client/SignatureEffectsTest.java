package io.github.exco9.questlogenvelope.client;

import io.github.exco9.questlogenvelope.mail.LetterSignature;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SignatureEffectsTest {
    @Test
    void shineRetainsTheInkHueInsteadOfTurningNearlyWhite() {
        int shine = SignatureEffects.shineColor(0xAA55FF);
        assertEquals(255, shine & 255);
        assertTrue(((shine >> 8) & 255) < 130);
        assertTrue(((shine >> 16) & 255) < 200);
    }
    @Test
    void writingMotionIsContinuousMonotonicAndSlowsAtBothEnds() {
        assertEquals(0, SignatureEffects.writingPosition(-1));
        assertEquals(1, SignatureEffects.writingPosition(2));
        float previous = 0;
        for (int i = 0; i <= 1000; i++) {
            float current = SignatureEffects.writingPosition(i / 1000F);
            assertTrue(current >= previous);
            assertTrue(current - previous < 0.002F);
            previous = current;
        }
        assertTrue(SignatureEffects.writingPosition(0.01F) < 0.001F);
        assertTrue(1 - SignatureEffects.writingPosition(0.99F) < 0.001F);
    }

    @Test
    void magicColorSurvivesJsonNbtAndPlayerResolution() {
        var signature = new LetterSignature("@s", 0x7B593D, 12, 40, 110, true, 0x22AACC);
        var json = new com.google.gson.JsonObject();
        signature.writeJson(json);
        assertEquals(signature, LetterSignature.fromJson(json));
        assertEquals(signature, LetterSignature.fromTag(signature.toTag()));
        assertEquals(0x22AACC, signature.resolvePlayer("Alex").magicColor());
        json.remove("signature_magic_color");
        assertEquals(LetterSignature.DEFAULT_MAGIC_COLOR, LetterSignature.fromJson(json).magicColor());
        json.addProperty("signature_magic_color", "invalid");
        assertEquals(LetterSignature.DEFAULT_MAGIC_COLOR, LetterSignature.fromJson(json).magicColor());
        assertEquals(signature.color(), SignatureEffects.blend(signature.color(), signature.magicColor(), 0));
        assertEquals(signature.magicColor(), SignatureEffects.blend(signature.color(), signature.magicColor(), 1));
    }
}
