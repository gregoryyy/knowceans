package org.knowceans.corpus;

public interface ICorpusResolver {

	public static final int KDOCS = 0;
	public static final int KTERMS = 1;
	public static final int KAUTHORS = 2;
	public static final int KCATEGORIES = 3;
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
	public abstract int hasLabelKeys(int i);

	/**
	 * resolve the numeric term id
	 * 
	 * @param t
	 * @return
	 */
	public abstract String resolveTerm(int t);

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
	public abstract String resolveCategory(int i);

	/**
	 * resolve the numeric author id
	 * 
	 * @param i
	 * @return
	 */
	public abstract String resolveAuthor(int i);

	/**
	 * resolve the numeric term id
	 * 
	 * @param i
	 * @return
	 */
	public abstract String resolveDocTitle(int i);

	/**
	 * resolve the numeric term id
	 * 
	 * @param i
	 * @return
	 */
	public abstract String resolveDocName(int i);

	/**
	 * resolve the numeric term id
	 * 
	 * @param i
	 * @return
	 */
	public abstract String resolveDocRef(int i);

	/**
	 * resolve the numeric volume id
	 * 
	 * @param i
	 * @return
	 */
	public abstract String resolveVolume(int i);

	/**
	 * resolve a label
	 * 
	 * @param type
	 * @param id
	 * @return
	 */
	public abstract String resolveLabel(int type, int id);

	/**
	 * reverse lookup of label to id
	 * 
	 * @param type
	 * @param label
	 * @return
	 */
	public abstract int getId(int type, String label);

}