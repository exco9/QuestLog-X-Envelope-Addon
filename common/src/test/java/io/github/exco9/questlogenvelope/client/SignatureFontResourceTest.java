package io.github.exco9.questlogenvelope.client;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class SignatureFontResourceTest {
    @Test
    void lowercaseAIsTheCorrectGlyphRatherThanThePreviousBacktick() throws Exception {
        var atlas = ImageIO.read(getClass().getResource("/assets/questlog_envelope/textures/font/signature.png"));
        int cell = 'a' - 32;
        String[] rows = {"......", "......", "......", "......", "..###.", ".#..#.", ".#.##.", "..##.#", "......"};
        for (int y = 0; y < rows.length; y++)
            for (int x = 0; x < 6; x++)
                assertEquals(rows[y].charAt(x) == '#',
                        (atlas.getRGB(cell % 16 * 16 + x, cell / 16 * 9 + y) >>> 24) == 255);
    }
    @Test
    void fontUsesAWholePixelBitmapWithoutAntialiasedEdges() throws Exception {
        try (var input = getClass().getResourceAsStream("/assets/questlog_envelope/textures/font/signature.png")) {
            assertNotNull(input);
            var image = ImageIO.read(input);
            assertEquals(256, image.getWidth());
            assertEquals(54, image.getHeight());
            int inkPixels = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int alpha = image.getRGB(x, y) >>> 24;
                    assertTrue(alpha == 0 || alpha == 255);
                    if (alpha == 255) inkPixels++;
                }
            }
            assertTrue(inkPixels > 500);
        }
        try (var input = getClass().getResourceAsStream("/assets/questlog_envelope/font/signature.json")) {
            assertNotNull(input);
            var json = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            var bitmap = json.getAsJsonArray("providers").get(1).getAsJsonObject();
            assertEquals("bitmap", bitmap.get("type").getAsString());
            assertEquals(9, bitmap.get("height").getAsInt());
            assertEquals(6, bitmap.getAsJsonArray("chars").size());
            for (var row : bitmap.getAsJsonArray("chars")) assertEquals(16, row.getAsString().length());
        }
    }

    @Test
    void everyGlyphPreservesTheOriginalPixelStrokes() throws Exception {
        var source = ImageIO.read(getClass().getResource("/assets/questlog_envelope/font/BitScript_r2.png"));
        var atlas = ImageIO.read(getClass().getResource("/assets/questlog_envelope/textures/font/signature.png"));
        var starts = new java.util.ArrayList<Integer>();
        for (int x = 0; x < source.getWidth(); x++)
            if ((source.getRGB(x, 0) & 0xFFFFFF) == 0x0000FF) starts.add(x);
        starts.add(source.getWidth());
        assertEquals(95, starts.size());
        for (int code = 33; code <= 126; code++) {
            int cell = code - 32;
            int glyph = code - 33;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 9; y++) {
                    boolean originalInk = x < starts.get(glyph + 1) - starts.get(glyph)
                            && (source.getRGB(starts.get(glyph) + x, y) & 0xFFFFFF) == 0;
                    assertEquals(originalInk, (atlas.getRGB(cell % 16 * 16 + x, cell / 16 * 9 + y) >>> 24) == 255,
                            "Altered glyph " + (char) code + " pixel " + x + "," + y);
                }
            }
        }
    }
}
