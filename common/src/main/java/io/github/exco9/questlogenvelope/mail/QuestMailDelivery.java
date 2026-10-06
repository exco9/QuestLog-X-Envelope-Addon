package io.github.exco9.questlogenvelope.mail;

import io.github.exco9.questlogenvelope.quest.QuestMailUnlocker;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.item.component.Id;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressLocation;
import io.github.mortuusars.envelope.world.mail.address.type.PlayerAddress;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import io.github.mortuusars.envelope.world.mail.delivery.DeliveryPhase;
import io.github.mortuusars.envelope.world.mail.delivery.DeliveryRoute;
import io.github.mortuusars.envelope.world.mail.delivery.TravelDuration;
import io.github.mortuusars.envelope.world.mail.service.ServiceAddressDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** Shared delivery logic for Questlog rewards that produce Envelope mail. */
public final class QuestMailDelivery {
    private QuestMailDelivery() {
    }

    /** Uses Envelope's normal travel time when a mailbox is available. */
    public static void dispatch(ServerPlayer player, ItemStack mail, @Nullable ResourceLocation senderId) {
        dispatch(player, mail, senderId, false);
    }

    /**
     * Dispatches Questlog reward mail through Envelope.
     *
     * @param expressMailbox when true and the player has a mailbox, a real service
     *                       pigeon is spawned near the mailbox and starts directly
     *                       in the approach phase. This keeps the visible delivery
     *                       while avoiding the long background trip from the service.
     */
    public static void dispatch(
            ServerPlayer player,
            ItemStack mail,
            @Nullable ResourceLocation senderId,
            boolean expressMailbox
    ) {
        MailService service = MailService.of(player.getServer().overworld());
        Address sender = resolveSender(service, senderId);
        Mail.setSender(mail, sender);
        Mail.writeToLog(mail, DeliveryRecord.sentFrom(sender));
        GroupedMailDelivery.enqueue(player, mail, expressMailbox);
    }

    /** Starts the single carrier after the short reward collection window. */
    static void dispatchNow(ServerPlayer player, ItemStack mail, boolean expressMailbox) {
        ServerLevel playerLevel = player.serverLevel();
        ServerLevel mailLevel = player.getServer().overworld();
        MailService service = MailService.of(mailLevel);
        Address sender = Mail.getSenderOrElse(mail, service.getAddress());
        PlayerAddress recipient = new PlayerAddress(player);

        // Set it eagerly so even an emergency fallback keeps the configured sender.
        Mail.setSender(mail, sender);

        if (service.getPlayerDefaultAddress(recipient).isPresent()) {
            if (expressMailbox) {
                startExpressMailboxDelivery(service, player, mail, sender, recipient);
            } else {
                service.getDeliveryManager().startService(Delivery.draft()
                        .deliver(mail)
                        .from(sender)
                        .to(recipient)
                        .owner(player));
            }
            return;
        }

        if (playerLevel == mailLevel) {
            startDirectPlayerDelivery(service, player, mail, sender, recipient);
            return;
        }

        Envelope.LOGGER.debug(
                "Questlog mail recipient '{}' has no mailbox and is outside the Overworld; dropping reward mail directly.",
                player.getScoreboardName()
        );
        dropImmediately(player, mail);
    }

    /** Starts a service pigeon close to the registered mailbox instead of simulating the full route. */
    private static void startExpressMailboxDelivery(
            MailService service,
            ServerPlayer player,
            ItemStack mail,
            Address sender,
            PlayerAddress recipient
    ) {
        ServerLevel level = service.getLevel();
        DeliveryRoute route = DeliveryRoute.build(level, sender, recipient);
        BlockPos fallbackPos = route.getRecipientPos().orElse(player.blockPosition());

        Mail.writeToLog(mail, DeliveryRecord.sentFrom(sender));
        startVisibleApproachDelivery(service, player, mail, sender, recipient, route, fallbackPos);
    }

    private static void startDirectPlayerDelivery(
            MailService service,
            ServerPlayer player,
            ItemStack mail,
            Address sender,
            PlayerAddress recipient
    ) {
        ServerLevel level = service.getLevel();
        BlockPos recipientPos = player.blockPosition();
        AddressLocation senderLocation = service.getLocationOf(sender);
        AddressLocation recipientLocation = AddressLocation.exact(recipientPos);
        Optional<BlockPos> hubPos = DeliveryRoute.getHubPosition(senderLocation, recipientLocation);

        DeliveryRoute route = new DeliveryRoute(
                senderLocation,
                recipientLocation,
                senderLocation.getPosition(),
                senderLocation.ascendTowards(level, hubPos),
                TravelDuration.basedOnDistance(senderLocation.getDistanceTo(level, hubPos)),
                hubPos,
                TravelDuration.basedOnDistance(recipientLocation.getDistanceTo(level, hubPos)),
                recipientLocation.ascendTowards(level, hubPos),
                Optional.of(recipientPos)
        );

        QuestMailMarker.markDirectPlayerDrop(mail);
        Mail.writeToLog(mail, DeliveryRecord.sentFrom(sender));
        startVisibleApproachDelivery(service, player, mail, sender, recipient, route, recipientPos);
    }

    private static void startVisibleApproachDelivery(
            MailService service,
            ServerPlayer player,
            ItemStack mail,
            Address sender,
            PlayerAddress recipient,
            DeliveryRoute route,
            BlockPos fallbackSpawnTarget
    ) {
        ServerLevel level = service.getLevel();

        service.getDeliveryManager().start(
                Delivery.draft()
                        .deliver(mail)
                        .from(sender)
                        .to(recipient)
                        .owner(player)
                        // The route is already known and the courier is spawned on
                        // the recipient side, so skip the long service/hub travel.
                        .startAtPhase(DeliveryPhase.APPROACHING_RECIPIENT),
                delivery -> {
                    delivery.setRoute(route);

                    Pigeon pigeon = Pigeon.createService(level);
                    BlockPos spawnPos = route.getRecipientAscendPos()
                            .orElseGet(() -> fallbackSpawnTarget.above(8));
                    pigeon.moveTo(
                            spawnPos.getX() + 0.5,
                            spawnPos.getY() + 0.5,
                            spawnPos.getZ() + 0.5,
                            player.getYRot(),
                            0.0F
                    );
                    pigeon.startDelivery(delivery);

                    if (!level.addFreshEntity(pigeon)) {
                        throw new IllegalStateException("Failed to spawn Envelope service pigeon");
                    }
                    return pigeon;
                }
        );
    }

    /**
     * Last-resort fallback used when Envelope throws while a Questlog reward is
     * being collected. The reward must never remain permanently unclaimable.
     */
    public static void dropImmediately(ServerPlayer player, ItemStack mail) {
        boolean success = GroupedMailReference.isCarrier(mail)
                ? GroupedMailDelivery.deliverImmediately(player, mail) : dropOneImmediately(player, mail);
        if (!success) throw new IllegalStateException("Failed to drop Questlog mail");
    }

    static boolean dropOneImmediately(ServerPlayer player, ItemStack mail) {
        PlayerAddress recipient = new PlayerAddress(player);
        ItemStack delivered = Mail.asDelivered(mail.copyWithCount(1));
        QuestMailMarker.clearDirectPlayerDrop(delivered);
        Mail.writeToLog(delivered, DeliveryRecord.arrivedTo(recipient));
        Mail.setId(delivered, Id.create(player.level()));

        if (player.drop(delivered, false) == null) return false;
        recordDelivered(player.getServer().overworld(), recipient.getString(), delivered);
        return true;
    }

    /** Records a quest marker and applies it immediately when the player is online. */
    public static void recordDelivered(ServerLevel level, String recipientName, ItemStack deliveredMail) {
        QuestMailMarker.get(deliveredMail).ifPresent(questId -> {
            PendingQuestMailSavedData.get(level).record(recipientName, questId);

            ServerPlayer player = level.getServer()
                    .getPlayerList()
                    .getPlayerByName(recipientName);

            if (player != null) {
                try {
                    QuestMailUnlocker.applyPending(player, questId);
                } catch (RuntimeException exception) {
                    // Physical delivery already succeeded. Keep its persisted marker,
                    // without making the courier replay the same physical item.
                    Envelope.LOGGER.error("Delivered quest mail progression is pending for {}.", recipientName, exception);
                }
            }
        });
    }

    private static Address resolveSender(MailService service, @Nullable ResourceLocation senderId) {
        if (senderId == null) {
            return service.getAddress();
        }

        ResourceKey<ServiceAddressDefinition> key = ResourceKey.create(
                Envelope.Registries.SERVICE_ADDRESS_DEFINITION,
                senderId
        );

        return ServiceAddress.get(service.getLevel().registryAccess(), key).map(Address.class::cast).orElseGet(() -> {
            Envelope.LOGGER.warn(
                    "Questlog Envelope sender service '{}' is not registered; using Envelope mail service instead.",
                    senderId
            );
            return service.getAddress();
        });
    }
}
