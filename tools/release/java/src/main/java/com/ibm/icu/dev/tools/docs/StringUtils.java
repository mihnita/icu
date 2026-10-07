package com.ibm.icu.dev.tools.docs;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
    // Matches both $FOO and ${FOO}. The grouping brackets will give us "FOO" in group(1)
    private final static Pattern PAT_ENV_VAR =
            Pattern.compile("\\$\\{{0,1}([a-zA-Z0-9_]+)\\}{0,1}");

    private StringUtils() { /* prevent creation */ }

    static String xmlEscape(String s) {
        if (s == null) return null;
        return s.replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;");
    }

    static File expandString(String str) {
        if (str == null) return null;
        if (str.startsWith("~")) {
            String home = System.getProperty("user.home");
            if (str.length() == 1) { // the string is "~"
                str = home;
            } else if (str.charAt(1) == '/' || str.charAt(1) == '\\') {
                str = home + str.substring(1);
            }
        }
        Matcher m = PAT_ENV_VAR.matcher(str);
        StringBuilder expanded = new StringBuilder();
        Map<String, String> env = System.getenv();
        while (m.find()) {
            String envValue = env.get(m.group(1));
            if (envValue == null) { // not set in environment, leave it as is
                envValue = m.group();
            }
            m.appendReplacement(expanded, envValue);
        }
        m.appendTail(expanded);
        return new File(expanded.toString());
    }
}
