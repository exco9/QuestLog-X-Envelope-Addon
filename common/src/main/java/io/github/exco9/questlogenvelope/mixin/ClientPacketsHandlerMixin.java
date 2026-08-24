package io.github.exco9.questlogenvelope.mixin;

import io.github.exco9.questlogenvelope.client.QuestLetterViewScreen;
import io.github.exco9.questlogenvelope.mail.QuestMailMarker;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.network.handler.ClientPacketsHandler;
import io.github.mortuusars.envelope.network.packet.clientbound.OpenLetterViewScreenS2CP;
import io.github.mortuusars.envelope.world.item.LetterItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Opens a custom-scaled view only for Questlog letters that request it. */
@Mixin(value = ClientPacketsHandler.class, remap = false)
public abstract class ClientPacketsHandlerMixin {
    @Inject(method = "openLetterViewScreen", at = @At("HEAD"), cancellable = true, remap = false)
    private static void questlogEnvelope$openScaledQuestLetter(
            OpenLetterViewScreenS2CP packet,
            CallbackInfo ci
    ) {
        ItemStack itemInHand = Minecrft.player().getItemInHand(packet.hand());
        if (itemInHand.getItem() instanceof LetterItem
                && QuestMailMarker.getFontScale(itemInHand) != 1.0F) {
            Minecrft.get().setScreen(new QuestLetterViewScreen(itemInHand, packet.hand()));
            ci.cancel();
        }
    }
}
