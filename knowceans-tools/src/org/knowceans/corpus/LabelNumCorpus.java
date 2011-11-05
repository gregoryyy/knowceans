/*
 * (C) Copyright 2005, Gregor Heinrich (gregor :: arbylon : net) (This file is
 * part of the lda-j (org.knowceans.lda.*) experimental software package.)
 */
/*
 * lda-j is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 */
/*
 * lda-j is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
/*
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * Created on Dec 3, 2004
 */
package org.knowceans.corpus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.knowceans.util.ArrayUtils;
import org.knowceans.util.Print;
import org.knowceans.util.Vectors;

/**
 * Represents a corpus of documents, using numerical data only.
 * <p>
 * 
 * @author heinrich
 */
public class LabelNumCorpus extends NumCorpus implements ILabelCorpus {

	/**
	 * test corpus reading and splitting
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// LabelNumCorpus nc = new LabelNumCorpus("berry95/berry95");
		LabelNumCorpus nc = new LabelNumCorpus("corpus-example/nips");
		nc.getDocLabels(LAUTHORS);
		nc.split(10, 0, new Random());

		System.out.println("train");
		LabelNumCorpus ncc = (LabelNumCorpus) nc.getTrainCorpus();
		System.out.println(ncc);
		int[][] x = ncc.getDocWords(new Random());
		System.out.println(Vectors.print(x));
		System.out.println("labels");
		int[][] a = ncc.getDocLabels(LAUTHORS);
		System.out.println(Vectors.print(a));

		System.out.println("test");
		ncc = (LabelNumCorpus) nc.getTestCorpus();
		System.out.println(ncc);
		x = ncc.getDocWords(new Random());
		System.out.println(Vectors.print(x));
		System.out.println("labels");
		a = ncc.getDocLabels(LAUTHORS);
		System.out.println(Vectors.print(a));

		System.out.println("document mapping");
		System.out.println(Vectors.print(nc.getOrigDocIds()));
	}

	/**
	 * the extensions for the label type constants in ILabelCorpus.L*
	 */
	public static final String[] labelExtensions = { ".authors", ".labels",
			".tags", ".vols", ".years", ".cite", ".ment" };

	public static final String[] labelNames = { "authors", "labels", "tags",
			"volumes", "years", "citations", "mentionings" };

	public static final int LDOCS = -2;
	public static final int LTERMS = -1;
	// these are the rows of the data field in label corpus
	public static final int LAUTHORS = 0;
	public static final int LCATEGORIES = 1;
	public static final int LTAGS = 2;
	public static final int LVOLS = 3;
	public static final int LYEARS = 4;
	public static final int LREFERENCES = 5;
	public static final int LMENTIONS = 6;

	// cardinality constraints for documents (how many per document)
	public static final int[] cardinalityOne = { LVOLS, LYEARS };
	public static final int[] cardinalityGeOne = { LAUTHORS };
	public static final int[] cardinalityGeZero = { LREFERENCES, LMENTIONS,
			LTAGS, LCATEGORIES };

	// may there be labels that don't appear (e.g., documents without inlinks,
	// authors without mentions, categories without instance)
	public static final int[] allowEmptyLabels = { LREFERENCES, LMENTIONS,
			LCATEGORIES };

	// these are relational metadata without key information that need to be
	// handled directly after filtering
	public static final int[] relationalLabels = { LREFERENCES, LMENTIONS };

	/**
	 * array of labels. Elements are filled as soon as readlabels is called.
	 */
	protected int[][][] labels;
	/**
	 * total count of labels
	 */
	protected int[] labelsW;
	/**
	 * total range of labels
	 */
	protected int[] labelsV;

	/**
     * 
     */
	public LabelNumCorpus() {
		super();
		init();
	}

	/**
	 * @param dataFilebase (filename without extension)
	 */
	public LabelNumCorpus(String dataFilebase) {
		super(dataFilebase);
		this.dataFilebase = dataFilebase;
		init();
	}

	/**
	 * @param dataFilebase (filename without extension)
	 * @param parmode if true read paragraph corpus
	 */
	public LabelNumCorpus(String dataFilebase, boolean parmode) {
		super(dataFilebase + (parmode ? ".par" : ""));
		this.dataFilebase = dataFilebase;
		init();
	}

	/**
	 * @param dataFilebase (filename without extension)
	 * @param readlimit number of docs to reduce corpus when reading (-1 =
	 *        unlimited)
	 * @param parmode if true read paragraph corpus
	 */
	public LabelNumCorpus(String dataFilebase, int readlimit, boolean parmode) {
		super(dataFilebase + (parmode ? ".par" : ""), readlimit);
		this.dataFilebase = dataFilebase;
		init();
	}

	/**
	 * create label corpus from standard one
	 * 
	 * @param corp
	 */
	public LabelNumCorpus(NumCorpus corp) {
		this.docs = corp.docs;
		this.numDocs = corp.numDocs;
		this.numTerms = corp.numTerms;
		this.numWords = corp.numWords;
		init();
	}

	protected void init() {
		labels = new int[labelExtensions.length][][];
		labelsW = new int[labelExtensions.length];
		labelsV = new int[labelExtensions.length];
	}

	/**
	 * checks whether the corpus has labels
	 * 
	 * @param kind according to label constants ILabelCorpus.L*
	 * @return 0 for no label values, 1 for yes, 2 for loaded, -1 for illegal
	 */
	public int hasLabels(int kind) {
		if (kind >= labels.length || kind < -2) {
			return -1;
		}
		if (kind < 0) {
			// we have docs and terms loaded
			if (docs != null)
				return 2;
		} else {
			// any metadata
			if (labels[kind] != null) {
				return 2;
			}
			File f = new File(this.dataFilebase + labelExtensions[kind]);
			if (f.exists()) {
				return 1;
			}
		}
		// not loaded
		return 0;
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
	 * loads and returns the document labels of given kind
	 */
	// @Override
	public int[][] getDocLabels(int kind) {
		if (hasLabels(kind) <= 0) {
			return null;
		}
		if (labels[kind] == null)
			readLabels(kind);
		return labels[kind];
	}

	/**
	 * get the labels for one document
	 * 
	 * @param m
	 * @param kind
	 * @return
	 */
	public int[] getDocLabels(int kind, int m) {
		if (hasLabels(kind) <= 0) {
			return null;
		}
		if (labels[kind] == null)
			readLabels(kind);
		return labels[kind][m];
	}

	/**
	 * return the maximum number of labels in any document
	 * 
	 * @param kind
	 * @return
	 */
	public int getLabelsMinN(int kind) {
		int min = Integer.MAX_VALUE;
		for (int m = 0; m < numDocs; m++) {
			min = min < labels[kind][m].length ? min : labels[kind][m].length;
		}
		return min;
	}

	/**
	 * return the maximum number of labels in any document
	 * 
	 * @param kind
	 * @return
	 */
	public int getLabelsMaxN(int kind) {
		int max = 0;
		for (int m = 0; m < numDocs; m++) {
			max = max < labels[kind][m].length ? labels[kind][m].length : max;
		}
		return max;
	}

	// @Override
	public int getLabelsW(int kind) {
		return labelsW[kind];
	}

	// @Override
	public int getLabelsV(int kind) {
		return labelsV[kind];
	}

	/**
	 * read a label file with one line per document and associated labels
	 * 
	 * @param kind
	 * @return
	 */
	private void readLabels(int kind) {
		ArrayList<int[]> data = new ArrayList<int[]>();
		int W = 0;
		int V = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(dataFilebase
					+ labelExtensions[kind]));
			String line;
			int j = 0;
			while ((line = br.readLine()) != null) {
				// remove additional info
				int c = line.indexOf(" : ");
				if (c > -1) {
					line = line.substring(0, c);
				}
				line = line.trim();
				if (line.length() == 0) {
					data.add(new int[0]);
					continue;
				}
				String[] parts = line.split(" ");
				int[] a = new int[parts.length];
				for (int i = 0; i < parts.length; i++) {
					a[i] = Integer.parseInt(parts[i].trim());
					if (a[i] >= V) {
						V = a[i] + 1;
					}
				}
				W += a.length;
				data.add(a);
				j++;
			}
			br.close();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		labels[kind] = data.toArray(new int[0][0]);
		labelsW[kind] = W;
		labelsV[kind] = V;
		Print.fln("labels loaded: %s: V = %d, W = %d", labelNames[kind], V, W);
	}

	// document filtering

	/**
	 * filter out documents with empty labels of the type in the set.
	 * 
	 * @param set of L constants
	 * @return old2new indices
	 */
	public int[] reduceEmptyLabels(final Set<Integer> labelTypes) {
		DocPredicate filter = new DocPredicate() {
			@Override
			public boolean doesApply(NumCorpus self, int m) {
				for (int ltype : labelTypes) {
					if (labels[ltype][m].length == 0) {
						return false;
					}
				}
				return true;
			}
		};
		return filterDocs(filter, null);
	}

	/**
	 * removes documents that have no inlinks and/or no outlinks.
	 * 
	 * @return
	 */
	// , TODO: boolean usementions
	public int[] reduceUnlinkedDocs(final boolean in, final boolean out) {

		// first create a list of incoming links by transposing the relation
		@SuppressWarnings("unchecked")
		final List<Integer>[] inlinks = new List[numDocs];
		for (int m = 0; m < inlinks.length; m++) {
			inlinks[m] = new ArrayList<Integer>();
		}

		final int[][] citations = labels[LREFERENCES];
		// TODO: add mentions
		// int[][] mentions = labels[LMENTIONS];
		// int[][] authors = labels[LAUTHORS];

		for (int m = 0; m < inlinks.length; m++) {
			for (int i = 0; i < citations[m].length; i++) {
				inlinks[citations[m][i]].add(m);
			}
		}

		DocPredicate filter = new DocPredicate() {
			@Override
			public boolean doesApply(NumCorpus self, int m) {
				if (in && out) {
					return inlinks[m].size() > 0 && citations[m].length > 0;
				} else if (!out) {
					return inlinks[m].size() > 0;
				} else if (!in) {
					return citations[m].length > 0;
				} else {
					return false;
				}
			}
		};
		return filterDocs(filter, null);
	}

	/**
	 * filter documents. Also updates the resolver. Vocabulary must be rebuilt
	 * separately because frequencies change: use filterTermsDf(). Because
	 * citations are directly affected, this label type is updated here, as
	 * well.
	 * 
	 * @param filter predicate to keep documents in list
	 * @param rand random number generator to be used generate a random
	 *        permutation, null if no random permutation
	 * 
	 * @return old2new indices
	 */
	public int[] filterDocs(DocPredicate filter, Random rand) {
		int[] old2new = super.filterDocs(filter, rand);
		// by now, we have filtered documents (even by label predicates) and
		// need to sync the labels to them
		int[][][] newLabels = new int[labelExtensions.length][][];
		int[] newLabelsW = new int[labelExtensions.length];
		for (int type = 0; type < labelExtensions.length; type++) {
			System.out.println("label type " + labelNames[type] + "...");
			if (labels[type] != null) {
				// numDocs is the new size
				newLabels[type] = new int[numDocs][];
				for (int m = 0; m < labels[type].length; m++) {
					if (old2new[m] >= 0) {
						System.out.println(String.format(
								"label m = %d, old2new[] = %d: %s", m,
								old2new[m], Vectors.print(labels[type][m])));
						newLabels[type][old2new[m]] = labels[type][m];
						newLabelsW[type] += labels[type][m].length;
					}
				}
			}
		}
		labels = newLabels;
		labelsW = newLabelsW;
		if (labels[LREFERENCES] != null) {
			System.out.println("filter references");
			// filter citations here, otherwise old2new is awkward to handle
			labelsW[LREFERENCES] = rewriteLabels(LREFERENCES, old2new);
			// determine number of unique cited documents
			// labelsV[LREFERENCES] = getVocabSize(labels[LREFERENCES]);
			// gaps allowed...
			labelsV[LREFERENCES] = numDocs;
		}
		return old2new;
	}

	/**
	 * count the number of distinct values in x
	 * 
	 * @param x
	 * @return
	 */
	protected int getVocabSize(int[][] x) {
		Set<Integer> refV = new HashSet<Integer>();
		for (int m = 0; m < x.length; m++) {
			for (int i = 0; i < x[m].length; i++) {
				refV.add(x[m][i]);
			}
		}
		int y = refV.size();
		return y;
	}

	/**
	 * calculates the document frequencies of the labels
	 * 
	 * @param label type
	 * @return
	 */
	public int[] calcLabelDocFreqs(int type) {
		// we construct term frequencies manually even if there may
		// be another source
		int[] df = new int[labelsV[type]];
		for (int m = 0; m < numDocs; m++) {
			for (int t = 0; t < labels[type][m].length; t++) {
				df[labels[type][m][t]]++;
			}
		}
		return df;
	}

	/**
	 * filter labels (of all types) that do not exist in the corpus
	 */
	public void filterLabels() {
		for (int type = 0; type < labelExtensions.length; type++) {
			if (Arrays.binarySearch(relationalLabels, type) >= 0) {
				// skip relations
				continue;
			}
			System.out.println("reduce labels type " + labelNames[type]);
			filterLabelsDf(type, 1);
		}
	}

	/**
	 * filter labels by frequency. The corpus resolver obtained by getResolver()
	 * is updated to the new label mapping. Note that this does not apply to the
	 * relational label type references and mentions. Instead, filterDocs
	 * directly updates references and filterLabelsDf(type=LAUTHORS, ... )
	 * updates mentions, so there's no need to manage old2new indices
	 * separately.
	 * 
	 * @param minDf all more scarce terms are excluded
	 * @param maxDf all more frequent terms are excluded
	 * @return array with new indices in old index elements or null if nothing
	 *         was changed.
	 */
	public int[] filterLabelsDf(int type, int minDf) {
		if (labels[type] == null) {
			return null;
		}
		// skip relational labels
		if (Arrays.binarySearch(relationalLabels, type) >= 0) {
			return null;
		}
		int[] df = calcLabelDocFreqs(type);
		// rewrite indices
		int[] old2new = new int[labelsV[type]];
		int newIndex = 0;
		for (int t = 0; t < labelsV[type]; t++) {
			if (df[t] < minDf) {
				old2new[t] = -1;
			} else {
				old2new[t] = newIndex;
				newIndex++;
			}
		}
		// rewrite corpus
		labelsW[type] = rewriteLabels(type, old2new);
		labelsV[type] = newIndex;

		if (type == LAUTHORS && labels[LMENTIONS] != null) {
			System.out.println("filter mentioned authors");
			// if mentionings, we should rewrite with authors' old2new
			labelsW[LMENTIONS] = rewriteLabels(LMENTIONS, old2new);
			// labelsV[LMENTIONS] = getVocabSize(labels[LMENTIONS]);
			// gaps allowed...
			labelsV[LMENTIONS] = labelsV[LAUTHORS];
		}

		// map to novel label indices (need to translate type)
		int keytype = CorpusResolver.labelId2keyExt[type];
		System.out.println(String.format(
				"label type = %s, id = %d --> key type = %s, id = %d ",
				labelNames[type], type, CorpusResolver.keyNames[keytype],
				keytype));
		getResolver()
				.filterLabels(CorpusResolver.labelId2keyExt[type], old2new);
		return old2new;
	}

	/**
	 * rewrite the labels of given type throughout the corpus, using mapping
	 * old2new
	 * 
	 * @param type
	 * @param old2new
	 * @param return number of label instances (words) in corpus
	 */
	protected int rewriteLabels(int type, int[] old2new) {
		int W = 0;
		for (int m = 0; m < numDocs; m++) {
			List<Integer> tt = new ArrayList<Integer>();
			int[] ll = labels[type][m];
			System.out.println("*** doc " + m);
			for (int i = 0; i < ll.length; i++) {
				int label = ll[i];
				if (old2new[label] >= 0) {
					tt.add(old2new[label]);
					W++;
					System.out.println("add " + label + "->" + old2new[label]);
				} else {
					System.out.println("dump " + label + "->" + old2new[label]);
				}
			}
			labels[type][m] = (int[]) ArrayUtils
					.asPrimitiveArray(tt, int.class);
		}
		return W;
	}

	// end document filtering

	@Override
	public void split(int order, int split, Random rand) {
		// get plain num corpora
		super.split(order, split, rand);

		// now also split labels
		int Mtest = splitstarts[split + 1] - splitstarts[split];
		labelsW = new int[labelExtensions.length];

		int[][][] trainLabels = new int[labelExtensions.length][numDocs - Mtest][];
		int[][][] testLabels = new int[labelExtensions.length][Mtest][];
		int[] trainLabelsW = new int[labelExtensions.length];
		int[] testLabelsW = new int[labelExtensions.length];

		int mstart = splitstarts[split];
		// for each label type
		for (int type = 0; type < labelExtensions.length; type++) {
			if (labels[type] == null) {
				continue;
			}
			int mtrain = 0;
			// before test split
			for (int m = 0; m < splitstarts[split]; m++) {
				trainLabels[type][mtrain] = labels[type][splitperm[m]];
				mtrain++;
			}
			// after test split
			for (int m = splitstarts[split + 1]; m < numDocs; m++) {
				trainLabels[type][mtrain] = labels[type][splitperm[m]];
				mtrain++;
			}
			// test split
			for (int m = 0; m < Mtest; m++) {
				testLabels[type][m] = labels[type][splitperm[m + mstart]];
				testLabelsW[type] += testLabels[type][m].length;
			}
			trainLabelsW[type] = labelsW[type] - testLabelsW[type];
		}
		// construct subcorpora
		trainCorpus = new LabelNumCorpus((NumCorpus) getTrainCorpus());
		testCorpus = new LabelNumCorpus((NumCorpus) getTestCorpus());
		LabelNumCorpus train = (LabelNumCorpus) trainCorpus;
		train.labels = trainLabels;
		train.labelsV = labelsV;
		train.labelsW = trainLabelsW;
		// readonly corpus we can copy this for split corpora, so the resolver
		// can be created directly from train (and test, below)
		train.dataFilebase = dataFilebase;
		LabelNumCorpus test = (LabelNumCorpus) testCorpus;
		test.labels = testLabels;
		test.labelsV = labelsV;
		test.labelsW = testLabelsW;
		test.dataFilebase = dataFilebase;
	}

	@Override
	public void write(String pathbase) throws IOException {
		// TODO: fully test
		super.write(pathbase);
		// write the stuff that labels add to the plain NumCorpus
		for (int type = 0; type < labelExtensions.length; type++) {
			if (labels[type] == null) {
				continue;
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(pathbase
					+ labelExtensions[type]));
			for (int m = 0; m < numDocs; m++) {
				// NOTE: null and zero-length are treated same, but null may be
				// an error
				if (labels[type][m] != null) {
					for (int n = 0; n < labels[type][m].length; n++) {
						if (n > 0) {
							bw.write(' ');
						}
						bw.write(Integer.toString(labels[type][m][n]));
					}
				}
				bw.append('\n');
			}
			bw.close();
		}
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		// corpus statistics
		sb.append(String.format("LabelNumCorpus instance:\n"));
		sb.append(String.format("file base: %s\n", dataFilebase));
		sb.append(String.format("docs: M = %d, V = %d, W = %d\n", getNumDocs(),
				getNumTerms(), getNumWords()));
		sb.append(String
				.format("labels (0 = not available, 1 = available, 2 = loaded):\n"));
		for (int i = 0; i < LabelNumCorpus.labelExtensions.length; i++) {
			sb.append(String.format(" %s = %d, .keys = %d\n",
					LabelNumCorpus.labelExtensions[i], hasLabels(i),
					resolver.hasLabelKeys(i + 2)));
			if (hasLabels(i) >= 2) {
				sb.append(String.format(
						"    V = %d, W = %d, N[m] = [%d, %d]\n", getLabelsV(i),
						getLabelsW(i), getLabelsMinN(i), getLabelsMaxN(i)));
			}
		}
		return sb.toString();
	}

	/**
	 * check the consistency of the corpus, basically checking for array sizes
	 * in conjunction with the index values contained.
	 * 
	 * @param resolver whether to include the resolver class
	 * @return error report or null if ok.
	 */
	public String check(boolean resolver) {
		StringBuffer sb = new StringBuffer();
		// TODO: we have the resolver (including labels) checked before the
		// numerical labels... suboptimal but ok for a consistency check but.
		String st = super.check(resolver);
		sb.append(st == null ? "" : st);

		for (int type = 0; type < labelExtensions.length; type++) {
			if (labels[type] != null) {
				if (labels[type].length != numDocs) {
					// each document needs to have a label
					sb.append(String
							.format("label type %s length = %d != document count M = %d\n",
									labelNames[type], labels[type].length,
									numDocs));
				} else {
					// check whether labels array size matches that of the
					// metadata
					int W = 0;
					int V = labelsV[type];
					int[] ll = new int[V];
					boolean needOne = Arrays.binarySearch(cardinalityGeOne,
							type) >= 0;
					boolean exactlyOne = Arrays.binarySearch(cardinalityOne,
							type) >= 0;
					// check W and availability of all labels
					for (int m = 0; m < numDocs; m++) {
						int[] row = labels[type][m];
						if (row == null) {
							sb.append(String.format(
									"label type %s document %d = null\n",
									labelNames[type], m));
							continue;
						}
						for (int n = 0; n < row.length; n++) {
							if (row[n] < labelsV[type]) {
								ll[row[n]]++;
							} else {
								sb.append(String
										.format("label type %s [%d][%d]  %d > V = %d\n",
												labelNames[type], m, n, row[n],
												V));
							}
						}
						if (((exactlyOne || needOne) && labels[type][m].length == 0)
								|| (exactlyOne && labels[type][m].length > 1)) {
							sb.append(String
									.format("label type %s : %d cardinality constraint broken: m = %d\n",
											labelNames[type], m));
						}
						W += labels[type][m].length;
					}
					boolean cannotBeEmpty = Arrays.binarySearch(
							allowEmptyLabels, type) < 0;
					if (cannotBeEmpty) {
						for (int t = 0; t < ll.length; t++) {
							if (ll[t] == 0) {
								sb.append(String.format(
										"label type %s : %d frequency = 0\n",
										labelNames[type], t));
							}
						}
					}
				}
			}
		}
		return sb.length() != 0 ? sb.toString() : null;
	}
}
