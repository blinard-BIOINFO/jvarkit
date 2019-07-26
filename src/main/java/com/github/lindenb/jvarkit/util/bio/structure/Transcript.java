/*
The MIT License (MIT)

Copyright (c) 2019 Pierre Lindenbaum

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
package com.github.lindenb.jvarkit.util.bio.structure;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;


public interface Transcript extends StrandedLocatable {
	/** get property map */
	public Map<String, String> getProperties();
	/** get associated gene */
	public Gene getGene();
	/** get transcript id */
	public String getId();
	/** return i-th exon scanning from 5' to 3', whatever strand */
	public Exon getExon(int index0);
	/** return i-th exon start scanning from 5' to 3', whatever strand */
	public int getExonStart(int index0);
	/** return i-th exon end scanning from 5' to 3', whatever strand */
	public int getExonEnd(int index0);
	/** return exons scanning from 5' to 3', whatever strand */
	public List<Exon> getExons();
    public char getStrand();
    public int getExonCount();
    /** weird transcripts when no exons was defined */
    public default boolean hasExon() {
    	return getExonCount()>0;
    }
    /** get all CDS in this exon */
	public List<Cds> getAllCds();

    
    public int getIntronCount();
	public List<Intron> getIntrons();
	/** return i-th intron scanning from 5' to 3', whatever strand */
	public Intron getIntron(int index0);

	/** get genomic transcription start position in the genome */
    public int getTxStart();
	/** get genomic transcription end position in the genome */
    public int getTxEnd();

    public boolean isNonCoding();
	public boolean isCoding();
	
	/** get translation start position in the genome
	 *  return txStart if it's not coding
	 *  return getCodonStart if strand '+'
	 *  return getCodonStart() if strand '-' && stop codon define
	 *  otherwise return getTxStart
	 *  
	 *  */
	@Deprecated
	public default int getCdsStart() {
		if(isNonCoding()) {
			return getTxStart();
			}
		else if(isPositiveStrand()) {
			if(hasCodonStartDefined()) return getCodonStart().get().getStart();
			return getTxStart();
			}
		else if(isNegativeStrand())
			{
			if(hasCodonStopDefined()) return getCodonStop().get().getStart();
			return getTxStart();
			}
		else
			{
			return getTxStart();
			}
		}
	/** get translation end position in the genome */
	public default int getCdsEnd() {
		if(isNonCoding()) {
			return getTxStart();//YES txStart !
			}
		else if(isPositiveStrand()) {
			if(hasCodonStopDefined()) return getCodonStop().get().getEnd();
			return getTxEnd();
			}
		else
			{
			if(hasCodonStartDefined()) return getCodonStart().get().getEnd();
			return getTxEnd();
			}
		}

	
	/** get transcript length (cumulative exons sizes )*/
	public default int getTranscriptLength() {
		return getExons().stream().
				mapToInt(T->getLengthOnReference()).
				sum();
		}
	
	/** return UTR on 5' side of genomic reference */
	public Optional<UTR> getUTR5();
	/** return UTR on 3' side of genomic reference */
	public Optional<UTR> getUTR3();

	/** return UTR on 5' side of transcript. strand matters */
	public default Optional<UTR> getTranscriptUTR5() {
		if(!hasCodonStartDefined()) return Optional.empty();
		if(isPositiveStrand()) return getUTR5();
		if(isNegativeStrand()) return getUTR3();
		return Optional.empty();
	}
	
	/** return UTR on 5' side of transcript. strand matters */
	public default Optional<UTR> getTranscriptUTR3() {
		if(!hasCodonStopDefined()) return Optional.empty();
		if(isPositiveStrand()) return getUTR3();
		if(isNegativeStrand()) return getUTR5();
		return Optional.empty();
	}

	
	
	public default boolean hasUTR() {
		return getUTR5().isPresent()|| getUTR3().isPresent();
	}
	
	/** get Codon start */
	public Optional<Codon> getCodonStart();
	/** get Codon end */
	public Optional<Codon> getCodonStop();
	
	/** test if start_codon defined. eg:  wget -q -O - "http://ftp.ensembl.org/pub/grch37/current/gtf/homo_sapiens/Homo_sapiens.GRCh37.87.gtf.gz" | gunzip -c | grep ENSG00000060138 */
	public default boolean hasCodonStartDefined() {
		return getCodonStart().isPresent();
		}
	/** test if stop codon defined. eg:  wget -q -O - "http://ftp.ensembl.org/pub/grch37/current/gtf/homo_sapiens/Homo_sapiens.GRCh37.87.gtf.gz" | gunzip -c | grep ENST00000327956 | grep stop */
	public default boolean hasCodonStopDefined() {
		return getCodonStop().isPresent();
		}

}
