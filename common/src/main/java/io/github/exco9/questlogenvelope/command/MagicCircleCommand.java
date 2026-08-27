package io.github.exco9.questlogenvelope.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.exco9.questlogenvelope.mail.QuestMagicCircle;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Internal client-to-server bridge for hold-to-activate magic circles. */
public final class MagicCircleCommand {
    private MagicCircleCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("questlog_envelope")
                .then(Commands.literal("magic_circle")
                        .then(Commands.literal("activate")
                                .then(Commands.argument("action_id", StringArgumentType.word())
                                        .executes(context -> activate(
                                                context.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(context, "action_id")
                                        ))))));
    }

    private static int activate(ServerPlayer player, String rawActionId) {
        final UUID actionId;
        try {
            actionId = UUID.fromString(rawActionId);
        } catch (IllegalArgumentException ignored) {
            return 0;
        }

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            QuestMagicCircle.ActivationResult result = QuestMagicCircle.activate(stack, player, actionId);
            if (result != QuestMagicCircle.ActivationResult.NOT_THIS_ITEM) {
                return result == QuestMagicCircle.ActivationResult.ACTIVATED ? 1 : 0;
            }
        }

        return 0;
    }
}
