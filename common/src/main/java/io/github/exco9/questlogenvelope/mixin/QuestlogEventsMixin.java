package io.github.exco9.questlogenvelope.mixin;

import com.mojang.brigadier.CommandDispatcher;
import io.github.exco9.questlogenvelope.command.MagicCircleCommand;
import io.github.exco9.questlogenvelope.quest.QuestMailUnlocker;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.infernalstudios.questlog.QuestlogEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hooks addon lifecycle behavior after Questlog has initialized the relevant server state. */
@Mixin(value = QuestlogEvents.class, remap = false)
public abstract class QuestlogEventsMixin {
    @Inject(method = "onServerPlayerLogin", at = @At("TAIL"), remap = false)
    private static void questlogEnvelope$afterQuestlogPlayerLoad(ServerPlayer player, CallbackInfo ci) {
        QuestMailUnlocker.applyPending(player);
    }

    @Inject(method = "registerCommands", at = @At("TAIL"), remap = false)
    private static void questlogEnvelope$registerCommands(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CallbackInfo ci
    ) {
        MagicCircleCommand.register(dispatcher);
    }
}
