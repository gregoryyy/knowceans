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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.knowceans.corpus.IRandomAccessTermCorpus;
import org.knowceans.map.IBijectiveMap;

/**
 * LuceneTermCorpus creates a TermCorpus interface around a lucene index. For
 * this, the lucene index needs a stored field with some document identification
 * (technically, not necessarily unique), and a term vector field with the
 * content.
 * <p>
 * This implementation uses a map of terms and documents to directly access
 * mapping information without searching the Lucene index. Use this class if
 * frequent lookups are necessary, e.g., for applications that focus on
 * topic-based search rather than full-text search.
 * <p>
 * refactored class from knowceans.corpus.base and freshmind
 * 
 * @author gregor heinrich (gregor :: arbylon . net)
 */
public class LuceneMapCorpus extends LuceneCorpus implements
		IRandomAccessTermCorpus {

	// /**
	// * Each regular term's document frequency.
	// */
	// protected ArrayList<Integer> docFreqs;
	/**
	 * Each document's term frequencies termid -> frequency(doc)
	 */
	protected ArrayList<Map<Integer, Integer>> docTerms;
	/**
	 * Index of document names from the
	 */
	private ArrayList<String> docNames = null;
	/**
	 * Words in the corpus;
	 */
	private int nWords;

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
	 * @param contentField
	 * @param docNameField
	 * @param minDf
	 * @param useLowDf
	 * @throws IOException
	 */
	public LuceneMapCorpus(String indexPath, String contentField,
			String docNameField, int minDf, boolean useLowDf)
			throws IOException {
		super(indexPath, docNameField, contentField, minDf, useLowDf);
		extract();
	}

	@Override
	protected void extract() throws IOException {
		super.extract();
		setupDocs();
	}

	/**
	 * Creates document-terms maps.
	 * 
	 * @throws IOException
	 */
	private void setupDocs() throws IOException {
		buildDocNames();
		buildDocTerms();
	}

	/**
	 * Create the list of document names from the Lucene index.
	 * <p>
	 * TODO: doc names could be read directly from the index, but then only an
	 * interface like getDocName(index) can be provided, as there exists no
	 * access to all document names at once.
	 * 
	 * @throws IOException
	 */
	private void buildDocNames() throws IOException {
		docNames = new ArrayList<String>();
		for (int i = 0; i < ir.numDocs(); i++) {
			Document d = ir.document(i);
			docNames.add(d.get(docNamesField));
		}
	}

	/**
	 * Create the term frequency maps for the corpus, with the reduced
	 * vocabulary according to the document frequencies.
	 * 
	 * @throws IOException
	 */
	private void buildDocTerms() throws IOException {

		docTerms = new ArrayList<Map<Integer, Integer>>();

		for (int doc = 0; doc < ir.numDocs(); doc++) {
			HashMap<Integer, Integer> thisDocTerms = new HashMap<Integer, Integer>();
			int[] freqs = ir.getTermFreqVector(doc, contentField)
					.getTermFrequencies();
			String[] terms = ir.getTermFreqVector(doc, contentField).getTerms();
			for (int term = 0; term < terms.length; term++) {
				// filter document frequency
				if (ir.docFreq(new Term(contentField, terms[term])) < minDf) {
					continue;
				}
				thisDocTerms.put(lookup(terms[term]), freqs[term]);
				nWords += freqs[term];
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knowceans.corpus.ITermCorpus#lookupDoc(int)
	 */
	public String lookupDoc(int doc) {
		return docNames.get(doc);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knowceans.freshmind.index.IFastCorpus#getDocNames()
	 */
	public ArrayList<String> getDocNames() {
		return docNames;
	}

	@Override
	public Map<Integer, Integer> getDocTerms(int doc) {
		return docTerms.get(doc);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knowceans.freshmind.index.IFastCorpus#getDocTerms()
	 */
	public ArrayList<Map<Integer, Integer>> getDocTerms() {
		return docTerms;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knowceans.freshmind.index.IFastCorpus#getTermIndex()
	 */
	public IBijectiveMap<String, Integer> getTermIndex() {
		return termIndex;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knowceans.corpus.IRandomAccessTermCorpus#getNwords()
	 */
	public int getNwords() {
		return nWords;
	}
}
