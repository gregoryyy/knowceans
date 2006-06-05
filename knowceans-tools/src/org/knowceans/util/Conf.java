/*
 * Copyright (c) 2002 Gregor Heinrich.  All rights reserved.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads configuration information from a properties file. Implemented as
 * singleton.
 * <p>
 * The file is looked up according to the following priorities list:
 * <ul>
 * <li>./[Content of system property conf.properties.file if set] (e.g.,
 * runtime jvm option
 * -Dknowceans.properties.file=d:\eclipse\workspace\indexer.properties)
 * <li>./[Main class name without package declaration and .class
 * suffix].properties
 * <li>./knowceans.properties
 * </ul>
 * <p>
 * This version allows the definition of variables that can be expanded at
 * readtime:
 * 
 * <pre> 
 * @{x1} in values will be expanded according to the respective property
 * @x1=val. The user MUST avoid circular references.
 * </pre>
 * 
 * @author heinrich
 */
public class Conf extends Properties {

    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3256728368379344693L;
    /**
     * Comment for <code>serialVersionUID</code>
     */
    protected static Conf instance;
    protected static String propFile = "knowceans.properties";
    protected static String basePath = ".";
    Pattern varPattern;

    /**
     * get the instance of the singleton object
     * 
     * @return
     */
    public static Conf get() {

        if (instance == null)
            instance = new Conf();

        return instance;
    }

    /**
     * get the named property from the singleton object
     * 
     * @return
     */
    public static String get(String key) {
        String p = Conf.get().getProperty(key);
        if (p != null) {
            p = instance.resolveVariables(p).trim();
        }
        return p;
    }
    

    /**
     * Get the property and replace the braced values with the array given.
     * 
     * @param key
     * @param braces
     * @return
     */
    private static Object get(String key, String[] braces) {
        String a = get(key);
        MessageFormat mf = new MessageFormat(a);
        return mf.format(braces);
    }

    /**
     * get a numeric value.
     * 
     * @param key
     * @return
     */
    public static double getDouble(String key) {
        String a = get(key);
        return Double.parseDouble(a);
    }

    public static float getFloat(String key) {
        return (float) getDouble(key);
    }

    /**
     * get a numeric value.
     * 
     * @param key
     * @return
     */
    public static long getLong(String key) {
        String a = get(key);
        return Long.parseLong(a);
    }

    /**
     * Get an integer value.
     * 
     * @param key
     * @return
     */
    public static int getInt(String key) {
        return (int) getLong(key);
    }

    /**
     * Get a double array from the file, where the vales are separated by comma,
     * semicolon or space.
     * 
     * @param key
     * @return
     */
    public static double[] getDoubleArray(String key) {
        String a = get(key);
        if (a == null || a.trim().equals("null"))
            return null;
        a = a.replaceAll(" +", " ").replaceAll(", ", ",");
        String[] ss = a.split("[;, ]");
        double[] ii = new double[ss.length];
        for (int i = 0; i < ii.length; i++) {
            ii[i] = Double.parseDouble(ss[i]);
        }
        return ii;
    }

    /**
     * Get an integer array from the file, where the vales are separated by
     * comma, semicolon or space.
     * 
     * @param key
     * @return
     */
    public static int[] getIntArray(String key) {
        String a = get(key);
        if (a == null || a.trim().equals("null"))
            return null;
        a = a.replaceAll(" +", " ").replaceAll(" *, *", ",");
        String[] ss = a.split("[;, ]");
        int[] ii = new int[ss.length];
        for (int i = 0; i < ii.length; i++) {
            ii[i] = Integer.parseInt(ss[i]);
        }
        return ii;
    }

    /**
     * get a boolean value: true and 1 are allowed for true, anything else for
     * false
     * 
     * @param key
     * @return
     */
    public static boolean getBoolean(String key) {
        String a = get(key);
        if (a.trim().equals("true") || a.trim().equals("1"))
            return true;
        return false;
    }

    /**
     * Protected constructor
     */
    protected Conf() {

        super();
        lookupPropFile();
        try {
            load(new FileInputStream(propFile));
            varPattern = Pattern.compile("(\\@\\{([^\\}]+)\\})+");
        } catch (FileNotFoundException e) {
            System.out.println("no properties file found: " + propFile);
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void lookupPropFile() {
        // property controlled file
        // System.out.println(System.getProperty("user.dir"));
        String temp = System.getProperty("knowceans.properties.file");
        if (temp != null) {
            propFile = temp;
            return;
        }
        // try to find a properties file that is called same as the main type
        String b = Which.main();
        b += ".properties";
        if (new File(b).exists()) {
            propFile = b;
            return;
        }
        // (default already in propfile)
    }

    /**
     * Resolves all variables of the argument string using the respective
     * properties. The method works recursively, so dependent variables are
     * resolved.
     * 
     * @param p
     * @return
     */
    private synchronized String resolveVariables(String line) {
        StringBuffer sb = new StringBuffer();
        Matcher m = varPattern.matcher(line);
        while (m.find()) {
            String a = m.group(2);
            String x = get("@".concat(a));
            if (x != null)
                m = m.appendReplacement(sb, x);
        }
        sb = m.appendTail(sb);
        if (sb.toString() == "")
            return line;
        return sb.toString();

    }

    /**
     * @return
     */
    public static String getBasePath() {
        return basePath;
    }

    /**
     * @return
     */
    public static String getPropFile() {
        return propFile;
    }

    /**
     * @param string
     */
    public static void setBasePath(String string) {
        basePath = string;
    }

    /**
     * @param string
     */
    public static void setPropFile(String string) {
        propFile = string;
    }

    public static void main(String[] args) {
        System.out.println(Conf.get("test", new String[]{"Gregor"}));
    }
}
