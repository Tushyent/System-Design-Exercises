package com.library.shorturl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTest {

    @Test
    @DisplayName("Should encode and decode numbers accurately")
    void testEncodeDecode() {
        long[] testValues = {0L, 1L, 61L, 62L, 125L, 1002345L, 9876543210L};

        for (long val : testValues) {
            String encoded = Base62Encoder.encode(val);
            assertNotNull(encoded);
            assertFalse(encoded.isBlank());

            long decoded = Base62Encoder.decode(encoded);
            assertEquals(val, decoded, "Decoded value must match original number for: " + val);
        }
    }
}
