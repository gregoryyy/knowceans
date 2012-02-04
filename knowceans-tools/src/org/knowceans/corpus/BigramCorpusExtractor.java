package org.knowceans.corpus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.knowceans.map.BijectiveHashMap;
import org.knowceans.util.Conf;
import org.knowceans.util.StopWatch;
import org.knowceans.util.UnHtml;

/**
 * Driver for ACL Anthology Network extraction with bigram extraction.
 * <p>
 * 
 * @author gregor
 * 
 */
public class BigramCorpusExtractor extends SimpleCorpusExtractor {

	public static void main(String[] args) {
		Conf.setPropFile("/data/workspace/knowceans-lda-simple/conf/aanx-bigram.conf");
		// LabelNumCorpus lnc = new LabelNumCorpus();
		// lnc.setDataFilebase(dest);

		BigramCorpusExtractor a = new BigramCorpusExtractor();
		// create svmlight-based corpus
		a.run();
	}

	public BigramCorpusExtractor() {
		srcbase = Conf.get("source.filebase");
		destbase = Conf.get("corpus.filebase");
		metadataFile = Conf.get("source.metadata.file");
		contentDir = Conf.get("source.fulltext.dir");
		citationFile = Conf.get("source.citation.file");
		docSize = Conf.getInt("corpus.abstract.text.chars");

		corpus = new CreateLabelNumCorpus(destbase);
		resolver = new CreateCorpusResolver(destbase);
		corpus.setResolver(resolver);

		mid2doc = new BijectiveHashMap<Integer, AanDocument>();
		aanid2mid = new BijectiveHashMap<String, Integer>();
	}

	/**
	 * run the corpus extractor
	 */
	public void run() {
		try {

			// strategy: we read all metadata, which basically is what we have
			// the relations and authorship for. we then try to match this with
			// content, only considering metadata with given content and vice
			// versa. the basis for comparison is the aan id of the

			StopWatch.start();

			debug("reading metadata");
			aanid2mid = readMetadata();
			debug("setting up corpus");
			createMetadata();
			debug("reading and indexing content, creating corpus and vocabulary");
			startIndex();
			readAndIndexContent(aanid2mid);
			finishIndex();
			System.out.println(String.format("corpus M = %d, V = %d, W = %d",
					corpus.numDocs, corpus.numTerms, corpus.numWords));
			CorpusResolver res = corpus.getResolver();
			System.out.println(res.resolveTerm(1));
			debug("writing to " + destbase);
			corpus.write(destbase, true);
			debug("done.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void finishIndex() throws Exception {
		corpus.compile();
		resolver.compile(true);
	}

	/**
	 * read data for the resolver and authorship information
	 * 
	 * @return
	 * 
	 * @throws IOException
	 * @throws NumberFormatException
	 */
	protected BijectiveHashMap<String, Integer> readMetadata()
			throws NumberFormatException, IOException {

		// just read in ids

		BufferedReader br;
		String line;
		br = new BufferedReader(new FileReader(srcbase + metadataFile));
		line = null;
		boolean skipdoc = false;
		AanDocument doc = null;
		aanid2mid = new BijectiveHashMap<String, Integer>();

		while ((line = br.readLine()) != null) {
			// replace html entities
			line = UnHtml.getText(line);
			line = line.trim();
			if (line.startsWith("id = ")) {
				
				skipdoc = false;
				String aanid = line.trim().substring(6, line.length() - 1);

				File f = new File(srcbase + contentDir + "/" + aanid + ".txt");
				if (!f.exists()) {
					// does not have content
					skipdoc = true;
					continue;
				}
				int mid = addAanid(aanid);
				if (mid >= 100) {
					// aanid will be one element larger --> use mid2doc to determine M 
					break;
				}
				doc = new AanDocument();
				doc.mid = mid;
				doc.aanid = aanid;
				mid2doc.put(mid, doc);

				// read content from corpus file
				BufferedReader brc = new BufferedReader(new FileReader(f));
				String linec = null;
				StringBuffer sb = new StringBuffer();
				// write content to .text file
				while ((linec = brc.readLine()) != null) {
					// line format is (\d+:\d+)\s+(.+); we need only content
					linec = linec.replaceAll("^.+\t", "").trim();
					sb.append(" " + linec);
				}
				brc.close();
				doc.content = UnHtml.getText(sb.toString());
			}  else if (!skipdoc) {
				if (line.startsWith("author = ")) {
					line = line.trim().substring(10, line.length() - 1);
					String[] aa = line.split("\\;");
					for (int i = 0; i < aa.length; i++) {
						aa[i] = aa[i].trim();
					}
					doc.authors = aa;
				} else if (line.startsWith("title = ")) {
					line = line.trim().substring(9, line.length() - 1);
					doc.title = line;
				} else if (line.startsWith("venue = ")) {
					line = line.trim().substring(9, line.length() - 1);
					doc.venue = line;
				} else if (line.startsWith("year = ")) {
					line = line.trim().substring(8, line.length() - 1);
					doc.year = line;
				}
			}
		}
		br.close();
		return aanid2mid;
	}

	/**
	 * based on the AanDocuments, the corpus is filled. Policy: Any document
	 * with empty metadata is stored with empty values, which may later be added
	 * or removed by filtering with an appropriate DocPredicate in
	 * LabelNumCorpus.
	 */
	@SuppressWarnings("static-access")
	protected void createMetadata() {
		int[] labelTypes = {};
		int M = mid2doc.size();
		corpus.allocLabels(M, labelTypes);

		resolver.initMapsForLabelTypes(labelTypes);
		resolver.allocKeyType(resolver.KDOCS, M);
		resolver.allocKeyType(resolver.KDOCNAME, M);

		for (int mid : mid2doc.keySet()) {
			AanDocument doc = mid2doc.get(mid);
			resolver.setValue(resolver.KDOCS, mid, doc.title);
			resolver.setValue(resolver.KDOCNAME, mid, doc.aanid);
			if (mid % 500 == 0) {
				// debug("m = " + mid);
			}
		}

		// can remove all maps by now (indexing is done separately)
		resolver.compile(true);
	}

	/**
	 * only uses information created in the mid2doc field
	 */
	@Override
	protected void readAndIndexContent(
			BijectiveHashMap<String, Integer> aanid2mid) throws Exception {
		// TODO Auto-generated method stub

		for (int mid : mid2doc.keySet()) {
			AanDocument doc = mid2doc.get(mid);
			indexDocument(doc, doc.content);
		}
	}

	/**
	 * index document in the given document writer
	 * 
	 * @param doc document with all metadata
	 * @param content additional content for the document (not stored in doc to
	 *        preserved overall memory)
	 */
	public void indexDocument(AanDocument aandoc, String content)
			throws Exception {
		// TODO: That's somewhat overkill because in readWriteContent we could
		// just save the file names. However, this is more flexible for other
		// corpora.
		String s = "";
		// only use a single field
		if (content != null) {
			s = content;
		}
		if (aandoc.title != null) {
			// title = escapeDb(title);
			s += " " + aandoc.title;
		}

		// tokenise text, remove stopwords, stem
		StringTokenizer st = new StringTokenizer(s);
		List<String> wordlist = new ArrayList<String>();
		String prevword = null;
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			String word = normalise(token);
			if (word != null && !word.equals("")) {
				wordlist.add(word);
				if (prevword != null) {
					wordlist.add(prevword + "+" + word);
				}
				// no bigrams around punctuation (which misses some
				// abbreviations)
				if (token.matches(".+[\\.\\,\\-].*")) {
					prevword = null;
				} else {
					prevword = word;
				}
			} else {
				// no bigram construction after stop word
				prevword = null;
			}
		}
		// System.out.println("words in document " + aandoc.mid + ": " +
		// wordlist);
		corpus.setDocContent(aandoc.mid, wordlist.toArray(new String[0]));
	}

}
