package com.example.gamin.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
public final class  VarInt {
    private static final int SEGMENT_BITS = 0x7F;
    private static final int CONTINUE_BIT = 0x80;

    public static List<Byte> writeVarInt(int value) {
        List<Byte> sonc = new ArrayList<>();
        while (true) {
            if ((value & ~SEGMENT_BITS) == 0) {
                sonc.add((byte) value);
                return sonc;
            }
            sonc.add((byte) ((value & SEGMENT_BITS) | CONTINUE_BIT));
            value >>>= 7;
        }
    }
    public static int readVarInt(InputStream in) throws IOException {
        int i = 0;
        int j = 0;
        while (true) {
            int k = in.read();
            i |= (k & 0x7F) << j++ * 7;
            //if (j > 5) throw new RuntimeException("VarInt too big");
            if ((k & 0x80) != 128) break;
        }
        return i;
    }

}


