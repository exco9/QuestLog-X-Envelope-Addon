package io.github.exco9.questlogenvelope.mixin;

import io.github.exco9.questlogenvelope.mail.QuestMagicSeal;
import io.github.mortuusars.envelope.world.item.SealedLetterItem;
import io.github.mortuusars.envelope.world.item.SealedPackageItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Runs addon magic-seal actions only after Envelope's normal hold-to-unseal completes. */
@Mixin(value = {SealedLetterItem.class, SealedPackageItem.class}, remap = false)
public abstract class SealedMailItemMixin {
    @Inject(method = "finishUsingItem", at = @At("RETURN"), remap = false)
    private void questlogEnvelope$activateMagicSeal(
            ItemStack stack,
            Level level,
            LivingEntity entity,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        QuestMagicSeal.activate(stack, level, entity);
        QuestMagicSeal.clear(cir.getReturnValue());
    }
}
