package io.github.exco9.questlogenvelope.network;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.util.UUID;

/** Cross-loader client bridge for the magic-circle activation payload. */
public final class MagicCircleNetworking {
    private MagicCircleNetworking() {
    }

    @ExpectPlatform
    public static void sendActivation(UUID actionId) {
        throw new AssertionError();
    }
}
