/*
 * Created on Nov 10, 2003 To change the template for this generated file go to
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.http.BrowserDetector;
import org.knowceans.map.InvertibleHashMap;

/**
 * Reads web logs from Apache's and Tomcat's logging files.
 * @author heinrich
 */
public class WeblogAnalyzer {

    private String directory;
    //	private String namePattern = "x.*2003(-09|-10|11).*\\.log";
    private String namePattern = ".*\\.log";
    //	private String namePattern = ".*\\.log";

    private static int count = 0;
    /* 
     * start=0, end=261
     * Group(0) = dhcp-153-96-112-2.ist.fhg.de - - [31/Oct/2003:10:55:21 1000] "\n
     * GET /xsearch?cmd=search&query=a-C%3AH-Schichten HTTP/1.0" 200 5933 \n
     * "http://fhgxpert.igd.fhg.de/xsearch?cmd=search&query=a-C%3AH-Schicht" \n
     * "Mozilla/4.0 (compatible; MSIE 5.01; Windows NT 5.0; T312461)"
     * Group(1) = dhcp-153-96-112-2.ist.fhg.de
     * Group(2) = 31/Oct/2003:10:55:21
     * Group(3) = /xsearch?cmd=search&query
     * Group(4) = a-C%3AH-Schichten
     * Group(5) =  MSIE 5.01
     * Group(6) =  Windows NT 5.0
     */

    /**
     * keeps the pattern of a log line.
     * <pre>
     * (.+) - .+ \[(.+) 1000\] "GET .+query=([^ &]+).* HTTP/1.\d" 200 \d+ ".+" "[^;]+;([^;]+);([^;]+).*\).*"
     * (.+) - .+ \[(.+) 1000\] "GET .+query=([^ &]+).* HTTP/1.\d" 200 \d+ ".+" "([^;]+;([^;]+);([^;]+).*\).*)"
     * </pre>
     */
    private String linePattern =
        "(.+) - .+ \\[(.+) 1000\\] \"GET .+query=([^ &]+).* "
            + "HTTP/1.\\d\" 200 \\d+ \".+\" \"([^;]+;([^;]+);([^;]+).*\\).*)\"";
    /**
     * the expected number of groups matched by a log line and
     * parsed with the linePattern
     */
    private int expectedGroupCount = 6;

    private Pattern pattern;
    /**
     * the date format used in the log files.
     */
    private String dateFormat = "dd/MMM/yyyy:HH:mm:ss";

    /**
     * key: query string, val: number of accesses. In principle, the values
     * could be Vectors of AccessRecords that store all information. 
     */
    private InvertibleHashMap<String, Integer> tableOfQueries;
    /**
     * key: date, val: number of accesses.
     */
    private InvertibleHashMap<String, Integer> tableOfDates;
    /**
     * key: browsers, val: number of accesses.
     */
    private InvertibleHashMap<String, Integer> tableOfBrowsers;
    /**
     * key: system, val: number of systems used.
     */
    private InvertibleHashMap<String, Integer> tableOfSystems;
    /**
     * key: host, val: number of accesses.
     */
    private InvertibleHashMap<String, Integer> tableOfHosts;
    private SimpleDateFormat dateParser;

    /**
     * @param directory
     */
    public WeblogAnalyzer(String directory) {

        this.directory = directory;
        pattern = Pattern.compile(linePattern);
        dateParser = new SimpleDateFormat(dateFormat);

        tableOfQueries = new InvertibleHashMap<String, Integer>();
        tableOfDates = new InvertibleHashMap<String, Integer>();
        tableOfBrowsers = new InvertibleHashMap<String, Integer>();
        tableOfSystems = new InvertibleHashMap<String, Integer>();
        tableOfHosts = new InvertibleHashMap<String, Integer>();

    }

    public WeblogAnalyzer(Tables data) {
        setTables(data);
    }

    /**
     * @param data
     */
    public void setTables(Tables data) {
        this.tableOfBrowsers = data.tableOfBrowsers;
        this.tableOfDates = data.tableOfDates;
        this.tableOfHosts = data.tableOfHosts;
        this.tableOfQueries = data.tableOfQueries;
        this.tableOfSystems = data.tableOfSystems;
    }

    public Tables getTables() {
        Tables data = new Tables();
        data.tableOfBrowsers = tableOfBrowsers;
        data.tableOfDates = tableOfDates;
        data.tableOfHosts = tableOfHosts;
        data.tableOfQueries = tableOfQueries;
        data.tableOfSystems = tableOfSystems;
        return data;
    }

    /**
     * 
     */
    public void start() {
        File dir = new File(directory);
        if (dir.isDirectory()) {
            String[] files = dir.list(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    if (name.matches(namePattern))
                        return true;
                    return false;
                }
            });
            //			System.out.println(Arrays.asList(files));
            for (int i = 0; i < files.length; i++) {
                processFile(files[i]);
            }
        } else
            System.out.println("Not a directory: " + directory);
    }

    public void processFile(String filename) {
        BufferedReader br = null;
        count = 0;
        System.out.flush();
        System.err.flush();
        String file = directory + "/" + filename;
        try {
            System.out.println("\nNow starting with " + file);
            br = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String line = null;

        try {
            while ((line = br.readLine()) != null) {
                processLine(line);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void processLine(String line) {

        Date date = null;

        Matcher m = pattern.matcher(line);
        if (!m.find() || m.groupCount() != expectedGroupCount)
            return;

        String host = m.group(1);
        incrementMapEntry(host, tableOfHosts);
        try {
            date = dateParser.parse(m.group(2));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        incrementMapEntry(date, tableOfDates);
        String query = URLDecoder.decode(m.group(3)).toLowerCase().trim();
        incrementMapEntry(query, tableOfQueries);
        String browser = m.group(4).trim();
        BrowserDetector bd = new BrowserDetector(browser);
        incrementMapEntry(
            bd.getBrowserName() + " " + bd.getBrowserVersion(),
            tableOfBrowsers);
        //        incrementMapEntry(browser, tableOfBrowsers);
        //        String system = m.group(6).toLowerCase().trim();
        incrementMapEntry(bd.getBrowserPlatform(), tableOfSystems);

        //        System.out.println(
        //            "host='"
        //                + host
        //                + "' date='"
        //                + date
        //                + "' query='"
        //                + query
        //                + "' browser='"
        //                + browser
        //                + "' system='"
        //                + system
        //                + "'.");
        query = " '" + query + "'";
        formattedPrint(query, false);
    }

    /**
     * @param query
     */
    private void formattedPrint(String word, boolean toError) {
        if (toError)
            System.err.print(word);
        else
            System.out.print(word);
        count += word.length();
        if (count > 70) {
            System.out.println("");
            count = 0;
        }

    }

    /**
     * increments the frequency of the category in the 
     * InvertibleHashMap provided in the arg. This method is designed to 
     * work with the class fields.
     * @param field
     */
    public void incrementMapEntry(String key, InvertibleHashMap<String, Integer> field) {
        Integer freq = null;
        if ((freq = field.get(key)) != null)
            field.put(key, freq + 1);
        else
            field.put(key, new Integer(1));
        if (field == tableOfQueries)
            if (freq != null) {
                String x = "=" + ((freq.intValue()) + 1) + " ";
                formattedPrint(x, false);
            } else {
                if (field == tableOfQueries) {
                    formattedPrint(" '" + key + "'=1", true);

                }
            }
    }

    /**
     * increments the frequency of the date in the 
     * InvertibleHashMap provided in the arg. This method is designed to 
     * work with the class fields.
     * @param field
     */
    public void incrementMapEntry(Date key, InvertibleHashMap field) {
        key.setMinutes(0);
        key.setHours(0);
        key.setSeconds(0);
        Integer freq;
        if ((freq = (Integer) field.get(key)) != null)
            field.put(key, new Integer(freq.intValue() + 1));
        else
            field.put(key, new Integer(1));
    }
    /**
     * @return Returns the dateFormat.
     */
    public String getDateFormat() {
        return dateFormat;
    }

    /**
     * @param dateFormat The dateFormat to set.
     */
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    /**
     * @return Returns the expectedGroupCount.
     */
    public int getExpectedGroupCount() {
        return expectedGroupCount;
    }

    /**
     * @param expectedGroupCount The expectedGroupCount to set.
     */
    public void setExpectedGroupCount(int expectedGroupCount) {
        this.expectedGroupCount = expectedGroupCount;
    }

    /**
     * @return Returns the linePattern.
     */
    public String getLinePattern() {
        return linePattern;
    }

    /**
     * @param linePattern The linePattern to set.
     */
    public void setLinePattern(String linePattern) {
        this.linePattern = linePattern;
    }

    /**
     * @return Returns the directory.
     */
    public String getDirectory() {
        return directory;
    }

    /**
     * @param directory The directory to set.
     */
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    /**
     * @return Returns the namePattern.
     */
    public String getNamePattern() {
        return namePattern;
    }

    /**
     * @param namePattern The namePattern to set.
     */
    public void setNamePattern(String namePattern) {
        this.namePattern = namePattern;
    }

    /**
     * @return Returns the dateParser.
     */
    public SimpleDateFormat getDateParser() {
        return dateParser;
    }

    /**
     * @param dateParser The dateParser to set.
     */
    public void setDateParser(SimpleDateFormat dateParser) {
        this.dateParser = dateParser;
    }

    /**
     * @return Returns the pattern.
     */
    public Pattern getPattern() {
        return pattern;
    }

    /**
     * @param pattern The pattern to set.
     */
    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

}
