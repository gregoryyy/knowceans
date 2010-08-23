/*
 * Created on Jan 24, 2010
 */
package org.knowceans.corpus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * CorpusResolver resolves indices into names.
 * 
 * @author gregor
 */
public class CorpusResolver {

	public static void main(String[] args) {
		CorpusResolver cr = new CorpusResolver("nips/nips");
		System.out.println(cr.getAuthor(2));
		System.out.println(cr.getLabel(20));
		System.out.println(cr.getDoc(501));
		System.out.println(cr.getTerm(1));
		System.out.println(cr.getTermId(cr.getTerm(1)));
	}

	public final String[] EXTENSIONS = { "docs", "vocab", "authors.key",
			"labels.key", "vols.key" };

	HashMap<String, Integer> termids;
	String[][] data = new String[EXTENSIONS.length][];
	String filebase;

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
		for (int i = 0; i < EXTENSIONS.length; i++) {
			String base = filebase;
			// read alternative vocabulary for paragraph mode
			if (parmode && EXTENSIONS[i].equals("vocab")) {
				base += ".par";
			}
			File f = new File(base + "." + EXTENSIONS[i]);
			if (f.exists()) {
				data[i] = load(f);
			}
		}
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
					a.add(line.substring(0, ii));
				} else {
					a.add(line);
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
	 * resolve the numeric term id
	 * 
	 * @param t
	 * @return
	 */
	public String getTerm(int t) {
		if (data[1] != null) {
			return data[1][t];
		} else {
			return null;
		}
	}

	/**
	 * find id for string term
	 * 
	 * @param term
	 * @return
	 */
	public int getTermId(String term) {
		if (termids == null) {
			termids = new HashMap<String, Integer>();
			for (int i = 0; i < data[1].length; i++) {
				termids.put(data[1][i], i);
			}
		}
		return termids.get(term);
	}

	/**
	 * resolve the numeric label id
	 * 
	 * @param i
	 * @return
	 */
	public String getLabel(int i) {
		if (data[3] != null) {
			return data[3][i];
		} else {
			return null;
		}
	}

	/**
	 * resolve the numeric author id
	 * 
	 * @param i
	 * @return
	 */
	public String getAuthor(int i) {
		if (data[2] != null) {
			return data[2][i];
		} else {
			return null;
		}
	}

	/**
	 * resolve the numeric term id
	 * 
	 * @param i
	 * @return
	 */
	public String getDoc(int i) {
		if (data[0] != null) {
			return data[0][i];
		} else {
			return null;
		}
	}

	/**
	 * resolve the numeric volume id
	 * 
	 * @param i
	 * @return
	 */
	public String getVol(int i) {
		if (data[4] != null) {
			return data[4][i];
		} else {
			return null;
		}
	}

	public String getLabel(int type, int id) {
		if (type == LabelNumCorpus.LTERMS) {
			return getTerm(id);
		} else if (type == LabelNumCorpus.LAUTHORS) {
			return getAuthor(id);
		} else if (type == LabelNumCorpus.LCATEGORIES) {
			return getLabel(id);
		} else if (type == LabelNumCorpus.LVOLS) {
			return getVol(id);
		} else if (type == LabelNumCorpus.LREFERENCES) {
			return null;
		} else if (type == LabelNumCorpus.LDOCS) {
			return getDoc(id);
		}
		return null;
	}

	public int getId(int type, String label) {
		if (type == LabelNumCorpus.LTERMS) {
			return getTermId(label);
		} else if (type == LabelNumCorpus.LAUTHORS) {
			return Arrays.asList(data[type]).indexOf(label);
		}
		return -1;
	}
}
