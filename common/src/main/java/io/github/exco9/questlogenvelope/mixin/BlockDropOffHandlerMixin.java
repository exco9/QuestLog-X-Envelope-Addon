package io.github.exco9.questlogenvelope.mixin;

import io.github.exco9.questlogenvelope.mail.QuestMailDelivery;
import io.github.mortuusars.envelope.world.mail.address.type.PlayerAddress;
import io.github.mortuusars.envelope.world.mail.dropoff.BlockDropOffHandler;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffContext;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockDropOffHandler.class, remap = false)
public abstract class BlockDropOffHandlerMixin {
    @Inject(method = "handle", at = @At("RETURN"), remap = false)
    private void questlogEnvelope$onDelivered(
            MailDropOffContext context,
            CallbackInfoReturnable<MailDropOffResult> cir
    ) {
        if (cir.getReturnValue() != MailDropOffResult.CONSUME || context.isReturned()) {
            return;
        }

        if (!(context.getDelivery().getRecipient() instanceof PlayerAddress recipient)) {
            return;
        }

        QuestMailDelivery.recordDelivered(context.getLevel(), recipient.getString(), context.getMail());
    }
}
