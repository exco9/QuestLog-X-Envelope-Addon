package io.github.exco9.questlogenvelope.mixin;

import io.github.exco9.questlogenvelope.quest.QuestMailUnlocker;
import net.minecraft.server.level.ServerPlayer;
import org.infernalstudios.questlog.QuestlogEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies offline mail unlocks only after Questlog has loaded/deserialized player quest data. */
@Mixin(value = QuestlogEvents.class, remap = false)
public abstract class QuestlogEventsMixin {
    @Inject(method = "onServerPlayerLogin", at = @At("TAIL"), remap = false)
    private static void questlogEnvelope$afterQuestlogPlayerLoad(ServerPlayer player, CallbackInfo ci) {
        QuestMailUnlocker.applyPending(player);
    }
}
