//·©·2025·and·later:·Unicode,·Inc.·and·others.
package com.ibm.icu.dev.tool.errorprone;

import java.io.PrintStream;
import java.util.Map;

class HtmlUtils {
    private final PrintStream wrt;

    HtmlUtils(PrintStream wrt) {
        this.wrt = wrt;
    }

    HtmlUtils openTag(String tag, Map<String, String> attributes, boolean standalone) {
        wrt.print("<" + tag);

        if (attributes != null) {
            for (Map.Entry<String, String> attr : attributes.entrySet()) {
                wrt.print(" " + attr.getKey() + "=\"" + escAttr(attr.getValue()) + "\"");
            }
        }

        if (standalone) {
            wrt.print(" />");
        } else {
            wrt.print(">");
        }

        return this;
    }

    HtmlUtils openTag(String tag, Map<String, String> attributes) {
        return openTag(tag, attributes, false);
    }

    HtmlUtils openTag(String tag) {
        return openTag(tag, null, false);
    }

    HtmlUtils closeTag(String tag) {
        wrt.println("</" + tag + ">");
        return this;
    }

    HtmlUtils tag(String tag) {
        return openTag(tag, null, true);
    }

    HtmlUtils text(String text, boolean escape) {
        if (escape) {
            wrt.print(escText(text));
        } else {
            wrt.print(text);
        }
        return this;
    }

    HtmlUtils text(String text) {
        return text(text, true);
    }

    HtmlUtils nl() {
        wrt.println();
        return this;
    }

    private static String escAttr(String text) {
        return text.replace("\"", "&quot;");
    }

    private static String escText(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
