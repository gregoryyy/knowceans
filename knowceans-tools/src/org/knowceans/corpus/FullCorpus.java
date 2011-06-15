package org.knowceans.corpus;

import java.io.IOException;
import java.util.Random;

/**
 * a full corpus is a label numeric corpus that uses a resolver to allow
 * consistent access especially to filtered data. Draft stage but usable.
 * 
 * @author gregor
 * 
 */
public class FullCorpus extends LabelNumCorpus implements ICorpusResolver {

	public static void main(String[] args) {
		FullCorpus fc = new FullCorpus("corpus-example/nips");
		fc.loadAllLabels();
		System.out.println(fc);

	}

	public FullCorpus() {
		super();
		initResolver();
	}

	public FullCorpus(NumCorpus corp) {
		super(corp);
		initResolver();
	}

	public FullCorpus(String dataFilebase, boolean parmode) {
		super(dataFilebase, parmode);
		initResolver();
	}

	public FullCorpus(String dataFilebase, int readlimit, boolean parmode) {
		super(dataFilebase, readlimit, parmode);
		initResolver();
	}

	public FullCorpus(String dataFilebase) {
		super(dataFilebase);
		initResolver();
	}

	private void initResolver() {
		// load resolver
		getResolver();
	}

	/**
	 * loads all labels (metadata)
	 */
	public void loadAllLabels() {
		for (int i = 0; i < LabelNumCorpus.labelExtensions.length; i++) {
			if (hasLabels(i) == 1) {
				System.out.println("loading " + LabelNumCorpus.labelNames[i]);
				getDocLabels(i);
			}
		}
	}

	/**
	 * filters the terms and updates the resolver accordingly
	 */
	@Override
	public int[] filterTermsDf(int minDf, int maxDf) {
		int[] old2new = super.filterTermsDf(minDf, maxDf);
		getResolver().filterTerms(old2new);
		return old2new;
	}

	/**
	 * reduce corpus and. Note: rand is currently ignored!
	 */
	@Override
	public void reduce(int ndocs, Random rand) {
		// just reduces the document list
		super.reduce(ndocs, rand);
		// now reduce the resolver fields

	}

	@Override
	public int hasValues(int i) {
		return resolver.hasValues(i);
	}

	public void writeTerms(String file) throws IOException {
		resolver.writeTerms(file);
	}

	@Override
	public String getTerm(int t) {
		return resolver.getTerm(t);
	}

	@Override
	public int getTermId(String term) {
		return resolver.getTermId(term);
	}

	@Override
	public String getLabel(int i) {
		return resolver.getLabel(i);
	}

	@Override
	public String getAuthor(int i) {
		return resolver.getAuthor(i);
	}

	@Override
	public String getDocTitle(int i) {
		return resolver.getDocTitle(i);
	}

	@Override
	public String getDocName(int i) {
		return resolver.getDocName(i);
	}

	@Override
	public String getDocRef(int i) {
		return resolver.getDocRef(i);
	}

	@Override
	public String getVol(int i) {
		return resolver.getVol(i);
	}

	@Override
	public String getLabel(int type, int id) {
		return resolver.getLabel(type, id);
	}

	@Override
	public int getId(int type, String label) {
		return resolver.getId(type, label);
	}

	/**
	 * write corpus stats to string
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		// corpus statistics
		sb.append(String.format("FullCorpus instance:\n"));
		sb.append(String.format("file base: %s\n", dataFilebase));
		sb.append(String.format("docs: M = %d, V = %d, W = %d\n", getNumDocs(),
				getNumTerms(), getNumWords()));
		sb.append(String.format("labels:\n"));
		for (int i = 0; i < LabelNumCorpus.labelExtensions.length; i++) {
			sb.append(String.format(" %s = %d, .keys = %d\n",
					LabelNumCorpus.labelExtensions[i], hasLabels(i),
					resolver.hasValues(i + 2)));
			if (hasLabels(i) >= 2) {
				sb.append(String.format("    V = %d, W = %d, max N[m] = %d\n",
						getLabelsV(i), getLabelsW(i), getLabelsMaxN(i)));
			}
		}
		return sb.toString();

	}
}
