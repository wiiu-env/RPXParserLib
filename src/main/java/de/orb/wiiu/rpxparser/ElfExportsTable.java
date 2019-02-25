package de.orb.wiiu.rpxparser;

import java.util.Iterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ElfExportsTable extends ElfSection implements Iterable<ElfExport> {

    ElfExportsTable(ElfReader reader, int offset) {
        super(reader, offset);
    }

    private ElfExport export(int index) {
        return new ElfExport(getSectionBuffer(), 8 + index * 8, name().startsWith(".d"));
    }

    @Override
    public int count() {
        return getSectionBuffer().getInt(0);
    }

    public Stream<ElfExport> stream() {
        return IntStream.range(0, count()).mapToObj(i -> export(i));
    }

    @Override
    public Iterator<ElfExport> iterator() {
        return new Iterator<ElfExport>() {
            private final int count = count();
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < count;
            }

            @Override
            public ElfExport next() {
                return export(index++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

}
