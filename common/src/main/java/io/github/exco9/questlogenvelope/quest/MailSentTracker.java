package io.github.exco9.questlogenvelope.quest;

import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.infernalstudios.questlog.core.QuestManager;
import org.infernalstudios.questlog.core.ServerPlayerManager;
import org.infernalstudios.questlog.core.quests.Quest;
import org.infernalstudios.questlog.core.quests.objectives.Objective;

import java.util.List;

/** Applies one outgoing Envelope mail event to the sending player's Questlog objectives. */
public final class MailSentTracker {
    private MailSentTracker() {
    }

    public static void record(ServerPlayer player, Address recipient, ItemStack mail) {
        if (ServerPlayerManager.INSTANCE == null || mail.isEmpty()) {
            return;
        }

        QuestManager manager = ServerPlayerManager.INSTANCE.getManagerByPlayer(player);
        for (Quest quest : manager.getAllQuests()) {
            progressRecursive(quest.prerequisites, recipient, mail);
            progressRecursive(quest.objectives, recipient, mail);
        }
    }

    private static void progressRecursive(List<Objective> objectives, Address recipient, ItemStack mail) {
        for (Objective objective : objectives) {
            if (objective instanceof MailSentObjective mailSent
                    && !mailSent.isCompleted()
                    && mailSent.accepts(recipient, mail)) {
                mailSent.sent();
            }

            if (!objective.getChildren().isEmpty()) {
                progressRecursive(objective.getChildren(), recipient, mail);
            }
        }
    }
}
