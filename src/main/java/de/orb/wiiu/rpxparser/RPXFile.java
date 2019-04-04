package de.orb.wiiu.rpxparser;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RPXFile {
    final ElfReader elf_reader;
    public static int MAX_FUNCTION_LENGTH_TO_COPY = 0;

    private static final int MIN_SYMBOLS = 10;

    public RPXFile(File f) throws IOException {
        this(Files.readAllBytes(f.toPath()));
    }

    public RPXFile(byte[] data) throws IOException {
        this(ByteBuffer.wrap(data));
    }

    public RPXFile(ByteBuffer buf) throws IOException {
        buf.position(0);
        elf_reader = new ElfReader(buf);
    }

    public List<ElfExport> getExports() {
        return elf_reader.sections() //
                .filter(section -> section instanceof ElfExportsTable) //
                .flatMap(m -> ((ElfExportsTable) m).stream()) //
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    public Map<String, List<RPLImport>> getImports() {
        return elf_reader.sections() //
                .filter(section -> section instanceof ElfRelocationTable) // We want to check ElfRelocationTable sections
                .flatMap(section -> ((ElfRelocationTable) section).stream()) // Get all relocations
                .flatMap(r -> r.symbol().isPresent() ? Stream.of(r.symbol().get()) : Stream.empty()) // Get symbols of relocations if existing
                .filter(symbol -> symbol.section().filter(s -> (s instanceof ElfImportsTable)).isPresent()) // Only keep symbols of ElfImportsTable section
                .map(symbol -> new RPLImport(symbol.name().orElseThrow(() -> new NoSuchElementException()),
                        ((ElfImportsTable) symbol.section().get()).rplname())) // Map to RPLImport
                .distinct() //
                .collect(Collectors.collectingAndThen( //
                        Collectors.groupingBy(RPLImport::getRplName, Collectors.toList()), // Group by RPLName
                        Collections::unmodifiableMap));
    }

    public Optional<byte[]> getFunctionData(ElfSymbol symbol) {
        return symbol.section().flatMap(section -> getFunctionData(section, symbol.value() - section.address(), (int) symbol.size()));
    }

    public Optional<byte[]> getFunctionData(ElfSection section, long _offset, int length) {
        if (_offset < section.address() || _offset > section.address() + section.size()) {
            return Optional.empty();
        }

        long offsetInSection = _offset - section.address();
        ByteBuffer buf = section.getSectionBuffer();

        buf.position((int) offsetInSection);

        byte[] data = new byte[(int) length];
        buf.get(data, 0, (int) length);

        return Optional.of(data);
    }

    public Optional<ElfSymbolTable> getSymbolTable() {
        return elf_reader.section(".symtab").map(section -> (ElfSymbolTable) section);
    }

    public Optional<ElfSection> getTextSection() {
        return elf_reader.section(".text");
    }

    public boolean hasSymbols() {
        return getFunctionSymbolsTextStream().limit(MIN_SYMBOLS).count() == MIN_SYMBOLS;
    }

    public Stream<ElfSymbol> getSymbols() {
        return getSymbolTable().map(st -> st.stream()).orElse(Stream.empty());
    }

    public Stream<ElfSymbol> getFunctionSymbolsTextStream() {
        return getSymbols().filter(s -> s.type() == ElfSymbol.STT_FUNC) // We are only interested in functions
                .filter(s -> s.name().map(name -> name).filter(name -> !name.isEmpty()).isPresent()) // Not interested in functions with an empty name
                .filter(s -> s.section().filter(m -> ".text".equals(m.name())).isPresent()); //
    }

    public List<ElfSymbol> getFunctionSymbolsText() {
        return getFunctionSymbolsTextStream().collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

}
