package com.autobuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Minimal .litematic reader used only for the material list. */
public final class LitematicMaterials {
    private LitematicMaterials() {}

    public static Map<String, Long> read(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))))) {
            int rootType = in.readUnsignedByte();
            if (rootType != 10) throw new IOException("Invalid NBT root");
            readString(in); // root name
            Object rootObj = readPayload(in, 10);
            if (!(rootObj instanceof Map<?, ?> root)) throw new IOException("Invalid NBT root compound");
            Object regionsObj = root.get("Regions");
            if (!(regionsObj instanceof Map<?, ?> regions)) throw new IOException("No Regions tag in schematic");

            Map<String, Long> result = new HashMap<>();
            for (Object regionObj : regions.values()) {
                if (!(regionObj instanceof Map<?, ?> region)) continue;
                List<?> palette = asList(region.get("BlockStatePalette"));
                long[] states = asLongArray(region.get("BlockStates"));
                Map<?, ?> size = asMap(region.get("Size"));
                if (palette == null || states == null || size == null || palette.isEmpty()) continue;

                int sx = Math.abs(number(size.get("x")));
                int sy = Math.abs(number(size.get("y")));
                int sz = Math.abs(number(size.get("z")));
                long volumeLong = (long) sx * sy * sz;
                if (volumeLong <= 0 || volumeLong > Integer.MAX_VALUE) continue;
                int volume = (int) volumeLong;
                int bits = Math.max(2, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
                long mask = (1L << bits) - 1L;

                for (int i = 0; i < volume; i++) {
                    long bitIndex = (long) i * bits;
                    int longIndex = (int) (bitIndex >>> 6);
                    int startBit = (int) (bitIndex & 63);
                    if (longIndex >= states.length) break;
                    long value = states[longIndex] >>> startBit;
                    int spill = startBit + bits - 64;
                    if (spill > 0 && longIndex + 1 < states.length) value |= states[longIndex + 1] << (bits - spill);
                    int paletteIndex = (int) (value & mask);
                    if (paletteIndex < 0 || paletteIndex >= palette.size()) continue;
                    Object entryObj = palette.get(paletteIndex);
                    if (!(entryObj instanceof Map<?, ?> entry)) continue;
                    Object nameObj = entry.get("Name");
                    if (!(nameObj instanceof String name) || name.equals("minecraft:air") || name.endsWith(":cave_air") || name.endsWith(":void_air") || name.equals("minecraft:nether_portal")) continue;
                    result.merge(name, 1L, Long::sum);
                }
            }
            return result;
        }
    }

    private static int number(Object o) { return o instanceof Number n ? n.intValue() : 0; }
    @SuppressWarnings("unchecked") private static Map<?, ?> asMap(Object o) { return o instanceof Map<?, ?> m ? m : null; }
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
