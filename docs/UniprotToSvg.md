# UniprotToSvg

![Last commit](https://img.shields.io/github/last-commit/lindenb/jvarkit.png)

plot uniprot to SVG


## Usage

```
Usage: java -jar dist/uniprot2svg.jar  [options] Files
Usage: uniprot2svg [options] Files
  Options:
    --exclude-type
      Exclude feature/@type matching that regular expression
    -h, --help
      print help and exit
    --helpFormat
      What kind of help. One of [usage,markdown,xml].
    --include-type
      Only feature/@type matching that regular expression
  * -o, --output
      An existing directory or a filename ending with the '.zip' or '.tar' or 
      '.tar.gz' suffix.
    --pedigree
      A pedigree file. tab delimited. Columns: family,id,father,mother, 
      sex:(0:unknown;1|male|M:male;2|female|F:female), phenotype 
      (-9|?|.:unknown;1|affected|case:affected;0|unaffected|control:unaffected) 
    --svg
      produce SVG only (default is HTML+SVG)
      Default: false
    --vcf
      annotated VCF. Whole file is loaded in memory, so you'd better use a 
      small vcf restricted to your proteins.
    --version
      print version and exit

```


## Keywords

 * uniprot
 * svg


## Compilation

### Requirements / Dependencies

* java [compiler SDK 11](https://jdk.java.net/11/). Please check that this java is in the `${PATH}`. Setting JAVA_HOME is not enough : (e.g: https://github.com/lindenb/jvarkit/issues/23 )


### Download and Compile

```bash
$ git clone "https://github.com/lindenb/jvarkit.git"
$ cd jvarkit
$ ./gradlew uniprot2svg
```

The java jar file will be installed in the `dist` directory.


## Creation Date

20220608

## Source code 

[https://github.com/lindenb/jvarkit/tree/master/src/main/java/com/github/lindenb/jvarkit/tools/uniprot/UniprotToSvg.java](https://github.com/lindenb/jvarkit/tree/master/src/main/java/com/github/lindenb/jvarkit/tools/uniprot/UniprotToSvg.java)


## Contribute

- Issue Tracker: [http://github.com/lindenb/jvarkit/issues](http://github.com/lindenb/jvarkit/issues)
- Source Code: [http://github.com/lindenb/jvarkit](http://github.com/lindenb/jvarkit)

## License

The project is licensed under the MIT license.

## Citing

Should you cite **uniprot2svg** ? [https://github.com/mr-c/shouldacite/blob/master/should-I-cite-this-software.md](https://github.com/mr-c/shouldacite/blob/master/should-I-cite-this-software.md)

The current reference is:

[http://dx.doi.org/10.6084/m9.figshare.1425030](http://dx.doi.org/10.6084/m9.figshare.1425030)

> Lindenbaum, Pierre (2015): JVarkit: java-based utilities for Bioinformatics. figshare.
> [http://dx.doi.org/10.6084/m9.figshare.1425030](http://dx.doi.org/10.6084/m9.figshare.1425030)

