/*
 * Created on Jan 24, 2010
 */
package org.knowceans.corpus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.knowceans.util.IndexQuickSort;
import org.knowceans.util.Vectors;

/**
 * LdaTopicCoherence calculates a measure similar to the pointwise mutual
 * information between one term and another, co-occurring one. After Mimno et
 * al. (EMNLP 2011)
 * 
 * @author gregor
 */
public class LdaTopicCoherence {

	// corpus forward index
	private int[][] docterms;
	// to cache co-occurrence values
	Map<Integer, Map<Integer, Integer>> term2term2df;
	private int[][] topic2termrank;
	private int[] df;
	private NumCorpus corpus;

	public static boolean debug = false;

	/**
	 * initialise with existing corpus
	 * 
	 * @param corpus
	 */
	public LdaTopicCoherence(NumCorpus corpus) {
		this.docterms = corpus.getDocTermsFreqs()[0];
		this.df = corpus.calcDocFreqs();
		this.corpus = corpus;
	}

	/**
	 * compute coherence values for all topics in phi, using the limit strongest
	 * terms.
	 * 
	 * @param phi
	 * @param limit
	 * @return
	 */
	public double[] getCoherence(double[][] phi, int limit) {
		int K = phi.length;
		double[] tc = new double[K];
		topic2termrank = new int[K][];
		for (int k = 0; k < phi.length; k++) {
			// get reverse ranking of terms by weight in phi
			int[] rank = IndexQuickSort.revsort(phi[k]);
			// ... and cut off at limit
			topic2termrank[k] = Vectors.sub(rank, 0, limit);
		}
		term2term2df = new HashMap<Integer, Map<Integer, Integer>>();
		for (int k = 0; k < K; k++) {
			tc[k] = topicCoherence(topic2termrank[k]);
		}
		return tc;
	}

	/**
	 * perform TC analysis
	 * 
	 * @param terms
	 * @return
	 */
	private double topicCoherence(int[] terms) {

		double tc = 0;
		for (int i = 1; i < terms.length; i++) {
			for (int j = 0; j < i - 1; j++) {
				int cooc = coocDf(terms[i], terms[j]);
				tc += Math.log((cooc + 1.) / df[terms[j]]);
			}
		}
		return tc;
	}

	/**
	 * get the co-occurrence df of both terms and cache it
	 * 
	 * @param term1 assuming term1 < term2
	 * @param term2
	 * @return
	 */
	private int coocDf(int term1, int term2) {
		if (term2 < term1) {
			return coocDf(term2, term1);
		}
		Map<Integer, Integer> t2f = term2term2df.get(term1);
		if (t2f != null) {
			Integer f = t2f.get(term2);
			if (f != null) {
				return f;
			}
		} else {
			t2f = new HashMap<Integer, Integer>();
			term2term2df.put(term1, t2f);
		}
		int freq = 0;
		// TODO: this is overkill --> create co-occurrence for complete set of
		// all terms considered in the topics
		for (int m = 0; m < docterms.length; m++) {
			if (Arrays.binarySearch(docterms[m], term1) >= 0
					&& Arrays.binarySearch(docterms[m], term2) >= 0) {
				freq++;
			}
		}
		t2f.put(term2, freq);
		if (debug) {
			System.out.println("coocc: "
					+ corpus.getResolver().resolveTerm(term1) + " "
					+ corpus.getResolver().resolveTerm(term2) + " = " + freq
					+ " " + (freq + 1.) / df[term2]);
		}
		return freq;
	}
}
