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

public class ElfRelocation {
    public static final int R_PPC_NONE = 0;

    final Optional<ElfSymbolTable> symtab;
    final long offset;
    final long info;
    final long addend;

    ElfRelocation(ByteBuffer buf, ElfRelocationTable rel, int offset, ElfReader reader) {
        this.symtab = rel.link().map(e -> (ElfSymbolTable) e);

        this.offset = buf.getInt(offset) & 0xffffffffL;
        this.info = buf.getInt(offset + 4) & 0xffffffffL;
        this.addend = rel.entrySize >= 12 ? buf.getInt(offset + 8) : 0;
    }

    public long offset() {
        return offset;
    }

    public Optional<ElfSymbol> symbol() {
        if (type() == R_PPC_NONE) {
            return Optional.empty();
        }
        return symtab.map(tab -> tab.symbol((int) ((info) >> 8)));
    }

    public int type() {
        return (int) info & 0xFF;
    }

    public long addend() {
        return addend;
    }

    @Override
    public String toString() {
        return symbol()//
                .map(s -> s.name().orElse("EMPTY_NAME") + '(' + type() + ')' + addend)//
                .orElse("NULL" + '(' + type() + ')' + addend);
    }
}
