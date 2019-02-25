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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public class ElfReader {
    public static final short ET_NONE = 0;
    public static final short ET_REL = 1;
    public static final short ET_EXEC = 2;
    public static final short ET_DYN = 3;
    public static final short ET_CORE = 4;
    public static final short ET_NUM = 5;
    public static final short ET_LOOS = (short) 0xfe00;
    public static final short ET_HIOS = (short) 0xfeff;
    public static final short ET_LOPROC = (short) 0xff00;
    public static final short ET_HIPROC = (short) 0xffff;

    public static final short EM_NONE = 0;
    public static final short EM_SPARC = 2;
    public static final short EM_386 = 3;
    public static final short EM_PPC = 20;
    public static final short EM_PPC64 = 21;
    public static final short EM_ARM = 40;
    public static final short EM_SPARCV9 = 43;
    public static final short EM_IA_64 = 50;
    public static final short EM_X86_64 = 62;
    public static final short EM_AARCH64 = 183;

    public static final byte ELFOSABI_SYSV = 0;
    public static final byte ELFOSABI_HPUX = 1;
    public static final byte ELFOSABI_NETBSD = 2;
    public static final byte ELFOSABI_GNU = 3;
    public static final byte ELFOSABI_SOLARIS = 6;
    public static final byte ELFOSABI_AIX = 7;
    public static final byte ELFOSABI_IRIX = 8;
    public static final byte ELFOSABI_FREEBSD = 9;
    public static final byte ELFOSABI_TRU64 = 10;
    public static final byte ELFOSABI_MODESTO = 11;
    public static final byte ELFOSABI_OPENBSD = 12;
    public static final byte ELFOSABI_ARM_AEABI = 64;
    public static final byte ELFOSABI_ARM = 97;
    public static final byte ELFOSABI_STANDALONE = -1;

    final ByteBuffer buf;
    final byte abi;
    final byte abiVersion;
    final short type;
    final short machine;
    final int version;
    final int flags;
    final long entry;
    final ElfSection[] sections;
    final ElfStringTable strtab;

    public static ElfReader create(String fileName) throws IOException {
        return ElfReader.create(new File(fileName));
    }

    public static ElfReader create(File file) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        try {
            return new ElfReader(raf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, raf.length()));
        } finally {
            raf.close();
        }
    }

    public ElfReader(ByteBuffer buf) throws IOException {
        this.buf = buf;
        byte[] ident = new byte[16];
        buf.get(ident);
        if (ident[0] != 0x7f || ident[1] != 'E' || ident[2] != 'L' || ident[3] != 'F') {
            throw new ElfException("Invalid ELF signature");
        }

        switch (ident[4]) {
        case 1:
            break;
        case 2:
            throw new ElfException("ELF64 not supported");
        default:
            throw new ElfException("Invalid ELF class");
        }

        switch (ident[5]) {
        case 1:
            buf.order(ByteOrder.LITTLE_ENDIAN);
            break;
        case 2:
            buf.order(ByteOrder.BIG_ENDIAN);
            break;
        default:
            throw new ElfException("Invalid ELF endian");
        }

        if (ident[6] != 1) {
            throw new ElfException("Invalid ELF version");
        }

        this.abi = ident[7];
        this.abiVersion = ident[8];

        this.type = buf.getShort(16);
        this.machine = buf.getShort(18);
        this.version = buf.getInt(20);

        this.entry = buf.getInt(24) & 0xffffffffL;
        this.flags = buf.getInt(36);
        this.sections = readSections(buf.getInt(32), buf.getShort(46) & 0xffff, buf.getShort(48) & 0xffff);
        this.strtab = (ElfStringTable) sections[buf.getShort(50) & 0xffff];
        if (strtab == null) {
            throw new ElfException(".strtab section was null");
        }
    }

    private ElfSection[] readSections(int start, int entrySize, int entries) {
        ElfSection[] sections = new ElfSection[entries];
        for (int i = 0; i < entries; i++) {
            sections[i] = ElfSection.read(this, start + i * entrySize);
        }
        return sections;
    }

    protected Stream<ElfSection> sections() {
        return Arrays.stream(sections).filter(s -> s != null);
    }

    public Optional<ElfSection> section(String name) {
        return sections().filter(section -> section.name().equals(name)).findFirst();
    }

    public ByteOrder endian() {
        return buf.order();
    }

    public byte abi() {
        return abi;
    }

    public byte abiVersion() {
        return abiVersion;
    }

    public short type() {
        return type;
    }

    public short machine() {
        return machine;
    }

    public int version() {
        return version;
    }

    public int flags() {
        return flags;
    }

    public long entry() {
        return entry;
    }
}
