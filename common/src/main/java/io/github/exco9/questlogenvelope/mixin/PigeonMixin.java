package io.github.exco9.questlogenvelope.mixin;

import io.github.exco9.questlogenvelope.mail.GroupedMailDelivery;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Pigeon.class, remap = false)
public abstract class PigeonMixin {
    @Inject(method = "diedWhileDelivering", at = @At("HEAD"), cancellable = true, remap = false)
    private void questlogEnvelope$retryCargo(ServerLevel level, DamageSource source, Delivery delivery, CallbackInfo ci) {
        if (GroupedMailDelivery.courierDied(level, delivery)) ci.cancel();
    }
}
