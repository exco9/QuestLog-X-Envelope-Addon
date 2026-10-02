package io.github.exco9.questlogenvelope.quest;

import com.google.gson.JsonObject;
import io.github.exco9.questlogenvelope.mail.LetterSignature;
import io.github.exco9.questlogenvelope.mail.SignatureActions;
import io.github.exco9.questlogenvelope.mail.QuestMagicCircle;
import io.github.exco9.questlogenvelope.mail.QuestMailDelivery;
import io.github.exco9.questlogenvelope.mail.QuestMailMarker;
import io.github.exco9.questlogenvelope.mail.QuestMailSeal;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.infernalstudios.questlog.core.quests.rewards.Reward;
import org.jetbrains.annotations.Nullable;

public final class LetterReward extends Reward {
    @Nullable private final ResourceLocation senderId;
    @Nullable private final ResourceLocation grantsQuestId;
    @Nullable private final ResourceLocation sealSymbolId;
    @Nullable private final String magicCircleCommand;
    private final String title;
    private final String text;
    private final LetterSignature signature;
    @Nullable private final String signatureCommand;
    private final boolean magicCircle;
    private final int magicCircleColor;
    private final int magicCircleMagicColor;
    private final int magicCircleX;
    private final int magicCircleY;
    private final int magicCircleSize;
    private final int magicCircleHoldMillis;

    public LetterReward(JsonObject definition) {
        super(definition);
        senderId = getOptionalId(definition, "sender");
        grantsQuestId = getOptionalId(definition, "grants_quest");
        sealSymbolId = getOptionalId(definition, "seal");
        title = definition.has("title") ? definition.get("title").getAsString() : "Letter";
        text = definition.has("text") ? definition.get("text").getAsString() : "";
        signature = LetterSignature.fromJson(definition);
        signatureCommand = getOptionalString(definition, "signature_command");
        magicCircle = definition.has("magic_circle")
                && definition.get("magic_circle").isJsonPrimitive()
                && definition.get("magic_circle").getAsBoolean();
        magicCircleColor = getColor(definition, "magic_circle_color", QuestMagicCircle.DEFAULT_COLOR);
        magicCircleMagicColor = getColor(definition, "magic_circle_magic_color", LetterSignature.DEFAULT_MAGIC_COLOR);
        magicCircleCommand = getOptionalString(definition, "magic_circle_command");

        magicCircleSize = QuestMagicCircle.clampSize(
                getInt(definition, "magic_circle_size", QuestMagicCircle.DEFAULT_SIZE)
        );
        magicCircleX = QuestMagicCircle.clampX(
                getInt(definition, "magic_circle_x", QuestMagicCircle.defaultX(magicCircleSize)),
                magicCircleSize
        );
        magicCircleY = QuestMagicCircle.clampY(
                getInt(definition, "magic_circle_y", QuestMagicCircle.defaultY(magicCircleSize)),
                magicCircleSize
        );
        magicCircleHoldMillis = QuestMagicCircle.clampHoldMillis((int) Math.round(
                getDouble(
                        definition,
                        "magic_circle_hold_seconds",
                        QuestMagicCircle.DEFAULT_HOLD_MILLIS / 1000.0
                ) * 1000.0
        ));
    }

    @Override
    public void applyReward(ServerPlayer player) {
        ItemStack letter = Mail.createLetter(Component.literal(text))
                .set(DataComponents.ITEM_NAME, Component.literal(title))
                .get();

        if (magicCircle) {
            QuestMagicCircle.attach(
                    letter,
                    player,
                    grantsQuestId,
                    magicCircleCommand,
                    magicCircleColor,
                    magicCircleX,
                    magicCircleY,
                    magicCircleSize,
                    magicCircleHoldMillis,
                    magicCircleMagicColor
            );
        } else if (grantsQuestId != null) {
            QuestMailMarker.set(letter, grantsQuestId);
        }

        signature.resolvePlayer(player.getGameProfile().getName()).attach(letter);
        SignatureActions.attach(letter, player, signatureCommand);
        letter = QuestMailSeal.apply(player, letter, sealSymbolId);

        try {
            QuestMailDelivery.dispatch(player, letter, senderId);
        } catch (RuntimeException exception) {
            Envelope.LOGGER.error(
                    "Failed to dispatch Questlog letter reward for {}. Dropping the letter directly instead.",
                    player.getScoreboardName(),
                    exception
            );
            try {
                QuestMailDelivery.dropImmediately(player, letter);
            } catch (RuntimeException fallbackException) {
                Envelope.LOGGER.error("Failed to drop Questlog letter reward fallback.", fallbackException);
            }
        } finally {
            super.applyReward(player);
        }
    }

    private static @Nullable ResourceLocation getOptionalId(JsonObject definition, String key) {
        if (!definition.has(key) || !definition.get(key).isJsonPrimitive()) {
            return null;
        }
        return ResourceLocation.tryParse(definition.get(key).getAsString());
    }

    private static @Nullable String getOptionalString(JsonObject definition, String key) {
        if (!definition.has(key) || !definition.get(key).isJsonPrimitive()) {
            return null;
        }
        String value = definition.get(key).getAsString().trim();
        return value.isEmpty() ? null : value;
    }

    private static int getInt(JsonObject definition, String key, int fallback) {
        if (!definition.has(key) || !definition.get(key).isJsonPrimitive()) {
            return fallback;
        }
        try {
            return definition.get(key).getAsInt();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static double getDouble(JsonObject definition, String key, double fallback) {
        if (!definition.has(key) || !definition.get(key).isJsonPrimitive()) {
            return fallback;
        }
        try {
            return definition.get(key).getAsDouble();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static int getColor(JsonObject definition, String key, int fallback) {
        if (!definition.has(key) || !definition.get(key).isJsonPrimitive()) {
            return fallback;
        }

        try {
            if (definition.get(key).getAsJsonPrimitive().isNumber()) {
                return definition.get(key).getAsInt() & 0xFFFFFF;
            }

            String value = definition.get(key).getAsString().trim();
            if (value.startsWith("#")) {
                value = value.substring(1);
            } else if (value.startsWith("0x") || value.startsWith("0X")) {
                value = value.substring(2);
            }
            if (value.length() != 6) {
                return fallback;
            }
            return Integer.parseInt(value, 16) & 0xFFFFFF;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
