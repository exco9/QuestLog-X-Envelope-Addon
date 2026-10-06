package io.github.exco9.questlogenvelope.mail;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.Id;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.type.PlayerAddress;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffContext;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffResult;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/** Bridges persistent cargo to one native Envelope courier and individual native drop-offs. */
public final class GroupedMailDelivery {
    private GroupedMailDelivery() { }

    public static void enqueue(ServerPlayer player, ItemStack mail, boolean express) {
        ServerLevel level = player.getServer().overworld();
        GroupedMailSavedData.get(level).enqueue(player.getUUID(), player.getScoreboardName(),
                mail, express, level.getGameTime());
    }

    public static void flush(MinecraftServer server) {
        ServerLevel level = server.overworld();
        var data = GroupedMailSavedData.get(level);
        for (var batch : data.ready(level.getGameTime())) {
            ServerPlayer recipient = server.getPlayerList().getPlayer(batch.recipient());
            if (recipient == null) continue; // Keep offline recipients' rewards until they log in.
            var attempt = data.beginDelivery(batch.id());
            ItemStack carrier = GroupedMailReference.attach(batch.first(), batch.id(), attempt);
            try {
                QuestMailDelivery.dispatchNow(recipient, carrier, batch.express());
                Envelope.LOGGER.debug("Dispatched grouped Questlog mail: {} items for {} in one courier",
                        batch.remaining(), batch.recipientName());
            } catch (RuntimeException exception) {
                Envelope.LOGGER.error("Failed to dispatch grouped Questlog mail; delivering remaining cargo directly.", exception);
                try {
                    if (!deliverImmediately(recipient, carrier)) data.retry(batch.id(), attempt, level.getGameTime());
                } catch (RuntimeException fallbackException) {
                    data.retry(batch.id(), attempt, level.getGameTime());
                    Envelope.LOGGER.error("Grouped mail fallback failed; remaining cargo is still queued.", fallbackException);
                }
            }
        }
    }

    public static boolean deliverImmediately(ServerPlayer player, ItemStack carrier) {
        var reference = GroupedMailReference.read(carrier).orElse(null);
        if (reference == null) return true;
        var data = GroupedMailSavedData.get(player.getServer().overworld());
        return data.deliver(reference.batch(), reference.attempt(), mail -> QuestMailDelivery.dropOneImmediately(player, mail));
    }

    /** Called only for carriers; ordinary player mail never passes through this path. */
    public static MailDropOffResult deliver(MailDropOffContext context,
                                            Function<MailDropOffContext, MailDropOffResult> nativeDropOff) {
        var reference = GroupedMailReference.read(context.getMail()).orElse(null);
        if (reference == null) return MailDropOffResult.CONSUME;
        var data = GroupedMailSavedData.get(context.getLevel());
        var batch = data.find(reference.batch(), reference.attempt()).orElse(null);
        if (batch == null) return MailDropOffResult.CONSUME;
        Delivery original = context.getDelivery();
        if (!original.getOwner().filter(batch.recipient()::equals).isPresent()
                || !(original.getRecipient() instanceof PlayerAddress recipient)
                || !recipient.getString().equalsIgnoreCase(batch.recipientName())) {
            data.retry(reference.batch(), reference.attempt(), context.getLevel().getGameTime());
            return MailDropOffResult.CONSUME;
        }
        boolean direct = QuestMailMarker.isDirectPlayerDrop(context.getMail());
        boolean complete;
        try {
            complete = data.deliver(reference.batch(), reference.attempt(), mail -> {
                ItemStack fallback = mail.copy();
                if (direct) QuestMailMarker.markDirectPlayerDrop(mail);
                var individual = new Delivery(original.getId(), original.getOwner(),
                        Mail.getSenderOrElse(mail, original.getSender()), original.getRecipient(), mail,
                        original.getRoute(), original.getPhase(), original.getPhaseProgress(), false);
                var child = new MailDropOffContext(context.getService(), context.getTarget(), individual);
                if (nativeDropOff.apply(child) == MailDropOffResult.CONSUME) return true;
                // The mailbox may fill halfway through the lot. Keep the rest visible beside it.
                return dropAt(context, fallback, recipient);
            });
        } catch (RuntimeException exception) {
            complete = false;
            Envelope.LOGGER.error("Grouped mail drop-off failed; remaining cargo will be retried.", exception);
        }
        if (!complete) data.retry(reference.batch(), reference.attempt(), context.getLevel().getGameTime());
        return MailDropOffResult.CONSUME;
    }

    public static boolean dropAt(MailDropOffContext context, ItemStack mail, PlayerAddress recipient) {
        ItemStack delivered = Mail.asDelivered(mail.copyWithCount(1));
        QuestMailMarker.clearDirectPlayerDrop(delivered);
        Mail.writeToLog(delivered, DeliveryRecord.arrivedTo(recipient));
        Mail.setId(delivered, Id.create(context.getLevel()));
        BlockPos position = context.getDelivery().getRoute().getRecipientPos()
                .orElseGet(() -> context.getDelivery().getRoute().getRecipientAscendPos()
                        .map(pos -> pos.below(8)).orElse(BlockPos.ZERO));
        var entity = new ItemEntity(context.getLevel(), position.getX() + 0.5,
                position.getY() + 1.35, position.getZ() + 0.5, delivered);
        entity.setDeltaMovement(new Vec3(0, -0.08, 0));
        entity.setPickUpDelay(8);
        if (!context.getLevel().addFreshEntity(entity)) return false;
        QuestMailDelivery.recordDelivered(context.getLevel(), recipient.getString(), delivered);
        return true;
    }

    public static boolean courierDied(ServerLevel level, Delivery delivery) {
        if (!GroupedMailReference.isCarrier(delivery.getMail())) return false;
        GroupedMailReference.read(delivery.getMail()).ifPresent(reference ->
                GroupedMailSavedData.get(level).retry(reference.batch(), reference.attempt(), level.getGameTime()));
        delivery.setMail(ItemStack.EMPTY);
        delivery.end();
        return true;
    }
}
