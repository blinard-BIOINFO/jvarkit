# VcfSplitNVariants

![Last commit](https://img.shields.io/github/last-commit/lindenb/jvarkit.png)

Split VCF to 'N' VCF files 


## Usage

```
Usage: java -jar dist/vcfsplitnvariants.jar  [options] Files
Usage: vcfsplitnvariants [options] Files
  Options:
    -f, --force
      overwrite existing files
      Default: false
    -h, --help
      print help and exit
    --helpFormat
      What kind of help. One of [usage,markdown,xml].
    --tbi, --index
      index on the fly as tbi files
      Default: false
    -m, --manifest
      write optional manifest to that file.
  * -o, --output, --prefix
      files prefix
    --variants-count
      number of variants. Or use --vcf-count
      Default: -1
    --vcf-count
      number of output vcf files. Or use --variants-count
      Default: -1
    --version
      print version and exit

```


## Keywords

 * vcf


## Compilation

### Requirements / Dependencies

* java [compiler SDK 11](https://jdk.java.net/11/). Please check that this java is in the `${PATH}`. Setting JAVA_HOME is not enough : (e.g: https://github.com/lindenb/jvarkit/issues/23 )


### Download and Compile

```bash
$ git clone "https://github.com/lindenb/jvarkit.git"
$ cd jvarkit
$ ./gradlew vcfsplitnvariants
```

The java jar file will be installed in the `dist` directory.


## Creation Date

202221122

## Source code 

[https://github.com/lindenb/jvarkit/tree/master/src/main/java/com/github/lindenb/jvarkit/tools/vcfsplit/VcfSplitNVariants.java](https://github.com/lindenb/jvarkit/tree/master/src/main/java/com/github/lindenb/jvarkit/tools/vcfsplit/VcfSplitNVariants.java)


## Contribute

- Issue Tracker: [http://github.com/lindenb/jvarkit/issues](http://github.com/lindenb/jvarkit/issues)
- Source Code: [http://github.com/lindenb/jvarkit](http://github.com/lindenb/jvarkit)

## License

The project is licensed under the MIT license.

## Citing

Should you cite **vcfsplitnvariants** ? [https://github.com/mr-c/shouldacite/blob/master/should-I-cite-this-software.md](https://github.com/mr-c/shouldacite/blob/master/should-I-cite-this-software.md)

The current reference is:

[http://dx.doi.org/10.6084/m9.figshare.1425030](http://dx.doi.org/10.6084/m9.figshare.1425030)

> Lindenbaum, Pierre (2015): JVarkit: java-based utilities for Bioinformatics. figshare.
> [http://dx.doi.org/10.6084/m9.figshare.1425030](http://dx.doi.org/10.6084/m9.figshare.1425030)


# motivation

Split VCF to 'N' VCF files

# example

```
$ java -jar dist/vcfsplitnvariants.jar --vcf-count 10 src/test/resources/rotavirus_rf.vcf.gz -o SPLITVCF --manifest jeter.mf --index -R src/test/resources/rotavirus_rf.fa
[INFO][VcfSplitNVariants]open SPLITVCF.00001.vcf.gz
[INFO][VcfSplitNVariants]open SPLITVCF.00002.vcf.gz
[INFO][VcfSplitNVariants]open SPLITVCF.00003.vcf.gz
[INFO][VcfSplitNVariants]open SPLITVCF.00004.vcf.gz
[INFO][VcfSplitNVariants]open SPLITVCF.00005.vcf.gz
[INFO][VcfSplitNVariants]open SPLITVCF.00006.vcf.gz
[INFO][VcfSplitNVariants]open SPLITVCF.00007.vcf.gz
[INFO][VcfSplitNVariants]open SPLITVCF.00008.vcf.gz
[INFO][VcfSplitNVariants]open SPLITVCF.00009.vcf.gz
[INFO][VcfSplitNVariants]open SPLITVCF.00010.vcf.gz

$ cat jeter.mf
vcf	count	contigs
/home/lindenb/src/jvarkit-git/SPLITVCF.00001.vcf.gz	5	RF01,RF03,RF04,RF06,RF09
/home/lindenb/src/jvarkit-git/SPLITVCF.00002.vcf.gz	5	RF02,RF03,RF05,RF06,RF10
/home/lindenb/src/jvarkit-git/SPLITVCF.00003.vcf.gz	5	RF02,RF03,RF05,RF07,RF10
/home/lindenb/src/jvarkit-git/SPLITVCF.00004.vcf.gz	5	RF02,RF03,RF05,RF07,RF10
/home/lindenb/src/jvarkit-git/SPLITVCF.00005.vcf.gz	5	RF02,RF04,RF05,RF07,RF11
/home/lindenb/src/jvarkit-git/SPLITVCF.00006.vcf.gz	4	RF02,RF04,RF05,RF07
/home/lindenb/src/jvarkit-git/SPLITVCF.00007.vcf.gz	4	RF03,RF04,RF05,RF08
/home/lindenb/src/jvarkit-git/SPLITVCF.00008.vcf.gz	4	RF03,RF04,RF06,RF08
/home/lindenb/src/jvarkit-git/SPLITVCF.00009.vcf.gz	4	RF03,RF04,RF06,RF09
/home/lindenb/src/jvarkit-git/SPLITVCF.00010.vcf.gz	4	RF03,RF04,RF06,RF09
```


