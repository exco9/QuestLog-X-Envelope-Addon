package io.github.exco9.questlogenvelope.quest;

import com.google.gson.JsonObject;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.LetterContent;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.infernalstudios.questlog.core.quests.objectives.Objective;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Progresses when the player manually dispatches qualifying Envelope mail from a mailbox.
 * All filters are optional and combine with AND semantics.
 */
public final class MailSentObjective extends Objective {
    public enum MailKind {
        ANY,
        LETTER,
        PACKAGE;

        static MailKind parse(String value) {
            if (value == null) {
                return ANY;
            }
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "letter" -> LETTER;
                case "package" -> PACKAGE;
                default -> ANY;
            };
        }
    }

    private final String recipient;
    private final MailKind mailKind;
    private final String textContains;
    @Nullable private final ResourceLocation itemId;
    private final int itemCount;

    public MailSentObjective(JsonObject definition) {
        super(definition);
        this.recipient = getString(definition, "recipient", "").trim();
        this.mailKind = MailKind.parse(getString(definition, "mail_kind", "any"));
        this.textContains = getString(definition, "text_contains", "").trim().toLowerCase(Locale.ROOT);
        this.itemId = parseId(getString(definition, "item", ""));
        this.itemCount = Math.max(1, getInt(definition, "item_count", 1));
    }

    public boolean accepts(Address recipientAddress, ItemStack mail) {
        if (!this.recipient.isEmpty()
                && !this.recipient.equalsIgnoreCase(recipientAddress.getString())) {
            return false;
        }

        boolean isLetter = mail.has(Envelope.DataComponents.LETTER_CONTENT);
        boolean isPackage = mail.has(Envelope.DataComponents.PACKAGE_CONTENTS);

        if (this.mailKind == MailKind.LETTER && !isLetter) {
            return false;
        }
        if (this.mailKind == MailKind.PACKAGE && !isPackage) {
            return false;
        }

        if (!this.textContains.isEmpty()) {
            if (!isLetter) {
                return false;
            }
            LetterContent content = mail.getOrDefault(
                    Envelope.DataComponents.LETTER_CONTENT,
                    LetterContent.EMPTY
            );
            if (!content.text().getString().toLowerCase(Locale.ROOT).contains(this.textContains)) {
                return false;
            }
        }

        if (this.itemId != null) {
            if (!isPackage) {
                return false;
            }
            PackageContents contents = mail.getOrDefault(
                    Envelope.DataComponents.PACKAGE_CONTENTS,
                    PackageContents.EMPTY
            );
            int matchingCount = 0;
            for (ItemStack stack : contents.copyItems()) {
                if (!stack.isEmpty() && this.itemId.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
                    matchingCount += stack.getCount();
                    if (matchingCount >= this.itemCount) {
                        break;
                    }
                }
            }
            if (matchingCount < this.itemCount) {
                return false;
            }
        }

        return true;
    }

    public void sent() {
        if (!isCompleted()) {
            setUnits(getUnits() + 1);
        }
    }

    private static String getString(JsonObject definition, String key, String fallback) {
        return definition.has(key) && definition.get(key).isJsonPrimitive()
                ? definition.get(key).getAsString()
                : fallback;
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

    private static @Nullable ResourceLocation parseId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return ResourceLocation.tryParse(value.trim());
    }
}
