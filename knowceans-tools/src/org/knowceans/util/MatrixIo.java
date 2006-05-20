/*
 * Created on 14.05.2006
 */
package org.knowceans.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * MatrixIo provides methods to load and save matrices and vectors to file,
 * which automatic compression if the file extension given is ".zip". The
 * standard methods are load/saveBinaryMatrix that load/save a double[][] with
 * the protocol rows,cols,foreach(row): foreach(col): double[row][col] :end
 * :end.
 * <p>
 * Custom protocols for more complex data can be easily constructed by opening a
 * stream using one of the open*Stream() methods, using the methods of the
 * Data*Stream classes and read/write* methods of this class for matrices and
 * vectors and then closing the stream using the close*Stream methods in this
 * class (provided for symmetry) or the close() methods in the Data*Stream
 * classes.
 * <p>
 * TODO: The binary methods could be considered for change to a subclass of
 * DataInputStream and DataOutputStream.
 * 
 * @author gregor
 */
public class MatrixIo {

    /**
     * Loads a matrix from a binary file, optionally a zip file. The method
     * actually reads a float matrix.
     * 
     * @param filename
     * @return
     */
    public static double[][] loadBinaryMatrix(String filename) {
        int m, n;
        double[][] a = null;
        int i = 0, j = 0;
        try {

            DataInputStream dis = openInputStream(filename);
            m = dis.readInt();
            n = dis.readInt();
            a = new double[m][n];
            for (i = 0; i < m; i++) {
                for (j = 0; j < n; j++) {
                    a[i][j] = dis.readFloat();
                }
            }
            closeInputStream(dis);

        } catch (IOException e) {
            System.err.println(i + " " + j);
            e.printStackTrace();
        }
        return a;
    }

    // compatibility matrix r/w

    /**
     * Writes matrix to binary file. If the file name ends with zip, the output
     * is zipped. Note: The method actually saves float values.
     * 
     * @param filename
     * @param a
     */
    public static void saveBinaryMatrix(String filename, double[][] a) {
        int i = 0, j = 0;

        try {
            DataOutputStream dos = openOutputStream(filename);
            dos.writeInt(a.length);
            dos.writeInt(a[0].length);
            for (i = 0; i < a.length; i++) {
                for (j = 0; j < a[0].length; j++) {
                    dos.writeFloat((float) a[i][j]);
                }
            }
            closeOutputStream(dos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println(i + " " + j);
            e.printStackTrace();
        }
    }

    /**
     * Read matrix from file.
     * 
     * @param bw
     * @return
     * @throws IOException
     */
    public static int[][] readIntMatrix(DataInputStream bw) throws IOException {
        int rows = bw.readInt();
        int[][] matrix = new int[rows][];
        for (int i = 0; i < rows; i++) {
            matrix[i] = readIntVector(bw);
        }
        return matrix;
    }

    // read methods

    /**
     * Read vector from file.
     * 
     * @param bw
     * @return
     * @throws IOException
     */
    public static int[] readIntVector(DataInputStream bw) throws IOException {
        int length = bw.readInt();
        int[] vector = new int[length];
        for (int i = 0; i < length; i++) {
            vector[i] = bw.readInt();
        }
        return vector;
    }

    /**
     * Read matrix from file.
     * 
     * @param bw
     * @return
     * @throws IOException
     */
    public static double[][] readDoubleMatrix(DataInputStream bw)
        throws IOException {
        int rows = bw.readInt();
        double[][] matrix = new double[rows][];
        for (int i = 0; i < rows; i++) {
            matrix[i] = readDoubleVector(bw);
        }
        return matrix;
    }

    /**
     * Read vector from file.
     * 
     * @param bw
     * @return
     * @throws IOException
     */
    public static double[] readDoubleVector(DataInputStream bw)
        throws IOException {
        int length = bw.readInt();
        double[] vector = new double[length];
        for (int i = 0; i < length; i++) {
            vector[i] = bw.readDouble();
        }
        return vector;
    }

    /**
     * Read matrix from file.
     * 
     * @param bw
     * @return
     * @throws IOException
     */
    public static float[][] readFloatMatrix(DataInputStream bw)
        throws IOException {
        int rows = bw.readInt();
        float[][] matrix = new float[rows][];
        for (int i = 0; i < rows; i++) {
            matrix[i] = readFloatVector(bw);
        }
        return matrix;
    }

    /**
     * Read vector from file.
     * 
     * @param bw
     * @return
     * @throws IOException
     */
    public static float[] readFloatVector(DataInputStream bw)
        throws IOException {
        int length = bw.readInt();
        float[] vector = new float[length];
        for (int i = 0; i < length; i++) {
            vector[i] = bw.readFloat();
        }
        return vector;
    }

    // write methods

    /**
     * Writes an integer matrix in the format
     * rows,cols1,a11,a12,a1...,cols2,a21,... This way, matrices can be stored
     * that have variable row lengths.
     * 
     * @param bw
     * @param matrix
     * @throws IOException
     */
    public static void writeIntMatrix(DataOutputStream bw, int[][] matrix)
        throws IOException {
        bw.writeInt(matrix.length);
        for (int i = 0; i < matrix.length; i++) {
            writeIntVector(bw, matrix[i]);
        }
    }

    /**
     * Writes an integer vector in the format size,v1,v2,...
     * 
     * @param bw
     * @param vector
     * @throws IOException
     */
    public static void writeIntVector(DataOutputStream bw, int[] vector)
        throws IOException {
        bw.writeInt(vector.length);
        for (int i = 0; i < vector.length; i++) {
            bw.writeInt(vector[i]);
        }
    }

    /**
     * Writes a double matrix in the format
     * rows,cols1,a11,a12,a1...,cols2,a21,...
     * 
     * @param bw
     * @param matrix
     * @throws IOException
     */
    public static void writeDoubleMatrix(DataOutputStream bw, double[][] matrix)
        throws IOException {
        bw.writeInt(matrix.length);
        for (int i = 0; i < matrix.length; i++) {
            writeDoubleVector(bw, matrix[0]);
        }
    }

    /**
     * Writes a double vector in the format size,v1,v2,...
     * 
     * @param bw
     * @param vector
     * @throws IOException
     */
    public static void writeDoubleVector(DataOutputStream bw, double[] vector)
        throws IOException {
        bw.writeInt(vector.length);
        for (int i = 0; i < vector.length; i++) {
            bw.writeDouble(vector[i]);
        }
    }

    /**
     * Writes a float matrix in the format rows,cols,a11,a12,a1...,a21,...
     * 
     * @param bw
     * @param matrix
     * @throws IOException
     */
    public static void writeFloatMatrix(DataOutputStream bw, float[][] matrix)
        throws IOException {
        bw.writeInt(matrix.length);
        for (int i = 0; i < matrix.length; i++) {
            writeFloatVector(bw, matrix[i]);
        }
    }

    /**
     * Writes a float vector in the format size,v1,v2,...
     * 
     * @param bw
     * @param vector
     * @throws IOException
     */
    public static void writeFloatVector(DataOutputStream bw, float[] vector)
        throws IOException {
        bw.writeInt(vector.length);
        for (int i = 0; i < vector.length; i++) {
            bw.writeFloat(vector[i]);
        }
    }

    // ascii methods

    public static String padSpace(String s, int length) {
        if (s == null)
            s = "[null]";
        StringBuffer b = new StringBuffer(s);
        for (int i = 0; i < length - s.length(); i++) {
            b.append(' ');
        }
        return b.substring(0, length);
    }

    static NumberFormat nf = new DecimalFormat("0.00000");

    /**
     * @param d
     * @return
     */
    public static String formatDouble(double d) {
        String x = nf.format(d);
        // String x = shadeDouble(d, 1);
        return x;

    }

    public static double[][] readAscii(String filename) {
        Vector<double[]> a = new Vector<double[]>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = br.readLine()) != null) {
                String[] fields = line.trim().split(" ");
                double[] row = new double[fields.length];
                for (int i = 0; i < fields.length; i++) {
                    row[i] = Double.parseDouble(fields[i]);
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
        return a.toArray(new double[0][0]);
    }

    /**
     * saves the matrix in ascii format
     */
    public static void saveAscii(String filename, double[][] a) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
            int d, k;
            for (d = 0; d < a.length; d++) {
                for (k = 0; k < a[0].length; k++) {
                    if (k > 0)
                        bw.write(' ');
                    bw.write(formatDouble(a[d][k]));
                }
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens a data output stream with optional zip compression. The returned
     * DataOutputStream can be written to and must be closed using
     * closeStream(DataOutputStream dos) or dos.close().
     * 
     * @param filename
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static DataOutputStream openOutputStream(String filename)
        throws FileNotFoundException, IOException {
        DataOutputStream dos = null;
        if (filename.endsWith(".zip")) {
            ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(
                filename));
            String name = new File(filename).getName();
            zip.putNextEntry(new ZipEntry(name.substring(0, name.length() - 3)
                + "bin"));
            dos = new DataOutputStream(new BufferedOutputStream(zip));
        } else {
            dos = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(filename)));
        }
        return dos;
    }

    /**
     * Close the data output, which results in flushing the write buffer and
     * closing the file.
     * 
     * @param dos
     * @throws IOException
     */
    public static void closeOutputStream(DataOutputStream dos)
        throws IOException {
        dos.close();
    }

    /**
     * Opens a data input stream with optional zip compression. The returned
     * DataInputStream can be read from and must be closed using
     * closeStream(DataOutputStream dos) or dos.close().
     * 
     * @param filename
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static DataInputStream openInputStream(String filename)
        throws IOException, FileNotFoundException {
        DataInputStream dis = null;

        if (filename.endsWith(".zip")) {

            ZipFile f = new ZipFile(filename);
            String name = new File(filename).getName();
            dis = new DataInputStream(new BufferedInputStream(f
                .getInputStream(f.getEntry(name.substring(0, name.length() - 3)
                    + "bin"))));
        } else {
            dis = new DataInputStream(new BufferedInputStream(
                new FileInputStream(filename)));
        }
        return dis;
    }

    /**
     * Close the input stream
     * 
     * @param dis
     * @throws IOException
     */
    public static void closeInputStream(DataInputStream dis) throws IOException {
        dis.close();
    }

}
