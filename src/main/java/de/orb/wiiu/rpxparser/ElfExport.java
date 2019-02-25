package de.orb.wiiu.rpxparser;

import java.nio.ByteBuffer;

public class ElfExport {

    private final long offset;
    private final int nameIndex;
    private final ByteBuffer buf;
    private final boolean isData;

    // Export SHT_RPL_EXPORTS name TLS flag
    private static final int EXN_RPL_TLS = 0x80000000;

    public ElfExport(ByteBuffer buf, int offset, boolean isData) {
        this.buf = buf;
        this.offset = buf.getInt(offset + 0) & 0xFFFFFFFFL;
        this.nameIndex = buf.getInt(offset + 4) & ~EXN_RPL_TLS;
        this.isData = isData;
    }

    public String name() {
        int pos = nameIndex;
        StringBuilder result = new StringBuilder();

        for (byte b; (b = buf.get(pos)) != 0; pos++) {
            result.append((char) b);
        }

        return result.toString();
    }

    public long offset() {
        return offset;
    }

    public boolean isData() {
        return isData;
    }

    @Override
    public String toString() {
        return String.format("%s@%08X(%s)", name(), offset(), (isData() ? "data" : "function"));
    }

}
