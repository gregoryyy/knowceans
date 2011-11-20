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

import java.util.Map;
import java.util.Random;

/**
 * ITermCorpus is the interface of a term corpus that provides lookup
 * functionality for terms and documents (from id to matrix index) as well as
 * access to term and word vectors of the corpus.
 * <p>
 * refactored class from knowceans.corpus.base and freshmind
 * 
 * @author gregor heinrich (gregor :: arbylon . net)
 */
public interface ITermCorpusAlt {

	/**
	 * look up term for id.
	 * 
	 * @param term
	 * @return term string or null if unknown.
	 */
	public String lookup(int term);

	/**
	 * look up id for term
	 * 
	 * @param term
	 * @return term id or -1 if unknown.
	 */
	public int lookup(String term);

	/**
	 * Get document name from id.
	 * 
	 * @return
	 */
	public String lookupDoc(int doc);

	/**
	 * Get document id from name.
	 * 
	 * @return
	 */
	public int lookupDoc(String doc);

	/**
	 * Get the document terms as a frequency map id->frequency.
	 * 
	 * @return
	 */
	public Map<Integer, Integer> getDocTerms(int doc);

	/**
	 * Number of documents in corpus
	 * 
	 * @return
	 */
	public int getNdocs();

	/**
	 * Number of terms in corpus
	 * 
	 * @return
	 */
	public int getNterms();

	/**
	 * Get word vectors for corpus. (To have this in the interface might be a
	 * bit constraining)
	 * 
	 * @param rand
	 * @return
	 */
	public int[][] getDocWords(Random rand);

	/**
	 * Get the document as a structure data set.
	 * 
	 * @param i
	 * @return
	 */
	public CorpusDocument getDocument(int i);

	/**
	 * Add the document to the end of the corpus, setting up all required fields
	 * for the document.
	 * 
	 * @param doc
	 */
	public void addDocument(CorpusDocument doc);

}
