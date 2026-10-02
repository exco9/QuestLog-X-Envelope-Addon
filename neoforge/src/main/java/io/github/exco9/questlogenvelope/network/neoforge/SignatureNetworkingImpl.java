package io.github.exco9.questlogenvelope.network.neoforge;

import io.github.exco9.questlogenvelope.mail.LetterSignature;
import io.github.exco9.questlogenvelope.network.SignLetterC2SP;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SignatureNetworkingImpl {
    private SignatureNetworkingImpl() { }

    public static void sign(InteractionHand hand, LetterSignature expected, String actionId) {
        PacketDistributor.sendToServer(new SignLetterC2SP(hand, expected, actionId));
    }
}
