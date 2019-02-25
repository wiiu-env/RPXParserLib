/*
 * Copyright 2016 Odnoklassniki Ltd, Mail.Ru Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.orb.wiiu.rpxparser;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class ElfSection {
    public static final int SHT_NULL = 0;
    public static final int SHT_PROGBITS = 1;
    public static final int SHT_SYMTAB = 2;
    public static final int SHT_STRTAB = 3;
    public static final int SHT_RELA = 4;
    public static final int SHT_HASH = 5;
    public static final int SHT_DYNAMIC = 6;
    public static final int SHT_NOTE = 7;
    public static final int SHT_NOBITS = 8;
    public static final int SHT_REL = 9;
    public static final int SHT_SHLIB = 10;
    public static final int SHT_DYNSYM = 11;
    public static final int SHT_LOPROC = 0x70000000;
    public static final int SHT_HIPROC = 0x7fffffff;
    public static final int SHT_LOUSER = 0x80000000;
    public static final int SHT_RPL_EXPORTS = 0x80000001;
    public static final int SHT_RPL_IMPORTS = 0x80000002;
    public static final int SHT_HIUSER = 0xffffffff;

    final ElfReader reader;
    final int nameIndex;
    final int type;
    final long flags;
    final long address;
    final long offset;
    final long orgSize; // the inside the .elf. may be the size of the compressed section
    final long size; // the true size of the section.
    final int linkIndex;
    final int info;
    final long align;
    final long entrySize;

    ElfSection(ElfReader reader, int offset) {
        ByteBuffer buf = reader.buf;
        this.reader = reader;
        this.nameIndex = buf.getInt(offset);
        this.type = buf.getInt(offset + 4);

        this.flags = buf.getInt(offset + 8) & 0xffffffffL;
        this.address = buf.getInt(offset + 12) & 0xffffffffL;
        this.offset = buf.getInt(offset + 16) & 0xffffffffL;
        this.orgSize = buf.getInt(offset + 20) & 0xffffffffL;
        this.linkIndex = buf.getInt(offset + 24);
        this.info = buf.getInt(offset + 28);
        this.align = buf.getInt(offset + 32);
        this.entrySize = buf.getInt(offset + 36);

        long tmp_size = orgSize;
        // Fix the size of section when its compressed.
        if ((flags & RPX_SHDR_ZLIB_FLAG) == RPX_SHDR_ZLIB_FLAG) {
            buf.position((int) this.offset);
            tmp_size = buf.getInt();
        }
        size = tmp_size;
    }

    public String name() {
        // we can be sure the strtab is NOT null.
        return reader.strtab.string(nameIndex);
    }

    public int type() {
        return type;
    }

    public long flags() {
        return flags;
    }

    public long address() {
        return address;
    }

    public long offset() {
        return offset;
    }

    public long size() {
        return size;
    }

    public Optional<ElfSection> link() {
        return Optional.ofNullable(reader.sections[linkIndex]);
    }

    public int info() {
        return info;
    }

    public long align() {
        return align;
    }

    public long entrySize() {
        return entrySize;
    }

    public int count() {
        return entrySize == 0 ? 0 : (int) (size / entrySize);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + name() + ')';
    }

    ByteBuffer curBuffer = null;

    public ByteBuffer getSectionBuffer() {
        if (curBuffer != null) {
            return curBuffer;
        }
        ByteBuffer buf = reader.buf;
        buf.position((int) offset);
        byte[] data = new byte[(int) orgSize];
        buf.get(data, 0, (int) orgSize);

        if ((flags & RPX_SHDR_ZLIB_FLAG) == RPX_SHDR_ZLIB_FLAG) {
            long section_size_inflated = buf.getInt((int) offset) & 0xFFFFFFFF;
            Inflater inflater = new Inflater();
            inflater.setInput(data, 4, (int) orgSize - 4); // the first byte is the size

            byte[] decompressed = new byte[(int) section_size_inflated];

            try {
                inflater.inflate(decompressed);
            } catch (DataFormatException e) {
                // TODO
                e.printStackTrace();
            }

            inflater.end();
            data = decompressed;
        }

        curBuffer = ByteBuffer.wrap(data).order(buf.order());
        return curBuffer;
    }

    public static final int RPX_SHDR_ZLIB_FLAG = 0x08000000;

    static ElfSection read(ElfReader reader, int offset) {
        int type = reader.buf.getInt(offset + 4);

        switch (type) {
        case SHT_NULL:
            return null;
        case SHT_SYMTAB:
        case SHT_DYNSYM:
            return new ElfSymbolTable(reader, offset);
        case SHT_STRTAB:
            return new ElfStringTable(reader, offset);
        case SHT_RELA:
        case SHT_REL:
            return new ElfRelocationTable(reader, offset);
        case SHT_RPL_IMPORTS:
            return new ElfImportsTable(reader, offset);
        case SHT_RPL_EXPORTS:
            return new ElfExportsTable(reader, offset);
        default:
            return new ElfSection(reader, offset);
        }
    }
}
