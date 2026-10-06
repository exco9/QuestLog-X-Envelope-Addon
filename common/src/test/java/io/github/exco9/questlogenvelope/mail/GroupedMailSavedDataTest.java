package io.github.exco9.questlogenvelope.mail;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GroupedMailSavedDataTest {
    private static final UUID PLAYER = UUID.randomUUID();

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private ItemStack mail(String title) {
        var stack = new ItemStack(Items.PAPER);
        stack.set(DataComponents.ITEM_NAME, Component.literal(title));
        new LetterSignature("Alex", 0x123456, 12, 1, 2, true).attach(stack);
        QuestMailMarker.set(stack, net.minecraft.resources.ResourceLocation.parse("test:" + title));
        return stack;
    }

    private GroupedMailSavedData reload(GroupedMailSavedData data) {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        return GroupedMailSavedData.load(data.save(new CompoundTag(), registries), registries);
    }

    @Test void fiveRewardsForOnePlayerBecomeOneReadyBatch() {
        var data = new GroupedMailSavedData();
        UUID batch = null;
        for (int i = 0; i < 5; i++) {
            var id = data.enqueue(PLAYER, "Alex", mail("letter" + i), i == 4, 100);
            if (batch == null) batch = id;
            assertEquals(batch, id);
        }
        assertTrue(data.ready(104).isEmpty());
        assertEquals(1, data.ready(105).size());
        assertEquals(5, data.ready(105).getFirst().remaining());
        assertTrue(data.ready(105).getFirst().express());
    }

    @Test void differentRecipientsAndLaterInFlightRewardsHaveSeparateCouriers() {
        var data = new GroupedMailSavedData();
        var first = data.enqueue(PLAYER, "Alex", mail("one"), false, 0);
        var second = data.enqueue(UUID.randomUUID(), "Sam", mail("two"), false, 0);
        assertNotEquals(first, second);
        data.beginDelivery(first);
        var later = data.enqueue(PLAYER, "Alex", mail("three"), false, 6);
        assertNotEquals(first, later);
        assertEquals(2, data.ready(11).size());
    }

    @Test void queueCopiesInputsAndPreservesIndividualMetadataOnReload() {
        var data = new GroupedMailSavedData();
        var source = mail("one");
        var id = data.enqueue(PLAYER, "Alex", source, true, 0);
        source.set(DataComponents.ITEM_NAME, Component.literal("changed"));
        var restored = reload(data);
        var batch = restored.ready(5).getFirst();
        assertEquals(id, batch.id());
        assertEquals(PLAYER, batch.recipient());
        assertEquals("one", batch.first().getHoverName().getString());
        assertEquals("test:one", QuestMailMarker.get(batch.first()).orElseThrow().toString());
        assertEquals("Alex", LetterSignature.read(batch.first()).text());
    }

    @Test void deliveredItemsAreNotReplayedAfterPartialFailureAndRestart() {
        var data = new GroupedMailSavedData();
        var id = data.enqueue(PLAYER, "Alex", mail("one"), false, 0);
        data.enqueue(PLAYER, "Alex", mail("two"), false, 0);
        var attempt = data.beginDelivery(id);
        var calls = new AtomicInteger();
        assertFalse(data.deliver(id, attempt, item -> calls.incrementAndGet() == 1));
        assertEquals(1, data.find(id, attempt).orElseThrow().remaining());
        var restored = reload(data);
        var received = new ArrayList<String>();
        assertTrue(restored.deliver(id, attempt, item -> {
            received.add(item.getHoverName().getString());
            return true;
        }));
        assertEquals(java.util.List.of("two"), received);
        assertTrue(restored.deliver(id, attempt, item -> { fail("Duplicate delivery"); return true; }));
    }

    @Test void callbackExceptionKeepsUnconsumedCargoForRetry() {
        var data = new GroupedMailSavedData();
        var id = data.enqueue(PLAYER, "Alex", mail("one"), false, 0);
        var attempt = data.beginDelivery(id);
        assertThrows(IllegalStateException.class, () -> data.deliver(id, attempt, item -> {
            throw new IllegalStateException("inbox failure");
        }));
        assertEquals(1, reload(data).find(id, attempt).orElseThrow().remaining());
    }

    @Test void interruptedCourierRetriesOnlyRemainingCargoAndInvalidatesOldAttempt() {
        var data = new GroupedMailSavedData();
        var id = data.enqueue(PLAYER, "Alex", mail("one"), false, 0);
        data.enqueue(PLAYER, "Alex", mail("two"), false, 0);
        var oldAttempt = data.beginDelivery(id);
        var calls = new AtomicInteger();
        data.deliver(id, oldAttempt, item -> calls.incrementAndGet() == 1);
        assertTrue(data.retry(id, oldAttempt, 20));
        assertFalse(data.retry(id, oldAttempt, 20));
        var restored = reload(data);
        assertTrue(restored.ready(24).isEmpty());
        assertEquals(1, restored.ready(25).getFirst().remaining());
        var attempt = restored.beginDelivery(id);
        assertNotEquals(oldAttempt, attempt);
        assertTrue(restored.deliver(id, oldAttempt, item -> { fail("Stale courier"); return true; }));
        assertEquals(1, restored.find(id, attempt).orElseThrow().remaining());
        assertTrue(restored.deliver(id, attempt, item -> true));
    }

    @Test void inFlightBatchIsNotRedispatchedAfterRestart() {
        var data = new GroupedMailSavedData();
        var id = data.enqueue(PLAYER, "Alex", mail("one"), false, 0);
        var attempt = data.beginDelivery(id);
        var restored = reload(data);
        assertTrue(restored.ready(100).isEmpty());
        assertTrue(restored.find(id, attempt).isPresent());
        assertThrows(IllegalStateException.class, () -> restored.beginDelivery(id));
    }
}
