/*
 * Created on Jan 24, 2010
 */
package org.knowceans.corpus;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import org.knowceans.corpus.CorpusResolver;
import org.knowceans.corpus.ILabelCorpus;
import org.knowceans.corpus.LabelNumCorpus;
import org.knowceans.util.IndexQuickSort;
import org.knowceans.util.RandomSamplers;
import org.knowceans.util.Vectors;

/**
 * LdaTeaLeaves shows presents documents and topics for evaluation of the
 * subjective topic coherence measures put forward by Chang et al. Reading Tea
 * Leaves -- How Humans Interpret Topic Models (NIPS 2009).
 * 
 * @author gregor
 */
public class LdaTeaLeaves {

	CorpusResolver resolver;

	private double[][] phi;
	private double[][] theta;
	private LabelNumCorpus corpus;
	private RandomSamplers rand;

	/**
	 * topic weight over corpus, k -> p_k
	 */
	private double[] pk;

	/**
	 * topic ranks k -> rank
	 */
	private int[] rk;

	private int[] theta2docIds;

	/**
	 * initialise with filebase
	 * 
	 * @param filebase
	 * @param theta
	 * @param phi
	 * @param rand
	 */
	public LdaTeaLeaves(String filebase, double[][] theta, double[][] phi,
			Random rand) {
		this(new LabelNumCorpus(filebase), theta, phi, rand);
	}

	/**
	 * initialise with existing corpus
	 * 
	 * @param corpus
	 * @param theta
	 * @param phi
	 * @param rand
	 */
	public LdaTeaLeaves(LabelNumCorpus corpus, double[][] theta,
			double[][] phi, Random rand) {
		this.resolver = corpus.getResolver();
		this.corpus = corpus;
		this.theta = theta;
		this.phi = phi;
		this.rand = new RandomSamplers(rand);
		// precalculate topic absolute weights and ranke
		getTopicWeights();
		getTopicRanks();
		// these are the ids of the original corpus
		theta2docIds = corpus.getOrigDocIds()[0];
	}

	/**
	 * create a pair of test documents, one with questions and the second with
	 * the solutions.
	 * 
	 * @param pathbase extended by .lda.teq and .lda.tea
	 * @param numdocs
	 * @param numtopics
	 * @throws IOException
	 */
	public void createTestDocs(String pathbase, int numdocs, int numtopics)
			throws IOException {
		BufferedWriter bwq = new BufferedWriter(new FileWriter(pathbase
				+ ".lda.teq"));
		BufferedWriter bwa = new BufferedWriter(new FileWriter(pathbase
				+ ".lda.tea"));

		// tea leaves for random documents
		int[] a = rand.randPerm(theta2docIds.length);
		numdocs = Math.min(numdocs, theta.length);
		for (int m = 0; m < numdocs; m++) {
			String[] doctest = printDocument(a[m], 3, 1, 20, 50);
			String head = "*** Document " + (m + 1) + " ***\n";
			bwq.append(head);
			bwq.append(doctest[0]).append("\n");
			bwa.append(head);
			bwa.append(doctest[1]).append("\n");
		}

		// tea leaves for random topics
		a = rand.randPerm(phi.length);
		numtopics = Math.min(numtopics, phi.length);
		for (int k = 0; k < numtopics; k++) {
			String[] topictest = printTopic(a[k], 5, 1);
			String head = "*** Topic " + (k + 1) + " ***\n";
			bwq.append(head);
			bwq.append(topictest[0]).append("\n");
			bwa.append(head);
			bwa.append(topictest[1]).append("\n");
		}
		bwq.close();
		bwa.close();
	}

	// high-level methods

	/**
	 * create string with document topic information
	 * 
	 * @param m doc id in model
	 * @param topics how many of the most likely topics to print
	 * @param intruders how many intruders (call with 0 to simply print topic)
	 * @param terms number of terms per topic
	 * @param terms number of terms for document
	 * @return strings for test and solution files
	 */
	public String[] printDocument(int m, int topics, int intruders,
			int ntopicTerms, int ndocTerms) {
		ntopicTerms = Math.min(phi[0].length, ntopicTerms);
		int K = phi.length;
		int morig = theta2docIds[m];
		StringBuffer btest = new StringBuffer();
		StringBuffer bsolv = new StringBuffer();
		btest.append(String.format("%d: %s (theta[%d])\n", morig,
				resolver.resolveDocTitle(morig), m));
		int[] x = corpus.getDocLabels(ILabelCorpus.LAUTHORS, morig);
		btest.append("authors:");
		for (int aa : x) {
			btest.append(" " + resolver.resolveAuthor(aa));
		}
		btest.append("\n");
		bsolv.append(btest);
		// add doc content to test
		btest.append("content:\n");
		int[] tt = corpus.getDoc(morig).getTerms();
		int[] ff = corpus.getDoc(morig).getCounts();
		int[] ranks = IndexQuickSort.reverse(IndexQuickSort.sort(ff));
		for (int t = 0; t < Math.min(ndocTerms, tt.length); t++) {
			if (t % 4 == 0 && t > 0) {
				btest.append("\n");
			}
			btest.append(String.format("\t%25s:%d", corpus.getResolver()
					.resolveTerm(tt[ranks[t]]), ff[ranks[t]]));
		}
		btest.append("\n");

		// sort topics by weight
		int[] a = IndexQuickSort.revsort(theta[m]);
		int[] k2rank = IndexQuickSort.inverse(a);
		int[] shortlist = new int[topics + intruders];

		// wanting too much?
		if (a.length < topics + intruders) {
			return new String[] {
					"error: topics must exceed number of positive and negative examples)",
					"" };
		}
		// best topics
		for (int k = 0; k < topics; k++) {
			shortlist[k] = a[k];
		}
		// worst topics
		for (int k = 0; k < intruders; k++) {
			shortlist[topics + k] = a[a.length - k - 1];
		}
		// scramble list
		int[] seq = rand.randPerm(topics + intruders);

		btest.append("\ntopics in document (choose 1-" + shortlist.length
				+ ", which matches worst):\n");
		bsolv.append("\ntopics in document (* = intruder):\n");
		for (int idxseq = 0; idxseq < seq.length; idxseq++) {
			int idxshortlist = seq[idxseq];
			int k = shortlist[idxshortlist];
			// sort and truncate topic terms
			int[] terms = Vectors.sub(IndexQuickSort.revsort(phi[k]), 0,
					ntopicTerms);
			// write query, test option has 1-based index
			btest.append(String.format("    %3d. %s", idxseq + 1,
					printTopic(k, terms, false, true)));
			// write solution, test option has 1-based index
			bsolv.append(String.format(
					"  %s %2d. k = %3d. p(k|m) = %6.3f/%d, rank %2d: %s",
					idxshortlist >= topics ? "*" : " ", idxseq + 1, k,
					theta[m][k] * K, K, k2rank[k],
					printTopic(k, terms, true, true)));
		}
		return new String[] { btest.toString(), bsolv.toString() };
	}

	/**
	 * print topic for testing
	 * 
	 * @param k
	 * @param nterms per topic
	 * @param nintruders
	 * @return
	 */
	public String[] printTopic(int k, int nterms, int nintruders) {

		nterms = Math.min(phi[0].length, nterms);
		int V = phi[0].length;
		StringBuffer btest = new StringBuffer();
		StringBuffer bsolv = new StringBuffer();

		// sort topics by weight
		int[] a = IndexQuickSort.revsort(phi[k]);
		int[] t2rank = IndexQuickSort.inverse(a);
		int[] shortlist = new int[nterms + nintruders];

		// wanting too much?
		if (a.length < nterms + nintruders) {
			return new String[] {
					"error: topics must exceed number of positive and negative examples)",
					"" };
		}
		// best topics
		for (int t = 0; t < nterms; t++) {
			shortlist[t] = a[t];
		}
		for (int t = 0; t < nintruders; t++) {
			// worst terms from last quarter
			int bad = rand.randUniform((int) (V / 4.));
			shortlist[nterms + t] = a[a.length - bad - 1];
		}
		// scramble list
		int[] seq = rand.randPerm(nterms + nintruders);

		btest.append("\nterms in topic (choose 1-" + shortlist.length
				+ ", which matches worst):\n");
		bsolv.append(String.format(
				"topic %3d (pk = %6.3f/V, V = %d, rank = %3d):\n", k,
				getTopicWeights()[k] * phi.length, phi.length,
				getTopicRanks()[k]));

		bsolv.append("\nterms in topic (* = intruder):\n");
		for (int idxseq = 0; idxseq < seq.length; idxseq++) {
			int t = shortlist[seq[idxseq]];
			String term = resolver.resolveTerm(t);
			// write query, test option has 1-based index
			btest.append(String.format("    %3d. %s\n", idxseq + 1, term));
			// write solution, test option has 1-based index
			bsolv.append(String.format(
					"  %s %2d. t = %3d. p(k|m) = %6.3f/V, rank %2d: %s\n",
					seq[idxseq] >= nterms ? "*" : " ", idxseq + 1, t, phi[k][t]
							* V, t2rank[t], term));
		}
		return new String[] { btest.toString(), bsolv.toString() };
	}

	// auxiliaries

	/**
	 * print topic with number of terms
	 * 
	 * @param k
	 * @param terms
	 * @param format
	 * @return
	 */
	private String printTopic(int k, int[] terms, boolean withWeights,
			boolean singleLine) {
		int K = phi.length;
		int V = phi[0].length;
		String newLine = singleLine ? " " : "\n\t";
		String u = "";
		if (withWeights) {
			u += String.format("topic %3d (pk = %6.3f/%d, rank = %3d):%s", k,
					getTopicWeights()[k] * K, K, getTopicRanks()[k], newLine);
		}

		for (int i = 0; i < terms.length; i++) {
			int t = terms[i];
			if (withWeights) {
				u += String.format("%s (%5.3f/V)%s", corpus.getResolver()
						.resolveTerm(t), phi[k][t] * V, newLine);
			} else {
				u += String.format("%s%s", corpus.getResolver().resolveTerm(t),
						newLine);
			}
		}
		u += "\n";
		return u;
	}

	/**
	 * get the topic ranks based on the topic weights
	 * 
	 * @return
	 */
	public int[] getTopicRanks() {
		getTopicWeights();
		if (rk == null) {
			// revsort = indices of topics, ranks = index of k in revsort
			rk = IndexQuickSort.inverse(IndexQuickSort.revsort(pk));
		}
		return rk;
	}

	/**
	 * get the relative weights of the topics according to theta. Weights are
	 * cached for repeated calls.
	 * 
	 * @return
	 */
	public double[] getTopicWeights() {
		if (pk == null) {
			pk = new double[phi.length];
			for (int k = 0; k < pk.length; k++) {
				for (int m = 0; m < theta.length; m++) {
					pk[k] += theta[m][k];
				}
			}
			double sum = Vectors.sum(pk);
			Vectors.mult(pk, 1 / sum);
		}
		return pk;
	}
}
