package io.github.exco9.questlogenvelope.client;

/** Client timing only; the server's item flag remains authoritative after reopening. */
public final class SignatureSigningSession {
    private final SignatureWritingAnimation animation = new SignatureWritingAnimation();
    private boolean started;
    private boolean requested;
    private long requestedAt;
    private long confirmedAt = -1;

    public boolean start(long now, boolean signed) {
        if (signed || isPending(now) || animation.isRunning(now)) return false;
        confirmedAt = -1;
        started = true;
        requested = false;
        return animation.start(now);
    }

    public boolean shouldCommit(long now) {
        return started && !requested && !animation.isRunning(now);
    }

    public void requested(long now) {
        requested = true;
        requestedAt = now;
    }

    public boolean isPending(long now) {
        return requested && now - requestedAt < 2000L;
    }

    /** A freshly acknowledged signature animates once; opening an already signed letter does not. */
    public void observeSigned(long now, boolean signed) {
        if (signed && started && requested && confirmedAt < 0) confirmedAt = now;
    }

    public float magicProgress(long now) {
        return confirmedAt < 0 ? 1 : Math.max(0, Math.min(1, (now - confirmedAt) / 650F));
    }

    public float particleProgress(long now) {
        return confirmedAt < 0 ? -1 : (now - confirmedAt) / 850F;
    }

    public boolean isWriting(long now) { return animation.isRunning(now); }
    public float progress(long now) { return animation.progress(now); }

    public void reset() {
        started = requested = false;
        confirmedAt = -1;
        animation.reset();
    }
}
