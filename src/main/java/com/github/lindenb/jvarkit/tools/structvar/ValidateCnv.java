/*
The MIT License (MIT)

Copyright (c) 2020 Pierre Lindenbaum

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

*/
package com.github.lindenb.jvarkit.tools.structvar;


import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import com.github.lindenb.jvarkit.io.IOUtils;
import com.github.lindenb.jvarkit.math.RunMedian;
import com.github.lindenb.jvarkit.math.stats.Percentile;
import com.github.lindenb.jvarkit.samtools.SAMRecordDefaultFilter;
import com.github.lindenb.jvarkit.samtools.util.SimpleInterval;
import com.github.lindenb.jvarkit.samtools.util.SimplePosition;
import com.github.lindenb.jvarkit.util.JVarkitVersion;
import com.github.lindenb.jvarkit.util.bio.DistanceParser;
import com.github.lindenb.jvarkit.util.bio.SequenceDictionaryUtils;
import com.github.lindenb.jvarkit.util.bio.bed.BedLine;
import com.github.lindenb.jvarkit.util.bio.bed.BedLineCodec;
import com.github.lindenb.jvarkit.util.iterator.LineIterator;
import com.github.lindenb.jvarkit.util.jcommander.Launcher;
import com.github.lindenb.jvarkit.util.jcommander.NoSplitter;
import com.github.lindenb.jvarkit.util.jcommander.Program;
import com.github.lindenb.jvarkit.util.log.Logger;
import com.github.lindenb.jvarkit.util.log.ProgressFactory;
import com.github.lindenb.jvarkit.variant.variantcontext.writer.WritingVariantsDelegate;

import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.CloserUtil;
import htsjdk.samtools.util.CoordMath;
import htsjdk.samtools.util.FileExtensions;
import htsjdk.samtools.util.Locatable;
import htsjdk.samtools.util.StringUtil;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.GenotypeBuilder;
import htsjdk.variant.variantcontext.StructuralVariantType;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.vcf.VCFConstants;
import htsjdk.variant.vcf.VCFFilterHeaderLine;
import htsjdk.variant.vcf.VCFFormatHeaderLine;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLine;
import htsjdk.variant.vcf.VCFHeaderLineType;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import htsjdk.variant.vcf.VCFIterator;
import htsjdk.variant.vcf.VCFIteratorBuilder;
import htsjdk.variant.vcf.VCFStandardHeaderLines;


/**
BEGIN_DOC

## Input

input is a VCF (or a BED file

for VCF, only SVTYPE=DEL/INS/DUP are considered



## Example

```
find DIR -type f -name "*.bam" > bam.list
$ java -jar ${JVARKIT_HOME}/dist/validatecnv.jar \
	-B bam.list --min-read-support-del 1 --median-adjust 0.5 --max-variance 15 --extend 0.33 20190320.MANTA.vcf 
```

END_DOC

 */
@Program(name="validatecnv",
	description="Experimental CNV Genotyping. Look variance of depths before/after putative known CNV.",
	keywords= {"cnv","bam","sam","vcf","depth"},
	modificationDate="20200330",
	generate_doc=false
	)
public class ValidateCnv extends Launcher
	{
	private static final Logger LOG = Logger.build(ValidateCnv.class).make();
	
	@Parameter(names={"-o","--out"},description=OPT_OUPUT_FILE_OR_STDOUT)
	private Path outputFile=null;
	@Parameter(names={"-B","--bams","--bam"},description="Path to bam. File with suffix .list is interpretted as a file containing a list of paths to bams.")
	private List<String> bamFiles = new ArrayList<>();
	@Parameter(names={"-R","--reference"},description=INDEXED_FASTA_REFERENCE_DESCRIPTION,required = true)
	private Path rererencePath = null;
	@Parameter(names={"-x","--extend"},description="Search the boundaries in a region that is 'x'*(CNV-length). So if x if 0.5, a region chr1:100-200 will be searched chr1:50-100 + chr1:200-250")
	private double extendFactor=0.5;	
	@Parameter(names={"-md","--min-dp"},description="At least one of the bounds must have a median-depth greater than this value.")
	private int min_depth = 20;
	@Parameter(names={"-cd","--critical-dp"},description="The middle section shouldn't have any point with a DP lower than this value (prevent HOM_VAR deletions)")
	private int critical_middle_depth = 1;
	@Parameter(names={"--min"},description="Min abs(SV) size to consider." +DistanceParser.OPT_DESCRIPTION ,converter=DistanceParser.StringConverter.class,splitter=NoSplitter.class)
	private int min_abs_sv_size = 50;
	@Parameter(names={"--max"},description="Max abs(SV) size to consider." +DistanceParser.OPT_DESCRIPTION ,converter=DistanceParser.StringConverter.class,splitter=NoSplitter.class)
	private int max_abs_sv_size = 1_000_000;
	@Parameter(names={"--dicard"},description="Dicard variant where all Called Genotypes have been filtered.")
	private boolean discard_all_filtered_variant = false;
	@Parameter(names={"--max-variance"},description="Maximum tolerated variance in one genomic segment.")
	private double max_variance = 10.0;
	@Parameter(names={"--median-adjust"},description="TODO.")
	private double median_factor = 0.33;
	@Parameter(names={"--min-read-support-del"},description="min number of read supporting deletion.")
	private int min_read_supporting_del=3;
	@Parameter(names={"--mapq"},description="min mapping quality.")
	private int min_mapq = 20;
	@Parameter(names={"-t","--treshold"},description="DUP if 1.5-x<=depth<=1.5+x . HET_DEL if 0.5-x<=depth<=0.5+x HOM_DEL if 0.0-x<=depth<=0.0+x")
	private float treshold = 0.05f;

	@ParametersDelegate
	private WritingVariantsDelegate writingVariantsDelegate = new WritingVariantsDelegate();


	private class BamInfo implements Closeable {
	final SamReader samReader;
		final String sampleName;
		BamInfo(final Path path) throws IOException {
			final SamReaderFactory samReaderFactory =  SamReaderFactory.makeDefault().
					referenceSequence(rererencePath).
					validationStringency(ValidationStringency.LENIENT)
					;
			this.samReader = samReaderFactory.open(path) ;
			if(!this.samReader.hasIndex()) {
				throw new IOException("Bam is not indexed : "+path);
				}

			
			this.sampleName =  samReader.getFileHeader().
					getReadGroups().
					stream().
					map(R->R.getSample()).
					filter(S->!StringUtil.isBlank(S)).
					findFirst().
					orElse(IOUtils.getFilenameWithoutCommonSuffixes(path));
			
			}
		@Override
		public void close() throws IOException {
			this.samReader.close();
			}
		}
	
	
	private CloseableIterator<Locatable> openVcfAsIterable(final String input) throws IOException {
		final VCFIteratorBuilder vcfIteratorBuilder = new VCFIteratorBuilder();
		final VCFIterator iter1 = input==null?vcfIteratorBuilder.open(stdin()):vcfIteratorBuilder.open(input);
		return new CloseableIterator<Locatable>() {
			@Override
			public boolean hasNext() {
				return iter1.hasNext();
				}

			@Override
			public Locatable  next() {
				final VariantContext ctx = iter1.next();
				final StructuralVariantType svType = ctx.getStructuralVariantType();
				if(!(svType==StructuralVariantType.DEL || svType==StructuralVariantType.DUP || svType==StructuralVariantType.INS) ) {
					return null;
					}
				
				if(ctx.getNAlleles()!=2) return null;
				return new SimpleInterval(iter1.next());
				}

			@Override
			public void close() {
				iter1.close();
				}
			};
		}
	private CloseableIterator<Locatable> openBedAsIterable(final String input) throws IOException {
		final BedLineCodec codec = new BedLineCodec();
		final BufferedReader iter1 = (input==null?IOUtils.openStreamForBufferedReader(stdin()):IOUtils.openURIForBufferedReading(input));
		final LineIterator iter2 = new LineIterator(iter1);
		return new CloseableIterator<Locatable>() {
			@Override
			public boolean hasNext() {
				return iter2.hasNext();
				}

			@Override
			public Locatable  next() {
				String line = iter2.next();
				if(BedLine.isBedHeader(line)) return null;
				return codec.decode(line);
				}

			@Override
			public void close() {
				iter2.close();
				CloserUtil.close(iter1);
				}
			};
		}
	@Override
	public int doWork(final List<String> args) {		
		if(this.extendFactor<=0)
			{
			LOG.error("bad extend factor "+this.extendFactor);
			return -1;
			}
		if(this.treshold<0f || this.treshold>=0.25f) {
			LOG.error("Bad treshold 0 < "+this.treshold+" >=0.25 ");
			return -1;
		}
		final Map<String,BamInfo> sample2bam = new HashMap<>();
		VariantContextWriter out = null;
		CloseableIterator<Locatable> iterIn = null;
		try
			{	
			final SAMSequenceDictionary dict = SequenceDictionaryUtils.extractRequired(this.rererencePath);

			final List<Path> bamPaths =IOUtils.unrollPaths(this.bamFiles);
			
			final String input = oneFileOrNull(args);
			if(input==null) {
				iterIn = openVcfAsIterable(null);
				}
			else if(input.endsWith(FileExtensions.BED) || input.endsWith(FileExtensions.BED+".gz")) {
				iterIn = openBedAsIterable(input);
				}
			else
				{
				iterIn = openVcfAsIterable(input);
				}
			
			/* register each bam */
			for(final Path p2: bamPaths) {				
				final BamInfo bi = new BamInfo(p2);
				
				
				if(sample2bam.containsKey(bi.sampleName)) {
					LOG.error("sample "+ bi.sampleName +" specified twice.");
					bi.close();
					return -1;
					}
				
				sample2bam.put(bi.sampleName, bi);
				}
			
			if(sample2bam.isEmpty()) {
				LOG.error("no bam was defined");
				return -1;
				}
			
			
			final Set<VCFHeaderLine> metadata = new HashSet<>();
			
			final VCFInfoHeaderLine infoSVSamples = 
					new VCFInfoHeaderLine("N_SAMPLES",
							1,
							VCFHeaderLineType.Integer,
							"Number of Samples that could carry a SV");
			metadata.add(infoSVSamples);
			
			final VCFInfoHeaderLine infoSvLen = 
					new VCFInfoHeaderLine("SVLEN",
							1,
							VCFHeaderLineType.Integer,
							"SV length");
			metadata.add(infoSvLen);

			
			final BiFunction<String, String, VCFFormatHeaderLine> makeFmt = (TAG,DESC)-> new VCFFormatHeaderLine(TAG,1,VCFHeaderLineType.Integer,DESC);
			
			final VCFFormatHeaderLine midVarianceDepth = makeFmt.apply("CBN","Middle variance depth or -1");
			metadata.add(midVarianceDepth);
			final VCFFormatHeaderLine formatCN =  new VCFFormatHeaderLine("CN",1,VCFHeaderLineType.Float,"normalized copy-number");
			metadata.add(formatCN);
			final VCFFormatHeaderLine nReadsSupportingDel = makeFmt.apply("RSD","number of split read supporting SV.");
			metadata.add(nReadsSupportingDel);

			
			final VCFFilterHeaderLine filterAllDel = new VCFFilterHeaderLine("ALL_DEL", "number of samples greater than 1 and all are deletions");
			metadata.add(filterAllDel);
			final VCFFilterHeaderLine filterAllDup = new VCFFilterHeaderLine("ALL_DUP", "number of samples  greater than  1 and all are duplication");
			metadata.add(filterAllDup);
			final VCFFilterHeaderLine filterNoSV= new VCFFilterHeaderLine("NO_SV", "There is no DUP or DEL in this variant");
			metadata.add(filterNoSV);
			final VCFFilterHeaderLine filterHomDel = new VCFFilterHeaderLine("HOM_DEL", "There is one Homozygous deletion.");
			metadata.add(filterHomDel);

			
			
			VCFStandardHeaderLines.addStandardFormatLines(metadata, true,
					VCFConstants.DEPTH_KEY,
					VCFConstants.GENOTYPE_KEY,
					VCFConstants.GENOTYPE_FILTER_KEY,
					VCFConstants.GENOTYPE_QUALITY_KEY
					);
			VCFStandardHeaderLines.addStandardInfoLines(metadata, true,
					VCFConstants.DEPTH_KEY,
					VCFConstants.END_KEY
					);

			
			final VCFHeader header = new VCFHeader(metadata,sample2bam.keySet());
			if(dict!=null) header.setSequenceDictionary(dict);
			JVarkitVersion.getInstance().addMetaData(this, header);
			
			final ProgressFactory.Watcher<VariantContext> progress = ProgressFactory.newInstance().
					dictionary(dict).
					logger(LOG).
					build();
			out =  this.writingVariantsDelegate.
					dictionary(header.getSequenceDictionary()).
					open(this.outputFile);
			out.writeHeader(header);
			
			final Allele DUP_ALLELE =Allele.create("<DUP>",false);
			final Allele DEL_ALLELE =Allele.create("<DEL>",false);
			final Allele REF_ALLELE =Allele.create("N",true);

			
			while(iterIn.hasNext())
				{
				final Locatable ctx = iterIn.next();
				if(ctx==null) continue;
				final SAMSequenceRecord ssr = dict.getSequence(ctx.getContig());
				if(ssr==null || ctx.getStart()>=ssr.getSequenceLength()) continue;
				
				final int svLen = ctx.getLengthOnReference();

				if(svLen< this.min_abs_sv_size) continue;
				if(svLen> this.max_abs_sv_size) continue;
				
				int n_samples_with_cnv  = 0;
				final SimplePosition breakPointLeft = new SimplePosition(ctx.getContig(), ctx.getStart());
				final SimplePosition breakPointRight = new SimplePosition(ctx.getContig(), ctx.getEnd());
				
				final int extend = 1+(int)(svLen*this.extendFactor);
				
				
				final int leftPos =  Math.max(1, breakPointLeft.getPosition()-extend);
				final int array_mid_start = breakPointLeft.getPosition()-leftPos;
				final int array_mid_end = breakPointRight.getPosition()-leftPos;
				final int rightPos =  Math.min(breakPointRight.getPosition()+extend, ssr.getSequenceLength());
				
					
				
				//System.err.println(""+leftPos+" "+ctx.getStart()+" "+ctx.getEnd()+" "+rightPos);
				
				
				final VariantContextBuilder vcb = new VariantContextBuilder();
				vcb.chr(ctx.getContig());
				vcb.start(ctx.getStart());
				vcb.stop(ctx.getEnd());
				vcb.attributes(new HashMap<>());
				vcb.attribute(VCFConstants.END_KEY, ctx.getEnd());
				
			    final Set<Allele> alleles = new HashSet<>();
			    alleles.add(REF_ALLELE);
				int count_dup = 0;
				int count_del = 0;
				final List<Genotype> genotypes = new ArrayList<>(sample2bam.size());
				Double badestGQ = null;
				final double raw_coverage[] = new double[CoordMath.getLength(leftPos,rightPos)];
				for(final String sampleName : sample2bam.keySet())
					{
					final BamInfo bi =sample2bam.get(sampleName);					
					
					Arrays.fill(raw_coverage, 0.0);

					int n_reads_supporting_deletions = 0;
					
					
					try(CloseableIterator<SAMRecord> iter2 = bi.samReader.queryOverlapping(
							ctx.getContig(),
							leftPos,
							rightPos
							)) {
						while(iter2.hasNext()) {
							final SAMRecord rec = iter2.next();
							if(!SAMRecordDefaultFilter.accept(rec,  this.min_mapq)) continue;

							final Cigar cigar = rec.getCigar();
							if(cigar==null || cigar.isEmpty()) continue;
							// any clip supporting deletion ?
							boolean read_supports_cnv = false;
							final int breakpoint_distance= 10;

							// any clip on left ?
							if(cigar.isLeftClipped() &&
								rec.getUnclippedStart() < rec.getAlignmentStart() && 
								new SimpleInterval(ctx.getContig(), rec.getUnclippedStart(), rec.getAlignmentStart()).
								withinDistanceOf(breakPointLeft,breakpoint_distance)) {
									read_supports_cnv  = true;
								}
							// any clip on right ?
							if(	!read_supports_cnv &&
								cigar.isRightClipped() &&
								rec.getAlignmentEnd() < rec.getUnclippedEnd() && 
								new SimpleInterval(ctx.getContig(), rec.getAlignmentEnd(), rec.getUnclippedEnd()).
								withinDistanceOf(breakPointRight,breakpoint_distance)) {
									read_supports_cnv  = true;
								}
							
							if(read_supports_cnv) {
								n_reads_supporting_deletions++;
							}
							
							
							int ref=rec.getStart();
							for(final CigarElement ce:cigar) {
								final CigarOperator op = ce.getOperator();
								if(op.consumesReferenceBases())
									{
									if(op.consumesReadBases()) {
										for(int x=0;x < ce.getLength() && ref + x - leftPos < raw_coverage.length ;++x)
											{
											final int p = ref + x - leftPos;
											if(p<0 || p>=raw_coverage.length) continue;
											raw_coverage[p]++;
											}
										}
									ref+=ce.getLength();
									}
								}
							}// end while iter record
						}//end try
					
					//run median to smooth spline
					final double smoothed_cov[]= new RunMedian(RunMedian.getTurlachSize(raw_coverage.length)).apply(raw_coverage);

					final double bounds_cov[] = IntStream.concat(
							IntStream.range(0, array_mid_start),
							IntStream.range(array_mid_end,smoothed_cov.length)
							).mapToDouble(IDX->raw_coverage[IDX]).
						toArray();
					
					
					final double medianBound = Percentile.median().evaluate(bounds_cov);
					if(Double.isNaN(medianBound) || medianBound==0) {
						final Genotype gt2 = GenotypeBuilder.createMissing(sampleName, 2);
						genotypes.add(gt2);
						continue;
						}
					
					// divide coverage per medianBound
					final double normalized_coverage[] = new double[smoothed_cov.length];
					for(int i=0;i< normalized_coverage.length;++i) {
						normalized_coverage[i] = smoothed_cov[i] / medianBound;
						}
					
					final double normDepth = Percentile.median().evaluate(normalized_coverage, array_mid_start, array_mid_end);
					
					
					final boolean is_sv;
					final boolean is_het_deletion = Math.abs(normDepth-0.5)<= this.treshold; 
					final boolean is_hom_deletion = Math.abs(normDepth-0.0)<= this.treshold; 
					final boolean is_het_dup = Math.abs(normDepth-1.5)<= this.treshold; 
					final boolean is_ref = Math.abs(normDepth-1.0)<= this.treshold; 
					final double theoritical_depth;

					final GenotypeBuilder gb;
					
					if(is_ref) {
						gb = new GenotypeBuilder(sampleName,Arrays.asList(REF_ALLELE,REF_ALLELE));						
						is_sv = false;
						theoritical_depth = 1.0;
						}
					else if(is_het_deletion) {
						gb = new GenotypeBuilder(sampleName,Arrays.asList(REF_ALLELE,DEL_ALLELE));						
						alleles.add(DEL_ALLELE);
						is_sv = true;
						theoritical_depth = 0.5;
						count_del++;
						}
					else if(is_hom_deletion) {
						gb = new GenotypeBuilder(sampleName,Arrays.asList(DEL_ALLELE,DEL_ALLELE));						
						alleles.add(DEL_ALLELE);
						vcb.filter(filterHomDel.getID());
						is_sv = true;
						theoritical_depth = 0.0;
						count_del++;
						}
					else if(is_het_dup) {
						gb = new GenotypeBuilder(sampleName,Arrays.asList(REF_ALLELE,DUP_ALLELE));
						alleles.add(DUP_ALLELE);
						is_sv = true;
						theoritical_depth = 1.5;
						count_dup++;
						}
					else
						{
						gb = new GenotypeBuilder(sampleName,Arrays.asList(Allele.NO_CALL,Allele.NO_CALL));
						is_sv = false;
						theoritical_depth = 1.0;
						}	
					if(is_sv) {
						n_samples_with_cnv++;
					}
					
					
					double gq = Math.abs(theoritical_depth-normDepth);
					gq = Math.min(0.5, gq);
					gq = gq * gq;
					gq = gq / 0.25;
					gq = 99 * (1.0 - gq);
					
					gb.GQ((int)gq);
					gb.attribute(formatCN.getID(), normDepth);
					
					if(badestGQ==null || badestGQ.compareTo(gq)>0) {
						badestGQ = gq;
					}
					
										
					gb.attribute(nReadsSupportingDel.getID(), n_reads_supporting_deletions);
					
					
					genotypes.add(gb.make());
					}
				
				
				if(alleles.size()<=1) continue;
				vcb.alleles(alleles);
				vcb.noID();
				vcb.genotypes(genotypes);
				vcb.attribute(infoSVSamples.getID(), n_samples_with_cnv);
				vcb.attribute(infoSvLen.getID(), svLen);
				
				if(count_dup == sample2bam.size() && sample2bam.size()!=1) {
					vcb.filter(filterAllDup.getID());
				}
				if(count_del == sample2bam.size() && sample2bam.size()!=1) {
					vcb.filter(filterAllDel.getID());
				}
				
				if(n_samples_with_cnv==0) {
					vcb.filter(filterNoSV.getID());
					}
				
				if(badestGQ!=null ) {
					vcb.log10PError(badestGQ/-10.0);
					}
				
				out.add(vcb.make());
				}
			progress.close();
			out.close();
			iterIn.close();
			return 0;
			}
		catch(final Throwable err)
			{
			LOG.error(err);
			return -1;
			}
		finally
			{
			CloserUtil.close(iterIn);
			CloserUtil.close(out);
			sample2bam.values().forEach(F->CloserUtil.close(F));
			}
		}
	
	
	public static void main(final String[] args) {
		new ValidateCnv().instanceMainWithExit(args);
		}
	}
