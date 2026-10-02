package io.github.exco9.questlogenvelope.network;

import dev.architectury.injectables.annotations.ExpectPlatform;
import io.github.exco9.questlogenvelope.mail.LetterSignature;
import net.minecraft.world.InteractionHand;

public final class SignatureNetworking {
    private SignatureNetworking() { }

    @ExpectPlatform
    public static void sign(InteractionHand hand, LetterSignature expected, String actionId) {
        throw new AssertionError();
    }
}
