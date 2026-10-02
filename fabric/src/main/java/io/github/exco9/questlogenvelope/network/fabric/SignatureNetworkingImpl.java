package io.github.exco9.questlogenvelope.network.fabric;

import io.github.exco9.questlogenvelope.mail.LetterSignature;
import io.github.exco9.questlogenvelope.network.SignLetterC2SP;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.InteractionHand;

public final class SignatureNetworkingImpl {
    private SignatureNetworkingImpl() { }

    public static void sign(InteractionHand hand, LetterSignature expected, String actionId) {
        ClientPlayNetworking.send(new SignLetterC2SP(hand, expected, actionId));
    }
}
