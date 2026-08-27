package io.github.exco9.questlogenvelope.network.fabric;

import io.github.exco9.questlogenvelope.network.ActivateMagicCircleC2SP;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class FabricMagicCircleNetworking {
    private static boolean registered;

    private FabricMagicCircleNetworking() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        PayloadTypeRegistry.playC2S().register(
                ActivateMagicCircleC2SP.TYPE,
                ActivateMagicCircleC2SP.STREAM_CODEC
        );
        ServerPlayNetworking.registerGlobalReceiver(
                ActivateMagicCircleC2SP.TYPE,
                (payload, context) -> payload.handle(context.player())
        );
    }
}
