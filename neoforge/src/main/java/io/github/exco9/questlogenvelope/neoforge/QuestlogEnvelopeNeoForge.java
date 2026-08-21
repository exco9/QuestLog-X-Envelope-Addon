package io.github.exco9.questlogenvelope.neoforge;

import io.github.exco9.questlogenvelope.QuestlogEnvelope;
import net.neoforged.fml.common.Mod;

@Mod(QuestlogEnvelope.MOD_ID)
public final class QuestlogEnvelopeNeoForge {
    public QuestlogEnvelopeNeoForge() {
        QuestlogEnvelope.init();
    }
}
