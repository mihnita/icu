//·©·2025·and·later:·Unicode,·Inc.·and·others.
package com.ibm.icu.dev.tool.errorprone;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseMavenOutForErrorProne {
    private static final String RE_ERROR_PRONE_START =
            "^\\[WARNING\\] ((?:[A-F]:)?[\\\\/a-zA-Z0-9_.\\-]+\\.java):\\[(\\d+),(\\d+)\\]"
                + " \\[(\\S+)\\] (.+)";

    public static List<ErrorEntry> parse(String baseDir, String fileName) throws IOException {
        Pattern pattern = Pattern.compile(RE_ERROR_PRONE_START);
        List<ErrorEntry> errorReport = new ArrayList<>();
        ErrorEntry currentError = null;

        if (baseDir != null) {
            baseDir = baseDir.replace('\\', '/');
        }
        for (String line : Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8)) {
            //    	for (String line : Files.readAllLines(Paths.get(fileName),
            // StandardCharsets.UTF_8)) {
            Matcher m = pattern.matcher(line);
            if (m.find()) {
                String path = line.substring(m.start(1), m.end(1)).replace('\\', '/');
                if (baseDir != null) {
                    if (path.startsWith(baseDir)) {
                        path = path.substring(baseDir.length());
                    }
                }
                // If we already had an error report in progress, save it
                addErrorToReport(errorReport, currentError);
                currentError =
                        new ErrorEntry(
                                path,
                                Integer.parseInt(line.substring(m.start(2), m.end(2))), // line
                                Integer.parseInt(line.substring(m.start(3), m.end(3))), // column
                                line.substring(m.start(4), m.end(4)), // error code
                                line.substring(m.start(5), m.end(5)) // message
                                );
            } else if (line.startsWith("  Did you mean ")) {
                if (currentError == null) {
                    error();
                } else {
                    currentError.addExtra(line.trim());
                }
            } else if (line.endsWith(")") && line.startsWith("    (see https://")) {
                if (currentError == null) {
                    error();
                } else {
                    // 9 is the length of "    (see "
                    currentError.url = line.substring(9, line.length() - 1);
                }
            } else if (line.equals(
                    "[WARNING] Unable to autodetect 'javac' path, using 'javac' from the"
                        + " environment.")) {
                currentError = addErrorToReport(errorReport, currentError);
            } else if (line.startsWith("[INFO]")) {
                currentError = addErrorToReport(errorReport, currentError);
            } else {
                System.out.println("I DON'T KNOW WHAT THIS IS: '" + line + "'");
                currentError = addErrorToReport(errorReport, currentError);
            }
        }
        // In case we had an
        currentError = addErrorToReport(errorReport, currentError);
        return errorReport;
    }

    static ErrorEntry addErrorToReport(List<ErrorEntry> errorReport, ErrorEntry currentError) {
        if (currentError != null) {
            errorReport.add(currentError);
        }
        return null;
    }

    private static void error() {
        // TODO Auto-generated method stub
    }
}
