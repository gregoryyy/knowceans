/*
 * Created on Jan 24, 2010
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
import java.util.HashMap;
import java.util.List;

import org.knowceans.util.Print;

/**
 * CorpusResolver resolves indices into names. To live-check for available data,
 * use LabelNumCorpus
 * 
 * @author gregor
 */
public class CorpusResolver implements ICorpusResolver {

	/**
	 * these indices correspond to ILabelCorpus.L* + 2 (docs and terms are -2
	 * and -1 there)
	 */
	public static final String[] keyExtensions = { "docs", "vocab",
			"authors.key", "labels.key", "tags.key", "vols.key", "years.key",
			"docnames", "docs.key", "abstracts", "vocab.key" };

	public static final String[] keyNames = { "documents", "terms", "authors",
			"labels", "tags", "volumes", "years" };

	public static final int[] docRelatedKeys = { KDOCS, KDOCNAME, KDOCREF,
			KDOCCONTENT };
	// TODO: add abstracts
	public static final int[] keyExt2labelId = { -2, -1, 0, 1, 2, 3, 4, -3 };
	public static final int[] labelId2keyExt = { 2, 3, 4, 5, 6, -1, -1, -1 };

	public static void main(String[] args) {
		CorpusResolver cr = new CorpusResolver("corpus-example/nips");
		System.out.println(cr.resolveCategory(20));
		System.out.println(cr.resolveDocRef(501));
		System.out.println(cr.resolveDocTitle(501));
		System.out.println(cr.resolveDocContent(501));
		System.out.println(cr.resolveTerm(1));
		System.out.println(cr.getTermId(cr.resolveTerm(1)));
	}

	HashMap<String, Integer> termids;
	String[][] data = new String[keyExtensions.length][];
	String filebase;

	@SuppressWarnings("unused")
	private boolean parmode;

	public CorpusResolver(String filebase) {
		this(filebase, false);
	}

	/**
	 * control paragraph mode (possibly different vocabulary)
	 * 
	 * @param filebase
	 * @param parmode
	 */
	public CorpusResolver(String filebase, boolean parmode) {
		this.parmode = parmode;
		this.filebase = filebase;
		for (int i = 0; i < keyExtensions.length; i++) {
			String base = filebase;
			// read alternative vocabulary for paragraph mode
			if (parmode && keyExtensions[i].equals("vocab")) {
				base += ".par";
			}
			File f = new File(base + "." + keyExtensions[i]);
			if (f.exists()) {
				data[i] = load(f);
			}
		}
		// set up terms
		setupTermIndex();
	}

	/**
	 * create a list to lookup terms
	 */
	protected void setupTermIndex() {
		if (termids == null && data[KTERMS] != null) {
			termids = new HashMap<String, Integer>();
			for (int i = 0; i < data[KTERMS].length; i++) {
				termids.put(data[KTERMS][i], i);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knowceans.corpus.IResolver#hasValues(int)
	 */
	@Override
	public int hasLabelKeys(int i) {
		if (i >= keyExtensions.length || i < 0) {
			return -1;
		}
		// in the current impl, labels are pre-fetched
		return (data[i] != null ? 2 : 0);
	}

	/**
	 * load from file removing every information after a = sign in each line
	 * 
	 * @param f
	 * @return array of label strings
	 */
	private String[] load(File f) {
		String[] strings = null;
		try {
			ArrayList<String> a = new ArrayList<String>();
			BufferedReader br = new BufferedReader(new FileReader(f));
			String line = null;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				int ii = line.indexOf('=');
				if (ii > -1) {
					a.add(line.substring(0, ii).trim());
				} else {
					a.add(line.trim());
				}
			}
			br.close();
			strings = a.toArray(new String[] {});
			return strings;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return strings;
	}

	/**
	 * filter corpus with term subset with new indices. Mapping must be
	 * one-to-one-or-less, use mergeTerms for one-to-many mapping.
	 * 
	 * @param old2new element (old index) contains new index
	 */
	public void filterTerms(int[] old2new) {
		HashMap<String, Integer> newids = new HashMap<String, Integer>();
		List<String> terms = new ArrayList<String>();
		// replace term ids.
		for (int i = 0; i < old2new.length; i++) {
			if (old2new[i] >= 0) {
				// update term index
				newids.put(resolveTerm(i), old2new[i]);
				terms.add(old2new[i], resolveTerm(i));
			}
		}
		data[KTERMS] = (String[]) terms.toArray(new String[0]);
		termids = newids;
		if (data[KTERMSOURCE] != null) {
			// remap term sources
			String[] termSources = new String[terms.size()];
			for (int i = 0; i < old2new.length; i++) {
				if (old2new[i] >= 0) {
					termSources[old2new[i]] = data[KTERMSOURCE][i];
				}
			}
			data[KTERMSOURCE] = termSources;
		}
	}

	/**
	 * filter labels
	 * 
	 * @param type key type
	 * @param old2new
	 */
	public void filterLabels(int type, int[] old2new) {
		List<String> labels = new ArrayList<String>();
		// replace term ids.
		for (int i = 0; i < old2new.length; i++) {
			if (old2new[i] >= 0) {
				// System.out.println("add key " + i + "->" + old2new[i]);
				// overwrite old string content at new index
				labels.add(old2new[i], resolveLabel(type, i));
			}
		}
		data[type] = (String[]) labels.toArray(new String[0]);
	}

	/**
	 * filter label arrays according to the new document ids. keys are not being
	 * touched
	 * 
	 * @param old2new
	 */
	public void filterDocs(int[] old2new) {
		int newM = 0;
		for (int m = 0; m < old2new.length; m++) {
			if (old2new[m] >= 0) {
				newM++;
			}
		}
		String[][] newData = new String[data.length][];
		for (int type = 0; type < data.length; type++) {
			if (data[type] != null) {
				if (Arrays.binarySearch(docRelatedKeys, type) >= 0) {
					newData[type] = new String[newM];
					for (int m = 0; m < old2new.length; m++) {
						if (old2new[m] >= 0) {
							newData[type][old2new[m]] = data[type][m];
						}
					}
				} else {
					// copy over complete array for non-document meta-data
					newData[type] = data[type];
				}
			}
		}
		// System.out.println("resolver docs = " + newData[KDOCS].length);
		data = newData;
	}

	/**
	 * write all key information loaded to the filebase. The directory must
	 * exist. Files are overwritten.
	 * 
	 * @param filebase
	 * @throws IOException
	 */
	public void write(String filebase) throws IOException {
		for (int type = 0; type < keyExtensions.length; type++) {
			if (data[type] != null) {
				write(filebase, type);
			}
		}
	}

	/**
	 * set the terms of the corpus
	 * 
	 * @param terms
	 * @param sourceSeparator separates the actual term from additional
	 *        information that is handled as term source, or null
	 */
	public void setTerms(String[] terms, String sourceSeparator) {
		if (sourceSeparator != null) {
			data[KTERMSOURCE] = terms;
			data[KTERMS] = new String[terms.length];
			for (int i = 0; i < terms.length; i++) {
				data[KTERMS][i] = terms[i].substring(0,
						terms[i].indexOf(sourceSeparator)).trim();
			}
		} else {
			data[KTERMS] = terms;
			data[KTERMSOURCE] = null;
		}
		// Print.arrays("\n", data);
	}

	/**
	 * write the term set to the file with filebase
	 * 
	 * @param filebase (no .vocab etc. appended)
	 * @param type key type (KTERMS, KLABELS etc.)
	 * @throws IOException
	 */
	public void write(String filebase, int type) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(filebase + "."
				+ keyExtensions[type]));
		for (String term : data[type]) {
			bw.append(term).append('\n');
		}
		bw.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knowceans.corpus.IResolver#getTerm(int)
	 */
	@Override
	public String resolveTerm(int t) {
		if (data[KTERMS] != null) {
			return data[KTERMS][t];
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knowceans.corpus.IResolver#getTermId(java.lang.String)
	 */
	@Override
	public int getTermId(String term) {
		Integer id = termids.get(term);
		if (id == null) {
			id = -1;
		}
		return id;
	}

	/**
	 * get string content for all keys
	 * 
	 * @param K-constant
	 * @return
	 */
	public String[] getStrings(int type) {
		return data[type];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knowceans.corpus.IResolver#getLabel(int)
	 */
	@Override
	public String resolveCategory(int i) {
		if (data[KCATEGORIES] != null) {
			return data[KCATEGORIES][i];
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knowceans.corpus.IResolver#getAuthor(int)
	 */
	@Override
	public String resolveAuthor(int i) {
		if (data[KAUTHORS] != null) {
			return data[KAUTHORS][i];
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knowceans.corpus.IResolver#getDoc(int)
	 */
	@Override
	public String resolveDocTitle(int i) {
		if (data[KDOCS] != null) {
			return data[KDOCS][i];
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knowceans.corpus.IResolver#getDocName(int)
	 */
	@Override
	public String resolveDocName(int i) {
		if (data[KDOCNAME] != null) {
			return data[KDOCNAME][i];
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knowceans.corpus.IResolver#getDocKey(int)
	 */
	@Override
	public String resolveDocRef(int i) {
		if (data[KDOCREF] != null) {
			return data[KDOCREF][i];
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knowceans.corpus.IResolver#getDocName(int)
	 */
	@Override
	public String resolveDocContent(int i) {
		if (data[KDOCCONTENT] != null) {
			return data[KDOCCONTENT][i];
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knowceans.corpus.IResolver#getVol(int)
	 */
	@Override
	public String resolveVolume(int i) {
		if (data[KVOLS] != null) {
			return data[KVOLS][i];
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knowceans.corpus.IResolver#getLabel(int, int)
	 */
	@Override
	public String resolveLabel(int type, int id) {
		if (type == KTERMS) {
			return resolveTerm(id);
		} else if (type == KAUTHORS) {
			return resolveAuthor(id);
		} else if (type == KCATEGORIES) {
			return resolveCategory(id);
		} else if (type == KVOLS) {
			return resolveVolume(id);
		} else if (type == KDOCREF) {
			return resolveDocRef(id);
		} else if (type == KDOCCONTENT) {
			return resolveDocContent(id);
		} else if (type == KDOCS) {
			return resolveDocTitle(id);
		} else if (type == KDOCNAME) {
			return resolveDocName(id);
		}
		return null;
	}

	/**
	 * resolve the kind of the label key
	 * 
	 * @param label key constant (KTERMS, KCATEGORIES etc.)
	 * @return
	 */
	public String resolveKeyType(int type) {
		if (type == KTERMS) {
			return "term";
		} else if (type == KAUTHORS) {
			return "author";
		} else if (type == KCATEGORIES) {
			return "category";
		} else if (type == KVOLS) {
			return "volume";
		} else if (type == KDOCREF) {
			return "document summary";
		} else if (type == KDOCCONTENT) {
			return "document content";
		} else if (type == KDOCS) {
			return "document title";
		} else if (type == KDOCNAME) {
			return "document name";
		}
		return null;
	}

	/**
	 * resolve the kind of the label
	 * 
	 * @param label constant L-constant (LTERMS, LCATEGORIES etc.)
	 * @return
	 */
	public String resolveLabelType(int type) {
		if (type == ILabelCorpus.LTERMS) {
			return "term";
		} else if (type == ILabelCorpus.LAUTHORS) {
			return "author";
		} else if (type == ILabelCorpus.LCATEGORIES) {
			return "category";
		} else if (type == ILabelCorpus.LVOLS) {
			return "volume";
		} else if (type == ILabelCorpus.LTAGS) {
			return "document tag";
		} else if (type == ILabelCorpus.LMENTIONS) {
			return "mention";
		} else if (type == ILabelCorpus.LREFERENCES) {
			return "reference";
		} else if (type == ILabelCorpus.LDOCS) {
			return "document name";
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knowceans.corpus.IResolver#getId(int, java.lang.String)
	 */
	@Override
	public int getId(int type, String label) {
		if (type == LabelNumCorpus.LTERMS) {
			return getTermId(label);
		} else if (type == LabelNumCorpus.LAUTHORS) {
			return Arrays.asList(data[type]).indexOf(label);
		}
		return -1;
	}

	/**
	 * check the consistency of the corpus resolver, basically checking for
	 * array sizes in conjunction with the respective corpus quanitities. Labels
	 * to be checked need to be loaded beforehand.
	 * 
	 * @param corpus the corpus to be used
	 * @return error report or "" if ok.
	 */
	public String check(NumCorpus corpus) {
		StringBuffer sb = new StringBuffer();

		// check documents and terms
		if (termids.size() != corpus.getNumTerms()) {
			sb.append(String.format("numTerms = %d != termids = %d\n",
					corpus.getNumTerms(), termids.size()));

		}
		if (data[KDOCS].length != corpus.getNumDocs()) {
			sb.append(String.format("numDocs = %d != data[docs].length = %d\n",
					corpus.getNumDocs(), data[KDOCS].length));
		}

		// advanced stuff for the labels
		if (corpus instanceof LabelNumCorpus) {
			LabelNumCorpus lc = (LabelNumCorpus) corpus;
			// for all key types, compare with labels
			for (int keyType = 0; keyType < keyExt2labelId.length; keyType++) {
				if (data[keyType] != null) {
					int labid = keyExt2labelId[keyType];
					if (labid < 0) {
						// documents and terms are handled by the superclass
						continue;
					}
					if (data[keyType].length != lc.getLabelsV(labid)) {
						sb.append(String.format(
								"keys %s length = %d != corpus V = %d\n",
								keyNames[keyType], data[keyType].length,
								lc.getLabelsV(keyExt2labelId[keyType])));
					}
				}
			}
			for (int keyType : docRelatedKeys) {
				if (data[keyType] != null) {
					if (data[keyType].length != corpus.getNumDocs()) {
						sb.append(String.format(
								"keys %s length = %d != corpus M = %d\n",
								keyNames[keyType], data[keyType].length,
								corpus.getNumDocs()));
					}
				}
			}
		}
		return sb.toString();
	}
}
