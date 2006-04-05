/*
 * Created on 05.04.2006
 */
package org.knowceans.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

/**
 * UnHtml converts html to text, html comes either from a reader, a string or an
 * url.
 * 
 * @author gregor
 */
public class UnHtml {

    public static String getText(String html) {
        return getText(new StringReader(html));
    }

    public static String getText(URL url) {
        try {
            URLConnection conn = url.openConnection();
            return getText(new InputStreamReader(conn.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    @SuppressWarnings("serial")
    public static String getText(Reader rd) {
        final StringBuffer buf = new StringBuffer(1024);
        try {
            HTMLDocument doc = new HTMLDocument() {
                public HTMLEditorKit.ParserCallback getReader(int pos) {
                    return new HTMLEditorKit.ParserCallback() {
                        public void handleText(char[] data, int pos) {
                            buf.append(data);
                            buf.append('\n');
                        }
                    };
                }
            };

            // Parse the HTML
            EditorKit kit = new HTMLEditorKit();
            kit.read(rd, doc, 0);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        // Return the text
        return buf.toString();
    }
}
