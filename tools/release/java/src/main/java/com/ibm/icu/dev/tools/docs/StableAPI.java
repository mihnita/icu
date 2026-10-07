// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 **********************************************************************
 * Copyright (C) 2016 and later: Unicode, Inc. and others.
 * Copyright (c) 2006-2013, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Created on 2006-7-24 ?
 * Moved from Java 1.4 to 1.5? API by srl 2009-01-16
 */
package com.ibm.icu.dev.tools.docs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/*
A utility to report the status change between two ICU releases

To use the utility
1. Generate the XML files
    (put the two ICU releases on your machine ^_^ )
    (generate 'Doxygen' file on Windows platform with Cygwin's help)
    Edit the generated 'Doxygen' file under ICU4C source directory
    a) GENERATE_XML           = YES
    b) Sync the ALIASES definiation
       (For example, copy the ALIASES defination from ICU 3.6
       Doxygen file to ICU 3.4 Doxygen file.)
    c) gerenate the XML files
2. Build the tool
    Download Apache Xerces Java Parser
    Build this file with the library
3. Edit the api-report-config.xml file & Change the file according your real configuration
4. Run the tool to generate the report.
*/

/**
 * CLI tool to report the status change between two ICU releases
 *
 * @author Raymond Yang
 */
public class StableAPI {

    private static final String DOC_FOLDER = "docFolder";
    private static final String INDEX_XML = "index.xml";
    private static final String ICU_SPACE_PREFIX = "ICU ";
    private static final String INITIALIZER_XPATH = "initializer";
    private static final String NAME_XPATH = "name";
    private static final String UVERSIONA = "uvernum_8h.xml";
    private static final String UVERSIONB = "uversion_8h.xml";
    private static final String U_ICU_VERSION = "U_ICU_VERSION";
    /* ICU 4.4+ */
    private static final String ICU_VERSION_XPATHA =
            "/doxygen/compounddef[@id='uvernum_8h'][@kind='file']/sectiondef[@kind='define']";
    /* ICU <4.4 */
    private static final String ICU_VERSION_XPATHB =
            "/doxygen/compounddef[@id='uversion_8h'][@kind='file']/sectiondef[@kind='define']";
    private static String ICU_VERSION_XPATH = ICU_VERSION_XPATHA;

    private static final String CXSLT = "dumpAllCFunc.xslt";
    private static final String CPPXSLT = "dumpAllCppFunc.xslt";
    private static final String RPTXSLT = "genReport.xslt";
    static final String MISSING = "(missing)";

    static final class CliArguments {
        String leftVer = null;
        File leftDir = null;
        String rightVer = null;
        File rightDir = null;
        File dumpCppXslt = null;
        File dumpCXslt = null;
        File reportXsl = null;
        File resultFile = null;
    }
    private CliArguments cliArguments;

    private String leftMilestone = "";
    private String rightMilestone = "";

    static Map<String, Set<String>> simplifications = new TreeMap<>();

    static void addSimplification(String prototype0, String prototype) {
        Set<String> s = simplifications.get(prototype);
        if (s == null) {
            s = new TreeSet<String>();
            simplifications.put(prototype, s);
        }
        s.add(prototype0);
    }

    static Set<String> getChangedSimplifications() {
        Set<String> output = new TreeSet<>();
        for (Map.Entry<String, Set<String>> e : simplifications.entrySet()) {
            if (e.getValue().size() > 1) {
                output.add(e.getKey());
            }
        }
        return output;
    }

    public static void main(String[] args)
            throws TransformerException,
                    ParserConfigurationException,
                    SAXException,
                    IOException,
                    XPathExpressionException {
        new StableAPI().run(args);
    }

    private void run(String[] args)
            throws XPathExpressionException,
                    TransformerException,
                    ParserConfigurationException,
                    SAXException,
                    IOException {
        cliArguments = parseArgs(args);

        Set<JoinedFunction> full = new TreeSet<>();

        System.err.println("Reading C++...");

        try (var dumpCppXsltStream = loadStream(CPPXSLT, "--cppxslt", cliArguments.dumpCppXslt)) {
            Set<JoinedFunction> setCpp = getFullList(dumpCppXsltStream, cliArguments.dumpCppXslt.getName());
            full.addAll(setCpp);
            System.out.println("read " + setCpp.size() + " C++.  Reading C:");
        }

        try (var dumpCXsltStream = loadStream(CXSLT, "--cxslt", cliArguments.dumpCXslt)) {
            Set<JoinedFunction> setC = getFullList(dumpCXsltStream, cliArguments.dumpCXslt.getName());
            full.addAll(setC);
            System.out.println("read " + setC.size() + " C. Setting node:");
        }

        Node fullList = setToNode(full);
        // t.dumpNode(fullList,"");

        System.out.println("Node set. Reporting:");

        reportSelectedFun(fullList);
        System.out.println("Done. Please check " + cliArguments.resultFile);

        Set<String> changedSimp = getChangedSimplifications();
        if (!changedSimp.isEmpty()) {
            System.out.println("--- changed simplifications ---");
            for (String k : changedSimp) {
                System.out.println(k);
                for (String s : simplifications.get(k)) {
                    System.out.println("\t" + s);
                }
            }
        }
    }
    
    private static File expandString(String str) {
        if (str == null) return null;
        if (str.startsWith("~")) {
            String home = System.getProperty("user.home");
            if (str.length() == 1) { // the string is "~"
                str = home;
            } else if (str.charAt(1) == '/' || str.charAt(1) == '\\') {
                str = home + str.substring(1);
            }
        }
        // Matches both $FOO and ${FOO}. Grouping will give us "FOO" in group(1)
        Pattern pat = Pattern.compile("\\$\\{{0,1}([a-zA-Z0-9_]+)\\}{0,1}");
        Matcher m = pat.matcher(str);
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

    private CliArguments parseArgs(String[] args) {
        CliArguments result = new CliArguments();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg == null || arg.length() == 0) {
                continue;
            }
            if (arg.equals("--help")) {
                printUsage();
            } else if (arg.equals("--oldver")) {
                result.leftVer = args[++i];
            } else if (arg.equals("--olddir")) {
                result.leftDir = expandString(args[++i]);
            } else if (arg.equals("--newver")) {
                result.rightVer = args[++i];
            } else if (arg.equals("--newdir")) {
                result.rightDir = expandString(args[++i]);
            } else if (arg.equals("--cxslt")) {
                result.dumpCXslt = expandString(args[++i]);
            } else if (arg.equals("--cppxslt")) {
                result.dumpCppXslt = expandString(args[++i]);
            } else if (arg.equals("--reportxslt")) {
                result.reportXsl = expandString(args[++i]);
            } else if (arg.equals("--resultfile")) {
                result.resultFile = expandString(args[++i]);
            } else {
                System.out.println("Unknown option: " + arg);
                printUsage();
            }
        }

        result.leftVer = trimICU(setVer(result.leftVer, "old", result.leftDir));
        result.rightVer = trimICU(setVer(result.rightVer, "new", result.rightDir));

        return result;
    }

    private InputStream loadStream(String name, String argName, File argFile) {
        InputStream stream = null;
        if (argFile != null) {
            try {
                stream = new FileInputStream(argFile);
                System.out.println("Loaded file " + argFile.getName());
            } catch (IOException ioe) {
                throw new RuntimeException(
                        "Error: Could not load "
                                + argName
                                + " "
                                + argFile.getPath()
                                + " - "
                                + ioe.toString(),
                        ioe);
            }
        } else {
            stream = StableAPI.class.getResourceAsStream(name);
            if (stream == null) {
                throw new InternalError(
                        "No resource found for "
                                + StableAPI.class.getPackage().getName()
                                + "/"
                                + name
                                + " -   use "
                                + argName);
            } else {
                System.out.println("Loaded resource " + name);
            }
        }
        return stream;
    }

    private static final Set<String> WARN_SET = new TreeSet<String>();

    private static void warn(String what) {
        if (!WARN_SET.contains(what)) {
            System.out.println("Warning: " + what);
            if (WARN_SET.isEmpty()) {
                System.out.println(" (These warnings are only printed one time each.)");
            }
            WARN_SET.add(what);
        }
    }

    static String trimICU(String ver) {
        Matcher icuVersionMatcher = Pattern.compile("ICU *\\d+(\\.\\d+){0,2}").matcher(ver);
        if (icuVersionMatcher.find()) {
            return icuVersionMatcher.group();
        } else {
            warn("@whatever not followed by ICU <version number>");
            return "";
        }
    }

    private String setVer(String prevVer, String whichVer, File dir) {
        String UVERSION = UVERSIONA;
        if (dir == null) {
            System.out.println("--" + whichVer + "dir not set.");
            printUsage(); /* exits */
        } else if (!dir.exists() || !dir.isDirectory()) {
            System.out.println(
                    "--" + whichVer + "dir=" + dir + " does not exist or is not a directory.");
            printUsage(); /* exits */
        }
        String result = null;
        // looking for: <name>U_ICU_VERSION</name> in uversion_8h.xml:
        // <initializer>&quot;3.8.1&quot;</initializer>
        try {
            File verFile = new File(dir, UVERSION);
            if (!verFile.exists()) {
                UVERSION = UVERSIONB;
                ICU_VERSION_XPATH = ICU_VERSION_XPATHB;
                verFile = new File(dir, UVERSION);
            } else {
                ICU_VERSION_XPATH = ICU_VERSION_XPATHA;
            }
            Document doc = XmlUtils.getDocument(verFile);
            DOMSource uvernum_h = new DOMSource(doc);
            XPath xpath = XPathFactory.newInstance().newXPath();

            Node defines =
                    (Node)
                            xpath.evaluate(
                                    ICU_VERSION_XPATH, uvernum_h.getNode(), XPathConstants.NODE);

            if (defines == null) {
                System.err.println(
                        "can't load from " + verFile.getName() + ":" + ICU_VERSION_XPATH);
            }

            NodeList nList = defines.getChildNodes();
            for (int i = 0; result == null && (i < nList.getLength()); i++) {
                Node ln = nList.item(i);
                if (!"memberdef".equals(ln.getNodeName())) {
                    continue;
                }
                Node name = (Node) xpath.evaluate(NAME_XPATH, ln, XPathConstants.NODE);
                if (name == null) continue;

                // System.err.println("Gotta node: " + name);

                Node nameVal = name.getFirstChild();
                if (nameVal == null) nameVal = name;

                String nameStr = nameVal.getNodeValue();
                if (nameStr == null) continue;

                // System.err.println("Gotta name: " + nameStr);

                if (nameStr.trim().equals(U_ICU_VERSION)) {
                    Node initializer =
                            (Node) xpath.evaluate(INITIALIZER_XPATH, ln, XPathConstants.NODE);
                    if (initializer == null) System.err.println("initializer with no value");
                    Node initVal = initializer.getFirstChild();
                    // if(initVal==null) initVal = initializer;
                    String initStr = initVal.getNodeValue().trim().replaceAll("\"", "");
                    result = ICU_SPACE_PREFIX + initStr;
                    System.err.println("Detected " + whichVer + " version: " + result);

                    String milestoneOf = "";

                    // TODO: #1 use UVersionInfo. (this tool doesn't depend on ICU4J yet)
                    // #2 move this to a utility function: strip/"explain" an ICU version #.
                    if (result.startsWith("ICU ")) {
                        String vers[] = result.substring(4).split("\\.");
                        int maj = Integer.parseInt(vers[0]);
                        int min = vers.length > 1 ? Integer.parseInt(vers[1]) : 0;
                        int micr = vers.length > 2 ? Integer.parseInt(vers[2]) : 0;
                        int patch = vers.length > 3 ? Integer.parseInt(vers[3]) : 0;
                        System.err.println(
                                " == "
                                        + Arrays.toString(vers)
                                        + " "
                                        + maj
                                        + " . "
                                        + min
                                        + " . "
                                        + micr
                                        + " . "
                                        + patch);
                        if (maj >= 49) {
                            // new scheme: 49 and following.
                            String truncVersion = "ICU " + maj;
                            if (min == 0) {
                                milestoneOf = " (m" + micr + ")";
                                System.err.println(
                                        "    .. "
                                                + milestoneOf
                                                + " is a milestone towards "
                                                + truncVersion);
                            } else if (min == 1) {
                                // Don't denote as milestone
                                result = "ICU " + (maj);
                                System.err.println(
                                        "    .. "
                                                + milestoneOf
                                                + " is the release of "
                                                + truncVersion);
                            } else {
                                milestoneOf =
                                        " (update #" + (min - 1) + ": " + result.substring(4) + ")";
                                result = "ICU " + (maj);
                                System.err.println(
                                        "    .. "
                                                + milestoneOf
                                                + " is an update to  "
                                                + truncVersion);
                            }
                            // always truncate to major # for comparing tags.
                            result = truncVersion;
                            if (maj >= 71) {
                                // Clear minor and micro version in API change report.
                                milestoneOf = "";
                            }
                        } else {
                            // old scheme - 1.0.* .. 4.8.*
                            String truncVersion = "ICU " + maj + "." + min;
                            if ((min % 2) == 1) {
                                milestoneOf = " (" + maj + "." + (min + 1) + "m" + micr + ")";
                                truncVersion = "ICU " + (maj) + "." + (min + 1);
                                System.err.println(
                                        "    .. "
                                                + milestoneOf
                                                + " is a milestone towards "
                                                + truncVersion);
                            } else if (micr == 0 && patch == 0) {
                                System.err.println(
                                        "    .. "
                                                + milestoneOf
                                                + " is the release of "
                                                + truncVersion);
                            } else {
                                milestoneOf = " (update " + micr + "." + patch + ")";
                                System.err.println(
                                        "    .. "
                                                + milestoneOf
                                                + " is an update to "
                                                + truncVersion);
                            }
                            result = truncVersion;
                        }
                        if (whichVer.equals("new")) {
                            rightMilestone = milestoneOf;
                        } else {
                            leftMilestone = milestoneOf;
                        }
                    }
                }
            }
            // dumpNode(defines,"");
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println(
                    "Warning: Couldn't get "
                            + whichVer
                            + " version from "
                            + UVERSION
                            + " - reverting to "
                            + prevVer);
            result = prevVer;
        }

        if (prevVer != null) {
            if (result != null) {
                if (!result.equals(prevVer)) {
                    System.err.println(
                            "Note: Detected "
                                    + result
                                    + " version but we'll use your requested --"
                                    + whichVer
                                    + "ver "
                                    + prevVer);
                    result = prevVer;
                    if (!rightMilestone.isEmpty() && whichVer.equals("new")) {
                        System.err.println(" .. ignoring milestone indicator " + rightMilestone);
                        rightMilestone = "";
                    }
                    if (!leftMilestone.isEmpty() && !whichVer.equals("new")) {
                        leftMilestone = "";
                    }
                } else {
                    System.err.println(
                            "Note: You don't need to use  '--"
                                    + whichVer
                                    + "ver "
                                    + result
                                    + "' anymore - we detected it correctly.");
                }
            } else {
                System.err.println(
                        "Note: Didn't detect version so we'll use your requested --"
                                + whichVer
                                + "ver "
                                + prevVer);
                result = prevVer;
                if (!rightMilestone.isEmpty() && whichVer.equals("new")) {
                    System.err.println(" .. ignoring milestone indicator " + rightMilestone);
                    rightMilestone = "";
                }
                if (!leftMilestone.isEmpty() && !whichVer.equals("new")) {
                    leftMilestone = "";
                }
            }
        }

        if (result == null) {
            System.err.println("prevVer=" + prevVer);
            System.err.println(
                    "Error: You'll need to use the option  \"--"
                            + whichVer
                            + "ver\"  because we could not detect an ICU version in "
                            + UVERSION);
            throw new InternalError(
                    "Error: You'll need to use the option  \"--"
                            + whichVer
                            + "ver\"  because we could not detect an ICU version in "
                            + UVERSION);
        }

        return result;
    }

    private static void printUsage() {
        System.out.println("Usage: StableAPI option* target*");
        System.out.println();
        System.out.println("Options:");
        System.out.println("    --help          Print this text");
        System.out.println("    --oldver        Version of old version of ICU (optional)");
        System.out.println("    --olddir        Directory that contains xml docs of old version");
        System.out.println("    --newver        Version of new version of ICU (optional)");
        System.out.println("    --newdir        Directory that contains xml docs of new version");
        System.out.println("    --cxslt         XSLT file for C docs");
        System.out.println("    --cppxslt       XSLT file for C++ docs");
        System.out.println("    --reportxslt    XSLT file for report docs");
        System.out.println("    --resultfile    Output file");
        System.exit(-1);
    }

    Transformer makeTransformer(InputStream is, String name) {
        if (is == null) {
            throw new InternalError("No inputstream set for " + name);
        }
        System.err.println("Transforming from: " + name);
        try {
            StreamSource ss = new StreamSource(is);
            ss.setSystemId(new File("."));
            Transformer t = TransformerFactory.newInstance().newTransformer(ss);
            if (t == null) {
                // Can this actually happen? If there is a failure, wouldn't it throw? 
                throw new InternalError("Couldn't make transformer for " + name);
            }
            return t;
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
            throw new InternalError(
                    "Couldn't make transformer for " + name + " - " + e.getMessageAndLocation());
        }
    }

    private void reportSelectedFun(Node joinedNode)
            throws TransformerException, ParserConfigurationException, SAXException, IOException {
        try (var reportXslStream = loadStream(RPTXSLT, "--reportxslt", cliArguments.reportXsl)) {
            Transformer report = makeTransformer(reportXslStream, RPTXSLT);
            // report.setParameter("leftStatus", leftStatus);
            report.setParameter("leftVer", cliArguments.leftVer);
            // report.setParameter("rightStatus", rightStatus);
            report.setParameter(
                    "ourYear",
                    Integer.valueOf(new Date().getYear()));
            report.setParameter("rightVer", cliArguments.rightVer);
            report.setParameter("rightMilestone", rightMilestone);
            report.setParameter("leftMilestone", leftMilestone);
            report.setParameter("dateTime", new GregorianCalendar().getTime());
            report.setParameter("notFound", MISSING);

            DOMSource src = new DOMSource(joinedNode);

            Result res = new StreamResult(cliArguments.resultFile);
            // DOMResult res = new DOMResult();
            report.transform(src, res);
            // dumpNode(res.getNode(),"");
        }
    }
    
    private Set<Function> getOneSideList(File dirName, Transformer transformer)
            throws TransformerException,
                   ParserConfigurationException,
                   SAXException,
                   IOException,
                   XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        String expression = "/list";
        DOMSource index = new DOMSource(XmlUtils.getDocument(new File(dirName, INDEX_XML)));
        DOMResult result = new DOMResult();
        transformer.setParameter(DOC_FOLDER, dirName);
        transformer.transform(index, result);

        Node list =
                (Node) xpath.evaluate(expression, result.getNode(), XPathConstants.NODE);
        if (list == null) {
            // dumpNode(xsltSource.getNode());
            XmlUtils.dumpNode(result.getNode());
            // dumpNode(leftIndex.getNode());
            System.out.flush();
            System.err.flush();
            throw new InternalError("getOneSideList() returned a null " + expression);
        }
        // dumpNode(leftList,"");
        return nodeToSet(list);
    }

    private Set<JoinedFunction> getFullList(InputStream dumpXsltStream, String dumpXsltFile)
            throws TransformerException,
                    ParserConfigurationException,
                    XPathExpressionException,
                    SAXException,
                    IOException {
        // prepare transformer
        Transformer transformer = makeTransformer(dumpXsltStream, dumpXsltFile);

        // InputSource leftSource = new InputSource(leftDir + "index.xml");
        DOMSource leftIndex = new DOMSource(XmlUtils.getDocument(new File(cliArguments.leftDir, INDEX_XML)));
        DOMResult leftResult = new DOMResult();
        transformer.setParameter(DOC_FOLDER, cliArguments.leftDir);
        transformer.transform(leftIndex, leftResult);

        Set<Function> leftSet = getOneSideList(cliArguments.leftDir, transformer);
        Set<Function> rightSet = getOneSideList(cliArguments.rightDir, transformer);
        saveReportToFile(Path.of("icu4c79_left.api3"), leftSet);
        saveReportToFile(Path.of("icu4c79_right.api3"), rightSet);

        return fullJoin(leftSet, rightSet);
    }

    private void saveReportToFile(Path filePath, Set<Function> set) throws IOException {
        try (var fr = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            for (Function func : set) {
                fr.write(func.toString());
                fr.write("\n");
            }
        }
    }
    
    /**
     * @param node
     * @return Set<Fun>
     */
    private Set<Function> nodeToSet(Node node) {
        Set<Function> s = new TreeSet<Function>();
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            s.add(Function.fromXml(n));
        }
        return s;
    }

    /**
     * @param set Set<JoinedFun>
     * @return
     * @throws ParserConfigurationException
     */
    private Node setToNode(Set<JoinedFunction> set) throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document doc = dbf.newDocumentBuilder().newDocument();
        Element root = doc.createElement("list");
        doc.appendChild(root);
        for (Iterator<JoinedFunction> iter = set.iterator(); iter.hasNext(); ) {
            JoinedFunction fun = iter.next();
            boolean leftShow = (fun.left != null && !"Internal".equals(fun.left.status));
            boolean rightShow = (fun.right != null && !"Internal".equals(fun.right.status));
            if (leftShow || rightShow) {
                root.appendChild(fun.toXml(doc));
            }
        }

        // add the 'changed' stuff
        Element root2 = doc.createElement("simplifications");
        root.appendChild(root2);
        {
            for (String simplification : getChangedSimplifications()) {
                Element subSimplification = doc.createElement("simplification");
                Element baseElement = doc.createElement("base");
                baseElement.appendChild(doc.createTextNode(simplification));
                subSimplification.appendChild(baseElement);

                root2.appendChild(subSimplification);

                for (String change : simplifications.get(simplification)) {
                    Element changeElement = doc.createElement("change");
                    changeElement.appendChild(doc.createTextNode(change));
                    subSimplification.appendChild(changeElement);
                }
            }
        }

        return doc;
    }

    /**
     * full-join two Set on 'prototype'
     *
     * @param left Set<Fun>
     * @param right Set<Fun>
     * @return Set<JoinedFun>
     */
    private static Set<JoinedFunction> fullJoin(Set<Function> left, Set<Function> right) {

        Set<JoinedFunction> joined = new TreeSet<JoinedFunction>(); // Set<JoinedFun>
        Set<Function> common = new TreeSet<Function>(); // Set<Fun>
        for (Iterator<Function> iter1 = left.iterator(); iter1.hasNext(); ) {
            Function f1 = iter1.next();
            for (Iterator<Function> iter2 = right.iterator(); iter2.hasNext(); ) {
                Function f2 = iter2.next();
                if (f1.equals(f2)) {
                    // should add left item to common set
                    // since we will remove common items with left set later
                    common.add(f1);
                    joined.add(JoinedFunction.fromTwoFun(f1, f2));
                    right.remove(f2);
                    break;
                }
            }
        }

        for (Iterator<Function> iter = common.iterator(); iter.hasNext(); ) {
            Function f = iter.next();
            left.remove(f);
        }

        for (Iterator<Function> iter = left.iterator(); iter.hasNext(); ) {
            Function f = iter.next();
            joined.add(JoinedFunction.fromLeftFun(f));
        }

        for (Iterator<Function> iter = right.iterator(); iter.hasNext(); ) {
            Function f = iter.next();
            joined.add(JoinedFunction.fromRightFun(f));
        }
        return joined;
    }


    static Formatter aFormatter = null;

    public static final String FORMAT_KEYWORDS[] = {"enum", "#define", "static"};

    /**
     * Attempt to use a pretty formatter
     *
     * @param prototype2
     * @return
     */
    public static String formatCode(String prototype2) {
        if (aFormatter == null) {
            String theFormatter = StableAPI.class.getPackage().getName() + ".CodeFormatter";
            try {
                @SuppressWarnings("unchecked")
                Class<Formatter> formatClass = (Class<Formatter>) Class.forName(theFormatter);
                aFormatter = (Formatter) formatClass.getConstructor().newInstance();
            } catch (Exception e) {
                System.err.println("Note: Couldn't load " + theFormatter);
                aFormatter = s -> {
                    String str = HTMLSafe(s.trim());
                    for (String keyword : FORMAT_KEYWORDS) {
                        if (str.startsWith(keyword)) {
                            str = str.replaceFirst(keyword, "<tt>" + keyword + "</tt>");
                        }
                    }
                    return str;
                };
            }
            if (aFormatter == null) {
                aFormatter = StableAPI::HTMLSafe;
            }
        }
        return aFormatter.formatCode(prototype2);
    }

    public static String HTMLSafe(String s) {
        if (s == null) return null;

        return s.replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;");
    }
}
