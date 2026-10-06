package io.github.exco9.questlogenvelope.quest;

import com.google.gson.JsonObject;
import io.github.exco9.questlogenvelope.mail.PendingQuestMailSavedData;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.resources.ResourceLocation;
import org.infernalstudios.questlog.core.quests.Quest;
import org.infernalstudios.questlog.core.QuestManager;
import org.infernalstudios.questlog.core.quests.display.QuestDisplayData;
import org.infernalstudios.questlog.core.quests.objectives.Objective;
import org.infernalstudios.questlog.core.quests.objectives.logic.AndObjective;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestMailUnlockerTest {
    private static final ResourceLocation ID = ResourceLocation.parse("test:mail");

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private MailReceivedObjective mail(int required) {
        JsonObject definition = new JsonObject();
        definition.addProperty("type", "questlog_envelope:mail_received");
        definition.addProperty("required_amount", required);
        return new MailReceivedObjective(definition);
    }

    private Quest quest(List<Objective> prerequisites, List<Objective> objectives) {
        JsonObject display = new JsonObject();
        display.addProperty("title", "Mail regression");
        display.addProperty("description", "Test quest");
        display.addProperty("icon", "minecraft:paper");
        // Preserve Questlog's real parent assignment and prerequisite gating;
        // only server synchronization is disabled in this unit-test fixture.
        return new Quest(new QuestDisplayData(display), prerequisites, objectives,
                List.of(), List.of(), ID, new QuestManager(null) {
                    @Override public boolean isClient() { return false; }
                }, false, false) {
            @Override public void markForUpdate() { }
        };
    }

    @Test void progressesPrerequisite() {
        var prerequisite = mail(1);
        var quest = quest(List.of(prerequisite), List.of());
        assertTrue(QuestMailUnlocker.unlockQuests(List.of(quest), ID));
        assertTrue(prerequisite.isCompleted());
    }

    @Test void progressesRegularObjective() {
        var objective = mail(1);
        var quest = quest(List.of(), List.of(objective));
        assertTrue(QuestMailUnlocker.unlockQuests(List.of(quest), ID));
        assertTrue(objective.isCompleted());
    }

    @Test void oneDeliveryReachesBothListsAndEveryMatchingQuest() {
        var prerequisite = mail(1);
        var objective = mail(1);
        var anotherObjective = mail(1);
        var first = quest(List.of(prerequisite), List.of(objective));
        var second = quest(List.of(), List.of(anotherObjective));
        assertTrue(QuestMailUnlocker.unlockQuests(List.of(first, second), ID));
        assertEquals(1, prerequisite.getUnits());
        assertEquals(1, objective.getUnits());
        assertEquals(1, anotherObjective.getUnits());
        assertFalse(QuestMailUnlocker.unlockQuests(List.of(first, second), ID));
    }

    @Test void severalDeliveriesCountOncePerMatchingObjectiveWithoutOverfilling() {
        var prerequisite = mail(2);
        var objective = mail(3);
        var quests = List.of(quest(List.of(prerequisite), List.of()), quest(List.of(), List.of(objective)));
        for (int event = 1; event <= 3; event++) {
            assertTrue(QuestMailUnlocker.unlockQuests(quests, ID));
            assertEquals(Math.min(event, 2), prerequisite.getUnits());
            assertEquals(event, objective.getUnits());
        }
        assertFalse(QuestMailUnlocker.unlockQuests(quests, ID));
    }

    @Test void nonMatchingMarkerAndLockedObjectiveDoNotConsumeAnEvent() {
        var prerequisite = mail(1);
        var objective = mail(1);
        var definition = new JsonObject();
        definition.addProperty("type", "questlog_envelope:mail_received");
        definition.addProperty("quest", "test:other");
        var other = new MailReceivedObjective(definition);
        var quest = quest(List.of(other), List.of(objective));
        assertFalse(QuestMailUnlocker.unlockQuests(List.of(quest), ID));
        assertEquals(0, objective.getUnits());
        assertFalse(QuestMailUnlocker.unlockQuests(List.of(quest(List.of(prerequisite), List.of())),
                ResourceLocation.parse("test:unrelated")));
        assertEquals(0, prerequisite.getUnits());
    }

    @Test void queuedDeliveriesAreConsumedOnceEachAndUnusableSurplusIsRetained() {
        var data = new PendingQuestMailSavedData();
        var objective = mail(2);
        var quests = List.of(quest(List.of(), List.of(objective)));
        for (int i = 0; i < 3; i++) data.record("Alex", ID);
        QuestMailUnlocker.applyPending(data, "Alex", ID, quests);
        assertEquals(2, objective.getUnits());
        assertEquals(1, data.getCount("Alex", ID));
        assertFalse(QuestMailUnlocker.unlockQuests(quests, ID));
        assertEquals(1, data.getCount("Alex", ID));
    }

    @Test void queuedMailForLockedObjectiveSurvivesUntilQuestUnlocks() {
        var objective = mail(2);
        // Give the prerequisite a different marker so these deliveries cannot unlock it.
        var definition = new JsonObject();
        definition.addProperty("type", "questlog_envelope:mail_received");
        definition.addProperty("quest", "test:other");
        var prerequisite = new MailReceivedObjective(definition);
        var quest = quest(List.of(prerequisite), List.of(objective));
        var data = new PendingQuestMailSavedData();
        data.record("Alex", ID);
        data.record("Alex", ID);
        QuestMailUnlocker.applyPending(data, "Alex", ID, List.of(quest));
        assertEquals(2, data.getCount("Alex", ID));
        assertEquals(0, objective.getUnits());
        prerequisite.receive();
        QuestMailUnlocker.applyPending(data, "Alex", ID, List.of(quest));
        assertEquals(2, objective.getUnits());
        assertEquals(0, data.getCount("Alex", ID));
        QuestMailUnlocker.applyPending(data, "Alex", ID, List.of(quest));
        assertEquals(2, objective.getUnits());
    }

    @Test void traversesNestedPrerequisitesAndObjectivesWithoutStoppingAtFirstChild() {
        var first = mail(1);
        var second = mail(1);
        var regular = mail(2);
        var definition = new JsonObject();
        definition.addProperty("name", "Nested mail");
        var prerequisiteGroup = new AndObjective(definition);
        prerequisiteGroup.getChildren().addAll(List.of(first, second));
        var objectiveGroup = new AndObjective(definition);
        objectiveGroup.getChildren().add(regular);
        var quests = List.of(quest(List.of(prerequisiteGroup), List.of(objectiveGroup)));
        assertTrue(QuestMailUnlocker.unlockQuests(quests, ID));
        assertTrue(first.isCompleted());
        assertTrue(second.isCompleted());
        assertEquals(1, regular.getUnits());
        assertTrue(QuestMailUnlocker.unlockQuests(quests, ID));
        assertTrue(objectiveGroup.isCompleted());
        assertFalse(QuestMailUnlocker.unlockQuests(quests, ID));
    }

    @Test void savedPendingEventsReplayOnceAndKeepOtherPlayersAndMarkersSeparate() throws Exception {
        var data = new PendingQuestMailSavedData();
        data.record("Alex", ID);
        data.record("ALEX", ID);
        data.record("OtherPlayer", ID);
        var otherId = ResourceLocation.parse("test:other");
        data.record("Alex", otherId);
        var constructor = PendingQuestMailSavedData.class.getDeclaredConstructor(net.minecraft.nbt.CompoundTag.class);
        constructor.setAccessible(true);
        var restored = constructor.newInstance(data.save(new net.minecraft.nbt.CompoundTag(), null));
        var objective = mail(2);
        var quests = List.of(quest(List.of(), List.of(objective)));
        QuestMailUnlocker.applyPending(restored, "alex", ID, quests);
        assertEquals(2, objective.getUnits());
        assertEquals(0, restored.getCount("Alex", ID));
        assertEquals(1, restored.getCount("OtherPlayer", ID));
        assertEquals(1, restored.getCount("Alex", otherId));
        var restarted = constructor.newInstance(restored.save(new net.minecraft.nbt.CompoundTag(), null));
        QuestMailUnlocker.applyPending(restarted, "Alex", ID, quests);
        assertEquals(2, objective.getUnits());
        assertEquals(0, restarted.getCount("Alex", ID));
    }
}
