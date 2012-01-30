package org.knowceans.corpus;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.knowceans.map.BijectiveHashMap;
import org.knowceans.util.Conf;
import org.knowceans.util.Vectors;

/**
 * Driver for ACL Anthology Network extraction with bigram extraction.
 * <p>
 * 
 * @author gregor
 * 
 */
public class BigramCorpusExtractor extends SimpleCorpusExtractor {

	public static void main(String[] args) {
		Conf.setPropFile("conf/aanx.conf");
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
				// no bigrams around punctuation (which misses some abbreviations)
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
		//System.out.println("words in document " + aandoc.mid + ": " + wordlist);
		corpus.setDocContent(aandoc.mid, wordlist.toArray(new String[0]));
	}

}
