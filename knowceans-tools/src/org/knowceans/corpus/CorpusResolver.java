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

/**
 * CorpusResolver resolves indices into names.
 * 
 * @author gregor
 */
public class CorpusResolver implements ICorpusResolver {

	/**
	 * these indices correspond to ILabelCorpus.L* + 2 (docs and terms are -2
	 * and -1 there)
	 */
	public static final String[] keyExtensions = { "docs", "vocab",
			"authors.key", "labels.key", "tags.key", "vols.key", "years.key" };

	public static final String[] keyNames = { "documents", "terms", "authors",
			"labels", "tags", "volumess", "years" };

	public static final int[] keyExt2labelId = { -2, -1, 0, 1, 2, 3, 4 };
	public static final int[] labelId2keyExt = { 2, 3, 3, 4, 5, -1, -1 };

	public static void main(String[] args) {
		CorpusResolver cr = new CorpusResolver("corpus-example/nips");
		System.out.println(cr.getLabel(20));
		System.out.println(cr.getDocTitle(501));
		System.out.println(cr.getTerm(1));
		System.out.println(cr.getTermId(cr.getTerm(1)));
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
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knowceans.corpus.IResolver#hasValues(int)
	 */
	@Override
	public int hasValues(int i) {
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
	 * filter corpus with term subset with new indices.
	 * 
	 * @param old2new element (old index) contains new index
	 */
	public void filterTerms(int[] old2new) {
		HashMap<String, Integer> newids = new HashMap<String, Integer>();
		List<String> terms = new ArrayList<String>();
		// replace term ids.
		for (int i = 0; i < old2new.length; i++) {
			if (old2new[i] >= 0) {
				newids.put(getTerm(i), old2new[i]);
				terms.add(old2new[i], getTerm(i));
			}
		}
		data[KTERMS] = (String[]) terms.toArray(new String[0]);
		termids = newids;
	}

	/**
	 * write the term set to the file
	 * 
	 * @param file (full file name, no .vocab appended)
	 * @throws IOException
	 */
	public void writeTerms(String file) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		for (String term : data[KTERMS]) {
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
	public String getTerm(int t) {
		if (data[KTERMS] != null) {
			return data[1][t];
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
		if (termids == null) {
			termids = new HashMap<String, Integer>();
			for (int i = 0; i < data[KTERMS].length; i++) {
				termids.put(data[KTERMS][i], i);
			}
		}
		Integer id = termids.get(term);
		if (id == null) {
			id = -1;
		}
		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knowceans.corpus.IResolver#getLabel(int)
	 */
	@Override
	public String getLabel(int i) {
		if (data[KLABELS] != null) {
			return data[KLABELS][i];
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
	public String getAuthor(int i) {
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
	public String getDocTitle(int i) {
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
	public String getDocName(int i) {
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
	public String getDocRef(int i) {
		if (data[KDOCREF] != null) {
			return data[KDOCREF][i];
		} else {
			return null;
		}
	}

	/**
	 * filters the documents according to the new index
	 * 
	 * @param index
	 */
	public void filterDocuments(int[] old2new) {
		throw new Error();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knowceans.corpus.IResolver#getVol(int)
	 */
	@Override
	public String getVol(int i) {
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
	public String getLabel(int type, int id) {
		if (type == KTERMS) {
			return getTerm(id);
		} else if (type == KAUTHORS) {
			return getAuthor(id);
		} else if (type == KLABELS) {
			return getLabel(id);
		} else if (type == KVOLS) {
			return getVol(id);
		} else if (type == KDOCREF) {
			return getDocRef(id);
		} else if (type == KDOCS) {
			return getDocTitle(id);
		} else if (type == KDOCNAME) {
			return getDocName(id);
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
}
