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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;

import com.sun.source.doctree.BlockTagTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.util.DocTrees;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_9)
public class GatherAPIData implements Doclet {
    private Elements elementUtils;
    private DocTrees docTrees;
    private TreeSet<APIInfo> results = new TreeSet<>(APIInfo.defaultComparator());
    private String srcName = ""; // default source name
    private String output; // name of output file to write
    private String base; // strip this prefix
    private Pattern pat;
    private boolean zip;
    private boolean gzip;
    private boolean internal;
    private boolean version;
//    final static private String SPY_FOLDER = "/Users/mnita/third_party/icu_work/icu.mihnita.bld/icu4j/";

    @Override
    public SourceVersion getSupportedSourceVersion() {
        // The documentation says "usually the latest version"
        // But even if at this time JDK 23 is already released, we
        // want to be able to compile / use this doclet with at least JDK 11.
        // So anything above RELEASE_11 is undefined
        return SourceVersion.RELEASE_11;
    }

    @Override
    public void init(Locale locale, Reporter reporter) {
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public Set<Option> getSupportedOptions() {
        return SUPPORTED_OPTIONS;
    }

    @Override
    public boolean run(DocletEnvironment environment) {
        elementUtils = environment.getElementUtils();
        docTrees = environment.getDocTrees();

        initFromOptions();
        doDocs(environment.getIncludedElements());

        try (OutputStream os = getOutputFileAsStream(output);
                OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
            BufferedWriter bw = new BufferedWriter(osw);
            bw.write(String.valueOf(APIInfo.VERSION) + APIInfo.SEP); // header version
            bw.write(srcName + APIInfo.SEP); // source name
            bw.write((base == null ? "" : base) + APIInfo.SEP); // base
            bw.newLine();
            writeResults(results, bw);
            bw.close(); // should flush, close all, etc
        } catch (IOException e) {
            RuntimeException re = new RuntimeException(e.getMessage());
            re.initCause(e);
            throw re;
        }

        return true;
    }

    private OutputStream getOutputFileAsStream(String output) throws IOException {
        if (output == null) {
            return System.out;
        }
        if (zip) {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(output + ".zip"));
            zos.putNextEntry(new ZipEntry(output));
            return zos;
        }
        if (gzip) {
            return new GZIPOutputStream(new FileOutputStream(output + ".gz"));
        }
        return new FileOutputStream(output);
    }

    private void doDocs(Collection<? extends Element> docs) {
        if (docs != null) {
            for (Element doc : docs) {
                doDoc(doc);
            }
        }
    }

    private void doDoc(Element doc) {
        if (ignore(doc)) return;

        // isClass() ==> CLASS || ENUM;
        // isInterface() ==> INTERFACE || ANNOTATION_TYPE
        if (doc.getKind().isClass() || doc.getKind().isInterface()) {
            doDocs(doc.getEnclosedElements());
        }

        APIInfo info = createInfo(doc);
        if (info != null) {
            results.add(info);
        }
    }

    private boolean isIgnoredEnumMethod(Element doc) {
        if (doc.getKind() == ElementKind.METHOD && doc.getEnclosingElement().getKind() == ElementKind.ENUM) {
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
        if (doc == null) {
            return true;
        }

        if (doc.getModifiers().contains(Modifier.PRIVATE) || doc.getModifiers().contains(Modifier.DEFAULT)) {
            return true;
        }

        if (isVisibilityPackage(doc.getModifiers())) {
            return true;
        }

        if (doc.getKind() == ElementKind.PACKAGE) {
            return true;
        }

        if (doc.toString().contains(".misc")) {
            System.out.println("misc: " + doc.toString()); {
                return true;
            }
        }

        if (isIgnoredEnumMethod(doc)) {
            return true;
        }

        if (IGNORE_NO_ARG_ABSTRACT_CTOR && isAbstractClassDefaultConstructor(doc)) {
            return true;
        }

        if (!internal) { // debug
            for (BlockTagTree tag : getTags(doc)) {
                if (tagKindIndex(tag) == TagKind.INTERNAL) {
                    return true;
                }
            }
        }

        if (pat != null && (doc.getKind().isClass() || doc.getKind().isInterface())) {
            if (!pat.matcher(doc.getSimpleName().toString()).matches()) {
                return true;
            }
        }

        return false;
    }

    private boolean isVisibilityPackage(Set<Modifier> modifiers) {
        if (modifiers.contains(Modifier.PUBLIC)
                || modifiers.contains(Modifier.PROTECTED)
                || modifiers.contains(Modifier.PRIVATE)) {
            return false;
        }
        return true;
    }

    private static void writeResults(Collection<APIInfo> c, BufferedWriter w) {
        for (APIInfo info : c) {
            info.writeln(w);
        }
    }

    private String trimBase(String arg) {
        String orgArg = arg;
        if (base != null) {
            for (int n = arg.indexOf(base); n != -1; n = arg.indexOf(base, n)) {
                arg = arg.substring(0, n) + arg.substring(n+base.length());
            }
        }
        return arg;
    }

    private APIInfo createInfo(Element doc) {
        if (ignore(doc)) return null;

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
        } else if (doc.getKind().isClass() || doc.getKind().isInterface()) {
            if (doc.getKind() == ElementKind.ENUM) {
                info.setEnum();
            } else {
                info.setClass();
            }
        } else if (doc.getKind() == ElementKind.ENUM_CONSTANT) {
            info.setEnumConstant();
        }

        PackageElement packageElement = elementUtils.getPackageOf(doc);
        info.setPackage(trimBase(packageElement.getQualifiedName().toString()));

        String className = (doc.getKind().isClass() || doc.getKind().isInterface() || doc.getEnclosingElement() == null)
                ? ""
                : withoutPackage(doc.getEnclosingElement());
        info.setClassName(className);

        String name = doc.getSimpleName().toString();
        if (doc.getKind() == ElementKind.CONSTRUCTOR) {
            // The constructor name is always `<init>` with the javax.lang APIs.
            // For backward compatibility with older generated files we use the class name instead.
            name = className;
        } else if (doc.getKind().isClass() || doc.getKind().isInterface()) {
            name = withoutPackage(doc);
        }
        info.setName(name);

        if (doc.getKind().isField()) {
            VariableElement fdoc = (VariableElement) doc;
            hackSetSignature(info, trimBase(fdoc.asType().toString()));
        } else if (doc.getKind().isClass() || doc.getKind().isInterface()) {
            TypeElement cdoc = (TypeElement) doc;

            if (!doc.getKind().isInterface() && cdoc.getModifiers().contains(Modifier.ABSTRACT)) {
                // interfaces are abstract by default, don't mark them as abstract
                info.setAbstract();
            }

            StringBuffer buf = new StringBuffer();
            if (cdoc.getKind().isClass()) {
                buf.append("extends ");
                buf.append(cdoc.getSuperclass().toString());
            }
            List<? extends TypeMirror> imp = cdoc.getInterfaces();
            if (!imp.isEmpty()) {
                if (buf.length() > 0) {
                    buf.append(" ");
                }
                buf.append("implements");
                for (int i = 0; i < imp.size(); ++i) {
                    if (i != 0) {
                        buf.append(",");
                    }
                    buf.append(" ");
                    buf.append(imp.get(i).toString()
                            .replaceAll("<[^<>]+>", "") // interfaces with parameters.
                            .replaceAll("<[^<>]+>", "") // 3 nesting levels should be enough
                            .replaceAll("<[^<>]+>", "") // max I've seen was 2
                    );
                }
            }
            hackSetSignature(info, trimBase(buf.toString()));
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

                String retSig = stringFromTypeMirror(emdoc.getReturnType());

                // Signature, as returned by default, can be something like this: "boolean<T>containsAll(java.util.Iterator<T>)"
                // The old API returned "boolean(java.util.Iterator<T>)"
                // Consider using the signature "as is" (including the method name)
                hackSetSignature(info, trimBase(retSig + toTheBracket(emdoc.toString())));
            } else {
                // constructor
                hackSetSignature(info, toTheBracket(emdoc.toString()));
            }
        } else {
            // TODO: throw here
            hackSetSignature(info, "TO_FIX_" + doc.getKind());
        }

        return info;
    }

    private static String stringFromTypeMirror(TypeMirror rrt) {
        StringBuilder retSig = new StringBuilder();
        rrt.accept(new MyTypeVisitor(), retSig);
        return retSig.toString();
    }

    private void hackSetSignature(APIInfo info, String value) {
        value = value.replace(",", ", ").replace(",  ", ", ");
        info.setSignature(value);
    }

    private String withoutPackage(Element enclosingElement) {
        if (enclosingElement == null) {
            return "";
        }

        String result = enclosingElement.toString();

        PackageElement pack = this.elementUtils.getPackageOf(enclosingElement);
        if (pack == null) {
            return result;
        }

        // Takes something like "com.ibm.icu.charset.CharsetCallback.Decoder"
        // and removes the package, resulting in "CharsetCallback.Decoder"
        // This can't really be done just by looking at the string form.
        String packName = pack.getQualifiedName().toString() + ".";
        return result.startsWith(packName) ? result.substring(packName.length()) : result;
    }

    private String toTheBracket(String str) {
        if (str == null) return null;
        int openBr = str.indexOf('(');
        return openBr > 1 ? str.substring(openBr) : str;
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
                    System.err.println("warning: no tag for " + doc);
                    return 0;
                } else if (res == APIInfo.STA_INTERNAL && !deprecatedFlag) {
                    System.err.println("warning: no @deprecated tag for @internal API: " + doc);
                }
                return res;
            }
        }

        List<BlockTagTree> tags = getTags(doc);
        Result result = new Result();
        String statusVer = "";
        for (BlockTagTree tag : tags) {
            TagKind ix = tagKindIndex(tag);

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
                    throw new RuntimeException("unknown index " + ix + " for tag: " + tag);
            }
        }

        if (version != null) {
            version[0] = statusVer;
        }
        return result.get();
    }

    private String getStatusVersion(BlockTagTree tag) {
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

    static private enum TagKind {
        UNKNOWN("<unknown>"),
        INTERNAL("internal"),
        DRAFT("draft"),
        STABLE("stable"),
        SINCE("since"),
        DEPRECATED("deprecated"),
        AUTHOR("author"),
        SEE("see"),
        VERSION("version"),
        PARAM("param"),
        RETURN("return"),
        THROWS("throws"),
        OBSOLETE("obsolete"),
        EXCEPTION("exception"),
        SERIAL("serial");

        private final String value;

        TagKind(String value) {
            this.value = value;
        }

        String getValue() {
            return value;
        }
    }

    private static TagKind tagKindIndex(BlockTagTree tag) {
        for (TagKind tk : TagKind.values()) {
            if (tk.getValue().equals(tag.getTagName())) {
                return tk;
            }
        }
        return TagKind.UNKNOWN;
    }

    private final static Set<Option> SUPPORTED_OPTIONS = Set.of(
        new GatherApiDataOption(1, "-name", "the_name", "the description of name"),
        new GatherApiDataOption(1, "-output", "the_output", "the description of output"),
        new GatherApiDataOption(1, "-base", "the_base", "the description of base"),
        new GatherApiDataOption(1, "--filter", "the_filter", "the description of filter"),
        new GatherApiDataOption(0, "-zip", "the description of zip"),
        new GatherApiDataOption(0, "-gzip", "the description of gzip"),
        new GatherApiDataOption(0, "-internal", "the description of internal"),
        new GatherApiDataOption(0, "-version", "the description of version")
    );

    private void initFromOptions() {
        for (Option opt : SUPPORTED_OPTIONS) {
            GatherApiDataOption option = (GatherApiDataOption) opt;
            switch (option.getName()) {
                case "-name":
                    this.srcName = option.getStringValue("");
                    break;
                case "-output":
                    this.output = option.getStringValue(null);
                    break;
                case "-base":
                    this.base = option.getStringValue(null); // should not include '.'
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
            return List.of(name);
        }

        @Override
        public String getParameters() {
            return this.paramName;
        }

        @Override
        public boolean process(String option, List<String> arguments) {
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

    private List<BlockTagTree> getTags(Element element) {
        List<BlockTagTree> result = new ArrayList<>();
        DocCommentTree dct = docTrees.getDocCommentTree(element);
        if (dct != null && dct.getBlockTags() != null) {
            for (DocTree btags : dct.getBlockTags()) {
                BlockTagTree tag = (BlockTagTree) btags;
                result.add((BlockTagTree) btags);
            }
        }
        return result;
    }

    static class MyTypeVisitor implements TypeVisitor<StringBuilder, StringBuilder> {
        @Override
        public StringBuilder visit(TypeMirror t, StringBuilder p) {
            // TODO: throw here and in all other un-handled cases
            return p.append(":v:TypeMirror:"+t);
        }

        @Override
        public StringBuilder visitPrimitive(PrimitiveType t, StringBuilder p) {
            // Happened
            // return p.append(":v:PrimitiveType:"+t);
            return p.append(t);
        }

        @Override
        public StringBuilder visitNull(NullType t, StringBuilder p) {
            return p.append(":v:NullType:"+t);
        }

        @Override
        public StringBuilder visitArray(ArrayType t, StringBuilder p) {
            // Happened
            // return p.append(":v:ArrayType:"+t);
            return p.append(t);
        }

        @Override
        public StringBuilder visitDeclared(DeclaredType t, StringBuilder p) {
            // Happened
            // return p.append(":v:DeclaredType:"+t);
            return p.append(t);
        }

        @Override
        public StringBuilder visitError(ErrorType t, StringBuilder p) {
            return p.append(":v:ErrorType:"+t);
        }

        @Override
        public StringBuilder visitTypeVariable(TypeVariable t, StringBuilder p) {
            // Happened
            String upperBound = t.getUpperBound().toString();
            p.append(t.asElement().getSimpleName());
            if (!"java.lang.Object".equals(upperBound)) {
                return p.append(" extends ").append(upperBound);
            }
            return p;
        }

        @Override
        public StringBuilder visitWildcard(WildcardType t, StringBuilder p) {
            return p.append(":v:WildcardType:"+t);
        }

        @Override
        public StringBuilder visitExecutable(ExecutableType t, StringBuilder p) {
            return p.append(":v:ExecutableType:"+t);
        }

        @Override
        public StringBuilder visitNoType(NoType t, StringBuilder p) {
            // Happened
            // return p.append(":v:NoType:"+t);
            return p.append(t);
        }

        @Override
        public StringBuilder visitUnknown(TypeMirror t, StringBuilder p) {
            return p.append(":v:TypeMirror:"+t);
        }

        @Override
        public StringBuilder visitUnion(UnionType t, StringBuilder p) {
            return p.append(":v:UnionType:"+t);
        }

        @Override
        public StringBuilder visitIntersection(IntersectionType t, StringBuilder p) {
            return p.append(":v:IntersectionType:"+t);
        }
    }

}
