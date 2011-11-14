/*
 * Created on Jan 24, 2010
 */
package org.knowceans.corpus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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

	private double[][] phi;
	private int[][] docterms;
	Map<Integer, Map<Integer, Integer>> term2term2df;
	private int[][] rank2bestTerms;
	private int[] df;

	/**
	 * initialise with existing corpus
	 * 
	 * @param corpus
	 * @param phi
	 */
	public LdaTopicCoherence(LabelNumCorpus corpus, double[][] phi) {
		this.docterms = corpus.getDocTermsFreqs()[0];
		this.phi = phi;
		this.df = corpus.calcDocFreqs();

	}

	/**
	 * 
	 * @return
	 */
	public double[] getCoherence(int limit) {

		double[] tc = new double[phi.length];

		int K = phi.length;
		rank2bestTerms = new int[K][];
		for (int k = 0; k < phi.length; k++) {
			int[] rank = IndexQuickSort.revsort(phi[k]);
			rank2bestTerms[k] = Vectors.sub(rank, 0, limit);
		}

		// to cache co-occurrence values
		term2term2df = new HashMap<Integer, Map<Integer, Integer>>();
		for (int k = 0; k < K; k++) {
			tc[k] = topicCoherence(rank2bestTerms[k]);
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
		for (int i = 2; i < terms.length; i++) {
			for (int j = 0; j < i - 1; j++) {
				int cooc = getCoocCount(terms[i], terms[j]);
				tc += Math.log((cooc + 1.) / df[terms[j]]);
			}
		}
		return tc;
	}

	/**
	 * get the co-occurrence count of both terms and cache it
	 * 
	 * @param term1 assuming term1 < term2
	 * @param term2
	 * @return
	 */
	private int getCoocCount(int term1, int term2) {
		if (term2 < term1) {
			return getCoocCount(term2, term1);
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
		return freq;
	}
}
