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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
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

        // The route recipient position is captured when the pigeon starts its
        // final approach. Spawn the item there rather than using player.drop(),
        // which visually makes a moving player look like they threw the letter.
        BlockPos dropPos = context.getDelivery()
                .getRoute()
                .getRecipientPos()
                .orElseGet(() -> context.getDelivery()
                        .getRoute()
                        .getRecipientAscendPos()
                        .map(pos -> pos.below(8))
                        .orElse(BlockPos.ZERO));

        ItemEntity droppedMail = new ItemEntity(
                context.getLevel(),
                dropPos.getX() + 0.5,
                dropPos.getY() + 1.35,
                dropPos.getZ() + 0.5,
                deliveredMail
        );
        droppedMail.setDeltaMovement(new Vec3(0.0, -0.08, 0.0));
        droppedMail.setPickUpDelay(8);
        context.getLevel().addFreshEntity(droppedMail);

        QuestMailDelivery.recordDelivered(context.getLevel(), recipient.getString(), deliveredMail);
        cir.setReturnValue(MailDropOffResult.CONSUME);
    }
}
