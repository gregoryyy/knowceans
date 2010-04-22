package org.knowceans.util;

import java.io.PrintStream;
import java.util.List;

/**
 * convenience class to print information
 * 
 * @author gregor
 * 
 */
public class Print {

	private static PrintStream sout = System.out;

	public static void setOutput(PrintStream s) {
		sout = s;
	}

	public static PrintStream getOutput() {
		return sout;
	}

	/**
	 * prints all objects via tostring methods
	 * 
	 * @param a
	 * @param b
	 */
	public static void strings(Object a, Object... b) {
		StringBuffer sb = new StringBuffer(a.toString());
		for (Object s : b) {
			sb.append(' ');
			sb.append(s);
		}
		System.out.println(sb);
	}

	/**
	 * prints all objects via tostring methods, separated by sep
	 * 
	 * @param a
	 * @param b
	 */
	public static void stringsep(String sep, Object... b) {
		StringBuffer sb = new StringBuffer();
		for (Object s : b) {
			sb.append(sep);
			sb.append(s);
		}
		System.out.println(sb);
	}

	/**
	 * checks whether there are arrays in the objects
	 * 
	 * @param a
	 * @param b
	 */
	public static void arrays(Object a, Object... b) {
		StringBuffer sb = new StringBuffer();
		printarray(sb, a);
		for (Object s : b) {
			sb.append(' ');
			printarray(sb, s);
		}
		sout.println(sb);
	}

	/**
	 * checks whether there are arrays in the objects
	 * 
	 * @param a
	 * @param b
	 */
	public static void arrayssep(String sep, Object... b) {
		StringBuffer sb = new StringBuffer();
		for (Object s : b) {
			sb.append(sep);
			printarray(sb, s);
		}
		sout.println(sb);
	}

	/**
	 * checks whether there are arrays in the objects
	 * 
	 * @param a
	 * @param b
	 */
	public static void arraysf(String format, Object... b) {
		StringBuffer sb = new StringBuffer();
		for (Object s : b) {
			sb.append(' ');
			printarray(sb, s, format);
		}
		sout.println(sb);
	}

	/**
	 * prints an array to sb
	 * 
	 * @param sb
	 * @param s
	 * @param format
	 */
	private static void printarray(StringBuffer sb, Object s, String format) {
		if (ArrayUtils.isArray(s)) {
			sb.append(Vectors.printf((double[]) s, format, ", "));
		} else {
			sb.append(s);
		}
	}

	/**
	 * prints the stack element of the current code location
	 */
	public static void whereami() {
		List<StackTraceElement> here = Which.fullstack();
		strings(here.get(here.size() - 1));
	}

	/**
	 * returns a string with file and line position of the current location
	 * 
	 * @return
	 */
	public static String fileline() {
		List<StackTraceElement> here = Which.fullstack();
		StackTraceElement hereami = here.get(here.size() - 1);
		return hereami.getFileName() + ":" + hereami.getLineNumber();
	}

	/**
	 * returns a string with class and method position of the current position
	 * 
	 * @return
	 */
	public static String classmethod() {
		List<StackTraceElement> here = Which.fullstack();
		StackTraceElement hereami = here.get(here.size() - 1);
		return hereami.getClassName() + "." + hereami.getMethodName();
	}

	private static void printarray(StringBuffer sb, Object s) {
		if (ArrayUtils.isArray(s)) {
			sb.append(Vectors.print(s));
		} else {
			sb.append(s);
		}
	}

}
