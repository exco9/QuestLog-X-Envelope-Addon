package io.github.exco9.questlogenvelope.quest;

import io.github.exco9.questlogenvelope.mail.PendingQuestMailSavedData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.infernalstudios.questlog.core.QuestManager;
import org.infernalstudios.questlog.core.ServerPlayerManager;
import org.infernalstudios.questlog.core.quests.Quest;
import org.infernalstudios.questlog.core.quests.objectives.Objective;

import java.util.List;

/** Bridges persisted Envelope deliveries into Questlog's native prerequisite progression. */
public final class QuestMailUnlocker {
    private QuestMailUnlocker() {
    }

    public static void applyPending(ServerPlayer player) {
        PendingQuestMailSavedData data = PendingQuestMailSavedData.get(mailDataLevel(player));
        for (ResourceLocation questId : data.getQuestIds(player.getScoreboardName())) {
            applyPending(player, questId);
        }
    }

    public static void applyPending(ServerPlayer player, ResourceLocation mailQuestId) {
        PendingQuestMailSavedData data = PendingQuestMailSavedData.get(mailDataLevel(player));

        while (data.getCount(player.getScoreboardName(), mailQuestId) > 0) {
            if (!unlockOne(player, mailQuestId)) {
                return;
            }
            data.consumeOne(player.getScoreboardName(), mailQuestId);
        }
    }

    private static ServerLevel mailDataLevel(ServerPlayer player) {
        // Envelope's MailService is an Overworld service. Keeping the pending
        // marker SavedData there makes delivery/unlock state independent of the
        // dimension the player happens to be in when the quest loads.
        return player.getServer().overworld();
    }

    private static boolean unlockOne(ServerPlayer player, ResourceLocation mailQuestId) {
        if (ServerPlayerManager.INSTANCE == null) {
            return false;
        }

        QuestManager manager = ServerPlayerManager.INSTANCE.getManagerByPlayer(player);
        for (Quest quest : manager.getAllQuests()) {
            if (unlockRecursive(quest.prerequisites, mailQuestId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean unlockRecursive(List<Objective> objectives, ResourceLocation mailQuestId) {
        for (Objective objective : objectives) {
            if (objective instanceof MailReceivedObjective mailReceived
                    && mailReceived.accepts(mailQuestId)
                    && !mailReceived.isCompleted()) {
                mailReceived.receive();
                return true;
            }

            if (!objective.getChildren().isEmpty() && unlockRecursive(objective.getChildren(), mailQuestId)) {
                return true;
            }
        }
        return false;
    }
}
