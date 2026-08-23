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

    /**
     * Uses normal Envelope service delivery when the player has a default mailbox.
     * Without a mailbox, starts a visible service pigeon directly near the player
     * when they are in the Overworld. Envelope's mail service only operates there;
     * in other dimensions we safely fall back to an immediate item drop.
     */
    public static void dispatch(ServerPlayer player, ItemStack mail, @Nullable ResourceLocation senderId) {
        ServerLevel playerLevel = player.serverLevel();
        ServerLevel mailLevel = player.getServer().overworld();
        MailService service = MailService.of(mailLevel);
        Address sender = resolveSender(service, senderId);
        PlayerAddress recipient = new PlayerAddress(player);

        // Set it eagerly so even an emergency fallback keeps the configured sender.
        Mail.setSender(mail, sender);

        if (service.getPlayerDefaultAddress(recipient).isPresent()) {
            service.getDeliveryManager().startService(Delivery.draft()
                    .deliver(mail)
                    .from(sender)
                    .to(recipient)
                    .owner(player));
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

        service.getDeliveryManager().start(
                Delivery.draft()
                        .deliver(mail)
                        .from(sender)
                        .to(recipient)
                        .owner(player)
                        // Skip Envelope's hub recipient check: it intentionally rejects
                        // PlayerAddress without a default mailbox.
                        .startAtPhase(DeliveryPhase.APPROACHING_RECIPIENT),
                delivery -> {
                    delivery.setRoute(route);

                    Pigeon pigeon = Pigeon.createService(level);
                    BlockPos spawnPos = route.getRecipientAscendPos().orElseGet(() -> recipientPos.above(8));
                    pigeon.moveTo(
                            spawnPos.getX() + 0.5,
                            spawnPos.getY() + 0.5,
                            spawnPos.getZ() + 0.5,
                            player.getYRot(),
                            0.0F
                    );
                    pigeon.startDelivery(delivery);

                    if (!level.addFreshEntity(pigeon)) {
                        throw new IllegalStateException("Failed to spawn direct Envelope service pigeon");
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
        PlayerAddress recipient = new PlayerAddress(player);
        ItemStack delivered = Mail.asDelivered(mail.copyWithCount(1));
        QuestMailMarker.clearDirectPlayerDrop(delivered);
        Mail.writeToLog(delivered, DeliveryRecord.arrivedTo(recipient));
        Mail.setId(delivered, Id.create(player.level()));

        player.drop(delivered, false);
        recordDelivered(player.getServer().overworld(), recipient.getString(), delivered);
    }

    /** Records a quest marker and applies it immediately when the player is online. */
    public static void recordDelivered(ServerLevel level, String recipientName, ItemStack deliveredMail) {
        QuestMailMarker.get(deliveredMail).ifPresent(questId -> {
            PendingQuestMailSavedData.get(level).record(recipientName, questId);

            ServerPlayer player = level.getServer()
                    .getPlayerList()
                    .getPlayerByName(recipientName);

            if (player != null) {
                QuestMailUnlocker.applyPending(player, questId);
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
