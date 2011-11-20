/*
 * Copyright (c) 2005-6 Gregor Heinrich. All rights reserved. Redistribution and
 * use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met: 1. Redistributions of source
 * code must retain the above copyright notice, this list of conditions and the
 * following disclaimer. 2. Redistributions in binary form must reproduce the
 * above copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESSED OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * Created on 14.05.2006
 */
package org.knowceans.corpus.refactor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.SimpleFSDirectory;
import org.knowceans.corpus.ITermCorpus;
import org.knowceans.map.BijectiveHashMap;
import org.knowceans.map.IBijectiveMap;
import org.knowceans.util.ArrayUtils;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * LuceneTermCorpus creates a TermCorpus interface around a lucene index. For
 * this, the lucene index needs a stored field with some document identification
 * (technically, not necessarily unique), and a term vector field with the
 * content. This implementation directly (hence its name) accesses the fields of
 * the lucene index.
 * <p>
 * The corpus can split the lucene index by a df threshold.
 * <p>
 * refactored class from knowceans.corpus.base and freshmind
 * 
 * @author gregor heinrich (gregor :: arbylon . net)
 */
public class LuceneCorpus implements ITermCorpus /* , IIndexEventSource */{

	protected static final int INDEX_UNKNOWN = -1;
	protected String indexpath;
	protected IndexReader ir;
	protected ArrayList<Integer> emptyDocs;

	/**
	 * Index of term<->id
	 */
	protected IBijectiveMap<String, Integer> termIndex = null;
	/**
	 * Index of lowDf term<->id, the id is above that of the termIndex, i.e.,
	 * one term-document matrix could created if necessary.
	 */
	protected IBijectiveMap<String, Integer> termIndexLowDf = null;
	/**
	 * Minimum document frequency for terms allowd in the regular term index.
	 */
	protected int minDf;
	/**
	 * Terms in the regular term index
	 */
	protected int nTerms;
	/**
	 * Terms in the lowDf index
	 */
	protected int nTermsLowDf;
	/**
	 * Lucene index field to extract the corpus information from.
	 */
	protected String contentField;
	/**
	 * Lucene index field to read the document names from.
	 */
	protected String docNamesField;

	private boolean useLowDf;

	/**
	 * Create a term corpus from the index found at the path, using the content
	 * field for the terms and the docNameField for the document names. Use only
	 * terms with document frequency above or equal to minDf and ignore or make
	 * accessible the low-df terms.
	 * <p>
	 * The content field must have been indexed.
	 * <p>
	 * The doc names field must have been stored.
	 * 
	 * @param indexPath
	 * @param docNameField
	 * @param contentField
	 * @param minDf
	 * @param useLowDf
	 * @throws IOException
	 */
	public LuceneCorpus(String indexPath, String docNameField,
			String contentField, int minDf, boolean useLowDf)
			throws IOException {
		this.indexpath = indexPath;
		this.minDf = minDf;
		this.contentField = contentField;
		this.docNamesField = docNameField;
		this.useLowDf = useLowDf;
		this.emptyDocs = new ArrayList<Integer>();
		extract();
	}

	/**
	 * Initialise the corpus with just access to the IndexReader. This is useful
	 * if only id information is required.
	 * 
	 * @param path
	 * @param docNamesField
	 * @throws IOException
	 */
	public LuceneCorpus(String path, String docNamesField) throws IOException {
		this.indexpath = path;
		this.docNamesField = docNamesField;
		this.ir = IndexReader.open(new SimpleFSDirectory(new File(indexpath)));
	}

	/**
	 * Initialise the corpus by extracting the files from the index. This method
	 * must be called exactly once before any get method is called.
	 * 
	 * @throws IOException
	 */
	protected void extract() throws IOException {
		this.ir = IndexReader.open(new SimpleFSDirectory(new File(indexpath)));
		setupIndex(useLowDf);
	}

	/**
	 * Creates term map and counts.
	 * 
	 * @param useIgnored
	 * @throws IOException
	 */
	protected void setupIndex(boolean useIgnored) throws IOException {

		ArrayList<String> ignoredTerms = buildTermIndex(useIgnored);
		if (useIgnored) {
			buildTermIndexLowDf(ignoredTerms);
		}
	}

	/**
	 * Create term index from the lucene index
	 * 
	 * @param useIgnored
	 * @return
	 * @throws IOException
	 */
	protected ArrayList<String> buildTermIndex(boolean useIgnored)
			throws IOException {
		ArrayList<String> ignoredTerms = null;
		if (useIgnored) {
			ignoredTerms = new ArrayList<String>();
		}
		termIndex = new BijectiveHashMap<String, Integer>();
		// TODO: local version of document frequency: decide if necessary (mem
		// vs. speed)
		// docFreqs = new ArrayList<Integer>();
		TermEnum ee = ir.terms();
		int i = 0;
		while (ee.next()) {
			Term t = ee.term();
			String f = t.field();
			if (!f.equals(contentField))
				continue;
			if (ir.docFreq(t) >= minDf && ok(t.text())) {
				// store in regular-term index
				termIndex.put(t.text(), i);
				i++;
			} else if (useIgnored) {
				// prepare for ignored-term index
				ignoredTerms.add(t.text());
			}
		}
		nTerms = termIndex.size();
		return ignoredTerms;
	}

	/**
	 * @param string
	 * @return
	 */
	private boolean ok(String string) {
		return string.matches("\\p{Alnum}+");
	}

	/**
	 * Create the hash map from the vector of low-df terms
	 * 
	 * @param ignoredTerms
	 */
	protected void buildTermIndexLowDf(ArrayList<String> ignoredTerms) {
		int lowDfIndex = termIndex.size();
		// create the map of ignored terms
		termIndexLowDf = new BijectiveHashMap<String, Integer>();
		for (String t : ignoredTerms) {
			termIndexLowDf.put(t, lowDfIndex);
			lowDfIndex++;
		}
		nTermsLowDf = termIndexLowDf.size();

		// get rid of temporary data
		ignoredTerms = null;
		System.gc();
	}

	public String lookupDoc(int doc) {
		Document d = null;
		try {
			d = ir.document(doc);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return d.get(docNamesField);
	}

	/**
	 * Add a document to the corpus. This implementation does not allow to use
	 * differing term indices. If the term index in the corpus is empty, the
	 * term index of the document is taken as the corpus term index
	 * automatically to allow buildup of a corpus via addDocument methods.
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knowceans.corpus.ITermCorpus#addDocument(org.knowceans.corpus.
	 * CorpusDocument)
	 */
	public void addDocument(CorpusDocument doc) {
		throw new NotImplementedException();
	}

	/**
	 * Get the document index of the document with string id docName.
	 * 
	 * @param docName
	 * @return
	 */
	public int lookupDoc(String docName) {
		try {
			IndexSearcher is = new IndexSearcher(new SimpleFSDirectory(
					new File(indexpath)));
			Term t = new Term(docNamesField, docName);
			Query q = new TermQuery(t);
			TopDocs h = is.search(q, 10);
			if (h.totalHits > 0) {
				return h.scoreDocs[0].doc;
			} else {
				return INDEX_UNKNOWN;
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return INDEX_UNKNOWN;
	}

	public Map<Integer, Integer> getDocTerms(int doc) {
		try {
			HashMap<Integer, Integer> thisDocTerms = new HashMap<Integer, Integer>();
			int[] freqs = ir.getTermFreqVector(doc, contentField)
					.getTermFrequencies();
			String[] terms = ir.getTermFreqVector(doc, contentField).getTerms();
			for (int term = 0; term < terms.length; term++) {
				// filter document frequency
				if (minDf > 1
						&& ir.docFreq(new Term(contentField, terms[term])) < minDf) {
					continue;
				}
				thisDocTerms.put(lookup(terms[term]), freqs[term]);
			}
			return thisDocTerms;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;

	}

	/**
	 * Get the documents as vectors of bag of words, i.e., per document, a
	 * scrambled array of term indices is generated.
	 * 
	 * @param rand random number generator or null to use standard generator
	 * @return
	 */
	public int[][] getDocWords(Random rand) {
		// words in documents
		int[][] documents = new int[getNdocs()][];
		for (int i = 0; i < getNdocs(); i++) {
			documents[i] = getDocWords(i, rand);
		}
		return documents;
	}

	/**
	 * Get the words of document doc as a scrambled sequence.
	 * <p>
	 * It seems that the getDocTerms... loop scales badly. Use LuceneMapCorpus
	 * for larger documents.
	 * 
	 * @param doc
	 * @param rand random number generator or null to use standard generator
	 * @return
	 */
	public int[] getDocWords(int doc, Random rand) {
		Vector<Integer> document = new Vector<Integer>();
		System.out.println("doc size: " + getDocTerms(doc).size() + "...");
		for (int id : getDocTerms(doc).keySet()) {
			for (int j = 0; j < getDocTerms(doc).get(id); j++) {
				document.add(id);
			}
		}
		// permute words so duplicates aren't juxtaposed
		System.out.println("shuffle");
		if (rand != null) {
			Collections.shuffle(document, rand);
		} else {
			Collections.shuffle(document);
		}
		System.out.println("... done");
		return (int[]) ArrayUtils.asPrimitiveArray(document, int[].class);
	}

	/**
	 * Get the words of an unknown document as a scrambled sequence (= search).
	 * This method accesses the Lucene search index.
	 * 
	 * @param string
	 */
	public int[] getDocWords(String string) {
		Vector<Integer> words = new Vector<Integer>();

		SimpleAnalyzer ana = new SimpleAnalyzer();
		TokenStream st = ana
				.tokenStream(contentField, new StringReader(string));
		TermAttribute termAttribute = st.getAttribute(TermAttribute.class);
		try {
			while (st.incrementToken()) {
				String term = termAttribute.term();
				int index = lookup(term);
				if (index != INDEX_UNKNOWN) {
					words.add(index);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		int[] a = (int[]) ArrayUtils.asPrimitiveArray(words, int[].class);
		return a;
	}

	/**
	 * Get the string for the particular index, either from the regular index or
	 * from the lowDf index.
	 * 
	 * @param term term index
	 * @return term or null if unknown
	 */
	public String lookup(int term) {
		return term < nTerms ? termIndex.getInverse(term)
				: termIndexLowDf != null ? termIndexLowDf.getInverse(term)
						: null;
	}

	/**
	 * Get the index of the particular term, either from the regular index or
	 * from the lowDf index, which results in an index >= nTerms.
	 * 
	 * @param term string
	 * @return index of the term or -1 (INDEX_UNKNOWN)
	 */
	public int lookup(String term) {
		Integer i = termIndex.get(term);
		if (i == null) {
			i = termIndexLowDf.get(term);
			if (i == null) {
				return INDEX_UNKNOWN;
			}
		}
		return i;
	}

	public int getNdocs() {
		return ir.numDocs();
	}

	public int getNterms() {
		return nTerms;
	}

	public int getNwords(int doc) {
		int size = 0;
		try {
			TermFreqVector u = ir.getTermFreqVector(doc, contentField);
			String[] terms = u.getTerms();
			for (int i = 0; i < terms.length; i++) {
				if (termIndex.containsKey(terms[i])) {
					size += u.getTermFrequencies()[i];
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return size;
	}

	/**
	 * Whether this document is non-empty after filtering.
	 * 
	 * @param doc
	 * @return
	 */
	public final boolean isEmptyDoc(int doc) {
		return emptyDocs.contains(doc);
	}

	/**
	 * Write the document titles in a file (one doc per line)
	 * 
	 * @param file
	 * @throws IOException
	 */
	public void writeDocList(String file) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		System.out.println("-> " + file);

		for (int i = 0; i < ir.numDocs(); i++) {
			String title = ir.document(i).get(docNamesField);
			bw.write(title + " : ");
			bw.write("\n");
		}
		bw.close();
	}

	/**
	 * Write the corpus to the file. (Important note: both numbers are doc
	 * frequency and equal since the Lucene index does not support term
	 * frequency).
	 * 
	 * @param filebase
	 * @throws IOException
	 */
	public void writeCorpus(String filebase) throws IOException {
		BufferedWriter bw;
		bw = new BufferedWriter(new FileWriter(filebase + ".corpus"));
		System.out.println("-> " + filebase + ".corpus");

		for (int i = 0; i < ir.numDocs(); i++) {
			TermFreqVector v = ir.getTermFreqVector(i, contentField);
			String[] terms = v.getTerms();
			int[] freqs = v.getTermFrequencies();

			int size = 0;
			StringBuffer b = new StringBuffer();
			for (int j = 0; j < terms.length; j++) {
				Integer id = termIndex.get(terms[j]);
				// id == null means low frequency --> ignore
				if (id != null) {
					b.append(" ").append(id).append(":").append(freqs[j]);
					size++;
				}
			}
			StringBuffer b2 = new StringBuffer();
			b2.append(size).append(b).append("\n");
			bw.write(b2.toString());
		}
		bw.close();
	}

	/**
	 * Write the vocabulary to the file. The corpus / term index is supposed to
	 * be reordered / split during extraction.
	 * 
	 * @param file
	 * @param sort sorts the vocabulary in alphabetical order
	 * @throws IOException
	 */
	public void writeVocabulary(String file, boolean sort) throws IOException {
		System.out.println("-> " + file);
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		Map<String, Integer> sortMap;

		if (sort) {
			// order vocabulary alphabetically: O(n * log(n))
			sortMap = new TreeMap<String, Integer>(termIndex);
		} else {
			sortMap = termIndex;
		}

		int ii = 0;
		for (Map.Entry<String, Integer> term : sortMap.entrySet()) {
			int df = ir.docFreq(new Term(contentField, term.getKey()));
			bw.write(term.getKey() + " = " + term.getValue());
			bw.write(" = " + df + " " + df);
			bw.write("\n");
			ii++;
		}
		bw.close();
	}

	@Override
	public int getNumTerms() {
		return nTerms;
	}

	@Override
	public int getNumDocs() {
		if (ir != null) {
			return ir.numDocs();
		}
		return -1;
	}

	@Override
	public int getNumWords() {
		return 0;
	}

	@Override
	public int[][][] getDocTermsFreqs() {
		return null;
	}
}
