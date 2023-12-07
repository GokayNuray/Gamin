package com.example.gamin.Minecraft;

import com.example.gamin.Utils.PacketUtils;

import java.util.Objects;

public class ChunkColumn {
    public short[][][][] chunk = new short[16][16][16][16];
    public long pos;
    public short bitmask;

    public static short getBlock(int x, int y, int z) {
        if (y >= 0 && y < 256) {
            long getPos;
            getPos = ((long) Math.floor((float) x / 16) << 32) | (((int) Math.floor((float) z / 16) & 0xffffffffL));
            if (PacketUtils.chunkColumnMap.containsKey(getPos)) {
                return Objects.requireNonNull(PacketUtils.chunkColumnMap.get(getPos)).chunk[y / 16][Math.floorMod(x, 16)][Math.floorMod(y, 16)][Math.floorMod(z, 16)];
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    public static byte getBlockId(short block) {
        return (byte) ((block & 0x000f) * 16 + (block & 0xf000) / 0x1000);
    }

    public static byte getBlockMetaData(short block) {
        return (byte) ((block & 0x00f0) + (block & 0x0f00) / 0x0100);
    }

    public static void setBlock(int x, int y, int z, short block) {
        long getPos;
        getPos = ((long) Math.floor((float) x / 16) << 32) | (((int) Math.floor((float) z / 16) & 0xffffffffL));
        if (PacketUtils.chunkColumnMap.containsKey(getPos)) {
            Objects.requireNonNull(PacketUtils.chunkColumnMap.get(getPos)).chunk[y / 16][Math.floorMod(x, 16)][Math.floorMod(y, 16)][Math.floorMod(z, 16)] = block;
        }
    }
}
