package io.github.exco9.questlogenvelope.mixin;

import io.github.exco9.questlogenvelope.mail.PendingQuestMailSavedData;
import io.github.exco9.questlogenvelope.mail.QuestMailMarker;
import io.github.exco9.questlogenvelope.quest.QuestMailUnlocker;
import io.github.mortuusars.envelope.world.mail.address.type.PlayerAddress;
import io.github.mortuusars.envelope.world.mail.dropoff.BlockDropOffHandler;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffContext;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffResult;
import net.minecraft.server.level.ServerPlayer;
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

        QuestMailMarker.get(context.getMail()).ifPresent(questId -> {
            PendingQuestMailSavedData.get(context.getLevel()).record(recipient.getString(), questId);

            ServerPlayer player = context.getLevel()
                    .getServer()
                    .getPlayerList()
                    .getPlayerByName(recipient.getString());

            if (player != null) {
                QuestMailUnlocker.applyPending(player, questId);
            }
        });
    }
}
