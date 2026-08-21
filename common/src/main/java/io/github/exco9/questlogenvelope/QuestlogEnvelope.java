package io.github.exco9.questlogenvelope;

import io.github.exco9.questlogenvelope.quest.MailReceivedObjective;
import net.minecraft.resources.ResourceLocation;
import org.infernalstudios.questlog.core.quests.EditorMetadata;
import org.infernalstudios.questlog.core.quests.QuestObjectiveRegistry;

public final class QuestlogEnvelope {
    public static final String MOD_ID = "questlog_envelope";

    private static boolean initialized;

    private QuestlogEnvelope() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        QuestObjectiveRegistry.register(
                id("mail_received"),
                MailReceivedObjective::new,
                new EditorMetadata(
                        "quest",
                        "Quest mail ID:",
                        "required_amount",
                        EditorMetadata.SuggestionType.QUEST
                )
        );
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
