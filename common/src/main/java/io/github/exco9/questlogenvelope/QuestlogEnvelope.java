package io.github.exco9.questlogenvelope;

import io.github.exco9.questlogenvelope.quest.LetterReward;
import io.github.exco9.questlogenvelope.quest.MailReceivedObjective;
import net.minecraft.resources.ResourceLocation;
import org.infernalstudios.questlog.core.quests.EditorMetadata;
import org.infernalstudios.questlog.core.quests.QuestObjectiveRegistry;
import org.infernalstudios.questlog.core.quests.QuestRewardRegistry;

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

        // Questlog automatically exposes registered objective types in its editor.
        // Leaving `quest` empty makes MailReceivedObjective use its parent quest id.
        QuestObjectiveRegistry.register(
                id("mail_received"),
                MailReceivedObjective::new,
                new EditorMetadata(
                        "quest",
                        "Letter quest marker (blank = this quest):",
                        "required_amount",
                        EditorMetadata.SuggestionType.QUEST
                )
        );

        // The native Questlog field provides Quest-ID autocomplete for grants_quest.
        // Sender/title/body/auto-claim are edited by our small client-side editor panel.
        QuestRewardRegistry.register(
                id("letter"),
                LetterReward::new,
                new EditorMetadata(
                        "grants_quest",
                        "Quest granted by letter (optional):",
                        null,
                        EditorMetadata.SuggestionType.QUEST
                )
        );
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
