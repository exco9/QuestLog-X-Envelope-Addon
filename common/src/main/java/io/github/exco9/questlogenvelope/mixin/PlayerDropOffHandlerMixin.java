package io.github.exco9.questlogenvelope.mixin;

import io.github.exco9.questlogenvelope.mail.QuestMailDelivery;
import io.github.exco9.questlogenvelope.mail.QuestMailMarker;
import io.github.mortuusars.envelope.world.item.component.Id;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.type.PlayerAddress;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffContext;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffResult;
import io.github.mortuusars.envelope.world.mail.dropoff.PlayerDropOffHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Handles the addon's direct-to-player fallback when no mailbox is registered. */
@Mixin(value = PlayerDropOffHandler.class, remap = false)
public abstract class PlayerDropOffHandlerMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private void questlogEnvelope$dropDirectMail(
            MailDropOffContext context,
            CallbackInfoReturnable<MailDropOffResult> cir
    ) {
        if (!(context.getTarget() instanceof PlayerAddress recipient)) {
            return;
        }

        ItemStack mail = context.getMail();
        if (mail.isEmpty() || !QuestMailMarker.isDirectPlayerDrop(mail)) {
            return;
        }

        ItemStack deliveredMail = Mail.asDelivered(mail.copyWithCount(1));
        QuestMailMarker.clearDirectPlayerDrop(deliveredMail);
        Mail.writeToLog(deliveredMail, DeliveryRecord.arrivedTo(recipient));
        Mail.setId(deliveredMail, Id.create(context.getLevel()));

        ServerPlayer player = context.getLevel()
                .getServer()
                .getPlayerList()
                .getPlayerByName(recipient.getString());

        if (player != null) {
            player.drop(deliveredMail, false);
        } else {
            // The player may disconnect during the pigeon's short approach.
            // Keep the mail at the originally targeted position.
            BlockPos dropPos = context.getDelivery()
                    .getRoute()
                    .getRecipientPos()
                    .orElse(BlockPos.ZERO);

            context.getLevel().addFreshEntity(new ItemEntity(
                    context.getLevel(),
                    dropPos.getX() + 0.5,
                    dropPos.getY() + 0.5,
                    dropPos.getZ() + 0.5,
                    deliveredMail
            ));
        }

        QuestMailDelivery.recordDelivered(context.getLevel(), recipient.getString(), deliveredMail);
        cir.setReturnValue(MailDropOffResult.CONSUME);
    }
}
