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
 * Created on 22.07.2006
 */
package org.knowceans.corpus.refactor;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.knowceans.map.IBijectiveMap;
import org.knowceans.util.ArrayUtils;

/**
 * CorpusDocument
 * <p>
 * refactored class from knowceans.corpus.base and freshmind
 * 
 * @author gregor heinrich (gregor :: arbylon . net)
 */
public class CorpusDocument {
	private Map<Integer, Integer> terms;
	private Vector<Integer> cat = null;
	private String title;
	private int[] actors;
	private int relation;

	/**
	 * termindex is to check the term indices in the terms map are based on the
	 * correct vocabulary.
	 */
	private IBijectiveMap<String, Integer> termIndex;

	/**
	 * Copies all fields (except termIndex, which is referenced) to the returned
	 * document.
	 * 
	 * @return
	 */
	public CorpusDocument copy() {
		CorpusDocument cd = new CorpusDocument();
		cd.terms = new HashMap<Integer, Integer>();
		cd.terms.putAll(terms);
		cd.cat = new Vector<Integer>(cat);
		cd.title = new String(title);
		cd.actors = (int[]) ArrayUtils.copy(actors);
		cd.relation = relation;
		cd.termIndex = termIndex;
		return cd;
	}

	public final int[] getActors() {
		return actors;
	}

	public final void setActors(int[] actors) {
		this.actors = actors;
	}

	public final int getRelation() {
		return relation;
	}

	public final void setRelation(int relation) {
		this.relation = relation;
	}

	public final String getTitle() {
		return title;
	}

	public final void setTitle(String title) {
		this.title = title;
	}

	public final Map<Integer, Integer> getTerms() {
		return terms;
	}

	public final void setTerms(Map<Integer, Integer> words) {
		this.terms = words;
	}

	public final IBijectiveMap<String, Integer> getTermIndex() {
		return termIndex;
	}

	public final void setTermIndex(IBijectiveMap<String, Integer> termIndex) {
		this.termIndex = termIndex;
	}

	public final Vector<Integer> getCategories() {
		return cat;
	}

	public final void setCat(Vector<Integer> cat) {
		this.cat = cat;
	}
}
