/*
 * Copyright (c) 2005 Gregor Heinrich.  All rights reserved.
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
/*
 * Created on May 26, 2005
 */
package org.knowceans.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Arguments is a convenience class that reads command line arguments for main
 * and validates them against a given format. The general grammar of a
 * commandline is:
 * <p>
 * 
 * <pre>
 *      commandline ::= command &quot; &quot; options arguments
 * </pre>
 * 
 * <p>
 * Here the options and arguments follow the grammar:
 * <p>
 * 
 * <pre>
 *             options   ::= ( option &quot; &quot; )+
 *             option    ::= &quot;-&quot; name (&quot; &quot; value)?
 *             name      ::= &lt;LITERAL&gt;  
 *             arguments ::= ( argument )+ 
 *             argument  ::= value
 *             value     ::= &lt;LITERAL&gt; | &quot;&quot;&quot;( &lt;LITERAL&gt; | &quot; &quot; )+ &quot;&quot;&quot;
 * </pre>
 * 
 * with some value. These values can be restricted to specific formats, which is
 * done using a format string
 * 
 * @author heinrich
 */
public class Arguments {

    String optionFormat = "(\\w+)(=([ilfdbspu0]))?";

    String types = "ilfdbspu";

    String argFormat = "[" + types + "]";

    /**
     * contains the option types
     */
    TreeMap<String, Character> optionTypes = new TreeMap<String, Character>();

    /**
     * contains the argument types of required arguments
     */
    StringBuffer argTypes = new StringBuffer();

    /**
     * contains the arguments
     */
    Vector<Object> arguments = new Vector<Object>();

    /**
     * contains the options
     */
    HashMap<String, Object> options = new HashMap<String, Object>();

    int minArgs = 0;

    int maxArgs = 0;

    public static void main(String[] args) {
        String[] commandline = "-xserv .4 -s test -v -words 34 -f \"test object\" -g 1.2 -t http://www.ud path path2 true"
            .split(" ");

        String format = "xserv=f words=i f=s g=f s=p v=0 r=0 t=u";
        String types = "pp|bb";
        Arguments arg = new Arguments(format, types);
        arg.parse(commandline);
        System.out.println(arg.getOption("xserv", .5));
        System.out.println(arg.getOption("g"));
        System.out.println(arg.getOption("t"));
        System.out.println(arg.getOption("v"));
        System.out.println(arg.getOptions());
        System.out.println(arg.getArguments());
        System.out.println(arg.maxArgs);
        System.out.println(arg);
    }

    /**
     * get the map of options.
     * 
     * @return
     */
    public HashMap<String, Object> getOptions() {
        return options;
    }

    /**
     * returns the named option value. If it has no parameter, Boolean.TRUE is
     * returned if given, otherwise Boolean.FALSE.
     * 
     * @param string
     *            key for the option parameter.
     * @return value of option parameter (that can be casted to the specific
     *         type) or null if not given at command line or not in format.
     * @throws IllegalArgumentException
     */
    public Object getOption(String string) throws IllegalArgumentException {
        Object obj = options.get(string);
        Character type = optionTypes.get(string);
        if (obj == null && type == null) {
            throw new IllegalArgumentException("Option " + string + " unknown.");
        }
        if (type == '0') {
            if (obj == null)
                obj = Boolean.FALSE;
        }
        return obj;
    }

    // jan 3532872
    /**
     * Same as getOption, but allows default value (whose type is NOT checked).
     * 
     * @param string
     * @param defaultValue
     * @return
     * @throws IllegalArgumentException
     */
    public Object getOption(String string, Object defaultValue)
        throws IllegalArgumentException {
        Object obj = getOption(string);
        if (obj == null) {
            return defaultValue;
        }
        return obj;
    }

    /**
     * get the vector of all arguments.
     * 
     * @return
     */
    public Vector<Object> getArguments() {
        return arguments;
    }

    /**
     * Returns the argument with index i and null if i is compliant with the
     * format but not specified at commandline.
     * 
     * @param i
     * @return
     * @throws IllegalArgumentException
     */
    public Object getArgument(int i) throws IllegalArgumentException {
        if (i > argTypes.length() - 1) {
            throw new IllegalArgumentException("Format supports only "
                + maxArgs + " arguments, not " + (i + 1) + ".");
        }
        if (i > arguments.size() - 1) {
            return null;
        }
        return arguments.get(i);
    }

    /**
     * Same as getArgument, but returns a default value if optional argument is
     * not set. The type of the default is NOT checked.
     * 
     * @param i
     * @param defaultValue
     * @return
     */
    public Object getArgument(int i, Object defaultValue) {
        Object obj = getArgument(i);
        if (obj == null) {
            return defaultValue;
        }
        return obj;
    }

    /**
     * Initialise the arguments parser with a format string and an argument type
     * specification. For the options and arguments, the formats are defined by
     * this constructor.
     * <p>
     * The format string for the options is composed of the following grammar:
     * 
     * <pre>
     *             foptions   ::= ( option )*
     *             foption    ::= name=format
     *             fname      ::= &lt;LITERAL&gt;
     *             fotype     ::= ( i | f | b | u | p | s | 0 )
     * </pre>
     * 
     * The literals of format correspond to the types int/long, float/double,
     * boolean, java.net.URL, java.io.File, java.lang.String and 0 for
     * unparametrised options
     * <p>
     * The format string for the arguments is composed of the following grammar:
     * 
     * <pre>
     *             farguments    ::= frequiredargs &quot;|&quot; foptionalargs
     *             frequiredargs ::= ( fatype )+
     *             foptionalargs ::= ( fatype )+
     *             fatype        ::= ( i | f | b | u | p | s )
     * </pre>
     * 
     * Note in the format specification that empty arguments are not possible.
     * 
     * @param optformat
     */
    public Arguments(String optformat, String argtypes) {
        Matcher m = Pattern.compile(optionFormat).matcher(optformat);
        while (m.find()) {
            String type = m.group(3) != null ? m.group(3) : "0";
            optionTypes.put(m.group(1), type.charAt(0));
        }
        minArgs = argtypes.indexOf('|');
        if (minArgs == -1) {
            minArgs = argtypes.length();
        }
        m = Pattern.compile(argFormat).matcher(argtypes);
        while (m.find()) {
            argTypes.append(m.group(0));
        }
        maxArgs = argTypes.length();
    }

    /**
     * parses the command line arguments string whose values can be found with
     * the getOption and getArgument methods afterwards.
     * 
     * @param a
     * @throws IllegalArgumentException
     */
    public void parse(String[] a) throws IllegalArgumentException {
        int nargs = 0;
        boolean needsparam = false;
        String option = "";
        for (int i = 0; i < a.length; i++) {
            if (a[i].startsWith("-")) {
                option = a[i].substring(1, a[i].length());
            } else {
                if (nargs > argTypes.length() - 1)
                    throw new IllegalArgumentException(
                        "Options do not comply with format. "
                            + "Check option parameters.");
                char type = argTypes.charAt(nargs);
                Object argument = getObject(a[i], type);
                if (argument == null) {
                    throw new IllegalArgumentException("Option " + option
                        + " has not required type " + type + ".");
                }
                arguments.add(argument);
                nargs++;
            }
            Character type = optionTypes.get(option);
            if (type == null) {
                throw new IllegalArgumentException("Option " + option
                    + " unknown.");
            }
            if (nargs > 0)
                continue;
            // read the parameter (if still reading options)
            if (type != '0') {
                i++;

                String value = "";
                // consume quoted parameters
                if (a[i].startsWith("\"")) {
                    a[i] = a[i].substring(1);
                    while (!a[i].endsWith("\"")) {
                        value += a[i] + " ";
                        i++;
                    }
                    a[i] = a[i].substring(0, a[i].length() - 1);
                }
                value += a[i];

                Object param = getObject(value, type);
                if (param == null) {
                    throw new IllegalArgumentException("Option " + option
                        + " has not required type " + type + ".");
                }
                options.put(option, param);
            } else {
                options.put(option, Boolean.TRUE);
            }
        }
        if (nargs < minArgs) {
            throw new IllegalArgumentException(
                "Number of required arguments is " + minArgs + ", but only "
                    + nargs + " given.");
        }
    }

    /**
     * @param string
     * @param b
     */
    private Object getObject(String string, char type)
        throws IllegalArgumentException {
        Object obj = null;
        try {
            if (type == 'b') {
                obj = Boolean.valueOf(string);
            } else if (type == 'f') {
                obj = Float.valueOf(string);
            } else if (type == 'd') {
                obj = Double.valueOf(string);
            } else if (type == 'i') {
                obj = Integer.valueOf(string);
            } else if (type == 'l') {
                obj = Long.valueOf(string);
            } else if (type == 's') {
                obj = string;
            } else if (type == 'u') {
                obj = new URL(string);
            } else if (type == 'p') {
                obj = new File(string).getCanonicalFile();
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Wrong parameter format ("
                + type + "): " + string);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Wrong URL format: " + string);
        } catch (IOException e) {
            throw new IllegalArgumentException("Wrong file name format: "
                + string);
        }
        return obj;
    }

    /**
     * describe the current arguments set
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Argument grammar:\n");
        sb.append("\nOptions:\n");
        for (String key : optionTypes.keySet()) {
            sb.append("  ").append("-").append(key).append(" : ").append(
                type(optionTypes.get(key))).append("\n");
        }
        if (minArgs > 0)
            sb.append("\nRequired argument types:\n");
        else 
            sb.append("\nNo required argument types.\n");
        for (int i = 0; i < minArgs; i++) {
            sb.append("  ").append(i + 1).append(" : ").append(
                type(argTypes.charAt(i))).append("\n");
        }
        if (maxArgs - minArgs > 0)
            sb.append("\nOptional argument types:\n");
        else 
            sb.append("\nNo optional argument types.\n");
        for (int i = minArgs; i < argTypes.length(); i++) {
            sb.append("  ").append(i + 1).append(" : ").append(
                type(argTypes.charAt(i))).append("\n");
        }
        return sb.toString();
    }

    public String type(char c) {
        if (c == 'i')
            return "int";
        if (c == 'l')
            return "long";
        if (c == 'f')
            return "float";
        if (c == 'd')
            return "double";
        if (c == 'p')
            return "java.io.File";
        if (c == 'u')
            return "java.net.URL";
        if (c == '0')
            return "(no value)";
        if (c == 'b')
            return "boolean";
        if (c == 's')
            return "java.lang.String";
        return "(unknown)";
    }
}
