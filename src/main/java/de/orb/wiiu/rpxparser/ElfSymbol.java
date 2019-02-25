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

public class ElfSymbol {
    public static final byte STB_LOCAL = 0;
    public static final byte STB_GLOBAL = 1;
    public static final byte STB_WEAK = 2;
    public static final byte STB_LOPROC = 13;
    public static final byte STB_HIPROC = 15;

    public static final byte STT_NOTYPE = 0;
    public static final byte STT_OBJECT = 1;
    public static final byte STT_FUNC = 2;
    public static final byte STT_SECTION = 3;
    public static final byte STT_FILE = 4;
    public static final byte STT_LOPROC = 13;
    public static final byte STT_HIPROC = 15;
    public static final int SHN_LORESERVE = 0xff00; /* Start of reserved indices */
    public static final int SHN_HIRESERVE = 0xffff; /* End of reserved indices */

    final ElfReader reader;
    final Optional<ElfStringTable> strtab;
    final int nameIndex;
    final int info;
    final byte other;
    final int sectionIndex;
    final long value;
    final long size;

    ElfSymbol(ByteBuffer buf, Optional<ElfStringTable> strtab, int offset, ElfReader reader) {
        this.reader = reader;
        this.strtab = strtab;

        this.nameIndex = buf.getInt(offset);

        this.value = buf.getInt(offset + 4) & 0xffffffffL;
        this.size = buf.getInt(offset + 8) & 0xffffffffL;
        this.info = buf.get(offset + 12) & 0xff;
        this.other = buf.get(offset + 13);
        this.sectionIndex = buf.getShort(offset + 14) & 0xffff;
    }

    public Optional<String> name() {
        return strtab.map(s -> s.string(nameIndex));
    }

    public long value() {
        return value;
    }

    public long size() {
        return size;
    }

    public byte bind() {
        return (byte) (info >> 4);
    }

    public byte type() {
        return (byte) (info & 0xf);
    }

    public byte other() {
        return other;
    }

    public Optional<ElfSection> section() {
        if (sectionIndex >= SHN_LORESERVE && sectionIndex <= SHN_HIRESERVE) {
            return Optional.empty();
        }
        return Optional.ofNullable(reader.sections[sectionIndex]);
    }

    @Override
    public String toString() {
        return name().map(name -> + '@' + Long.toHexString(value)).orElse("EMPTY_NAME@" + Long.toHexString(value));
    }
}
