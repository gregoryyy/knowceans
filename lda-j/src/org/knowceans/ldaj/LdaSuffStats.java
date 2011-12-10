/*
 * (C) Copyright 2004-2009, Gregor Heinrich (gregor :: arbylon : net) 
 * (This file is part of the lda-j (org.knowceans.ldaj.*) experimental software 
 * package, a port of lda-c Copyright David Blei.)
 */
/*
 * lda-j is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 */
/*
 * lda-j is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
/*
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * Created on Dec 3, 2004
 */
package org.knowceans.ldaj;

import java.util.Arrays;

import org.knowceans.lda.Corpus;
import org.knowceans.lda.Document;
import org.knowceans.util.Cokus;

/**
 * Represents a corpus of documents.
 * <p>
 * lda-c reference: struct lda_suffstats in lda.h and functions *_lda_suffstats
 * and *_ss.
 * 
 * @author heinrich
 */
public class LdaSuffStats {

    public static final int NUM_INIT = 1;

    double[][] classWord;
    double[] classTotal;
    int numDocs;
    double alphaSuffstats;

    // 2009: lda_suffstats* new_lda_suffstats(lda_model* model);
    public LdaSuffStats(LdaModel model) {
        int num_topics = model.getNumTopics();
        int num_terms = model.getNumTerms();
        classTotal = new double[num_topics];
        classWord = new double[num_topics][num_terms];
    }

    // 2009: void corpus_initialize_ss(lda_suffstats* ss, lda_model* model, corpus* c);
    public void corpusInitialize(LdaModel model, Corpus corpus) {
        int num_topics = model.getNumTopics();
        int i, k, d, n;
        Document doc;

        for (k = 0; k < num_topics; k++) {
            for (i = 0; i < NUM_INIT; i++) {
                d = (int) Math.floor(Cokus.randDouble() * corpus.getNumDocs());
                //printf("initialized with document %d\n", d);
                System.out.println("initialized with document " + d);
                doc = corpus.getDoc(d);
                for (n = 0; n < doc.getLength(); n++) {
                    classWord[k][doc.getWord(n)] += doc.getCount(n);
                }
            }
            for (n = 0; n < model.getNumTerms(); n++) {
                classWord[k][n] += 1.0;
                // TODO: this looks fishy: should be +1 not everytime the word
                //classTotal[k] = classTotal[k] + classWord[k][n];
                classTotal[k] += classWord[k][n];
            }
        }

    }

    // 2009: void random_initialize_ss(lda_suffstats* ss, lda_model* model);
    public void randomInitialize(LdaModel model) {
        int num_topics = model.getNumTopics();
        int num_terms = model.getNumTerms();
        int k, n;
        for (k = 0; k < num_topics; k++) {
            for (n = 0; n < num_terms; n++) {
                classWord[k][n] += 1.0 / num_terms + Cokus.randDouble();
                classTotal[k] += classWord[k][n];
            }
        }
    }

    // 2009: void zero_initialize_ss(lda_suffstats* ss, lda_model* model);
    public void zeroInitialize(LdaModel model) {
        Arrays.fill(classTotal, 0);
        for (int k = 0; k < model.getNumTopics(); k++) {
            Arrays.fill(classWord[k], 0);
        }
        numDocs = 0;
        alphaSuffstats = 0;
    }

}
