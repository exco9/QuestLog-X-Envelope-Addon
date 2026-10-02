package io.github.exco9.questlogenvelope.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SignatureWritingAnimationTest {
    @Test
    void idleAndCompletedStatesShowTheWholeSignature() {
        var animation = new SignatureWritingAnimation();
        assertEquals(1.0F, animation.progress(0));
        assertFalse(animation.isRunning(0));
        assertTrue(animation.start(100));
        assertEquals(0.0F, animation.progress(100));
        assertEquals(0.5F, animation.progress(100 + SignatureWritingAnimation.DURATION_MS / 2));
        assertEquals(1.0F, animation.progress(100 + SignatureWritingAnimation.DURATION_MS));
        assertFalse(animation.isRunning(100 + SignatureWritingAnimation.DURATION_MS));
    }

    @Test
    void repeatedClicksDoNotRestartInkOrReplaySoundWhileWriting() {
        var animation = new SignatureWritingAnimation();
        assertTrue(animation.start(100));
        assertFalse(animation.start(650));
        assertEquals(0.5F, animation.progress(650));
        assertTrue(animation.start(1200));
        assertEquals(0.0F, animation.progress(1200));
    }

    @Test
    void closingOrResettingRestoresTheSignatureAndAllowsReplay() {
        var animation = new SignatureWritingAnimation();
        animation.start(10);
        animation.reset();
        assertEquals(1.0F, animation.progress(20));
        assertFalse(animation.isRunning(20));
        assertTrue(animation.start(20));
    }

    @Test
    void clockJumpsAndSlowFramesKeepProgressBounded() {
        var animation = new SignatureWritingAnimation();
        animation.start(500);
        assertEquals(0.0F, animation.progress(400));
        assertEquals(1.0F, animation.progress(50000));
    }
}
