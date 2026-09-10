package io.github.exco9.questlogenvelope.network.fabric;

import io.github.exco9.questlogenvelope.network.ActivateMagicCircleC2SP;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.UUID;

public final class MagicCircleNetworkingImpl {
    private MagicCircleNetworkingImpl() {
    }

    public static void sendActivation(UUID actionId) {
        ClientPlayNetworking.send(new ActivateMagicCircleC2SP(actionId));
    }
}
