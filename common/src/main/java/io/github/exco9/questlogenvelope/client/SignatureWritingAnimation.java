package io.github.exco9.questlogenvelope.client;

/** Screen-local, replayable animation; never modifies the letter or its quest actions. */
public final class SignatureWritingAnimation {
    public static final long DURATION_MS = 1100L;
    private long startedAt = -1L;

    public boolean start(long now) {
        if (isRunning(now)) return false;
        startedAt = now;
        return true;
    }

    public float progress(long now) {
        if (startedAt < 0L) return 1.0F;
        return Math.max(0.0F, Math.min(1.0F, (now - startedAt) / (float) DURATION_MS));
    }

    public boolean isRunning(long now) {
        return startedAt >= 0L && progress(now) < 1.0F;
    }

    public void reset() {
        startedAt = -1L;
    }
}
