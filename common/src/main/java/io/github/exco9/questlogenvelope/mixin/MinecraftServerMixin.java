package io.github.exco9.questlogenvelope.mixin;

import io.github.exco9.questlogenvelope.mail.GroupedMailDelivery;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "tickServer", at = @At("TAIL"))
    private void questlogEnvelope$flushMail(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        GroupedMailDelivery.flush((MinecraftServer) (Object) this);
    }
}
