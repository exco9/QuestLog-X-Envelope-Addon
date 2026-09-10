package io.github.exco9.questlogenvelope.mixin;

import io.github.exco9.questlogenvelope.quest.MailSentTracker;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.mail.delivery.DeliveryDraft;
import io.github.mortuusars.envelope.world.mail.delivery.DeliveryManager;
import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records only physical mailbox dispatches started with a pigeon. */
@Mixin(value = DeliveryManager.class, remap = false)
public abstract class DeliveryManagerMixin {
    @Inject(
            method = "start(Lio/github/mortuusars/envelope/world/entity/Pigeon;Lio/github/mortuusars/envelope/world/mail/delivery/DeliveryDraft;)V",
            at = @At("HEAD"),
            remap = false
    )
    private void questlogEnvelope$recordPlayerMail(
            Pigeon pigeon,
            DeliveryDraft draft,
            CallbackInfo ci
    ) {
        // Normal player mail leaves from a concrete mailbox address. Service
        // deliveries created by this addon use startService/start(draft, factory)
        // and therefore never enter this hook.
        if (!(draft.getSender() instanceof BlockAddress)) {
            return;
        }

        draft.getOwner().ifPresent(owner -> {
            DeliveryManager manager = (DeliveryManager) (Object) this;
            ServerPlayer player = manager.getMailService()
                    .getLevel()
                    .getServer()
                    .getPlayerList()
                    .getPlayer(owner);
            if (player != null) {
                MailSentTracker.record(player, draft.getRecipient(), draft.getMail().copy());
            }
        });
    }
}
