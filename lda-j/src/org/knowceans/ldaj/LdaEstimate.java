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
 * Created on Jan 4, 2005
 */
package org.knowceans.ldaj;

import static org.knowceans.util.Gamma.digamma;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.knowceans.lda.Corpus;
import org.knowceans.lda.Document;
import org.knowceans.lda.Utils;
import org.knowceans.util.Cokus;

/**
 * lda parameter estimation
 * <p>
 * lda-c reference: functions in lda-estimate.c.
 * 
 * @author heinrich
 */
public class LdaEstimate {

    /*
     * For remote debugging: -Xdebug -Xnoagent
     * -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=
     * <MyJdwpDebugPort>
     */
    static int LAG = 10;

    static int NUM_INIT = 1;

    public static float EM_CONVERGED;

    public static int EM_MAX_ITER;

    public static boolean ESTIMATE_ALPHA;

    static double INITIAL_ALPHA;

    static int K;

    static {
        Cokus.seed(4357);
    }

    /**
     * iterate_document
     */
    // 2009: double doc_e_step(document* doc, double* gamma, double** phi,
    // lda_model* model, lda_suffstats* ss)
    public static double docEStep(Document doc, double[] gamma, double[][] phi,
        LdaModel model) {
        double likelihood;
        int n, k;

        // posterior inference

        likelihood = LdaInference.ldaInference(doc, model, gamma, phi);

        // update sufficient statistics

        double gamma_sum = 0;
        for (k = 0; k < model.getNumTopics(); k++) {
            gamma_sum += gamma[k];
            model.ss.alphaSuffstats += digamma(gamma[k]);
        }
        model.ss.alphaSuffstats -= model.getNumTopics() * digamma(gamma_sum);

        for (n = 0; n < doc.getLength(); n++) {
            for (k = 0; k < model.getNumTopics(); k++) {
                model.ss.classWord[k][doc.getWord(n)] += doc.getCount(n)
                    * phi[n][k];
                model.ss.classTotal[k] += doc.getCount(n) * phi[n][k];
            }
        }

        model.ss.numDocs = model.ss.numDocs + 1;

        return likelihood;
    }

    /**
     * saves the gamma parameters of the current dataset
     */
    // 2009: void save_gamma(char* filename, double** gamma, int num_docs, int num_topics)
    static void saveGamma(String filename, double[][] gamma, int numDocs,
        int numTopics) {
        if (LdaModel.SAVEBINARY) {
            saveGammaBinary(filename + ".bin", gamma, numDocs, numTopics);
            if (!LdaModel.SAVETXT)
                return;
        }
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
            int d, k;
            for (d = 0; d < numDocs; d++) {
                for (k = 0; k < numTopics; k++) {
                    if (k > 0)
                        bw.write(' ');
                    bw.write(Utils.formatDouble(gamma[d][k]));
                }
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * saves the gamma parameters of the current dataset in binary form
     */
    static void saveGammaBinary(String filename, double[][] gamma, int numDocs,
        int numTopics) {
        try {
            DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(filename)));
            int d, k;
            dos.writeInt(numDocs);
            dos.writeInt(numTopics);
            for (d = 0; d < numDocs; d++) {
                for (k = 0; k < numTopics; k++) {
                    dos.writeFloat((float) gamma[d][k]);
                }
            }
            dos.flush();
            dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * run_em
     */
    // 2009: void run_em(char* start, char* directory, corpus* corpus)
    public static LdaModel runEm(String start, String directory, Corpus corpus) {
        try {
            BufferedWriter likelihoodFile;
            String filename;
            int d;
            LdaModel model;
            double[][] varGamma, phi;

            // allocate variational parameters

            varGamma = new double[corpus.getNumDocs()][K];
            phi = new double[corpus.getMaxCorpusLength()][K];

            if (start.equals("seeded")) {
                model = new LdaModel(corpus.getNumTerms(), K);
                model.ss = new LdaSuffStats(model);
                model.ss.corpusInitialize(model, corpus);
                model.mle(false);
                model.setAlpha(INITIAL_ALPHA);
            } else if (start.equals("random")) {
                model = new LdaModel(corpus.getNumTerms(), K);
                model.ss = new LdaSuffStats(model);
                model.ss.randomInitialize(model);
                model.mle(false);
                model.setAlpha(INITIAL_ALPHA);
            } else {
                model = new LdaModel(start);
                model.ss = new LdaSuffStats(model);
            }
            filename = directory + "/000";
            model.save(filename);

            // run expectation maximization

            int i = 0;
            double likelihood = Double.NEGATIVE_INFINITY, likelihoodOld = 0, converged = 1;
            filename = directory + "/" + "likelihood.dat";
            likelihoodFile = new BufferedWriter(new FileWriter(filename));
            NumberFormat nf = new DecimalFormat("000");
            String itername = "";

            while (((converged < 0) || (converged > EM_CONVERGED) || (i <= 2))
                && (i <= EM_MAX_ITER)) {
                i++;
                System.out.println("**** em iteration " + i + " ****");
                likelihood = 0;
                model.ss.zeroInitialize(model);

                // e-step

                for (d = 0; d < corpus.getNumDocs(); d++) {
                    if ((d % 100) == 0) {
                        System.out.println("document " + d);
                    }
                    likelihood += docEStep(corpus.getDoc(d), varGamma[d], phi,
                        model);
                }

                // m-step

                model.mle(ESTIMATE_ALPHA);

                // check convergence

                converged = (likelihoodOld - likelihood) / likelihoodOld;
                if (converged < 0) {
                    // TODO: move VAR_MAX_ITER
                    LdaInference.VAR_MAX_ITER = LdaInference.VAR_MAX_ITER * 2;
                }
                likelihoodOld = likelihood;

                // output model and likelihood

                // fprintf(likelihood_file, "%10.10f\t%5.5e\n", likelihood,
                // converged);
                likelihoodFile.write(likelihood + "\t" + converged + "\n");
                likelihoodFile.flush();

                if ((i % LAG) == 0) {
                    // sprintf(filename,"%s/%03d",directory, i);
                    itername = nf.format(i);
                    filename = directory + "/" + itername;
                    model.save(filename);

                    filename = directory + "/" + itername + ".gamma";
                    saveGamma(filename, varGamma, corpus.getNumDocs(), model
                        .getNumTopics());
                }
            }

            // output the final model

            itername = nf.format(i);
            filename = directory + "/final";
            model.save(filename);
            filename = directory + "/" + itername + "final.gamma";
            saveGamma(filename, varGamma, corpus.getNumDocs(), model
                .getNumTopics());
            likelihoodFile.close();

            // output the word assignments (for visualization)
            //sprintf(filename, "%s/word-assignments.dat", directory);
            BufferedWriter wasnFile;
            wasnFile = new BufferedWriter(new FileWriter(directory
                + "/word-assignments.dat"));
            for (d = 0; d < corpus.getNumDocs(); d++) {
                if ((d % 100) == 0) {
                    System.out.println("final e step document " + d);
                }
                likelihood += LdaInference.ldaInference(corpus.getDoc(d),
                    model, varGamma[d], phi);
                writeWordAssignment(wasnFile, corpus.getDoc(d), phi, model);
            }

            return model;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 2009: void write_word_assignment(FILE* f, document* doc, double** phi, lda_model* model)
    private static void writeWordAssignment(BufferedWriter wasnFile,
        Document doc, double[][] phi, LdaModel model) throws IOException {
        int n;

        //fprintf(f, "%03d", doc->length);
        wasnFile.write(doc.getLength());
        for (n = 0; n < doc.getLength(); n++) {
            //fprintf(f, " %04d:%02d",
            wasnFile.write(" " + doc.getWord(n) + ":"
                + Utils.argmax(phi[n], model.getNumTopics()));
        }
        wasnFile.write('\n');
        wasnFile.flush();
    }

    /**
     * read settings.
     */
    // 2009: void read_settings(char* filename)
    public static void readSettings(String filename) {
        String alphaAction = "";

        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = br.readLine()) != null) {

                if (line.startsWith("var max iter ")) {
                    LdaInference.VAR_MAX_ITER = Integer.parseInt(line
                        .substring(13).trim());
                } else if (line.startsWith("var convergence ")) {
                    LdaInference.VAR_CONVERGED = Float.parseFloat(line
                        .substring(16).trim());
                } else if (line.startsWith("em max iter ")) {
                    EM_MAX_ITER = Integer.parseInt(line.substring(12).trim());
                } else if (line.startsWith("em convergence ")) {
                    EM_CONVERGED = Float.parseFloat(line.substring(15).trim());
                } else if (line.startsWith("alpha ")) {
                    alphaAction = line.substring(6).trim();
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

        if (alphaAction.equals("fixed")) {
            ESTIMATE_ALPHA = false;
        } else {
            ESTIMATE_ALPHA = true;
        }
    }

    /**
     * inference only
     */
    // 2009: void infer(char* model_root, char* save, corpus* corpus)
    public static void infer(String modelRoot, String save, Corpus corpus) {
        String filename;
        int d;
        LdaModel model;
        double[][] varGamma, phi;
        double likelihood;
        Document doc;

        model = new LdaModel(modelRoot);
        varGamma = new double[corpus.getNumDocs()][model.getNumTopics()];
        filename = save + "-lda-lhood.dat";

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
            for (d = 0; d < corpus.getNumDocs(); d++) {
                if ((d % 100) == 0)
                    System.out.println("document " + d);

                doc = corpus.getDoc(d);
                phi = new double[doc.getLength()][model.getNumTopics()];
                likelihood = LdaInference.ldaInference(doc, model, varGamma[d],
                    phi);

                bw.write(Utils.formatDouble(likelihood));
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        filename = save + "-gamma.dat";
        saveGamma(filename, varGamma, corpus.getNumDocs(), model.getNumTopics());
    }

    /**
     * main
     */
    // 2009: int main(int argc, char* argv[])
    public static void main(String[] args) {

        Corpus corpus;

        if (args.length < 1) {
            usage();
            return;
        }

        // command: lda est 0.10 16 settings.txt berry/berry.dat seeded
        // berry.model
        if (args[0].equals("est")) {
            if (args.length < 7) {
                usage();
                return;
            }

            INITIAL_ALPHA = Float.parseFloat(args[1]);
            K = Integer.parseInt(args[2]);
            readSettings(args[3]);
            corpus = new Corpus(args[4]);
            @SuppressWarnings("unused")
            boolean a = new File(args[6]).mkdir();

            System.out.println("LDA estimation. Settings:");
            System.out.println("\tvar max iter " + LdaInference.VAR_MAX_ITER);
            System.out.println("\tvar convergence "
                + LdaInference.VAR_CONVERGED);
            System.out.println("\tem max iter " + EM_MAX_ITER);
            System.out.println("\tem convergence " + EM_CONVERGED);
            System.out.println("\testimate alpha " + ESTIMATE_ALPHA);

            runEm(args[5], args[6], corpus);

        } else {
            // command: lda inf settings.txt berry.model berry/berry.dat
            // berry.inf
            if (args.length < 5) {
                System.out
                    .println("usage\n: lda inf <settings> <model> <data> <name>\n");
                System.out
                    .println("example\n: lda inf settings.txt ..\\ap.model ..\\aptest ..\\aptest.inf\n");
                return;
            }
            readSettings(args[1]);

            System.out.println("LDA inference. Settings:");
            System.out.println("\tvar max iter " + LdaInference.VAR_MAX_ITER);
            System.out.println("\tvar convergence "
                + LdaInference.VAR_CONVERGED);
            System.out.println("\tem max iter " + EM_MAX_ITER);
            System.out.println("\tem convergence " + EM_CONVERGED);
            System.out.println("\testimate alpha " + ESTIMATE_ALPHA);

            corpus = new Corpus(args[3]);

            infer(args[2], args[4], corpus);

        }
        System.out.println("EXIT.");
    }

    private static void usage() {
        System.out
            .println("usage:\n\t lda est <initial alpha> <k> <settings> <data> <random/seeded/*> <directory>");
        System.out
            .println("example:\n\t lda est 0.1 100 settings.txt ./nips/nips.corpus seeded ./nips.model");
        return;
    }
}
