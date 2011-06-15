package org.knowceans.corpus;

public interface ICorpusResolver {

	public static final int KDOCS = 0;
	public static final int KTERMS = 1;
	public static final int KAUTHORS = 2;
	public static final int KLABELS = 3;
	public static final int KTAGS = 4;
	public static final int KVOLS = 5;
	public static final int KYEAR = 6;
	// document name
	public static final int KDOCNAME = 7;
	// full reference for easy lookup
	public static final int KDOCREF = 8;

	/**
	 * check whether labels exist
	 * 
	 * @param i
	 * @return 0 for no label values, 1 for yes, 2 for loaded, -1 for illegal
	 *         index
	 */
	public abstract int hasValues(int i);

	/**
	 * resolve the numeric term id
	 * 
	 * @param t
	 * @return
	 */
	public abstract String getTerm(int t);

	/**
	 * find id for string term or return -1 if unknown
	 * 
	 * @param term
	 * @return
	 */
	public abstract int getTermId(String term);

	/**
	 * resolve the numeric label id
	 * 
	 * @param i
	 * @return
	 */
	public abstract String getLabel(int i);

	/**
	 * resolve the numeric author id
	 * 
	 * @param i
	 * @return
	 */
	public abstract String getAuthor(int i);

	/**
	 * resolve the numeric term id
	 * 
	 * @param i
	 * @return
	 */
	public abstract String getDocTitle(int i);

	/**
	 * resolve the numeric term id
	 * 
	 * @param i
	 * @return
	 */
	public abstract String getDocName(int i);

	/**
	 * resolve the numeric term id
	 * 
	 * @param i
	 * @return
	 */
	public abstract String getDocRef(int i);

	/**
	 * resolve the numeric volume id
	 * 
	 * @param i
	 * @return
	 */
	public abstract String getVol(int i);

	public abstract String getLabel(int type, int id);

	public abstract int getId(int type, String label);

}