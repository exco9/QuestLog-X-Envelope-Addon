package io.github.exco9.questlogenvelope.network.neoforge;

import io.github.exco9.questlogenvelope.network.ActivateMagicCircleC2SP;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public final class MagicCircleNetworkingImpl {
    private MagicCircleNetworkingImpl() {
    }

    public static void sendActivation(UUID actionId) {
        PacketDistributor.sendToServer(new ActivateMagicCircleC2SP(actionId));
    }
}
