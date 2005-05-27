/*
 * Copyright (c) 2005 Gregor Heinrich. All rights reserved. Redistribution and
 * use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met: 1. Redistributions of source
 * code must retain the above copyright notice, this list of conditions and the
 * following disclaimer. 2. Redistributions in binary form must reproduce the
 * above copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESSED OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
 * <code>
 * commandline ::= command &quot; &quot; options arguments
 * </code>
 * <p>
 * Here the options and arguments follow the grammar:
 * <p>
 * <code>
 * options   ::= ( option &quot; &quot; )+
 * option    ::= &quot;-&quot; name (&quot; &quot; value)?
 * name      ::= &lt;LITERAL&gt;  
 * arguments ::= ( argument )+ 
 * argument  ::= value
 * value     ::= &lt;LITERAL&gt; | &quot;&quot;&quot;( &lt;LITERAL&gt; | &quot; &quot; )+ &quot;&quot;&quot;
 * </code>
 * with some value. Values can be restricted to specific formats, which is done
 * using a format string. Known types are int, float, long, double, boolean,
 * String, File and URL. String and File values can contain spaces if put within
 * quotes.
 * 
 * @author heinrich
 */
public class Arguments {

    String helpFormat = "\\s*(\\{([^\\}]+)\\})?";

    String types = "ilfdbspu";

    String optionFormat = "(\\w+)(=([" + types + "0]))?" + helpFormat;

    String argFormat = "([" + types + "])" + helpFormat;

    /**
     * contains the option types
     */
    TreeMap<String, Character> optionTypes = new TreeMap<String, Character>();

    /**
     * contains help information
     */
    TreeMap<String, String> help = new TreeMap<String, String>();

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
        String[] commandline = "-xserverstarttime .4 -s test -v -words 34 -f \"test object\" -g 1.2 -t http://www.ud path path2 true"
            .split(" ");

        String format = "xserverstarttime=f {xserv test constant} " + //
            "words=i {number of words}" + //
            "f=s {file string} " + //
            "g=f {global weight} " + //
            "s=p {output file} " + //
            "v=0 {verbose} " + "r=0 {reloadable\n(for the server)} t=u";
        String types = "p {infile} p {outfile} | b {use patterns} b {debug}";
        Arguments arg = new Arguments(format, types);
        arg.parse(commandline);
        System.out.println(arg.getOption("xserverstarttime", .5));
        System.out.println(arg.getOption("g"));
        System.out.println(arg.getOption("t"));
        System.out.println(arg.getOption("v"));
        System.out.println(arg.getOptions());
        System.out.println(arg.getArguments());
        System.out.println(arg.maxArgs);
        System.out.println(arg);
    }

    /**
     * Initialise the arguments parser with a format string and an argument type
     * specification. For the options and arguments, the formats are defined by
     * this constructor.
     * <p>
     * The format string for the options is composed of the following grammar:
     * <code>
     * foptions   ::= ( option )*
     * foption    ::= name=fotype " "? ( "{" fhelp "}" )?
     * fname      ::= &lt;LITERAL&gt;
     * fotype     ::= ( i | l | f | d | b | u | p | s | 0 )
     * fhelp      ::= &lt;LITERAL&gt;
     * </code>
     * The literals of fotype correspond to the types int, float, long, double,
     * boolean, java.net.URL, java.io.File (p), java.lang.String, and void (0)
     * for unparametrised options
     * <p>
     * The format string for the arguments is composed of the following grammar:
     * <code>
     * farguments    ::= frequiredargs &quot;|&quot; foptionalargs
     * frequiredargs ::= ( fatype " "? ( "{" fhelp "}" )? )+
     * foptionalargs ::= ( fatype " "? ( "{" fhelp "}" )? )+)+
     * fatype        ::= ( i | l | f | d | b | u | p | s )
     * fhelp      ::= &lt;LITERAL&gt;
     * </code>
     * Note in the format specification that empty arguments are not possible.
     * The help strings can include line breaks "\n".
     * 
     * @param optformat
     */
    public Arguments(String optformat, String argtypes) {
        Matcher m = Pattern.compile(optionFormat).matcher(optformat);
        while (m.find()) {
            String type = m.group(3) != null ? m.group(3) : "0";
            optionTypes.put(m.group(1), type.charAt(0));
            String desc = m.group(5);
            if (desc != null) {
                help.put(m.group(1), desc);
            }
        }
        minArgs = argtypes.replaceAll(" ", "").replaceAll("\\{[^\\}]+\\}", "")
            .indexOf('|');
        if (minArgs == -1) {
            minArgs = argtypes.length();
        }
        m = Pattern.compile(argFormat).matcher(argtypes);
        int narg = 1;
        while (m.find()) {
            argTypes.append(m.group(1));
            String desc = m.group(3);
            if (desc != null) {
                help.put(Integer.toString(narg), desc);
                narg++;
            }
        }
        maxArgs = argTypes.length();
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
        sb
            .append("Runtime argument order: <options> <required arguments> <optional arguments>\n");
        sb.append("\nOptions:\n");
        int maxOption = 0;
        for (String a : optionTypes.keySet()) {
            maxOption = Math.max(maxOption, a.length());
        }
        maxOption += 8;
        for (String key : optionTypes.keySet()) {
            sb.append("  ").append("-").append(key);
            spacePad(sb, maxOption);
            sb.append(type(optionTypes.get(key)));
            addDescription(sb, key, maxOption + 10);
            sb.append("\n");

        }
        if (minArgs > 0)
            sb.append("\nRequired arguments:\n");
        else
            sb.append("\nNo required arguments.\n");
        for (int i = 0; i < maxArgs; i++) {
            if (i == minArgs) {
                if (maxArgs - minArgs > 0)
                    sb.append("\nOptional arguments:\n");
                else
                    sb.append("\nNo optional arguments.\n");
            }
            sb.append("  ").append(i + 1);
            spacePad(sb, maxOption);
            sb.append(type(argTypes.charAt(i)));
            addDescription(sb, Integer.toString(i + 1), maxOption + 10);
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * @param sb
     * @param key
     */
    private void addDescription(StringBuffer sb, String key, int position) {
        String desc = help.get(key);
        if (desc == null)
            return;
        String[] lines = desc.split("\n");
        for (int i = 0; i < lines.length; i++) {
            spacePad(sb, position);
            sb.append("# ").append(lines[i]);
            if (i < lines.length - 1)
                sb.append("\n");
        }
    }

    /**
     * @return
     */
    public void spacePad(StringBuffer b, int length) {
        int linelength = b.length() - b.lastIndexOf("\n");
        for (int i = 0; i < length - linelength; i++) {
            b.append(' ');
        }
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
            // return "java.io.File";
            return "filename";
        if (c == 'u')
            // return "java.net.URL";
            return "url";
        if (c == '0')
            return "(void)";
        if (c == 'b')
            return "boolean";
        if (c == 's')
            // return "java.lang.String";
            return "string";
        return "(unknown)";
    }
}
