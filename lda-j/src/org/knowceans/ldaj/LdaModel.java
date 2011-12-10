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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.knowceans.lda.Utils;

import static java.lang.Math.*;

/**
 * wrapper for an LDA model.
 * <p>
 * lda-c reference: Combines the struct lda_model in lda.h and the code in
 * lda-model.h
 * 
 * @author heinrich
 */
public class LdaModel {

    double[][] logProbW;
    private double alpha;
    private int numTopics;
    private int numTerms;

    LdaSuffStats ss;

    public static boolean SAVEBINARY = false;

    public static boolean SAVETXT = true;

    /**
     * create an empty lda model with parameters:
     * 
     * @param numTerms number of terms in dictionary
     * @param numTopics number of topics
     */
    // 2009: lda_model* new_lda_model(int num_terms, int num_topics)
    LdaModel(int numTerms, int numTopics) {
        this.numTopics = numTopics;
        this.numTerms = numTerms;
        this.alpha = 1;
        logProbW = new double[numTopics][numTerms];
    }

    /**
     * create an lda model from information read from the files below modelRoot,
     * i.e. {root}.beta and {root}.other.
     * 
     * @param modelRoot
     */
    // 2009: lda_model* load_lda_model(char* model_root)
    public LdaModel(String modelRoot) {
        String filename;
        int i, j;
        double x, alpha = 0;

        filename = modelRoot + ".other";

        System.out.println("loading " + filename + "\n");

        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = br.readLine()) != null) {

                if (line.startsWith("num_topics ")) {
                    numTopics = Integer.parseInt(line.substring(11).trim());
                } else if (line.startsWith("num_terms ")) {
                    numTerms = Integer.parseInt(line.substring(10).trim());
                } else if (line.startsWith("alpha ")) {
                    alpha = Double.parseDouble(line.substring(6).trim());
                }
            }
            br.close();
            ss = new LdaSuffStats(this);
            this.alpha = alpha;
            filename = modelRoot + ".beta";
            System.out.println("loading " + filename);
            br = new BufferedReader(new FileReader(filename));
            for (i = 0; i < numTopics; i++) {
                line = br.readLine();
                String[] fields = line.trim().split(" ");
                for (j = 0; j < numTerms; j++) {
                    // fscanf(fileptr, "%f", &x);
                    x = Double.parseDouble(fields[j]);
                    logProbW[i][j] = x;
                }
            }
            br.close();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 2009: void lda_mle(lda_model* model, lda_suffstats* ss, int estimate_alpha)
    void mle(boolean estAlpha) {
        int k;
        int w;

        for (k = 0; k < numTopics; k++) {
            for (w = 0; w < numTerms; w++) {
                if (ss.classWord[k][w] > 0) {
                    logProbW[k][w] = log(ss.classWord[k][w])
                        - log(ss.classTotal[k]);
                } else {
                    logProbW[k][w] = -100;
                }
            }
        }
        if (estAlpha) {
            alpha = LdaAlpha
                .opt_alpha(ss.alphaSuffstats, ss.numDocs, numTopics);

            //printf("new alpha = %5.5f\n", model->alpha);
            System.out.println("new alpha = " + alpha + "\n");
        }
    }

    /**
     * deallocate lda model (dummy)
     */
    // 2009: void free_lda_model(lda_model*);
    public void free() {
        // nothing to do in Java
    }

    /**
     * save an lda model
     * 
     * @param modelRoot
     */
    // 2009: void save_lda_model(lda_model*, char*);
    public void save(String modelRoot) {
        int i, j;

        if (SAVEBINARY) {
            saveBinary(modelRoot);
            if (!SAVETXT)
                return;
        }

        String filename = modelRoot + ".beta";

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
            for (i = 0; i < this.numTopics; i++) {
                for (j = 0; j < this.numTerms; j++) {
                    if (j > 0)
                        bw.write(' ');
                    bw.write(Utils.formatDouble(this.logProbW[i][j]));
                }
                bw.newLine();
            }
            bw.newLine();
            bw.close();
            filename = modelRoot + ".other";
            bw = new BufferedWriter(new FileWriter(filename));
            bw.write("num_topics " + numTopics + "\n");
            bw.write("num_terms " + numTerms + "\n");
            bw.write("alpha " + alpha + "\n");
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * save an lda model with beta in binary format
     * 
     * @param modelRoot
     */
    public void saveBinary(String modelRoot) {
        int i, j;

        String filename = modelRoot + ".beta.bin";

        try {
            DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(filename)));
            dos.writeInt(numTopics);
            dos.writeInt(numTerms);
            for (i = 0; i < this.numTopics; i++) {
                for (j = 0; j < this.numTerms; j++) {
                    dos.writeFloat((float) (this.logProbW[i][j]));
                }
            }
            dos.close();
            filename = modelRoot + ".other";
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
            bw.write("num_topics " + numTopics + "\n");
            bw.write("num_terms " + numTerms + "\n");
            bw.write("alpha " + alpha + "\n");
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return
     */
    public double getAlpha() {
        return alpha;
    }

    /**
     * @return
     */
    public double[] getClassTotal() {
        return ss.classTotal;
    }

    /**
     * @param cls
     * @return
     */
    public double getClassTotal(int cls) {
        return ss.classTotal[cls];
    }

    /**
     * @param cls
     * @param total
     */
    public void setClassTotal(int cls, double total) {
        ss.classTotal[cls] = total;
    }

    /**
     * @param cls
     * @param total
     */
    public void addClassTotal(int cls, double total) {
        ss.classTotal[cls] += total;
    }

    /**
     * @return
     */
    public double[][] getClassWord() {
        return ss.classWord;
    }

    /**
     * @param cls
     * @param word
     * @return
     */
    public double getClassWord(int cls, int word) {
        return ss.classWord[cls][word];
    }

    /**
     * @param cls
     * @param word
     * @param value
     */
    public void setClassWord(int cls, int word, double value) {
        ss.classWord[cls][word] = value;
    }

    /**
     * @param cls
     * @param word
     * @param value
     */
    public void addClassWord(int cls, int word, double value) {
        ss.classWord[cls][word] += value;
    }

    /**
     * @return
     */
    public int getNumTerms() {
        return numTerms;
    }

    /**
     * @return
     */
    public int getNumTopics() {
        return numTopics;
    }

    /**
     * @param d
     */
    public void setAlpha(double d) {
        alpha = d;
    }

    /**
     * @param ds
     */
    public void setClassTotal(double[] ds) {
        ss.classTotal = ds;
    }

    /**
     * @param ds
     */
    public void setClassWord(double[][] ds) {
        ss.classWord = ds;
    }

    /**
     * @param i
     */
    public void setNumTerms(int i) {
        numTerms = i;
    }

    /**
     * @param i
     */
    public void setNumTopics(int i) {
        numTopics = i;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("Model {numTerms=" + numTerms + " numTopics=" + numTopics
            + " alpha=" + alpha + "}");
        return b.toString();
    }

}
