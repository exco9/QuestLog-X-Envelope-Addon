package io.github.exco9.questlogenvelope;

import io.github.exco9.questlogenvelope.quest.LetterReward;
import io.github.exco9.questlogenvelope.quest.MailReceivedObjective;
import io.github.exco9.questlogenvelope.quest.PackageReward;
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
                        "Letter/package quest marker (blank = this quest):",
                        "required_amount",
                        EditorMetadata.SuggestionType.QUEST
                )
        );

        // Questlog owns the common reward fields and Quest-ID autocomplete.
        // Envelope-specific fields are edited by the addon client screens.
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

        QuestRewardRegistry.register(
                id("package"),
                PackageReward::new,
                new EditorMetadata(
                        "grants_quest",
                        "Quest granted by package (optional):",
                        null,
                        EditorMetadata.SuggestionType.QUEST
                )
        );
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
