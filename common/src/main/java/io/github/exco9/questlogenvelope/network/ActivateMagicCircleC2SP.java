package io.github.exco9.questlogenvelope.network;

import io.github.exco9.questlogenvelope.QuestlogEnvelope;
import io.github.exco9.questlogenvelope.mail.QuestMagicCircle;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** Serverbound request emitted after a client completes the magic-circle hold. */
public record ActivateMagicCircleC2SP(String actionId) implements CustomPacketPayload {
    public static final Type<ActivateMagicCircleC2SP> TYPE = new Type<>(
            QuestlogEnvelope.id("activate_magic_circle")
    );

    public static final StreamCodec<FriendlyByteBuf, ActivateMagicCircleC2SP> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ActivateMagicCircleC2SP::actionId,
            ActivateMagicCircleC2SP::new
    );

    public ActivateMagicCircleC2SP(UUID actionId) {
        this(actionId.toString());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayer player) {
        final UUID parsedActionId;
        try {
            parsedActionId = UUID.fromString(this.actionId);
        } catch (IllegalArgumentException ignored) {
            return;
        }

        // The server never trusts item/action data supplied by the client. The
        // packet only identifies the one-shot action; the actual command/quest
        // stays in server SavedData and the held letter must match this UUID.
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            QuestMagicCircle.ActivationResult result = QuestMagicCircle.activate(stack, player, parsedActionId);
            if (result != QuestMagicCircle.ActivationResult.NOT_THIS_ITEM) {
                return;
            }
        }
    }
}
