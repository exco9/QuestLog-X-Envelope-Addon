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

/** Applies each persisted Envelope delivery to all matching Questlog prerequisites and objectives. */
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
        if (ServerPlayerManager.INSTANCE == null) {
            return;
        }
        QuestManager manager = ServerPlayerManager.INSTANCE.getManagerByPlayer(player);
        applyPending(data, player.getScoreboardName(), mailQuestId, manager.getAllQuests());
    }

    static void applyPending(PendingQuestMailSavedData data, String playerName,
                             ResourceLocation mailQuestId, List<Quest> quests) {
        while (data.getCount(playerName, mailQuestId) > 0) {
            if (!unlockQuests(quests, mailQuestId)) {
                return;
            }
            data.consumeOne(playerName, mailQuestId);
        }
    }

    private static ServerLevel mailDataLevel(ServerPlayer player) {
        // Envelope's MailService is an Overworld service. Keeping the pending
        // marker SavedData there makes delivery/unlock state independent of the
        // dimension the player happens to be in when the quest loads.
        return player.getServer().overworld();
    }

    static boolean unlockQuests(List<Quest> quests, ResourceLocation mailQuestId) {
        boolean progressed = false;
        for (Quest quest : quests) {
            progressed |= unlockRecursive(quest.prerequisites, mailQuestId);
            progressed |= unlockRecursive(quest.objectives, mailQuestId);
        }
        return progressed;
    }

    private static boolean unlockRecursive(List<Objective> objectives, ResourceLocation mailQuestId) {
        boolean progressed = false;
        for (Objective objective : objectives) {
            if (objective instanceof MailReceivedObjective mailReceived
                    && mailReceived.accepts(mailQuestId)
                    && !mailReceived.isCompleted()) {
                int before = mailReceived.getUnits();
                mailReceived.receive();
                // Questlog can reject progression while prerequisites are unmet.
                // A pending delivery must not be lost when no units changed.
                progressed |= mailReceived.getUnits() > before;
            }

            progressed |= unlockRecursive(objective.getChildren(), mailQuestId);
        }
        return progressed;
    }
}
