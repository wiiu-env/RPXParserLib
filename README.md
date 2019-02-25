# RPXParserLib - A Java RPX/RPL parser library

A library to parse the executables of the the Wii U.

Example usages:
```
// Load RPX/RPL
RPXFile rpxFile = new RPXFile(new File("test.rpx"));

// Get all function symbols from the .text section
boolean hasSymbols = rpxFile.hasSymbols(); // Check if the file is not stripped.
List<ElfSymbol> functionSymbols = rpxFile.getFunctionSymbolsText();
// Get the data for a given ElfSymbol.
Optional<byte[]> functionData = rpxFile.getFunctionData(symbol);

// Get all imports
Map<String, List<RPLImport>> imports = rpxFile.getImports();

// Get all exports
List<ElfExport> exports = rpxFile.getImports();
```

# Use in projects
This library can be easily used via [jitpack.io](https://jitpack.io/) .  
In the following the usage with maven will be explained. 
Please take a look at the jitpack website for information on usage with other tools.  

Add the jitpack repository to the `pom.xml`
```
<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```
Then add the library as an dependency.
```
<dependency>
    <groupId>com.github.wiiu-env</groupId>
    <artifactId>RPXParserLib</artifactId>
    <version></version>
</dependency>
```
As the version, you can use any tag or hash of this repository.

# Credits
Based on: https://github.com/odnoklassniki/one-elf
References:  
- https://github.com/decaf-emu/ida_rpl_loader
- https://github.com/aerosoul94/ida_gel/tree/master/src/wiiu
- https://github.com/Relys/rpl2elf