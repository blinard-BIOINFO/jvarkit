# SamRemoveDuplicatedNames

![Last commit](https://img.shields.io/github/last-commit/lindenb/jvarkit.png)

remove duplicated names in sorted BAM


## Usage

```
Usage: java -jar dist/samrmdupnames.jar  [options] Files
Usage: samrmdupnames [options] Files
  Options:
    --bamcompression
      Compression Level. 0: no compression. 9: max compression;
      Default: 5
    -h, --help
      print help and exit
    --helpFormat
      What kind of help. One of [usage,markdown,xml].
    -o, --out
      Output file. Optional . Default: stdout
    -R, --reference
      Indexed fasta Reference file. This file must be indexed with samtools 
      faidx and with picard CreateSequenceDictionary
    --report
      Only report duplicate names in the output bam file
      Default: false
    --samoutputformat
      Sam output format.
      Default: SAM
      Possible Values: [BAM, SAM, CRAM]
    --validation-stringency
      SAM Reader Validation Stringency
      Default: LENIENT
      Possible Values: [STRICT, LENIENT, SILENT]
    --version
      print version and exit

```


## Keywords

 * sam
 * bam


## Compilation

### Requirements / Dependencies

* java [compiler SDK 11](https://jdk.java.net/11/). Please check that this java is in the `${PATH}`. Setting JAVA_HOME is not enough : (e.g: https://github.com/lindenb/jvarkit/issues/23 )


### Download and Compile

```bash
$ git clone "https://github.com/lindenb/jvarkit.git"
$ cd jvarkit
$ ./gradlew samrmdupnames
```

The java jar file will be installed in the `dist` directory.


## Creation Date

20221207

## Source code 

[https://github.com/lindenb/jvarkit/tree/master/src/main/java/com/github/lindenb/jvarkit/tools/samrmdupnames/SamRemoveDuplicatedNames.java](https://github.com/lindenb/jvarkit/tree/master/src/main/java/com/github/lindenb/jvarkit/tools/samrmdupnames/SamRemoveDuplicatedNames.java)


## Contribute

- Issue Tracker: [http://github.com/lindenb/jvarkit/issues](http://github.com/lindenb/jvarkit/issues)
- Source Code: [http://github.com/lindenb/jvarkit](http://github.com/lindenb/jvarkit)

## License

The project is licensed under the MIT license.

## Citing

Should you cite **samrmdupnames** ? [https://github.com/mr-c/shouldacite/blob/master/should-I-cite-this-software.md](https://github.com/mr-c/shouldacite/blob/master/should-I-cite-this-software.md)

The current reference is:

[http://dx.doi.org/10.6084/m9.figshare.1425030](http://dx.doi.org/10.6084/m9.figshare.1425030)

> Lindenbaum, Pierre (2015): JVarkit: java-based utilities for Bioinformatics. figshare.
> [http://dx.doi.org/10.6084/m9.figshare.1425030](http://dx.doi.org/10.6084/m9.figshare.1425030)



### Examples


#### Example 1


```

java -jar  dist/samgrep.jar -R r001  -- samtools-0.1.18/examples/toy.sam 

@HD     VN:1.4  SO:unsorted
@SQ     SN:ref  LN:45
@SQ     SN:ref2 LN:40
@PG     ID:0    PN:com.github.lindenb.jvarkit.tools.samgrep.SamGrep     VN:dac03b80e9fd88a15648b22550e57d10c9bed725     CL:-R r001 samtools-0.1.18/examples/toy.sam
r001    163     ref     7       30      8M4I4M1D3M      =       37      39      TTAGATAAAGAGGATACTG     *       XX:B:S,12561,2,20,112
r001    83      ref     37      30      9M      =       7       -39     CAGCGCCAT       *

```


#### Example 4


```

java -jar  dist/samgrep.jar -R r001 -- -n 1 samtools-0.1.18/examples/toy.sam 

@HD     VN:1.4  SO:unsorted
@SQ     SN:ref  LN:45
@SQ     SN:ref2 LN:40
@PG     ID:0    PN:com.github.lindenb.jvarkit.tools.samgrep.SamGrep     VN:dac03b80e9fd88a15648b22550e57d10c9bed725     CL:-R r001 -n 1 samtools-0.1.18/examples/toy.sam
r001    163     ref     7       30      8M4I4M1D3M      =       37      39      TTAGATAAAGAGGATACTG     *       XX:B:S,12561,2,20,112

```







### Motivation

I got a BAM file with the same read duplicated. They have the same position, the same flags


