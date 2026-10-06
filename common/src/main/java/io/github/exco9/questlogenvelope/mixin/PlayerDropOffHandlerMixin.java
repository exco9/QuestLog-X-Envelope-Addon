package io.github.exco9.questlogenvelope.mixin;

import io.github.exco9.questlogenvelope.mail.GroupedMailDelivery;
import io.github.exco9.questlogenvelope.mail.GroupedMailReference;
import io.github.exco9.questlogenvelope.mail.QuestMailMarker;
import io.github.mortuusars.envelope.world.mail.address.type.PlayerAddress;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffContext;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffResult;
import io.github.mortuusars.envelope.world.mail.dropoff.PlayerDropOffHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Handles grouped rewards and the direct-to-player no-mailbox fallback. */
@Mixin(value = PlayerDropOffHandler.class, remap = false)
public abstract class PlayerDropOffHandlerMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private void questlogEnvelope$dropDirectMail(MailDropOffContext context,
                                               CallbackInfoReturnable<MailDropOffResult> cir) {
        if (!(context.getTarget() instanceof PlayerAddress recipient)) return;
        if (GroupedMailReference.isCarrier(context.getMail())) {
            cir.setReturnValue(GroupedMailDelivery.deliver(context,
                    child -> ((PlayerDropOffHandler) (Object) this).handle(child)));
            return;
        }
        if (context.getMail().isEmpty() || !QuestMailMarker.isDirectPlayerDrop(context.getMail())) return;
        cir.setReturnValue(GroupedMailDelivery.dropAt(context, context.getMail(), recipient)
                ? MailDropOffResult.CONSUME : MailDropOffResult.PASS);
    }
}
