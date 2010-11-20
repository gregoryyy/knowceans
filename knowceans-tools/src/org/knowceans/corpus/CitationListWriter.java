package org.knowceans.corpus;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * write the list of authors for each paper
 * 
 * @author gregor
 * 
 */
public class CitationListWriter {

public static void main(String[] args) throws IOException {
    LabelNumCorpus c = new LabelNumCorpus(
            "./corpus-example/nips");
    CorpusResolver r = new CorpusResolver(
            "./corpus-example/nips");
    int[][] lab = c.getDocLabels(ILabelCorpus.LAUTHORS);
    int[][] vol = c.getDocLabels(ILabelCorpus.LVOLS);
    int[][] tag = c.getDocLabels(ILabelCorpus.LCATEGORIES);

    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < c.numDocs; i++) {
        sb.append(String.format("%04d ", i));
        sb.append(r.getDocName(i)).append(": ");
        for (int j = 0; j < lab[i].length; j++) {
            sb.append(r.getAuthor(lab[i][j])).append(", ");
        }
        sb.append("\"").append(r.getDoc(i)).append(
            "\", vol. ");
        sb.append(r.getVol(vol[i][0])).append(", cat. ");
        sb.append(r.getLabel(ILabelCorpus.LCATEGORIES,
            tag[i][0]));
        sb.append("\n");
    }
    System.out.println(sb);
    BufferedWriter bw = new BufferedWriter(new FileWriter(
            "./corpus-example/documents.txt"));
    bw.append(sb.toString());
    bw.close();
}
}
