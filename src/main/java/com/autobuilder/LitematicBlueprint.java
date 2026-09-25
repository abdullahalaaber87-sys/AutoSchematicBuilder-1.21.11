package com.autobuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Reads block positions from a .litematic file. */
public final class LitematicBlueprint {
    private LitematicBlueprint() {}

    public record BlockEntry(int x, int y, int z, String blockId) {}

    public static List<BlockEntry> read(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))))) {
            if (in.readUnsignedByte() != 10) throw new IOException("Invalid NBT root");
            readString(in);
            Object rootObj = readPayload(in, 10);
            if (!(rootObj instanceof Map<?, ?> root)) throw new IOException("Invalid NBT root compound");
            Object regionsObj = root.get("Regions");
            if (!(regionsObj instanceof Map<?, ?> regions)) throw new IOException("No Regions tag in schematic");

            List<BlockEntry> result = new ArrayList<>();
            for (Object regionObj : regions.values()) {
                if (!(regionObj instanceof Map<?, ?> region)) continue;
                List<?> palette = asList(region.get("BlockStatePalette"));
                long[] states = asLongArray(region.get("BlockStates"));
                Map<?, ?> size = asMap(region.get("Size"));
                Map<?, ?> pos = asMap(region.get("Position"));
                if (palette == null || size == null || palette.isEmpty() || (states == null && palette.size() != 1)) continue;

                int rawX = number(size.get("x"));
                int rawY = number(size.get("y"));
                int rawZ = number(size.get("z"));
                int sx = Math.abs(rawX), sy = Math.abs(rawY), sz = Math.abs(rawZ);
                int px = pos == null ? 0 : number(pos.get("x"));
                int py = pos == null ? 0 : number(pos.get("y"));
                int pz = pos == null ? 0 : number(pos.get("z"));
                long volumeLong = (long) sx * sy * sz;
                if (volumeLong <= 0 || volumeLong > Integer.MAX_VALUE) continue;
                int volume = (int) volumeLong;
                int bits = Math.max(2, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
                long mask = (1L << bits) - 1L;

                for (int i = 0; i < volume; i++) {
                    int x = i % sx;
                    int z = (i / sx) % sz;
                    int y = i / (sx * sz);
                    int paletteIndex = palette.size() == 1 ? 0 : paletteIndex(states, i, bits, mask);
                    if (paletteIndex < 0 || paletteIndex >= palette.size()) continue;
                    Object entryObj = palette.get(paletteIndex);
                    if (!(entryObj instanceof Map<?, ?> entry)) continue;
                    Object nameObj = entry.get("Name");
                    if (!(nameObj instanceof String name)) continue;
                    if (name.equals("minecraft:air") || name.endsWith(":cave_air") || name.endsWith(":void_air") || name.equals("minecraft:nether_portal")) continue;

                    int ox = px + (rawX < 0 ? -x : x);
                    int oy = py + (rawY < 0 ? -y : y);
                    int oz = pz + (rawZ < 0 ? -z : z);
                    result.add(new BlockEntry(ox, oy, oz, name));
                }
            }

            result.sort(Comparator.comparingInt(BlockEntry::y)
                    .thenComparingInt(e -> Math.abs(e.x()) + Math.abs(e.z())));
            return result;
        }
    }

    private static int paletteIndex(long[] states, int index, int bits, long mask) {
        long bitIndex = (long) index * bits;
        int longIndex = (int) (bitIndex >>> 6);
        int startBit = (int) (bitIndex & 63);
        if (longIndex >= states.length) return -1;
        long value = states[longIndex] >>> startBit;
        int spill = startBit + bits - 64;
        if (spill > 0 && longIndex + 1 < states.length) value |= states[longIndex + 1] << (bits - spill);
        return (int) (value & mask);
    }

    private static int number(Object o) { return o instanceof Number n ? n.intValue() : 0; }
    private static Map<?, ?> asMap(Object o) { return o instanceof Map<?, ?> m ? m : null; }
    private static List<?> asList(Object o) { return o instanceof List<?> l ? l : null; }
    private static long[] asLongArray(Object o) { return o instanceof long[] a ? a : null; }

    private static Object readPayload(DataInputStream in, int type) throws IOException {
        return switch (type) {
            case 1 -> in.readByte();
            case 2 -> in.readShort();
            case 3 -> in.readInt();
            case 4 -> in.readLong();
            case 5 -> in.readFloat();
            case 6 -> in.readDouble();
            case 7 -> { int n=in.readInt(); byte[] a=new byte[n]; in.readFully(a); yield a; }
            case 8 -> readString(in);
            case 9 -> { int t=in.readUnsignedByte(), n=in.readInt(); List<Object> l=new ArrayList<>(Math.max(0,n)); for(int i=0;i<n;i++) l.add(readPayload(in,t)); yield l; }
            case 10 -> { Map<String,Object> m=new LinkedHashMap<>(); while(true){ int t=in.readUnsignedByte(); if(t==0) break; String name=readString(in); m.put(name,readPayload(in,t)); } yield m; }
            case 11 -> { int n=in.readInt(); int[] a=new int[n]; for(int i=0;i<n;i++) a[i]=in.readInt(); yield a; }
            case 12 -> { int n=in.readInt(); long[] a=new long[n]; for(int i=0;i<n;i++) a[i]=in.readLong(); yield a; }
            default -> throw new IOException("Unsupported NBT tag: " + type);
        };
    }

    private static String readString(DataInputStream in) throws IOException {
        int len = in.readUnsignedShort();
        byte[] b = new byte[len]; in.readFully(b);
        return new String(b, StandardCharsets.UTF_8);
    }
}
