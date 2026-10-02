package io.github.exco9.questlogenvelope.network.neoforge;

import io.github.exco9.questlogenvelope.QuestlogEnvelope;
import io.github.exco9.questlogenvelope.network.ActivateMagicCircleC2SP;
import io.github.exco9.questlogenvelope.network.SignLetterC2SP;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = QuestlogEnvelope.MOD_ID)
public final class NeoForgeMagicCircleNetworking {
    private NeoForgeMagicCircleNetworking() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                ActivateMagicCircleC2SP.TYPE,
                ActivateMagicCircleC2SP.STREAM_CODEC,
                (payload, context) -> payload.handle((ServerPlayer) context.player())
        );
        registrar.playToServer(SignLetterC2SP.TYPE, SignLetterC2SP.STREAM_CODEC,
                (payload, context) -> payload.handle((ServerPlayer) context.player()));
    }
}
