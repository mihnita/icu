//·©·2025·and·later:·Unicode,·Inc.·and·others.
package com.ibm.icu.dev.tool.errorprone;

import com.google.common.collect.ImmutableBiMap;
import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.scanner.BuiltInCheckerSuppliers;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class ErrProneReport {
    private final static String SORTTABLE_JS_FILE = "sorttable.js";
    private final static String HTML_REPORT_FILE = "errorprone.html";

    public static String guessIcuDir() throws IOException {
        URL z = ErrProneReport.class.getResource("/");
        String folder = z.getPath();
        try {
            Path path = Paths.get(z.toURI());
            // The sub-folder of the class file is icu4j/tools/build/target/classes
            // So we need to call getParent 5 times
            return path.getParent().getParent().getParent().getParent().getParent().toString();
        } catch (URISyntaxException e) {
        }
        return folder;
    }

    public static void genReport(String icuDir, String mavenStdOut, String outDir, String baseUrl) throws IOException {
//        String sss = guessIcuDir();
//        System.out.println("GUESSED: " + sss);
//        System.exit(2);

        List<ErrorEntry> errors =
                ParseMavenOutForErrorProne.parse(icuDir, mavenStdOut);
        ImmutableBiMap<String, BugCheckerInfo> knowErrors =
                BuiltInCheckerSuppliers.allChecks().getAllChecks();

        // Fix severity
        // When we enable error-prone the compiler fails on the first error-prone error.
        // So we configured error-prone to always report errors as warnings instead.
        // Here we go back to error prone and restore the proper severity level.
        for (ErrorEntry error : errors) {
            BugCheckerInfo found = knowErrors.get(error.type);
            error.severity = found == null
                    ? "unknown"
                    : found.defaultSeverity().toString().toLowerCase(Locale.US);
        }

        genReport(errors, outDir, baseUrl);
        extractJS(outDir);
    }

    private static void extractJS(String outFolder) throws IOException {
        try (InputStream is = ErrProneReport.class.getResourceAsStream(SORTTABLE_JS_FILE)) {
            Files.copy(is, Paths.get(outFolder, SORTTABLE_JS_FILE), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static void genReport(List<ErrorEntry> errors, String outDir, String baseUrl) throws IOException {
        Path outFileName = Paths.get(outDir, HTML_REPORT_FILE);
        System.out.println("Report generated: " + outFileName.toUri());
        try (PrintStream wrt = new PrintStream(outFileName.toString(), StandardCharsets.UTF_8)) {
            HtmlUtils hu = new HtmlUtils(wrt);
            hu.openTag("html");
            // head start
            hu.openTag("head");

            hu.openTag("style");
            hu.text("  body { font-family: sans-serif; }\n");
            hu.text("  td { border: 1px solid black; padding: 0 .5em; }\n");
            hu.text("  table, th { border: 2px solid black; border-collapse: collapse; }\n");
            hu.text("  th { cursor: pointer; background-color: #aaa; }\n");
            hu.text("  table.sortable tbody tr:nth-child(2n) td { background: #ddd; }\n");
            hu.text("  table.sortable tbody tr:nth-child(2n+1) td { background: #eee; }\n");

            hu.text("  .fileName { font-family: monospace; }\n");
            hu.text("  .tag, .suggestion { font-family: monospace; white-space: pre; }\n");
            hu.text("  .severity_warning { color:#aa0; }\n");
            hu.text("  .severity_error { color:#f00; }\n");
            hu.text("  .severity_unknown { color:#f0f; }\n");

            // Tinker with the table sortable
            hu.text("  th::after { content: \" \\23F6\\23F7\"; }\n");
            hu.text("  th.sorttable_sorted::after { content: \" \\23F7\"; }\n");
            hu.text("  th.sorttable_sorted_reverse::after { content: \" \\23F6\"; }\n");
            hu.text("  #sorttable_sortfwdind, #sorttable_sortrevind { display: none; }\n");
            hu.closeTag("style");

            hu.openTag("script", Map.of("src", "sorttable.js"));
            hu.closeTag("script");
            hu.closeTag("head");
            // head end

            // body start
            hu.openTag("body");

            SimpleDateFormat sdf = new SimpleDateFormat("y-MMMM-dd, HH:mm:ss", Locale.US);
            String title = "ErrorProne report, " + sdf.format(new Date());
            hu.openTag("h1").text(title).closeTag("h1");

            hu.openTag("table", Map.of("class", "sortable"));

            hu.openTag("thead");
            hu.openTag("tr");
            hu.openTag("th").text("File and line number").closeTag("th");
            hu.openTag("th").text("Severity").closeTag("th");
            hu.openTag("th").text("Issue type").closeTag("th");
            hu.openTag("th").text("Message").closeTag("th");
            //			hu.openTag("th").text("Suggestion (\"Did you mean\")").closeTag("th");
            hu.closeTag("tr");
            hu.closeTag("thead");

            for (ErrorEntry error : errors) {

                hu.openTag("tr");

                String visiblePath = error.path + ":[" + error.line + "," + error.column + "]";
                String url = baseUrl + "/"
                                + error.path
                                + "#L"
                                + error.line;
                hu.openTag("td", Map.of("class", "fileName"));
                hu.openTag("a", Map.of("href", url)).text(visiblePath).closeTag("a");
                hu.closeTag("td");

                hu.openTag("td", Map.of("class", "severity_" + error.severity))
                        .text(error.severity)
                        .closeTag("td");

                hu.openTag("td", Map.of("class", "tag"));
                Map<String, String> attr = error.url == null ? Map.of() : Map.of("href", error.url);
                hu.openTag("a", attr).text(error.type).closeTag("a");
                hu.closeTag("td");

                hu.openTag("td", Map.of("class", "desc"));
                hu.text(error.message);
                if (error.extra != null) {
                    hu.openTag("hr");
                    String extra = error.extra;
                    if (extra.startsWith("Did you mean '") && extra.endsWith("'?")) {
                        hu.indent();
                        hu.text("Did you mean ");
                        hu.openTag("br");
                        hu.indent();
                        hu.openTag("code");
                        extra = extra.substring(14, extra.length() - 2);
                        hu.text(extra);
                        hu.closeTag("code");
                    } else {
                        hu.text(extra);
                    }
                }
                hu.closeTag("td");

                hu.closeTag("tr");
            }

            hu.closeTag("table");
            hu.closeTag("body");
            // body end

            hu.closeTag("html");
        }
    }
}
