package de.orb.wiiu.rpxparser;

import java.nio.ByteBuffer;

public class ElfImportsTable extends ElfRelocationTable {

    ElfImportsTable(ElfReader reader, int offset) {
        super(reader, offset);
    }

    public String rplname() {
        ByteBuffer buf = getSectionBuffer();
        int pos = (int) 8;

        StringBuilder result = new StringBuilder();

        for (byte b; (b = buf.get(pos)) != 0; pos++) {
            result.append((char) b);
        }
        return result.toString();
    }

}
