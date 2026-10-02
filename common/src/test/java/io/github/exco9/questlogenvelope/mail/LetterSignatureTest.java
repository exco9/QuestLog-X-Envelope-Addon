package io.github.exco9.questlogenvelope.mail;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LetterSignatureTest {
    @Test
    void legacyLettersHaveNoSignature() {
        assertFalse(LetterSignature.fromJson(JsonParser.parseString("{\"text\":\"Old letter\"}").getAsJsonObject()).enabled());
        assertFalse(LetterSignature.fromTag(new CompoundTag()).enabled());
    }

    @Test
    void signatureSurvivesRewardJsonAndItemMetadataRoundTrips() {
        LetterSignature signature = new LetterSignature("Éléonore d’Arwen", 0x315C82, 22, 13, 86, true);
        JsonObject json = new JsonObject();
        json.addProperty("magic_circle", true);
        signature.writeJson(json);
        assertEquals(signature, LetterSignature.fromJson(json));
        assertEquals(signature, LetterSignature.fromTag(signature.toTag()));
        assertTrue(json.get("magic_circle").getAsBoolean());
    }

    @Test
    void malformedOptionalFieldsUseDefaultsAndLayoutIsBounded() {
        JsonObject json = JsonParser.parseString("""
                {"signature":" Test\\nName ", "signature_color":{}, "signature_size":"oops",
                 "signature_x":-900, "signature_y":900, "signature_frame":{}}
                """).getAsJsonObject();
        LetterSignature signature = LetterSignature.fromJson(json);
        assertEquals("Test Name", signature.text());
        assertEquals(LetterSignature.DEFAULT_COLOR, signature.color());
        assertEquals(LetterSignature.DEFAULT_SIZE, signature.size());
        assertEquals(0, signature.x());
        assertEquals(QuestMagicCircle.WRITABLE_HEIGHT - 1, signature.y());
        assertFalse(signature.framed());
        assertEquals(LetterSignature.MAX_SIZE, new LetterSignature("a", -1, 10000, 0, 0, false).size());
    }

    @Test
    void disablingRemovesOnlySignatureSettings() {
        JsonObject json = new JsonObject();
        new LetterSignature("Arwen", 0, 16, 0, 0, true).writeJson(json);
        json.addProperty("text", "Letter body");
        json.addProperty("magic_circle_color", "#FF00FF");
        LetterSignature.EMPTY.writeJson(json);
        assertFalse(json.has("signature"));
        assertFalse(json.has("signature_frame"));
        assertFalse(json.has("signature_color"));
        assertEquals(2, json.size());
    }

    @Test
    void plainTextIsBoundedWithoutBreakingUnicodeSurrogatePairs() {
        LetterSignature signature = new LetterSignature("😀".repeat(100), 0, 16, 0, 0, false);
        assertEquals(80, signature.text().codePointCount(0, signature.text().length()));
        assertFalse(Character.isHighSurrogate(signature.text().charAt(signature.text().length() - 1)));
        assertFalse(new LetterSignature(" \n\t ", 0, 16, 0, 0, false).enabled());
        assertFalse(LetterSignature.fromJson(JsonParser.parseString("{\"signature\":null}").getAsJsonObject()).enabled());
    }

    @Test
    void acceptsAllDocumentedColorFormats() {
        for (String color : new String[]{"#315C82", "0x315C82", "315C82"}) {
            JsonObject json = new JsonObject();
            json.addProperty("signature", "Arwen");
            json.addProperty("signature_color", color);
            assertEquals(0x315C82, LetterSignature.fromJson(json).color());
        }
    }

    @Test
    void playerPlaceholderResolvesForRecipientWithoutChangingTheTemplate() {
        LetterSignature template = new LetterSignature("Signed by @s", 0, 12, 30, 100, true);
        LetterSignature resolved = template.resolvePlayer("Arwen_42");
        assertEquals("Signed by Arwen_42", resolved.text());
        assertEquals("Signed by @s", template.text());
        assertEquals(resolved, LetterSignature.fromTag(resolved.toTag()));
        JsonObject json = new JsonObject();
        template.writeJson(json);
        assertEquals("Signed by @s", LetterSignature.fromJson(json).text());
        assertEquals(template.color(), resolved.color());
        assertEquals(template.framed(), resolved.framed());
    }

    @Test
    void onlyThePlayerTokenIsResolvedAndNamesAreLiteral() {
        var signature = new LetterSignature("@someone mail@s.test @p @s", 0, 12, 0, 0, false);
        assertEquals("@someone mail@s.test @p Player$1\\name",
                signature.resolvePlayer("Player$1\\name").text());
        assertEquals("Éléonore", new LetterSignature("@s", 0, 12, 0, 0, false).resolvePlayer("Éléonore").text());
    }
}
