package io.github.exco9.questlogenvelope;

import io.github.exco9.questlogenvelope.quest.LetterReward;
import io.github.exco9.questlogenvelope.quest.MailReceivedObjective;
import io.github.exco9.questlogenvelope.quest.MailSentObjective;
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

        // Works both as a normal objective and as a prerequisite. The recipient
        // is edited in Questlog's native target field; extra mail/content filters
        // are configured in the addon's options screen.
        QuestObjectiveRegistry.register(
                id("mail_sent"),
                MailSentObjective::new,
                new EditorMetadata(
                        "recipient",
                        "Recipient address (blank = any):",
                        "required_amount",
                        EditorMetadata.SuggestionType.NONE
                )
        );

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
