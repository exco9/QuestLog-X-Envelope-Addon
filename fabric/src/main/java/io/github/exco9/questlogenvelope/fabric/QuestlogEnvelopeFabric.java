package io.github.exco9.questlogenvelope.fabric;

import io.github.exco9.questlogenvelope.QuestlogEnvelope;
import io.github.exco9.questlogenvelope.network.fabric.FabricMagicCircleNetworking;
import net.fabricmc.api.ModInitializer;

public final class QuestlogEnvelopeFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        QuestlogEnvelope.init();
        FabricMagicCircleNetworking.register();
    }
}
