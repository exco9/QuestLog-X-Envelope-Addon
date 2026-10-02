package io.github.exco9.questlogenvelope.mail;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.Optional;
import java.util.function.Consumer;

/** Commands remain on the server; delivered letters only carry an opaque one-shot id. */
public final class SignatureActions {
    private SignatureActions() { }

    public static Optional<UUID> getActionId(ItemStack stack) {
        var custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) return Optional.empty();
        var tag = custom.copyTag().getCompound("questlog_envelope").getCompound("signature");
        return tag.hasUUID("action_id") ? Optional.of(tag.getUUID("action_id")) : Optional.empty();
    }

    public static void attach(ItemStack stack, ServerPlayer recipient, @Nullable String command) {
        if (!LetterSignature.read(stack).enabled() || command == null || command.isBlank()) return;
        UUID id = UUID.randomUUID();
        MagicCircleSavedData.getSignatures(recipient.serverLevel())
                .register(id, recipient.getUUID(), null, command.trim());
        attachId(stack, id);
    }

    static void attachId(ItemStack stack, UUID id) {
        CompoundTag custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag addon = custom.getCompound("questlog_envelope");
        CompoundTag signature = addon.getCompound("signature");
        signature.putUUID("action_id", id);
        addon.put("signature", signature);
        custom.put("questlog_envelope", addon);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
    }

    public static boolean sign(ItemStack stack, ServerPlayer player, LetterSignature expected) {
        return sign(stack, expected, player.getUUID(), MagicCircleSavedData.getSignatures(player.serverLevel()),
                definition -> QuestMagicCircle.executeActions(player, definition));
    }

    static boolean sign(ItemStack stack, LetterSignature expected, UUID player,
                        MagicCircleSavedData ledger, Consumer<MagicCircleSavedData.ActionDefinition> execute) {
        if (!expected.enabled() || !LetterSignature.read(stack).equals(expected) || LetterSignature.isSigned(stack)) return false;
        var tag = stack.get(DataComponents.CUSTOM_DATA).copyTag()
                .getCompound("questlog_envelope").getCompound("signature");
        if (!tag.hasUUID("action_id")) return LetterSignature.sign(stack, expected);
        UUID id = tag.getUUID("action_id");
        var definition = ledger.getAction(id);
        if (definition.isEmpty()) {
            // An unsigned duplicate of an already consumed letter becomes visually signed, without replaying commands.
            return ledger.isConsumed(id) && LetterSignature.sign(stack, expected);
        }
        if (!player.equals(definition.get().owner()) || !ledger.claim(id)) return false;
        if (!LetterSignature.sign(stack, expected)) return false;
        execute.accept(definition.get());
        return true;
    }
}
