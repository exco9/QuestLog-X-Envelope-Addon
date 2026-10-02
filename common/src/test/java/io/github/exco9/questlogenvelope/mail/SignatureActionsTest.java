package io.github.exco9.questlogenvelope.mail;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class SignatureActionsTest {
    private final LetterSignature signature = new LetterSignature("Alex", 0, 12, 0, 0, true);

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private ItemStack letter(UUID id) {
        var stack = new ItemStack(Items.PAPER);
        signature.attach(stack);
        if (id != null) SignatureActions.attachId(stack, id);
        return stack;
    }

    @Test void commandExecutesOnceAfterSigningEvenForAnUnsignedCopy() {
        var owner = UUID.randomUUID();
        var id = UUID.randomUUID();
        var ledger = new MagicCircleSavedData();
        ledger.register(id, owner, null, "/say Signed by @s");
        var original = letter(id);
        var copy = original.copy();
        var calls = new AtomicInteger();
        assertTrue(SignatureActions.sign(original, signature, owner, ledger, action -> {
            assertTrue(LetterSignature.isSigned(original));
            assertEquals(owner, action.owner());
            assertEquals("/say Signed by @s", action.actions().getFirst().value());
            calls.incrementAndGet();
        }));
        assertFalse(SignatureActions.sign(original, signature, owner, ledger, ignored -> calls.incrementAndGet()));
        assertTrue(SignatureActions.sign(copy, signature, owner, ledger, ignored -> calls.incrementAndGet()));
        assertTrue(LetterSignature.isSigned(copy));
        assertEquals(1, calls.get());
        assertTrue(ledger.isConsumed(id));
        assertFalse(original.get(DataComponents.CUSTOM_DATA).copyTag().toString().contains("/say"));
    }

    @Test void wrongOwnerAndChangedSignatureCannotConsumeCommand() {
        var owner = UUID.randomUUID();
        var id = UUID.randomUUID();
        var ledger = new MagicCircleSavedData();
        ledger.register(id, owner, null, "say hello");
        var stack = letter(id);
        assertFalse(SignatureActions.sign(stack, signature, UUID.randomUUID(), ledger, ignored -> fail()));
        assertFalse(SignatureActions.sign(stack, LetterSignature.EMPTY, owner, ledger, ignored -> fail()));
        assertFalse(LetterSignature.isSigned(stack));
        assertFalse(ledger.isConsumed(id));
        assertTrue(ledger.getAction(id).isPresent());
    }

    @Test void unknownActionIsRejectedAndLegacyDecorativeLettersStillSign() {
        var owner = UUID.randomUUID();
        var ledger = new MagicCircleSavedData();
        var forged = letter(UUID.randomUUID());
        assertFalse(SignatureActions.sign(forged, signature, owner, ledger, ignored -> fail()));
        assertFalse(LetterSignature.isSigned(forged));
        var decorative = letter(null);
        assertTrue(SignatureActions.sign(decorative, signature, owner, ledger, ignored -> fail()));
    }

    @Test void signatureAndCircleLedgersCannotConsumeEachOthersActions() {
        var id = UUID.randomUUID();
        var owner = UUID.randomUUID();
        var circles = new MagicCircleSavedData();
        var signatures = new MagicCircleSavedData();
        circles.register(id, owner, null, "say circle");
        assertFalse(SignatureActions.sign(letter(id), signature, owner, signatures, ignored -> fail()));
        assertTrue(circles.getAction(id).isPresent());
        signatures.register(id, owner, null, "say signature");
        assertTrue(SignatureActions.sign(letter(id), signature, owner, signatures, ignored -> {}));
        assertFalse(circles.isConsumed(id));
    }

    @Test void commandCannotSignAnIdenticalLetterWithADifferentActionId() {
        UUID id = UUID.randomUUID();
        var packet = new io.github.exco9.questlogenvelope.network.SignLetterC2SP(
                net.minecraft.world.InteractionHand.MAIN_HAND, signature, id.toString());
        assertTrue(packet.matchesAction(letter(id)));
        assertFalse(packet.matchesAction(letter(UUID.randomUUID())));
        assertFalse(packet.matchesAction(letter(null)));
    }

    @Test void consumedCommandsRemainConsumedAfterSavingAndReloading() throws Exception {
        UUID id = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        var ledger = new MagicCircleSavedData();
        ledger.register(id, owner, null, "say once");
        assertTrue(ledger.claim(id));
        var saved = ledger.save(new net.minecraft.nbt.CompoundTag(), null);
        var constructor = MagicCircleSavedData.class.getDeclaredConstructor(net.minecraft.nbt.CompoundTag.class);
        constructor.setAccessible(true);
        var restored = constructor.newInstance(saved);
        assertTrue(restored.isConsumed(id));
        assertTrue(SignatureActions.sign(letter(id), signature, owner, restored, ignored -> fail()));
        assertFalse(restored.claim(id));
    }
}
