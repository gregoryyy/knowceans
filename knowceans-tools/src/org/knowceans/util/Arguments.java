/*
 * Created on May 26, 2005
 */
package org.knowceans.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Arguments is a convenience class that reads command line arguments for main
 * and validates them against a given format.The general grammar of a
 * commandline is:
 * <p>
 * <code>
 * commandline ::= command " " options arguments
 * </code> Here the options
 * follow the grammar: <code>
 * options   ::= ( option " " )+
 * option    ::= "-" name (" " value)?
 * name      ::= <LITERAL>  
 * arguments ::= ( argument )+ 
 * argument  ::= value
 * </code>
 * with some value. These values can be restricted to specific formats, which is
 * done using a format string
 * 
 * @author heinrich
 */
public class Arguments {

    String optionFormat = "(\\w+)(=([ifbspu0]))?";

    String types = "ifbspu";

    String argFormat = "[" + types + "]";

    /**
     * contains the option types
     */
    HashMap<String, String> optionTypes = new HashMap<String, String>();

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
        String[] commandline = "-xserv .4 -s xxx -v -words 34 -f -g 1.2 -t http://www.ud path path2 true"
            .split(" ");

        String format = "xserv=f words=i f=0 g=f s=p v=0 r=0 t=u";
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
        String type = optionTypes.get(string);
        if (obj == null && type == null) {
            throw new IllegalArgumentException("Option " + string + " unknown.");
        }
        if (type.equals("0")) {
            if (obj == null)
                obj = Boolean.FALSE;
        }
        return obj;
    }

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
     * <code>
     * foptions   ::= ( option )*
     * foption    ::= name=format
     * fname      ::= <LITERAL>
     * fotype     ::= ( i | f | b | u | p | s | 0 )
     * </code>
     * The literals of format correspond to the types int/long, float/double,
     * boolean, java.net.URL, java.io.File, java.lang.String and 0 for
     * unparametrised options
     * <p>
     * The format string for the arguments is composed of the following grammar:
     * <code>
     * farguments    ::= requiredargs "|" optionalargs
     * frequiredargs ::= ( format )+
     * foptionalargs ::= ( format )+
     * fatype        ::= ( i | f | b | u | p | s )
     * </code>
     * Note in the format specification that empty arguments are not possible.
     * 
     * @param optformat
     */
    public Arguments(String optformat, String argtypes) {
        Matcher m = Pattern.compile(optionFormat).matcher(optformat);
        while (m.find()) {
            String type = m.group(3) != null ? m.group(3) : "0";
            optionTypes.put(m.group(1), type);
        }
        minArgs = argtypes.indexOf('|');
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
                if (nargs > maxArgs) {
                    throw new IllegalArgumentException(
                        "Too many arguments. Check parameters and format.");
                }
                char type = argTypes.charAt(nargs);
                Object argument = getObject(a[i], type);
                if (argument == null) {
                    throw new IllegalArgumentException("Option " + option
                        + " has not required type " + type + ".");
                }
                arguments.add(argument);
                nargs++;
            }
            String type = optionTypes.get(option);
            if (type == null) {
                throw new IllegalArgumentException("Option " + option
                    + " unknown.");
            }
            if (nargs > 0)
                continue;
            // read the parameter (if still reading options)
            if (!type.equals("0")) {
                i++;
                Object param = getObject(a[i], type.charAt(0));
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
                obj = Double.valueOf(string);
            } else if (type == 's') {
                obj = string;
            } else if (type == 'i') {
                obj = Long.valueOf(string);
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
}
