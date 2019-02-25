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

import java.util.Iterator;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ElfSymbolTable extends ElfSection implements Iterable<ElfSymbol> {

    ElfSymbolTable(ElfReader reader, int offset) {
        super(reader, offset);
    }

    public ElfSymbol symbol(int index) {
        return new ElfSymbol(getSectionBuffer(), link().map(e -> (ElfStringTable) e), (int) (index * entrySize), reader);
    }

    public Optional<ElfSymbol> symbol(String name) {
        return stream() //
                .filter(n -> n.name().filter(s_name -> s_name.equals(name)).isPresent()) //
                .findAny();
    }

    public Stream<ElfSymbol> stream() {
        return IntStream.range(0, count()).mapToObj(i -> symbol(i));
    }

    @Override
    public Iterator<ElfSymbol> iterator() {
        return new Iterator<ElfSymbol>() {
            private final int count = count();
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < count;
            }

            @Override
            public ElfSymbol next() {
                return symbol(index++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
