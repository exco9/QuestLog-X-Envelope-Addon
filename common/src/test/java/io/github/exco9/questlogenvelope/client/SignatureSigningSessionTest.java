package io.github.exco9.questlogenvelope.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SignatureSigningSessionTest {
    @Test
    void magicBurstOnlyRunsOnceForAFreshlyConfirmedSignature() {
        var session = new SignatureSigningSession();
        session.observeSigned(10, true);
        assertEquals(-1, session.particleProgress(10));
        assertEquals(1, session.magicProgress(10));
        session.start(100, false);
        session.requested(1200);
        session.observeSigned(1250, true);
        assertEquals(0, session.magicProgress(1250));
        assertEquals(0, session.particleProgress(1250));
        session.observeSigned(1800, true);
        assertTrue(session.magicProgress(1800) > 0.8F);
        assertEquals(1, session.magicProgress(1900));
        assertTrue(session.particleProgress(2200) > 1);
        session.reset();
        session.observeSigned(2500, true);
        assertEquals(-1, session.particleProgress(2500));
    }
    @Test
    void signedLettersCannotStartAgain() {
        var session = new SignatureSigningSession();
        assertFalse(session.start(100, true));
        assertFalse(session.shouldCommit(2000));
    }

    @Test
    void requestsExactlyOnceAfterTheAnimationAndLocksPendingClicks() {
        var session = new SignatureSigningSession();
        assertTrue(session.start(100, false));
        assertFalse(session.shouldCommit(1199));
        assertTrue(session.shouldCommit(1200));
        session.requested(1200);
        assertFalse(session.shouldCommit(2000));
        assertFalse(session.start(2000, false));
        assertTrue(session.isPending(2000));
        assertFalse(session.start(3300, true));
    }

    @Test
    void cancelledAnimationsNeverCommitAndFailedRequestsCanRetry() {
        var session = new SignatureSigningSession();
        session.start(0, false);
        session.reset();
        assertFalse(session.shouldCommit(5000));
        assertTrue(session.start(5000, false));
        session.requested(6100);
        assertFalse(session.isPending(8100));
        assertTrue(session.start(8100, false));
    }
}
