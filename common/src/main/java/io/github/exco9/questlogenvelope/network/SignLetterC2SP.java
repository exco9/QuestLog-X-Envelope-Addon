package io.github.exco9.questlogenvelope.network;

import io.github.exco9.questlogenvelope.QuestlogEnvelope;
import io.github.exco9.questlogenvelope.mail.LetterSignature;
import io.github.exco9.questlogenvelope.mail.SignatureActions;
import io.github.mortuusars.envelope.world.item.LetterItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.NotNull;

/** Commits signing to the actual held item, including legacy letters without signing metadata. */
public record SignLetterC2SP(InteractionHand hand, LetterSignature expected, String expectedActionId) implements CustomPacketPayload {
    public static final Type<SignLetterC2SP> TYPE = new Type<>(QuestlogEnvelope.id("sign_letter"));
    public static final StreamCodec<FriendlyByteBuf, SignLetterC2SP> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeEnum(payload.hand());
                var signature = payload.expected();
                buffer.writeUtf(signature.text(), LetterSignature.MAX_TEXT_LENGTH * 2);
                buffer.writeInt(signature.color());
                buffer.writeInt(signature.size());
                buffer.writeInt(signature.x());
                buffer.writeInt(signature.y());
                buffer.writeBoolean(signature.framed());
                buffer.writeInt(signature.magicColor());
                buffer.writeUtf(payload.expectedActionId(), 36);
            },
            buffer -> new SignLetterC2SP(buffer.readEnum(InteractionHand.class),
                    new LetterSignature(buffer.readUtf(LetterSignature.MAX_TEXT_LENGTH * 2), buffer.readInt(),
                            buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readBoolean(), buffer.readInt()), buffer.readUtf(36))
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayer player) {
        var stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof LetterItem)) return;
        if (!matchesAction(stack)) return;
        if (SignatureActions.sign(stack, player, expected)) {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
    }

    public boolean matchesAction(net.minecraft.world.item.ItemStack stack) {
        return expectedActionId.equals(SignatureActions.getActionId(stack).map(java.util.UUID::toString).orElse(""));
    }
}
