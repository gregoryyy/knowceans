/*
 * Created on 03.04.2007
 */
package org.knowceans.util;

import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NamedGroupRegex allows to name groups in regular expressions, similar to
 * python, but using simpler syntax:
 *
 * <pre>
 * ({name}groupstring)
 * </pre>
 *
 * and the substitution syntax
 *
 * <pre>
 * substitionst${name}ring
 * </pre>
 *
 * The following syntax can be used for back references:
 *
 * <pre>
 * ({name}groupstring) ${name}
 * </pre>
 *
 * (Because of the string in braces, the $ can be disambiguated from a line
 * ending.)
 * <p>
 * Usage: Before a pattern with named groups is used, a NamedGroupsDecoder
 * object is created for the pattern using <text>new NamedGroupsDecoder(String
 * pattern)</text>. This preprocesses the regex into a java-compliant string
 * that can be used with the Pattern class, which is accessed using <text>String
 * getJavaPattern()</text>, and an internal mapping of group names to group
 * numbers. Groups then can be accessed via the method <text>int
 * getGroup(String)</text>, or directly in the Matcher, <text>
 * m.group(ng.getNamedGroup(String))</text>. When substitutions are needed, the
 * second constructor can be used: <text>NamedGroupsDecoder(String pattern,
 * String substitution)</text>, which also creates a substitution string with
 * java-compliant backrefs. This is accessed using <text>String getJavaSub()</text>
 * and can be directly used in the methods <text>Matcher.replaceFirst/All(String
 * replacement)</text>.
 *
 * @author gregor
 */
public class NamedGroupsRegex {

    /**
     * named group string
     */
    // neg lookbehind for escapes, identify non-capturing groups (group 1)
    // and the content of any named groups (group 2)
    private static Pattern ng = Pattern
        .compile("(?<!\\\\)\\((\\?)?(?:\\{(\\w+)\\})?");

    /**
     * named group substitution string
     */
    // named group substitution
    private static Pattern ngs = Pattern.compile("(?<!\\\\)\\(\\{\\w+\\}");

    /**
     * named backref
     */
    // neg lookbehind for escapes, positive lookbehind and lookahead for braces
    private static Pattern nb = Pattern.compile("(?<!\\\\)\\$\\{(\\w+)\\}");

    /**
     * name-to-number group matching
     */
    private Hashtable<String, Integer> name2group;

    /**
     * java-compliant substitution pattern
     */
    private String javaSub;

    /**
     * java-compliant pattern string
     */
    private String javaPattern;

    public static void main(String[] args) {
        String s = "teststring with some groups.";
        // define some named groups
        String p = "({tt}tes.).+?({uu}so..)";
        String q = "tt=${tt}, uu=${uu}";

        NamedGroupsRegex named = new NamedGroupsRegex(p, q);
        // named replacement
        System.out.println(s);
        s = s.replaceAll(named.getJavaPattern(), named.getJavaSub());
        System.out.println(s);

        s = "how wow pow sow now row vow tow cow mow cow mow";
        p = "({repeat}\\w.)..${repeat}";

        named = new NamedGroupsRegex(p);
        p = named.getJavaPattern();
        System.out.println(p);
        Matcher m = Pattern.compile(p).matcher(s);
        if (m.find()) {
            System.out.println(m.group(named.getGroup("repeat")));
        }

    }

    /**
     * Creates a decoder for a regex string
     *
     * @param pattern
     */
    public NamedGroupsRegex(String pattern) {
        findNamedGroups(pattern);
    }

    /**
     * Creates a decoder for regex and replacement string
     *
     * @param pattern
     * @param replacement
     */
    public NamedGroupsRegex(String pattern, String replacement) {
        this(pattern);
        javaSub = replaceNamedBackrefs(replacement, true);
    }

    /**
     * fills the table of named groups and creates the java-compliant regex
     * string.
     *
     * @param pattern
     */
    private void findNamedGroups(String pattern) {

        name2group = new Hashtable<String, Integer>();

        Matcher m = ng.matcher(pattern);
        int groupno = 0;
        while (m.find()) {
            // group 1 is the ? for non-capturing groups
            if (m.group(1) == null) {
                // capturing group detected
                groupno++;
                // is it a named group?
                if (m.group(2) != null) {
                    name2group.put(m.group(2), groupno);
                }
            }
        }
        // make anonymous group from named group
        m = ngs.matcher(pattern);
        javaPattern = m.replaceAll("\\(");
        javaPattern = replaceNamedBackrefs(javaPattern, false);
    }

    /**
     * creates the java-compliant replacement string
     *
     * @param string
     * @param insubstitution true if in the substitution string, false if in the
     *        regex
     */
    private String replaceNamedBackrefs(String string, boolean insubstitution) {

        Matcher m = nb.matcher(string);
        StringBuffer b = new StringBuffer();
        while (m.find()) {
            String name = m.group(1);
            StringBuffer c = new StringBuffer();
            if (insubstitution) {
                c.append("\\$");
            } else {
                c.append("\\\\");
            }
            c.append(name2group.get(name));
            m.appendReplacement(b, c.toString());
        }
        m.appendTail(b);
        return b.toString();
    }

    /**
     * get java-compliant regex string
     *
     * @return
     */
    public final String getJavaPattern() {
        return javaPattern;
    }

    /**
     * get java-compliant substitution / replacement string
     *
     * @return
     */
    public final String getJavaSub() {
        return javaSub;
    }

    public final int getGroup(String name) {
        return name2group.get(name);
    }

}
