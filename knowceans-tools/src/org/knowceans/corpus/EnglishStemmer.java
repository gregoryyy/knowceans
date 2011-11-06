package org.knowceans.corpus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.knowceans.map.IMultiMap;
import org.knowceans.map.InvertibleHashMultiMap;
import org.knowceans.util.Print;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.porterStemmer;

/**
 * uses Snowball to stem the given vocabulary.
 * 
 * @author gregor
 * 
 */
public class EnglishStemmer {

	public static void main(String[] args) throws Throwable {
		String filebase = "corpus-example/nips";
		LabelNumCorpus corpus = new LabelNumCorpus(filebase);

		String[] terms = corpus.getResolver().getStrings(CorpusResolver.KTERMS);
		int[] df = corpus.calcDocFreqs();
		EnglishStemmer es = new EnglishStemmer();
		String[] newTerms = es.stemTerms(terms);

		int[] old2new = new int[terms.length];
		String[] newIndex = es.createTermMapping(terms, df, newTerms, old2new);
		for (int i = 0; i < newIndex.length; i++) {
			System.out.println(newIndex[i]);
		}
		Print.arraysSep("\n", old2new);
	}

	private SnowballStemmer stemmer;

	/**
	 * apply stemming to the corpus, updating both the tokens and term index in
	 * its resolver.
	 * 
	 * @param corpus a corpus with a resolver
	 * @return the old2new mapping applied to the corpus
	 */
	public int[] stem(NumCorpus corpus) {
		int[] old2new = null;
		try {
			String[] terms = corpus.getResolver().getStrings(
					CorpusResolver.KTERMS);
			int[] df = corpus.calcDocFreqs();
			EnglishStemmer es = new EnglishStemmer();
			String[] stemmed;
			stemmed = stemTerms(terms);
			old2new = new int[terms.length];
			String[] newIndex = es.createTermMapping(terms, df, stemmed,
					old2new);
			corpus.mergeTerms(old2new, newIndex);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return old2new;
	}

	public class Term {
		String value;
		int id;
		int df;

		Term(String value, int id, int df) {
			this.value = value;
			this.id = id;
			this.df = df;
		}

		@Override
		public String toString() {
			return value + ":" + id + ":" + df;
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return value.equals(obj);
		}
	}

	/**
	 * create mapping from term index to new term index. The
	 * 
	 * @param terms old terms, unique
	 * @param newTerms new terms, with duplicates
	 * @param old2new [out] mapping of old terms to new ones
	 * @return array of new vocabulary that corresponds to old2new (no
	 *         duplicates, so the length of this array is the size of the new
	 *         vocabulary).
	 */
	public String[] createTermMapping(String[] terms, int[] df,
			String[] newTerms, int[] old2new) {
		InvertibleHashMultiMap<Term, String> old2newMap = new InvertibleHashMultiMap<Term, String>();
		for (int i = 0; i < terms.length; i++) {
			Term oldTerm = new Term(terms[i], i, df[i]);
			old2newMap.add(oldTerm, newTerms[i]);
		}
		IMultiMap<String, Term> new2oldMap = old2newMap.getInverse();

		List<Term> ordered = new ArrayList<Term>();
		int i = 0;
		for (String term : new2oldMap.keySet()) {
			int tdf = 0;
			for (Term oldTerm : new2oldMap.get(term)) {
				tdf += oldTerm.df;
			}
			ordered.add(new Term(term, i, tdf));
			i++;
		}
		// sort terms by reverse df, then forward lexicographically
		Collections.sort(ordered, new Comparator<Term>() {

			@Override
			public int compare(Term o1, Term o2) {
				if (o1.df < o2.df) {
					return 1;
				} else if (o1.df > o2.df) {
					return -1;
				} else
					return o1.value.compareTo(o2.value);
			}
		});

		String[] newIndex = new String[new2oldMap.size()];
		for (int j = 0; j < ordered.size(); j++) {
			newIndex[j] = ordered.get(j).value;
			// create inverse mapping
			Set<Term> oldTerms = new2oldMap.get(newIndex[j]);
			String x = "";
			for (Term oldTerm : oldTerms) {
				// x += " " + oldTerm.toString();
				x += " " + oldTerm.value;
				old2new[oldTerm.id] = j;
			}
			newIndex[j] += /* ":" + j + */" <-" + x;
		}
		return newIndex;
	}

	/**
	 * stem
	 * 
	 * @param terms
	 * @return stemmed terms
	 * @throws Throwable
	 */
	public String[] stemTerms(String[] terms) throws Throwable {
		String[] newTerms = new String[terms.length];
		stemmer = new porterStemmer();
		for (int i = 0; i < terms.length; i++) {
			newTerms[i] = stem(terms[i]);
		}
		return newTerms;
	}

	/**
	 * stem the current term. This method is synchronised because the stemmer is
	 * not thread-safe.
	 * 
	 * @param word
	 * @return
	 */
	public synchronized String stem(String word) {
		stemmer.setCurrent(word);
		stemmer.stem();
		return stemmer.getCurrent();
	}
}
