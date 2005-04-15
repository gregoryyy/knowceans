/*
 * Created on Nov 10, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
/*
 * Copyright (c) 2003 Gregor Heinrich.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESSED 
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY 
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN 
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package org.knowceans.util;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.knowceans.map.InvertibleHashMap;

/**
 * for serialization of the results without having to face incompatibilities
 * when developing WeblogAnalyzer.
 * @author heinrich
 */
public class Tables {
    /**
     * creates empty hashtables
     */
    public Tables() {

        tableOfQueries = new InvertibleHashMap<String, Integer>();
        tableOfDates = new InvertibleHashMap<String, Integer>();
        tableOfBrowsers = new InvertibleHashMap<String, Integer>();
        tableOfSystems = new InvertibleHashMap<String, Integer>();
        tableOfHosts = new InvertibleHashMap<String, Integer>();

    }

    /**
    * key: query string, val: number of accesses. In principle, the values
    * could be Vectors of AccessRecords that store all information. 
    */
    public InvertibleHashMap<String, Integer> tableOfQueries;
    /**
     * key: date, val: number of accesses.
     */
    public InvertibleHashMap<String, Integer> tableOfDates;
    /**
     * key: browsers, val: number of accesses.
     */
	public InvertibleHashMap<String, Integer> tableOfBrowsers;
    /**
     * key: system, val: number of systems used.
     */
	public InvertibleHashMap<String, Integer> tableOfSystems;
    /**
     * key: host, val: number of accesses.
     */
	public InvertibleHashMap<String, Integer> tableOfHosts;

    /**
     * Can read an xml or a zipped version of the serialized 
     * object. If zipped, the file itself is the entry in the zip with
     * the same name and xml replacing zip "test.zip" --> "test.xml".
     */
    public void readFromFile(String inputFile) {
        System.out.println("Reading from " + inputFile);
        if (inputFile.endsWith(".xml")) {
            try {
                readFromStream(new FileInputStream(inputFile));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            if (inputFile.endsWith(".zip")) {
                try {
                    File zip = new File(inputFile);
                    String xml = zip.getName().replaceAll("\\.zip$", "\\.xml");
                    System.out.println(" - Opening zip.");
                    ZipFile f = new ZipFile(zip);
                    System.out.println(" - Trying to find " + xml);
                    readFromStream(f.getInputStream(f.getEntry(xml)));
                    System.out.println(" -> ok.");
                    f.close();
                } catch (ZipException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.err.println(
                    " - Input file " + inputFile + ": unknown extension.");
            }

        }
    }

    /**
     * reads an object from the stream and closes it.
     * @param is
     */
    public void readFromStream(InputStream is) {
        XMLDecoder in = new XMLDecoder(is);
        tableOfQueries = (InvertibleHashMap<String, Integer>) in.readObject();
        tableOfDates = (InvertibleHashMap<String, Integer>) in.readObject();
        tableOfBrowsers = (InvertibleHashMap<String, Integer>) in.readObject();
        tableOfSystems = (InvertibleHashMap<String, Integer>) in.readObject();
        tableOfHosts = (InvertibleHashMap<String, Integer>) in.readObject();
        System.out.println(" - objects successfully read.");
        in.close();
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Can write to an xml or a zipped file to serialize the 
     * object. If zipped, the file itself is the entry in the zip with
     * the same name and xml replacing zip "test.zip" --> "test.xml".
     */
    public void writeToFile(String outputFile) {
        System.out.println("Writing to " + outputFile);
        if (outputFile.endsWith(".xml")) {
            try {
                writeToStream(new FileOutputStream(outputFile));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            if (outputFile.endsWith(".zip")) {
                try {
                    File zip = new File(outputFile);
                    String xml = zip.getName().replaceAll("\\.zip$", "\\.xml");
                    System.out.println(" - Opening zip.");
                    ZipOutputStream z =
                        new ZipOutputStream(new FileOutputStream(zip));
                    z.putNextEntry(new ZipEntry(xml));
                    System.out.println(" - Trying to write " + xml);
                    writeToStream(z);
                    System.out.println(" -> ok.");
                    //                    z.closeEntry();
                    z.close();
                } catch (ZipException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.err.println(
                    " - Output file " + outputFile + ": unknown extension.");
            }
        }
    }

    /**
     * Object stream only for testing.
     */
    public void writeToStream(OutputStream os) {
        XMLEncoder out = new XMLEncoder(os);
        out.writeObject(tableOfQueries);
        out.writeObject(tableOfDates);
        out.writeObject(tableOfBrowsers);
        out.writeObject(tableOfSystems);
        out.writeObject(tableOfHosts);
        out.close();
    }

}