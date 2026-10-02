package io.github.exco9.questlogenvelope.client;

import io.github.exco9.questlogenvelope.mail.LetterSignature;
import io.github.exco9.questlogenvelope.mail.QuestMagicCircle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SignatureLayoutTest {
    @Test
    void entireSignatureStaysOnPaperAtEverySizeAndEdge() {
        for (int size = LetterSignature.MIN_SIZE; size <= LetterSignature.MAX_SIZE; size++) {
            for (int measuredWidth : new int[]{0, 10, 60, 140, 900}) {
                for (int coordinate : new int[]{-100, 0, 40, 900}) {
                    var signature = new LetterSignature("Éléonore", 0, size, coordinate, coordinate, true);
                    var box = SignatureRenderer.layout(measuredWidth, signature, 17, 21);
                    assertTrue(box.x() >= 17);
                    assertTrue(box.y() >= 21);
                    assertTrue(box.x() + box.width() <= 17 + QuestMagicCircle.WRITABLE_WIDTH);
                    assertTrue(box.y() + box.height() <= 21 + QuestMagicCircle.WRITABLE_HEIGHT);
                    assertTrue(box.scale() > 0);
                }
            }
        }
    }

    @Test
    void longNamesFitAndUnframedSignaturesHaveIdenticalPlacement() {
        var framed = new LetterSignature("A long signatory name", 0, 28, 90, 120, true);
        var plain = new LetterSignature(framed.text(), framed.color(), framed.size(), framed.x(), framed.y(), false);
        var box = SignatureRenderer.layout(500, framed, 0, 0);
        assertEquals(QuestMagicCircle.WRITABLE_WIDTH, box.width());
        assertEquals(box, SignatureRenderer.layout(500, plain, 0, 0));
        assertTrue(box.contains(box.x(), box.y()));
        assertFalse(box.contains(box.x() + box.width(), box.y()));
    }

    @Test
    void frameHugsOneLineInsteadOfLeavingAnEmptySecondLine() {
        var signature = new LetterSignature("test", 0, 28, 0, 0, true);
        var box = SignatureRenderer.layout(20, signature, 0, 0);
        assertEquals(31, box.height());
        assertTrue(box.height() < signature.size() * 1.5);
        var normal = SignatureRenderer.layout(20, LetterSignature.EMPTY, 0, 0);
        assertEquals(13, normal.height());
    }

    @Test
    void normalSignatureScalingUsesWholeScreenPixels() {
        for (int guiScale = 1; guiScale <= 4; guiScale++) {
            for (int size = 8; size <= 28; size++) {
                var box = SignatureRenderer.layout(40, new LetterSignature("Arwen", 0, size, 0, 0, false), 0, 0, guiScale);
                if (box.scale() * guiScale >= 1) {
                    assertEquals(Math.rint(box.scale() * guiScale), box.scale() * guiScale, 0.0001);
                }
            }
        }
    }
}
