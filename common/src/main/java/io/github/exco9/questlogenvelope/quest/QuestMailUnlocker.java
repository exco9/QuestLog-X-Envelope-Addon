package io.github.exco9.questlogenvelope.quest;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.infernalstudios.questlog.core.QuestManager;
import org.infernalstudios.questlog.core.ServerPlayerManager;
import org.infernalstudios.questlog.core.quests.Quest;
import org.infernalstudios.questlog.core.quests.objectives.Objective;

import java.util.List;

/**
 * Bridges a delivered quest-mail marker into Questlog's native prerequisite progression.
 */
public final class QuestMailUnlocker {
    private QuestMailUnlocker() {
    }

    public static boolean unlock(ServerPlayer player, ResourceLocation mailQuestId) {
        if (ServerPlayerManager.INSTANCE == null) {
            return false;
        }

        QuestManager manager = ServerPlayerManager.INSTANCE.getManagerByPlayer(player);
        boolean changed = false;

        for (Quest quest : manager.getAllQuests()) {
            changed |= unlockRecursive(quest.prerequisites, mailQuestId);
        }

        // Objective#setUnits() already marks its parent quest for update, which triggers
        // Questlog's normal sync/trigger/completion handling. No parallel quest state is kept here.
        return changed;
    }

    private static boolean unlockRecursive(List<Objective> objectives, ResourceLocation mailQuestId) {
        boolean changed = false;

        for (Objective objective : objectives) {
            if (objective instanceof MailReceivedObjective mailReceived
                    && mailReceived.accepts(mailQuestId)
                    && !mailReceived.isCompleted()) {
                mailReceived.receive();
                changed = true;
            }

            if (!objective.getChildren().isEmpty()) {
                changed |= unlockRecursive(objective.getChildren(), mailQuestId);
            }
        }

        return changed;
    }
}
