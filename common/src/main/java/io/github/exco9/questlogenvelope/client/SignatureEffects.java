package io.github.exco9.questlogenvelope.client;

/** Continuous motion and color math, independent of rendering and inventory state. */
public final class SignatureEffects {
    private SignatureEffects() { }

    public static int shineColor(int ink) {
        return blend(ink, 0xFFFFFF, 0.30F);
    }

    public static float writingPosition(float progress) {
        float t = Math.max(0, Math.min(1, progress));
        // Gentle acceleration/deceleration without the stop-start rhythm of linear pixel steps.
        return t * t * (3 - 2 * t);
    }

    public static int blend(int from, int to, float progress) {
        float t = writingPosition(progress);
        int result = 0;
        for (int shift : new int[]{16, 8, 0}) {
            int a = (from >> shift) & 255;
            int b = (to >> shift) & 255;
            result |= Math.round(a + (b - a) * t) << shift;
        }
        return result;
    }
}
