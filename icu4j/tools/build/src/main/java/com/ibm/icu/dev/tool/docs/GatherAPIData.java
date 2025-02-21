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
import java.util.ArrayList;
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

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.util.Elements;

import com.sun.source.doctree.BlockTagTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

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

    @Override
    public SourceVersion getSupportedSourceVersion() {
        System.out.println("=== SPY === getSupportedSourceVersion()");
        // The documentation says "usually the latest version"
        // But even if at this time JDK 23 is already released, we
        // want to be able to compile / use this doclet with at least JDK 11.
        // So anything above RELEASE_11 is undefined 
        return SourceVersion.RELEASE_11;
    }

    @Override
    public boolean run(DocletEnvironment environment) {
        System.out.println("=== SPY === run(DocletEnvironment)");
        elementUtils = environment.getElementUtils();
        docTrees = environment.getDocTrees();

        initFromOptions();
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

        if (doc.getKind().isClass() || doc.getKind().isInterface()) {
            TypeElement cdoc = (TypeElement)doc;
            doDocs(cdoc.getEnclosedElements());
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
//        if (doc.getKind() == ElementKind.PACKAGE) return true;
//        if (doc.getKind() == ElementKind.FIELD) return true;
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
            List<BlockTagTree> tags = getTags(doc);
            for (BlockTagTree tag : tags) {
                if (tagKindIndex(tag) == INTERNAL) { return true; }
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
        String orgArg = arg;
        if (base != null) {
            for (int n = arg.indexOf(base); n != -1; n = arg.indexOf(base, n)) {
                arg = arg.substring(0, n) + arg.substring(n+base.length());
            }
        }
        System.out.println(  "trimBase('" + orgArg + "') = '" + arg + "'");
        return arg;
    }

    public APIInfo createInfo(Element doc) {
        System.out.println("=============");
        dumpElement(doc);

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
        // System.out.println("ai1:" + info.toStringX());
        if (version) {
            info.includeStatusVersion(true);
        }
        // System.out.println("ai2:" + info.toStringX());

        // status
        String[] version = new String[1];
        info.setType(APIInfo.STA, tagStatus(doc, version));
        info.setStatusVersion(version[0]);
        // System.out.println("ai22:" + info.toStringX());

        // visibility
        if (doc.getModifiers().contains(Modifier.PUBLIC)) {
            // System.out.println("ai30:" + info.toStringX());
            info.setPublic();
        } else if (doc.getModifiers().contains(Modifier.PROTECTED)) {
            // System.out.println("ai31:" + info.toStringX());
            info.setProtected();
        } else if (doc.getModifiers().contains(Modifier.PRIVATE)) {
            // System.out.println("ai32:" + info.toStringX());
            info.setPrivate();
        } else {
            // System.out.println("ai33:" + info.toStringX());
            // default is package
        }
        // System.out.println("ai3X:" + info.toStringX());

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
            // So for backward compatibility with older generated files
            // we use the class name instead
            name = className;
        } else if (doc.getKind().isClass() || doc.getKind().isInterface()) {
            name = withoutPackage(doc);
        }
        info.setName(name);

        // System.out.println("ai4:" + info.toStringX());
        if (doc.getKind().isField()) {
            VariableElement fdoc = (VariableElement)doc;
            // System.out.println("aiA:" + info.toStringX());
            info.setSignature(trimBase(fdoc.asType().toString()));
        } else if (doc.getKind().isClass() || doc.getKind().isInterface()) {
            // System.out.println("aiB:" + info.toStringX());
            TypeElement cdoc = (TypeElement)doc;

            if (doc.getKind().isClass() && cdoc.getModifiers().contains(Modifier.ABSTRACT)) {
                // interfaces are abstract by default, don't mark them as abstract
                info.setAbstract();
            }

            StringBuffer buf = new StringBuffer();
            if (cdoc.getKind().isClass()) {
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
            // System.out.println("aiC:" + info.toStringX());
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
                info.setSignature(trimBase(emdoc.getReturnType().toString() + emdoc.toString().substring(name.length())));
            } else {
                // constructor
                info.setSignature(trimBase(emdoc.getReturnType().toString() + emdoc.toString().substring(name.length())));
                info.setSignature(trimBase(emdoc.toString()));
            }
        } else {
            // System.out.println("aiD:" + info.toStringX());
//            System.out.println("=== SPYSPY: TO_FIX " + doc.getKind());
            info.setSignature("TO_FIX_" + doc.getKind());
        }

        System.out.println("final:" + info.toStringX());
        
        return info;
    }

    private String withoutPackage(Element enclosingElement) {
        System.out.print("SPY: withoutPackage('" + enclosingElement + "') = ");
        if (enclosingElement == null) {
            System.out.println("''");
            return "";
        }

        String result = enclosingElement.toString();

        PackageElement pack = this.elementUtils.getPackageOf(enclosingElement);
        if (pack == null) {
            System.out.println("'" + result + "'");
            return result;
        }
        // Takes something like "com.ibm.icu.charset.CharsetCallback.Decoder"
        // and removes the package, resulting in "CharsetCallback.Decoder"
        // This can't really be done just by looking at the string form.
        String packName = pack.getQualifiedName().toString() + ".";
        result = result.startsWith(packName) ? result.substring(packName.length()) : result;
        System.out.println("'" + result + "'");
        return result;
    }

    private void dumpElement(Element doc) {
        String extras = "";
        switch (doc.getKind()) {
            case CLASS:
            case ENUM:
            case INTERFACE:
            case ANNOTATION_TYPE:
                TypeElement te = (TypeElement) doc;
                extras = String.format("te.enclos:'%s' te.inter:'%s'"
                        + " te.nest:'%s' te.qual:'%s' te.simp:'%s'"
                        + " te.supr:'%s' te.typparam:'%s' ", 
                    te.getEnclosingElement(),
                    te.getInterfaces(),
                    te.getNestingKind(),
                    te.getQualifiedName(),
                    te.getSimpleName(),
                    te.getSuperclass(),
                    te.getTypeParameters());
                break;
            case CONSTRUCTOR:
            case METHOD:
                ExecutableElement ee = (ExecutableElement) doc;
                extras = String.format("ee.param:'%s' ee.rcvr:'%s'"
                        + " ee.ret:'%s' ee.simp:'%s' ee.throw:'%s'"
                        + " ee.typparam:'%s' ",
                    ee.getParameters(),
                    ee.getReceiverType(),
                    ee.getReturnType(),
                    ee.getSimpleName(),
                    ee.getThrownTypes(),    
                    ee.getTypeParameters());
                break;
        }
        System.out.printf("Element { toStr:'%s' kind:'%s' modi:'%s' sn:'%s' cls:'%s' enclos:'%s' pack:'%s' %s}%n",
                doc.toString(),
                doc.getKind(),
                doc.getModifiers(),
                doc.getSimpleName(),
                doc.getClass(),
                doc.getEnclosingElement(),
                elementUtils.getPackageOf(doc).getQualifiedName(),
                extras
        );
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

        List<BlockTagTree> tags = getTags(doc);
        // System.out.println("tags:" + tags);
        Result result = new Result();
        String statusVer = "";
        for (BlockTagTree tag : tags) {
            int ix = tagKindIndex(tag);
            // System.out.println("  tag:" + tag + " // " + ix);

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

    private static String getTagName(BlockTagTree tag) {
        return "@" + tag.getTagName();
    }

    private static String getTagText(BlockTagTree tag) {
        String name = getTagName(tag);
        String fullText = tag.toString();
        if (fullText.startsWith(name)) {
            return fullText.substring(name.length());
        }
        return fullText;
    }
    
    private static int tagKindIndex(BlockTagTree tag) {
        final String[] tagKinds = {
            "@internal", "@draft", "@stable", "@since", "@deprecated", "@author", "@see",
            "@version", "@param", "@return", "@throws", "@obsolete", "@exception", "@serial"
        };
        // System.out.println("declaredType:" + tag);
        for (int i = 0; i < tagKinds.length; ++i) {
            if (getTagName(tag).equals(tagKinds[i])) {
                return i;
            }
        }
        return UNKNOWN;
    }
    
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
        System.out.printf("SPY CURRENT { srcName:%s, output:%s, base:%s, pat:%s, zip:%s, gzip:%s, internal:%s, version:%s }%n",
                srcName, output, base, pat, zip, gzip, internal, version);
            
    }

    @Override
    public void init(Locale locale, Reporter reporter) {
        System.out.println("\n\n=== SPY === init(" + locale.toLanguageTag() + ")");
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

    private List<BlockTagTree> getTags(Element element) {
        List<BlockTagTree> result = new ArrayList<>();
        DocCommentTree dct = docTrees.getDocCommentTree(element);
        if (dct != null && dct.getBlockTags() != null) {
            for (DocTree btags : dct.getBlockTags()) {
                BlockTagTree tag = (BlockTagTree) btags;
                result.add((BlockTagTree) btags);
            }
        }
        ExecutableElement ee; // METHOD, CONSTRUCTOR ::: Element, Parameterizable
        // getEnclosingElement(), getNestingKind(), getSuperclass(), getTypeParameters()
        TypeElement te; // CLASS, INTERFACE, ENUM ::: Element, Parameterizable, QualifiedNameable
        ModuleElement me; // MODULE ::: Element, QualifiedNameable
        PackageElement pe; // PACKAGE ::: Element, QualifiedNameable
        VariableElement ve; // ::: Element
        Parameterizable pr; // ::: Element
        QualifiedNameable qn; // getQualifiedName() ::: Element
        TypeParameterElement tpe; // ::: Element

//        isInterface => INTERFACE || ANNOTATION_TYPE
//        isClass() => CLASS || ENUM
//        isField() => FIELD || ENUM_CONSTANT
//        ANNOTATION_TYPE;
//        ENUM_CONSTANT;
//        EXCEPTION_PARAMETER;
//        FIELD;
//        LOCAL_VARIABLE;
//        OTHER;
//        PACKAGE;
//        PARAMETER;
//        RESOURCE_VARIABLE;
//        STATIC_INIT; // ?
//        TYPE_PARAMETER;
        
        
        return result;
    }

}
/*
[      890] === SPYSPY: CLASS       ::: com.sun.tools.javac.code.Symbol$ClassSymbol => SUCCESS: TypeElement
[      362] === SPYSPY: ENUM        ::: com.sun.tools.javac.code.Symbol$ClassSymbol => SUCCESS: TypeElement
[      164] === SPYSPY: INTERFACE   ::: com.sun.tools.javac.code.Symbol$ClassSymbol => SUCCESS: TypeElement
[     1258] === SPYSPY: CONSTRUCTOR ::: com.sun.tools.javac.code.Symbol$MethodSymbol => SUCCESS: ExecutableElement
[     8068] === SPYSPY: METHOD      ::: com.sun.tools.javac.code.Symbol$MethodSymbol => SUCCESS: ExecutableElement
*/
