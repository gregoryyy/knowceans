package org.knowceans.corpus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Searches an ICorpus using its resolver using inverted indices
 * 
 * @author gregor
 * 
 */
public class CorpusSearcher {

	/**
	 * driver for search engine
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		//String filebase = "corpus-example/berry95";
		String filebase = "corpus-example/nips";
		LabelNumCorpus corpus = new LabelNumCorpus(filebase);
		corpus.loadAllLabels();
		CorpusResolver cr = corpus.getResolver();
		System.out.println(cr.getTermId("test"));
		System.out.println(corpus.check(true));
		System.out.println("reduce corpus to 20 docs");

		// reduce the documents without both references and citations
		// corpus.reduceUnlinkedDocs(true, true);

		// choose the first 20 documents
		corpus.reduce(6, new Random());
		// corpus.reduce(6, null);
		// adjust the vocabulary
		corpus.filterTermsDf(2, 10);
		System.out.println(corpus.check(true));
		corpus.filterLabels();

		//
		System.out.println(cr.getTermId("test"));
		System.out.println(corpus.check(true));
		CorpusSearcher cs = new CorpusSearcher(corpus);
		cs.interact();
	}

	private LabelNumCorpus corpus;
	private CorpusResolver resolver;
	private String help = "Query (or .q to quit, ENTER to page results, .d <rank> or .m <id> to view doc, .t <prefix> to view terms list,\n"
			+ "       .a, .c <prefix> to view authors, categories list, .A, .C <prefix> to view particular item, .h, ? for this message):";

	/**
	 * inverted index
	 */
	private Map<Integer, Map<Integer, Integer>> termDocFreqIndex;
	private int[] docFreqs;
	private Map<Integer, Set<Integer>> authorIndex;
	private Map<Integer, Set<Integer>> labelIndex;
	private String[][] keyLists;

	/**
	 * inits the searcher with the given corpus, which needs to have a resolver
	 * to be used with human-readable queries. Loads any index found for the
	 * corpus.
	 * 
	 * @param corpus
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public CorpusSearcher(LabelNumCorpus corpus) throws IOException,
			ClassNotFoundException {
		this(corpus, false);
	}

	/**
	 * create corpus but reindex
	 * 
	 * @param corpus
	 * @param reindex false to load index or index and save, true to create
	 *        index temporarily
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public CorpusSearcher(LabelNumCorpus corpus, boolean reindex)
			throws IOException, ClassNotFoundException {
		this.corpus = corpus;
		this.resolver = corpus.getResolver();
		keyLists = new String[CorpusResolver.keyExtensions.length][];
		if (!reindex && !loadIndex()) {
			System.out.println("indexing");
			createIndex();
			System.out.println("saving to " + corpus.dataFilebase + ".idx");
			saveIndex();
		} else {
			// load a fresh index
			System.out.println("indexing");
			createIndex();
		}
	}

	/**
	 * opens the inverted index, if the corpus has file information and the
	 * respective file exists.
	 * 
	 * @return true if index was loaded
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked")
	private boolean loadIndex() throws IOException, ClassNotFoundException {
		File index = new File(corpus.dataFilebase + ".idx");
		if (index.exists()) {
			ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(
					new FileInputStream(index)));
			// get directory (this may be also done by fixed sequence and null
			// objects)
			String directory = ois.readUTF();
			String[] objects = directory.split(" ");
			for (String object : objects) {
				if (object.equals("terms")) {
					termDocFreqIndex = (Map<Integer, Map<Integer, Integer>>) ois
							.readObject();
					docFreqs = (int[]) ois.readObject();
				} else if (object.equals("authors")) {
					authorIndex = (Map<Integer, Set<Integer>>) ois.readObject();
				} else if (object.equals("labels")) {
					labelIndex = (Map<Integer, Set<Integer>>) ois.readObject();
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * save the index
	 * 
	 * @throws IOException
	 */
	private void saveIndex() throws IOException {
		// INFO: inefficient but avoids deps. to e.g. Prevayler
		File index = new File(corpus.dataFilebase + ".idx");
		ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(
				new FileOutputStream(index)));
		String directory = "";
		if (termDocFreqIndex != null) {
			directory += " " + "terms";
		}
		if (authorIndex != null) {
			directory += " " + "authors";
		}
		if (labelIndex != null) {
			directory += " " + "labels";
		}
		oos.writeUTF(directory);
		if (termDocFreqIndex != null) {
			oos.writeObject(termDocFreqIndex);
			oos.writeObject(docFreqs);
		}
		if (authorIndex != null) {
			oos.writeObject(authorIndex);
		}
		if (labelIndex != null) {
			oos.writeObject(labelIndex);
		}
		oos.flush();
		oos.close();
	}

	/**
	 * interactively search the index
	 */
	public void interact() {
		try {
			System.out.println(help);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));
			String lastQuery = "";
			List<Result> results = null;
			int listtype = -1;
			int listpos = -1;
			int resultsPage = 0;
			int pageSize = 10;
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.equals(".q")) {
					break;
				} else if (line.equals("")) {
					// continue paging
					if (results != null) {
						resultsPage++;
						printResults(lastQuery, results,
								resultsPage * pageSize, pageSize);
					} else if (listpos >= 0) {
						resultsPage++;
						printListPage(listtype, listpos + resultsPage
								* pageSize, pageSize);
					}
				} else if (line.startsWith(".d") && line.length() > 2) {
					String arg = line.substring(2);
					if (!arg.matches("[0-9]+")) {
						continue;
					}
					int rank = Integer.parseInt(arg);
					if (results != null && results.size() > rank) {
						System.out.println("result rank " + rank + ":");
						int id = results.get(rank).id;
						printDoc(lastQuery, id);
						System.out
								.println("***********************************");
					}
				} else if (line.startsWith(".m") && line.length() > 2) {
					String arg = line.substring(2);
					if (!arg.matches("[0-9]+")) {
						continue;
					}
					int m = Integer.parseInt(arg);
					System.out.println("document id " + m + ":");
					printDoc(lastQuery, m);
					System.out.println("***********************************");
				} else if (line.startsWith(".A") && line.length() > 2) {
					String prefix = line.substring(2).trim();
					System.out.println("author prefix " + prefix + ":");
					int pos = searchList(ICorpusResolver.KAUTHORS, prefix);
					printAuthor(pos);
					System.out.println("***********************************");

				} else if (line.startsWith(".C") && line.length() > 2) {
					String prefix = line.substring(2).trim();
					System.out.println("category prefix " + prefix + ":");
					int pos = searchList(ICorpusResolver.KCATEGORIES, prefix);
					printCategory(pos);
					System.out.println("***********************************");

				} else if (line.startsWith(".t") || line.startsWith(".a")
						|| line.startsWith(".c") || line.startsWith(".d")) {
					results = null;
					resultsPage = 0;
					listpos = 0;
					String prefix = line.substring(2).trim();
					if (line.charAt(1) == 't') {
						listtype = ICorpusResolver.KTERMS;
					} else if (line.charAt(1) == 'a') {
						listtype = ICorpusResolver.KAUTHORS;
					} else if (line.charAt(1) == 'c') {
						listtype = ICorpusResolver.KCATEGORIES;
					} else if (line.charAt(1) == 'd') {
						listtype = ICorpusResolver.KDOCREF;
					} else {
						// error
					}
					listpos = searchList(listtype, prefix);
					printListPage(listtype, listpos, pageSize);
				} else if (line.startsWith(".h") || line.startsWith("?")) {
					System.out.println(help);
				} else if (line.startsWith(".s")) {
					// print statistics
					System.out.println(corpus);
				} else {
					results = search(line);
					resultsPage = 0;
					listpos = -1;
					System.out.println(results.size() + " results: ");
					printResults(line, results, 0, pageSize);
					System.out.println("***********************************");
					System.out.println("query:");
					lastQuery = line;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * load list of a given type
	 * 
	 * @param type
	 */
	protected void loadList(int type) {
		if (keyLists[type] == null) {
			String[] aa = resolver.getStrings(type);
			if (aa != null) {
				keyLists[type] = new String[aa.length];
				for (int i = 0; i < aa.length; i++) {
					if (aa[i] == null) {
						aa[i] = CorpusResolver.keyNames[type] + i;
					}
				}
				System.arraycopy(aa, 0, keyLists[type], 0, aa.length);
				Arrays.sort(keyLists[type]);
			}
		}
	}

	/**
	 * search the list of the type, printing pageSize results
	 * 
	 * @param type
	 * @param prefix
	 * @return
	 */
	protected int searchList(int type, String prefix) {
		loadList(type);
		// search for entry point
		int pos = Arrays.binarySearch(keyLists[type], prefix);
		if (pos < 0) {
			pos = -pos - 1;
		}
		return pos;
	}

	/**
	 * print one page of the list starting at start
	 * 
	 * @param type
	 * @param start
	 * @param pageSize
	 * @return new start position
	 */
	protected int printListPage(int type, int start, int pageSize) {
		int listpos;
		for (listpos = start; listpos < start + pageSize; listpos++) {
			if (listpos < keyLists[type].length) {
				System.out.println(keyLists[type][listpos]);
			}
		}
		return listpos;
	}

	/**
	 * print the document
	 * 
	 * @param id
	 */
	protected void printDoc(String query, int id) {
		String title = resolver.resolveDocRef(id);
		String content = resolver.resolveDocContent(id);
		if (title != null) {
			title = highlight(title, query);
			System.out.println(wordWrap(title, 80));
		}
		if (content != null) {
			content = highlight(content, query);
			System.out.println(wordWrap(content, 80));
		}
		if (corpus.hasLabels(LabelNumCorpus.LREFERENCES) == 2) {
			int[] refs = corpus.getDocLabels(LabelNumCorpus.LREFERENCES, id);
			if (refs.length > 0) {
				System.out.println("References:");
				for (int ref : refs) {
					System.out.println(resolver.resolveDocRef(ref));
				}
			}
		}
		// TODO: this is repeated overkill
		int[][] allrefs = corpus.getDocLabels(LabelNumCorpus.LREFERENCES);
		if (allrefs != null) {
			Set<Integer> inrefs = new TreeSet<Integer>();
			for (int m = 0; m < corpus.numDocs; m++) {
				for (int i = 0; i < allrefs[m].length; i++) {
					if (allrefs[m][i] == id) {
						inrefs.add(m);
					}
				}
			}
			if (inrefs.size() > 0) {
				System.out.println("Citations:");
				for (int m : inrefs) {
					System.out.println(resolver.resolveDocRef(m));
				}
			}
		}
		String[] terms = query.split(" ");
		for (String term : terms) {
			// TODO: this is redundant with the actual search
			int termid = resolver.getTermId(term);
			if (termid >= 0) {
				System.out.println(term + ", " + termid + " df = "
						+ docFreqs[termid] + ", tf = "
						+ termDocFreqIndex.get(termid).get(id));
			}
		}
	}

	/**
	 * prints the given author
	 * 
	 * @param pos position in author list
	 */
	private void printAuthor(int pos) {
		int id = getIdForPos(CorpusResolver.KAUTHORS, pos);
		System.out.println("Author #" + pos + ", id = " + id + ": "
				+ resolver.resolveAuthor(id) + ":");
		System.out.println("Documents: ");
		Set<Integer> docs = authorIndex.get(id);

		int i = 0;
		for (int doc : docs) {
			i++;
			System.out.println(i + ". " + resolver.resolveDocRef(doc));
		}
		if (corpus.hasLabels(LabelNumCorpus.LMENTIONS) == 2) {
			int[][] ment = corpus.getDocLabels(LabelNumCorpus.LMENTIONS);
			Set<Integer> ments = new HashSet<Integer>();
			for (int m = 0; m < ment.length; m++) {
				for (i = 0; i < ment[m].length; i++) {
					if (ment[m][i] == id && !authorIndex.get(id).contains(m)) {
						ments.add(m);
					}
				}
			}
			if (ments.size() > 0) {
				System.out.println("Mentions:");
				for (int m : ments) {
					System.out.println(resolver.resolveDocRef(m));
				}
			}
		}
	}

	/**
	 * prints the given category
	 * 
	 * @param pos position in label list displayed
	 */
	private void printCategory(int pos) {
		int id = getIdForPos(CorpusResolver.KCATEGORIES, pos);
		System.out.println("Category #" + pos + ", id = " + id + ": "
				+ resolver.resolveCategory(id));
		System.out.println("Documents: ");
		Set<Integer> docs = labelIndex.get(id);
		if (docs == null) {
			System.out.println("[empty]");
			return;
		}
		int i = 0;
		for (int doc : docs) {
			i++;
			System.out.println(i + ". " + resolver.resolveDocRef(doc));
		}
	}

	protected int getIdForPos(int type, int pos) {
		String[] labels = resolver.getStrings(type);
		int id = -1;
		// TODO: more efficient than repeated linear search
		for (id = 0; id < labels.length; id++) {
			if (labels[id].equals(keyLists[type][pos])) {
				break;
			}
		}
		return id;
	}

	/**
	 * highlight query terms in content
	 * 
	 * @param content
	 * @param query
	 * @return
	 */
	private String highlight(String content, String query) {
		if (query == null || query.equals("")) {
			return content;
		}
		String[] terms = query.split(" ");
		for (String term : terms) {
			content = content.replaceAll("\\s(?i)" + term + "\\s", " ***"
					+ term + "*** ");
		}

		return content;
	}

	/**
	 * word wrap the document before column
	 * 
	 * @param content
	 * @param i
	 * @return
	 */
	private String wordWrap(String content, int columns) {
		if (content == null) {
			return null;
		}
		// word wrap line
		StringBuffer sb = new StringBuffer();
		int prevword = 0;
		int curword = 0;
		int curline = 0;
		int lastspace = content.lastIndexOf(' ');
		if (lastspace == -1) {
			return content;
		}
		while (curword < lastspace) {
			prevword = curword;
			curword = content.indexOf(' ', curword + 1);
			if (curword - curline > columns) {
				sb.append(content.substring(curline, prevword).trim()).append(
						'\n');
				curline = prevword;
			}
		}
		sb.append(content.substring(curline).trim());
		return sb.toString();
	}

	/**
	 * print query and search result
	 * 
	 * @param query
	 * @param results
	 * @param start
	 * @param count
	 */
	private void printResults(String query, List<Result> results, int start,
			int count) {
		System.out.println("results for query \"" + query + "\":");
		for (int i = start; i < start + count; i++) {
			if (i >= results.size()) {
				return;
			}
			Result result = results.get(i);
			System.out.println(i + ". score = " + result.score + ", id = "
					+ result.id);
			System.out.println("\t" + resolver.resolveDocRef(result.id));
		}
	}

	/**
	 * result item
	 */
	class Result implements Comparable<Result> {
		int id;
		double score;

		Result(int id, double score) {
			this.id = id;
			this.score = score;
		}

		@Override
		public int compareTo(Result that) {
			// default ranking order is reverse
			return this.score == that.score ? 0 : this.score < that.score ? 1
					: -1;
		}
	}

	// /////////////////////////

	/**
	 * index a set of strings
	 * 
	 * @param corpus
	 */
	private void createIndex() {
		termDocFreqIndex = new HashMap<Integer, Map<Integer, Integer>>();
		docFreqs = new int[corpus.getNumTerms()];

		for (int m = 0; m < corpus.getNumDocs(); m++) {
			Document document = corpus.docs[m];
			// tokenize document
			for (int i = 0; i < document.numTerms; i++) {
				addTermDoc(m, document.getTerm(i), document.getCount(i));
			}
		}
		if (corpus.hasLabels(LabelNumCorpus.LAUTHORS) == 2) {
			authorIndex = indexLabels(LabelNumCorpus.LAUTHORS);
		}
		if (corpus.hasLabels(LabelNumCorpus.LCATEGORIES) == 2) {
			labelIndex = indexLabels(LabelNumCorpus.LCATEGORIES);
		}
	}

	/**
	 * index the label type by creating an inverted index to lookup documents
	 * 
	 * @param type
	 * @return
	 */
	protected HashMap<Integer, Set<Integer>> indexLabels(int type) {
		HashMap<Integer, Set<Integer>> index = new HashMap<Integer, Set<Integer>>();
		for (int m = 0; m < corpus.getNumDocs(); m++) {
			int[] lab = corpus.labels[type][m];
			for (int i = 0; i < lab.length; i++) {
				Set<Integer> docs = index.get(lab[i]);
				if (docs == null) {
					docs = new HashSet<Integer>();
					index.put(lab[i], docs);
				}
				docs.add(m);
			}
		}
		return index;
	}

	/**
	 * add term-doc pair to inverted index
	 * 
	 * @param doc
	 * @param term
	 */
	public void addTermDoc(int doc, int term, int freq) {
		Map<Integer, Integer> doc2freq = termDocFreqIndex.get(term);
		// term still unknown
		if (doc2freq == null) {
			doc2freq = new HashMap<Integer, Integer>();
			termDocFreqIndex.put(term, doc2freq);
		}
		doc2freq.put(doc, freq);
		docFreqs[term]++;
	}

	/**
	 * search for a single term in the corpus
	 * 
	 * @param term
	 * @return
	 */
	public Map<Integer, Double> findTerm(String term) {
		int termid = resolver.getTermId(term);
		Map<Integer, Double> results = new HashMap<Integer, Double>();
		Map<Integer, Integer> docs2freqs = termDocFreqIndex.get(termid);
		if (docs2freqs != null) {
			for (Entry<Integer, Integer> doc : docs2freqs.entrySet()) {
				// each match is scored 1
				results.put(doc.getKey(), (double) doc.getValue());
			}
		}
		return results;
	}

	/**
	 * search for a set of words in the corpus
	 * 
	 * @param query
	 * @return
	 */
	public List<Result> search(String query) {
		Map<Integer, Double> scoreMap = new HashMap<Integer, Double>();
		// tokenise query
		String[] terms = query.split(" ");
		// get individual results and merge scores
		for (int i = 0; i < terms.length; i++) {
			Map<Integer, Double> termRes = findTerm(terms[i]);
			// intersection
			scoreMap = (i == 0) ? termRes : mergeResults(scoreMap, termRes,
					true);
		}
		// create results list from merged results
		List<Result> results = new ArrayList<Result>();
		for (Entry<Integer, Double> result : scoreMap.entrySet()) {
			results.add(new Result(//
					result.getKey(), result.getValue()));
		}
		Collections.sort(results);
		return results;
	}

	/**
	 * merge the scores the maps
	 * 
	 * @param map1
	 * @param map2
	 * @param intersection intersect (true) or unite (false)
	 * @param result
	 */
	private Map<Integer, Double> mergeResults(Map<Integer, Double> map1,
			Map<Integer, Double> map2, boolean intersection) {
		HashMap<Integer, Double> result = new HashMap<Integer, Double>();
		Set<Integer> mergedKeys = map1.keySet();
		if (!intersection) {
			mergedKeys.addAll(map2.keySet());
		} else {
			mergedKeys.retainAll(map2.keySet());
		}
		// now add the values for the merged keys
		for (int key : mergedKeys) {
			Double val1 = map1.get(key);
			Double val2 = map2.get(key);

			if (val1 == null) {
				val1 = 0.;
			}
			if (val2 == null) {
				val2 = 0.;
			}
			result.put(key, val1 + val2);
		}
		return result;
	}
}
