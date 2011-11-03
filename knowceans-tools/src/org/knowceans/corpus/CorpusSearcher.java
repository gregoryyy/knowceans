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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
		String filebase = "corpus-example/nips";
		LabelNumCorpus corpus = new LabelNumCorpus(filebase);
		CorpusSearcher cs = new CorpusSearcher(corpus);
		cs.interact();
	}

	private LabelNumCorpus corpus;
	private CorpusResolver resolver;

	/**
	 * inverted index
	 */
	private Map<Integer, Map<Integer, Integer>> termDocFreqIndex;
	private int[] docFreqs;
	private Map<Integer, Set<Integer>> authorIndex;
	private Map<Integer, Set<Integer>> labelIndex;

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
		this.corpus = corpus;
		this.resolver = corpus.getResolver();
		if (!loadIndex()) {
			System.out.println("indexing");
			createIndex();
			System.out.println("saving to " + corpus.dataFilebase + ".idx");
			saveIndex();
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
			System.out.println("Query (.q to quit):");
			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.equals(".q"))
					break;
				List<Result> results = search(line);
				printResults(line, results);
				System.out.println("***********************************");
				System.out.println("query:");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * print query and search result
	 * 
	 * @param query
	 * @param results
	 */
	private void printResults(String query, List<Result> results) {
		int i = 0;
		System.out.println("results for query \"" + query + "\":");
		for (Result result : results) {
			System.out.println(++i //
					+ ". score = " + result.score + ", id = " + result.id);
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

	// ////// multi-term queries require merging of scores
	// ///////

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
