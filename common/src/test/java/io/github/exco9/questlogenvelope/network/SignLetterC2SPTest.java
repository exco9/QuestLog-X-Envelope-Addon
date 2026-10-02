package io.github.exco9.questlogenvelope.network;

import io.github.exco9.questlogenvelope.mail.LetterSignature;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SignLetterC2SPTest {
    @Test
    void exactSignatureAndHandSurviveNetworkRoundTrip() {
        var signature = new LetterSignature("Éléonore 😀", 0x315C82, 12, 14, 100, true, 0x22AACC);
        for (var hand : InteractionHand.values()) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                var packet = new SignLetterC2SP(hand, signature, java.util.UUID.randomUUID().toString());
                SignLetterC2SP.STREAM_CODEC.encode(buffer, packet);
                assertEquals(packet, SignLetterC2SP.STREAM_CODEC.decode(buffer));
                assertFalse(buffer.isReadable());
            } finally {
                buffer.release();
            }
        }
    }
}
