import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import javax.imageio.ImageIO;

/** Run with `java scripts/GenerateSignatureFont.java` from the repository root. */
class GenerateSignatureFont {
    public static void main(String[] args) throws Exception {
        var source = ImageIO.read(new File("common/src/main/resources/assets/questlog_envelope/font/bitscript_r2.png"));
        var starts = new ArrayList<Integer>();
        // Only blue marks delimit glyphs. The black top of '$' is NOT a boundary.
        for (int x = 0; x < source.getWidth(); x++)
            if ((source.getRGB(x, 0) & 0xFFFFFF) == 0x0000FF) starts.add(x);
        starts.add(source.getWidth());
        if (starts.size() != 95) throw new IllegalStateException("Unexpected BitScript glyph boundaries");
        BufferedImage atlas = new BufferedImage(256, 54, BufferedImage.TYPE_INT_ARGB);
        for (int code = 33; code < 127; code++) {
            int cell = code - 32;
            int glyph = code - 33;
            for (int x = starts.get(glyph); x < starts.get(glyph + 1); x++)
                for (int y = 0; y < source.getHeight() - 1; y++)
                    if ((source.getRGB(x, y) & 0xFFFFFF) == 0)
                        atlas.setRGB(cell % 16 * 16 + x - starts.get(glyph), cell / 16 * 9 + y, 0xFFFFFFFF);
        }
        File target = new File("common/src/main/resources/assets/questlog_envelope/textures/font/signature.png");
        target.getParentFile().mkdirs();
        ImageIO.write(atlas, "png", target);
    }
}
