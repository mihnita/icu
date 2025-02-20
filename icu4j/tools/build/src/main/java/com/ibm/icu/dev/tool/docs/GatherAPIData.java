// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
 *******************************************************************************
 * Copyright (C) 2004-2014, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

/**
 * Generate a list of ICU's public APIs, sorted by qualified name and signature
 * public APIs are all non-internal, non-package apis in com.ibm.icu.[lang|math|text|util].
 * For each API, list
 * - public, package, protected, or private (PB PK PT PR)
 * - static or non-static (STK NST)
 * - final or non-final (FN NF)
 * - synchronized or non-synchronized (SYN NSY)
 * - stable, draft, deprecated, obsolete (ST DR DP OB)
 * - abstract or non-abstract (AB NA)
 * - constructor, member, field (C M F)
 *
 * Requires JDK 1.5 or later
 *
 * Sample compilation:
 * c:/doug/java/jdk1.5/build/windows-i586/bin/javac *.java
 *
 * Sample execution
 * c:/j2sdk1.5/bin/javadoc
 *   -classpath c:/jd2sk1.5/lib/tools.jar
 *   -doclet com.ibm.icu.dev.tool.docs.GatherAPIData
 *   -docletpath c:/doug/icu4j/tools/build/out/lib/icu4j-build-tools.jar
 *   -sourcepath c:/doug/icu4j/main/classes/core/src
 *   -name "ICU4J 4.2"
 *   -output icu4j42.api2
 *   -gzip
 *   -source 1.5
 *   com.ibm.icu.lang com.ibm.icu.math com.ibm.icu.text com.ibm.icu.util
 *
 * todo: provide command-line control of filters of which subclasses/packages to process
 * todo: record full inheritance hierarchy, not just immediate inheritance
 * todo: allow for aliasing comparisons (force (pkg.)*class to be treated as though it
 *       were in a different pkg/class hierarchy (facilitates comparison of icu4j and java)
 */

package com.ibm.icu.dev.tool.docs;

// standard release sdk won't work, need internal build to get access to javadoc
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.DeclaredType;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class GatherAPIData implements Doclet {
    private TreeSet<APIInfo> results = new TreeSet<>(APIInfo.defaultComparator());
    private String srcName = "Current"; // default source name
    private String output; // name of output file to write
    private String base; // strip this prefix
    private Pattern pat;
    private boolean zip;
    private boolean gzip;
    private boolean internal;
    private boolean version;

    public static int optionLength(String option) {
        if (option.equals("-name")) {
            return 2;
        } else if (option.equals("-output")) {
            return 2;
        } else if (option.equals("-base")) {
            return 2;
        } else if (option.equals("-filter")) {
            return 2;
        } else if (option.equals("-zip")) {
            return 1;
        } else if (option.equals("-gzip")) {
            return 1;
        } else if (option.equals("-internal")) {
            return 1;
        } else if (option.equals("-version")) {
            return 1;
        }
        return 0;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        System.out.println("=== SPY === getSupportedSourceVersion()");
        return SourceVersion.RELEASE_11;
    }

    @Override
    public boolean run(DocletEnvironment environment) {
        System.out.println("=== SPY === run(DocletEnvironment)");
        initFromOptions();
        eu = environment.getElementUtils();
        doDocs(environment.getIncludedElements());

        OutputStream os = System.out;
        if (output != null) {
            ZipOutputStream zos = null;
            try {
                if (zip) {
                    System.out.println("SPY OUT_FILE: " + output + ".zip");
                    zos = new ZipOutputStream(new FileOutputStream(output + ".zip"));
                    zos.putNextEntry(new ZipEntry(output));
                    os = zos;
                } else if (gzip) {
                    System.out.println("SPY OUT_FILE: " + output + ".gz");
                    os = new GZIPOutputStream(new FileOutputStream(output + ".gz"));
                } else {
                    System.out.println("SPY OUT_FILE: " + output);
                    os = new FileOutputStream(output);
                }
            }
            catch (IOException e) {
                RuntimeException re = new RuntimeException(e.getMessage());
                re.initCause(e);
                throw re;
            }
            finally {
                if (zos != null) {
                    try {
                        zos.close();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }

        BufferedWriter bw = null;
        try {
            OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
            bw = new BufferedWriter(osw);

            // writing data file
            bw.write(String.valueOf(APIInfo.VERSION) + APIInfo.SEP); // header version
            bw.write(srcName + APIInfo.SEP); // source name
            bw.write((base == null ? "" : base) + APIInfo.SEP); // base
            bw.newLine();
            writeResults(results, bw);
            bw.close(); // should flush, close all, etc
        } catch (IOException e) {
            try { bw.close(); } catch (IOException e2) {}
            RuntimeException re = new RuntimeException("write error: " + e.getMessage());
            re.initCause(e);
            throw re;
        }

        return true;
    }

    private void doDocs(Collection<? extends Element> docs) {
        if (docs != null) {
            for (Element doc : docs) {
                doDoc(doc);
            }
        }
    }

    int indent = 0;
    void indent() {
        for (int i = 0; i < indent; i++) {
            System.out.print("    ");
        }
    }

    private void doDoc(Element doc) {
//        indent();
//        System.out.println("    ==== SPY: " + eu.getPackageOf(doc) + "..." + doc);
        if (ignore(doc)) return;

        if (doc.getKind() == ElementKind.CLASS || doc.getKind() == ElementKind.INTERFACE) {
            TypeElement cdoc = (TypeElement)doc;
            indent++;
            doDocs(cdoc.getEnclosedElements());
            indent--;
            //todo doDocs(cdoc.fields());
            //todo doDocs(cdoc.constructors());
            //todo doDocs(cdoc.methods());
            //todo doDocs(cdoc.enumConstants());
            // don't call this to iterate over inner classes,
            // root.classes already includes them
            // doDocs(cdoc.innerClasses());
        }

        APIInfo info = createInfo(doc);
        if (info != null) {
            results.add(info);
        }
    }

    // Sigh. Javadoc doesn't indicate when the compiler generates
    // the values and valueOf enum methods.  The position of the
    // method for these is not always the same as the position of
    // the class, though it often is, so we can't use that.

    private boolean isIgnoredEnumMethod(Element doc) {
        if (doc.getKind() == ElementKind.METHOD && doc.getEnclosingElement().getKind() == ElementKind.ENUM) {
            // System.out.println("*** " + doc.qualifiedName() + " pos: " +
            //                    doc.position().line() +
            //                    " contained by: " +
            //                    doc.containingClass().name() +
            //                    " pos: " +
            //                    doc.containingClass().position().line());
            // return doc.position().line() == doc.containingClass().position().line();

            String name = doc.getSimpleName().toString();
            // assume we don't have enums that overload these method names.
            return "values".equals(name) || "valueOf".equals(name);
        }
        return false;
    }

    // isSynthesized also doesn't seem to work.  Let's do this, documenting
    // synthesized constructors for abstract classes is kind of weird.
    // We can't actually tell if the constructor was synthesized or is
    // actually in the docs, but this shouldn't matter.  We don't really
    // care if we didn't properly document the draft status of
    // default constructors for abstract classes.

    // Update: We mandate a no-arg synthetic constructor with explicit
    // javadoc comments by the policy. So, we no longer ignore abstract
    // class's no-arg constructor blindly. -Yoshito 2014-05-21

    private boolean isAbstractClassDefaultConstructor(Element doc) {
        return doc.getKind() == ElementKind.CONSTRUCTOR
            && doc.getEnclosingElement().getModifiers().contains(Modifier.ABSTRACT)
            && ((ExecutableElement) doc).getParameters().isEmpty();
    }

    private static final boolean IGNORE_NO_ARG_ABSTRACT_CTOR = false;

    private boolean ignore(Element doc) {
        if (doc == null) return true;
        if (doc.getModifiers().contains(Modifier.PRIVATE) || doc.getModifiers().contains(Modifier.DEFAULT)) return true;
        //todo if (doc.getKind() == ElementKind.FIELD && doc.getModifiers().contains(Modifier.SYNTHETIC)) return true;
        if (doc.getKind() == ElementKind.PACKAGE) return true;
        if (doc.getKind() == ElementKind.FIELD) return true;
        if (doc.toString().contains(".misc")) {
            System.out.println("misc: " + doc.toString()); return true;
        }
        if (isIgnoredEnumMethod(doc)) {
            return true;
        }

        if (IGNORE_NO_ARG_ABSTRACT_CTOR && isAbstractClassDefaultConstructor(doc)) {
            return true;
        }

        //todo: if (false && doc.qualifiedName().indexOf("LocaleDisplayNames") != -1) {
        //todo:   System.err.print("*** " + doc.qualifiedName() + ":");
        //todo:   if (doc.isClass()) System.err.print(" class");
        //todo:   if (doc.isConstructor()) System.err.print(" constructor");
        //todo:   if (doc.isEnum()) System.err.print(" enum");
        //todo:   if (doc.isEnumConstant()) System.err.print(" enum_constant");
        //todo:   if (doc.isError()) System.err.print(" error");
        //todo:   if (doc.isException()) System.err.print(" exception");
        //todo:   if (doc.isField()) System.err.print(" field");
        //todo:   if (doc.isInterface()) System.err.print(" interface");
        //todo:   if (doc.isMethod()) System.err.print(" method");
        //todo:   if (doc.isOrdinaryClass()) System.err.print(" ordinary_class");
        //todo:   System.err.println();
        //todo: }

        if (!internal) { // debug
            List<? extends AnnotationMirror> tags = doc.getAnnotationMirrors();
            for (AnnotationMirror tag : tags) {
                if (tagKindIndex(tag.getAnnotationType()) == INTERNAL) { return true; }
            }
        }
        if (pat != null && (doc.getKind() == ElementKind.CLASS || doc.getKind() == ElementKind.INTERFACE)) {
            if (!pat.matcher(doc.getSimpleName().toString()).matches()) {
                return true;
            }
        }
        return false;
    }

    private static void writeResults(Collection<APIInfo> c, BufferedWriter w) {
        for (APIInfo info : c) {
            //todo info.writeln(w);
            info.writelnX(w);
        }
    }

    private String trimBase(String arg) {
        if (base != null) {
            for (int n = arg.indexOf(base); n != -1; n = arg.indexOf(base, n)) {
                arg = arg.substring(0, n) + arg.substring(n+base.length());
            }
        }
        return arg;
    }

    public APIInfo createInfo(Element doc) {

        // Doc. name
        // Doc. isField, isMethod, isConstructor, isClass, isInterface
        // ProgramElementDoc. containingClass, containingPackage
        // ProgramElementDoc. isPublic, isProtected, isPrivate, isPackagePrivate
        // ProgramElementDoc. isStatic, isFinal
        // MemberDoc.isSynthetic
        // ExecutableMemberDoc isSynchronized, signature
        // Type.toString() // e.g. "String[][]"
        // ClassDoc.isAbstract, superClass, interfaces, fields, methods, constructors, innerClasses
        // FieldDoc type
        // ConstructorDoc qualifiedName
        // MethodDoc isAbstract, returnType

        APIInfo info = new APIInfo();
        if (version) {
            info.includeStatusVersion(true);
        }

        // status
        String[] version = new String[1];
        info.setType(APIInfo.STA, tagStatus(doc, version));
        info.setStatusVersion(version[0]);

        // visibility
        if (doc.getModifiers().contains(Modifier.PUBLIC)) {
            info.setPublic();
        } else if (doc.getModifiers().contains(Modifier.PROTECTED)) {
            info.setProtected();
        } else if (doc.getModifiers().contains(Modifier.PRIVATE)) {
            info.setPrivate();
        } else {
            // default is package
        }

        // static
        if (doc.getModifiers().contains(Modifier.STATIC)) {
            info.setStatic();
        } else {
            // default is non-static
        }

        // final
        if (doc.getModifiers().contains(Modifier.FINAL) && doc.getKind() != ElementKind.ENUM) {
            info.setFinal();
        } else {
            // default is non-final
        }

        // type
        if (doc.getKind() == ElementKind.FIELD) {
            info.setField();
        } else if (doc.getKind() == ElementKind.METHOD) {
            info.setMethod();
        } else if (doc.getKind() == ElementKind.CONSTRUCTOR) {
            info.setConstructor();
        } else if (doc.getKind() == ElementKind.CLASS || doc.getKind() == ElementKind.INTERFACE) {
            if (doc.getKind() == ElementKind.ENUM) {
                info.setEnum();
            } else {
                info.setClass();
            }
        } else if (doc.getKind() == ElementKind.ENUM_CONSTANT) {
            info.setEnumConstant();
        }

        PackageElement packageElement = eu.getPackageOf(doc);
        info.setPackage(trimBase(packageElement.getQualifiedName().toString()));

        String className = (doc.getKind() == ElementKind.CLASS || doc.getKind() == ElementKind.INTERFACE || doc.getEnclosingElement() == null)
                ? ""
                : doc.getEnclosingElement().getSimpleName().toString();
        info.setClassName(className);

        String name = doc.getSimpleName().toString();
        if (doc.getKind() == ElementKind.CONSTRUCTOR) {
            // Workaround for Javadoc incompatibility between 7 and 8.
            // Javadoc 7 prepends enclosing class name for a nested
            // class's constructor. We need to generate the same format
            // because existing ICU API signature were generated with
            // Javadoc 7 or older verions.
            int dotIdx = className.lastIndexOf('.');
            if (!name.contains(".") && dotIdx > 0) {
                name = className.substring(0, dotIdx + 1) + name;
            }
        }
        info.setName(name);

//        tryAll(doc);
        if (doc.getKind() == ElementKind.FIELD) {
            VariableElement fdoc = (VariableElement)doc;
            info.setSignature(trimBase(fdoc.asType().toString()));
        } else if (doc.getKind() == ElementKind.CLASS) {
            TypeElement cdoc = (TypeElement)doc;

            if (cdoc.getModifiers().contains(Modifier.ABSTRACT)) {
                // interfaces are abstract by default, don't mark them as abstract
                info.setAbstract();
            }

            StringBuffer buf = new StringBuffer();
            if (cdoc.getKind() == ElementKind.CLASS) {
                buf.append("extends ");
                buf.append(cdoc.getSuperclass().toString());
            }
            List<? extends TypeMirror> imp = cdoc.getInterfaces();
            if (imp != null && imp.size() > 0) {
                if (buf.length() > 0) {
                    buf.append(" ");
                }
                buf.append("implements");
                for (int i = 0; i < imp.size(); ++i) {
                    if (i != 0) {
                        buf.append(",");
                    }
                    buf.append(" ");
                    buf.append(imp.get(i).toString());
                }
            }
            info.setSignature(trimBase(buf.toString()));
        } else if (doc.getKind() == ElementKind.METHOD || doc.getKind() == ElementKind.CONSTRUCTOR) {
            ExecutableElement emdoc = (ExecutableElement)doc;
            if (emdoc.getModifiers().contains(Modifier.SYNCHRONIZED)) {
                info.setSynchronized();
            }

            if (doc.getKind() == ElementKind.METHOD) {
                if (emdoc.getModifiers().contains(Modifier.ABSTRACT)) {
                    // Workaround for Javadoc incompatibility between 7 and 8.
                    // isAbstract() returns false for a method in an interface
                    // on Javadoc 7, while Javadoc 8 returns true. Because existing
                    // API signature data files were generated before, we do not
                    // set abstract if a method is in an interface.
                    if (!emdoc.getEnclosingElement().getKind().isInterface()) {
                        info.setAbstract();
                    }
                }
                info.setSignature(trimBase(emdoc.getReturnType().toString() + emdoc.toString()));
            } else {
                // constructor
                info.setSignature(trimBase(emdoc.toString()));
            }
        } else {
//            System.out.println("=== SPYSPY: TO_FIX " + doc.getKind());
            info.setSignature("TO_FIX_" + doc.getKind());
        }

        return info;
    }

    private int tagStatus(final Element doc, String[] version) {
        class Result {
            boolean deprecatedFlag = false;
            int res = -1;
            void set(int val) {
                if (res != -1) {
                    boolean isValid = true;
                    if (val == APIInfo.STA_DEPRECATED) {
                        // @internal and @obsolete should be always used along with @deprecated.
                        // no change for status
                        isValid = (res == APIInfo.STA_INTERNAL || res == APIInfo.STA_OBSOLETE);
                        deprecatedFlag = true;
                    } else if (val == APIInfo.STA_INTERNAL) {
                        // @deprecated should be always used along with @internal.
                        // update status
                        if (res == APIInfo.STA_DEPRECATED) {
                            res = val;  // APIInfo.STA_INTERNAL
                        } else {
                            isValid = false;
                        }
                    } else if (val == APIInfo.STA_OBSOLETE) {
                        // @deprecated should be always used along with @obsolete.
                        // update status
                        if (res == APIInfo.STA_DEPRECATED) {
                            res = val;  // APIInfo.STA_OBSOLETE
                        } else {
                            isValid = false;
                        }
                    } else {
                        // two different status tags must not co-exist, except for
                        // following two cases:
                        // 1. @internal and @deprecated
                        // 2. @obsolete and @deprecated
                        isValid = false;
                    }
                    if (!isValid) {
                        System.err.println("bad doc: " + doc + " both: "
                                           + APIInfo.getTypeValName(APIInfo.STA, res) + " and: "
                                           + APIInfo.getTypeValName(APIInfo.STA, val));
                        return;
                    }
                } else {
                    // ok to replace with new tag
                    res = val;
                    if (val == APIInfo.STA_DEPRECATED) {
                        deprecatedFlag = true;
                    }
                }
            }
            int get() {
                if (res == -1) {
//todo                    System.err.println("warning: no tag for " + doc);
//todo                    return 0;
                } else if (res == APIInfo.STA_INTERNAL && !deprecatedFlag) {
                    System.err.println("warning: no @deprecated tag for @internal API: " + doc);
                }
                return res;
            }
        }

        List<? extends AnnotationMirror> tags = doc.getAnnotationMirrors();
        Result result = new Result();
        String statusVer = "";
        for (AnnotationMirror tag : tags) {
            String kind = tag.getAnnotationType().toString();
            int ix = tagKindIndex(tag.getAnnotationType());

            switch (ix) {
            case INTERNAL:
                result.set(internal ? APIInfo.STA_INTERNAL : -2); // -2 for legacy compatibility
                statusVer = getStatusVersion(tag);
                break;

            case DRAFT:
                result.set(APIInfo.STA_DRAFT);
                statusVer = getStatusVersion(tag);
                break;

            case STABLE:
                result.set(APIInfo.STA_STABLE);
                statusVer = getStatusVersion(tag);
                break;

            case DEPRECATED:
                result.set(APIInfo.STA_DEPRECATED);
                statusVer = getStatusVersion(tag);
                break;

            case OBSOLETE:
                result.set(APIInfo.STA_OBSOLETE);
                statusVer = getStatusVersion(tag);
                break;

            case SINCE:
            case EXCEPTION:
            case VERSION:
            case UNKNOWN:
            case AUTHOR:
            case SEE:
            case PARAM:
            case RETURN:
            case THROWS:
            case SERIAL:
                break;

            default:
                throw new RuntimeException("unknown index " + ix + " for tag: " + kind);
            }
        }

        if (version != null) {
            version[0] = statusVer;
        }
        return result.get();
    }

    private String getStatusVersion(AnnotationMirror tag) {
        String text = tag.toString();
        if (text != null && text.length() > 0) {
            // Extract version string
            int start = -1;
            int i = 0;
            for (; i < text.length(); i++) {
                char ch = text.charAt(i);
                if (ch == '.' || (ch >= '0' && ch <= '9')) {
                    if (start == -1) {
                        start = i;
                    }
                } else if (start != -1) {
                    break;
                }
            }
            if (start != -1) {
                return text.substring(start, i);
            }
        }
        return "";
    }

    private static final int UNKNOWN = -1;
    private static final int INTERNAL = 0;
    private static final int DRAFT = 1;
    private static final int STABLE = 2;
    private static final int SINCE = 3;
    private static final int DEPRECATED = 4;
    private static final int AUTHOR = 5;
    private static final int SEE = 6;
    private static final int VERSION = 7;
    private static final int PARAM = 8;
    private static final int RETURN = 9;
    private static final int THROWS = 10;
    private static final int OBSOLETE = 11;
    private static final int EXCEPTION = 12;
    private static final int SERIAL = 13;

    private static int tagKindIndex(DeclaredType declaredType) {
        final String[] tagKinds = {
            "@internal", "@draft", "@stable", "@since", "@deprecated", "@author", "@see",
            "@version", "@param", "@return", "@throws", "@obsolete", "@exception", "@serial"
        };

        for (int i = 0; i < tagKinds.length; ++i) {
            if (declaredType.toString().equals(tagKinds[i])) {
                return i;
            }
        }
        return UNKNOWN;
    }
    
    Elements eu;
    private final static Set<Option> SUPPORTED_OPTIONS = new HashSet<>();
    static {
        SUPPORTED_OPTIONS.add(new GatherApiDataOption(1, "-name", "the_name", "the description of name"));
        SUPPORTED_OPTIONS.add(new GatherApiDataOption(1, "-output", "the_output", "the description of output"));
        SUPPORTED_OPTIONS.add(new GatherApiDataOption(1, "-base", "the_base", "the description of base"));
        SUPPORTED_OPTIONS.add(new GatherApiDataOption(1, "--filter", "the_filter", "the description of filter"));
        SUPPORTED_OPTIONS.add(new GatherApiDataOption(0, "-zip", "the description of zip"));
        SUPPORTED_OPTIONS.add(new GatherApiDataOption(0, "-gzip", "the description of gzip"));
        SUPPORTED_OPTIONS.add(new GatherApiDataOption(0, "-internal", "the description of internal"));
        SUPPORTED_OPTIONS.add(new GatherApiDataOption(0, "-version", "the description of version"));
    }
    
    private void initFromOptions() {
        for (Option opt : SUPPORTED_OPTIONS) {
            GatherApiDataOption option = (GatherApiDataOption) opt;
            System.out.println("    === SPY === option(" + option.getName() + " = " + option.getStringValue(null) + "/" + option.getBooleanValue(null) + ")");
            switch (option.getName()) {
                case "-name":
                    this.srcName = option.getStringValue("defaultName");
                    break;
                case "-output":
                    this.output = option.getStringValue("defaultOutput");
                    break;
                case "-base":
                    this.base = option.getStringValue("defaultBase"); // should not include '.'
                    break;
                case "-filter":
                    String filt = option.getStringValue(null);
                    if (filt != null) {
                        this.pat = Pattern.compile(filt, Pattern.CASE_INSENSITIVE);
                    }
                    break;
                case "-zip":
                    this.zip = option.getBooleanValue(false);
                    break;
                case "-gzip":
                    this.gzip = option.getBooleanValue(false);
                    break;
                case "-internal":
                    this.internal = option.getBooleanValue(false);
                    break;
                case "-version":
                    this.version = option.getBooleanValue(false);
                    break;
            }
        }
        System.out.printf("SPY CURRENT { srcName:%s, output:%s, base:%s, pat:%s, zip:%s, gzip:%s, internal:%s, version:%s }%n",
                srcName, output, base, pat, zip, gzip, internal, version);
            
    }

    @Override
    public void init(Locale locale, Reporter reporter) {
        System.out.println("=== SPY === init(" + locale.toLanguageTag() + ")");
    }

    @Override
    public String getName() {
        System.out.println("=== SPY === getName()");
        return this.getClass().getSimpleName();
    }

    @Override
    public Set<Option> getSupportedOptions() {
        System.out.println("=== SPY === getSupportedOptions()");
        return SUPPORTED_OPTIONS;
    }

    static class GatherApiDataOption implements jdk.javadoc.doclet.Doclet.Option {
        final int length;
        final String name;
        final String paramName;
        final String description;
        String strValue = null;
        Boolean boolValue = null;

        private GatherApiDataOption(int length, String name, String description) {
            this(length, name, null, description); // no parameter
        }

        private GatherApiDataOption(int length, String name, String paramName, String description) {
            this.length = length;
            this.name = name;
            this.paramName = paramName;
            this.description = description;
        }

        @Override
        public int getArgumentCount() {
            return length;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public Option.Kind getKind() {
            return Option.Kind.STANDARD;
        }

        @Override
        public List<String> getNames() {
//            System.out.println("======== SPY: getNames()");
            return List.of(name);
        }

        @Override
        public String getParameters() {
            System.out.println("======== SPY GatherApiDataOption: getParameters()");
            return this.paramName;
        }

        @Override
        public boolean process(String option, List<String> arguments) {
            System.out.printf("======== SPY GatherApiDataOption: %s:%d process(option:'%s', arguments:%s)%n",
                    name, length,
                    option, arguments);
            if (!option.equals(name)) {
                return false;
            }
            if (length == 0) {
                boolValue = true;
                return true;
            }
            if (arguments == null || arguments.size() < 1) {
                return false;
            }
            strValue = arguments.get(0);
            return true;
        }
        
        public String getName() {
            return name;
        }

        public Boolean getBooleanValue(Boolean fallbackValue) {
            return boolValue != null ? boolValue : fallbackValue;
        }

        public String getStringValue(String fallbackValue) {
            return strValue != null ? strValue : fallbackValue;
        }
    }

    private void tryAll(Element doc) {
        System.out.print("=== SPYSPY: " + doc.getKind() + " ::: " + doc.getClass().getName() + " ::class:" + doc + " => ");
        try { ExecutableElement ee = (ExecutableElement) doc; System.out.println(" SUCCESS: ExecutableElement"); return; } catch (Exception e) {}
        try { ModuleElement me = (ModuleElement) doc; System.out.println(" SUCCESS: ModuleElement");return; } catch (Exception e) {}
        try { PackageElement pe = (PackageElement) doc; System.out.println(" SUCCESS: PackageElement");return; } catch (Exception e) {}
        try { TypeElement te = (TypeElement) doc; System.out.println(" SUCCESS: TypeElement");return; } catch (Exception e) {}
        try { VariableElement ve = (VariableElement) doc;System.out.println(" SUCCESS: VariableElement"); return; } catch (Exception e) {}
        System.out.println(" FAILURE");
    }
}
/*
[      890] === SPYSPY: CLASS ::: com.sun.tools.javac.code.Symbol$ClassSymbol => SUCCESS: TypeElement
[     1258] === SPYSPY: CONSTRUCTOR ::: com.sun.tools.javac.code.Symbol$MethodSymbol => SUCCESS: ExecutableElement
[      362] === SPYSPY: ENUM ::: com.sun.tools.javac.code.Symbol$ClassSymbol => SUCCESS: TypeElement
[      164] === SPYSPY: INTERFACE ::: com.sun.tools.javac.code.Symbol$ClassSymbol => SUCCESS: TypeElement
[     8068] === SPYSPY: METHOD ::: com.sun.tools.javac.code.Symbol$MethodSymbol => SUCCESS: ExecutableElement
[      164] === SPYSPY: TO_FIX
*/
