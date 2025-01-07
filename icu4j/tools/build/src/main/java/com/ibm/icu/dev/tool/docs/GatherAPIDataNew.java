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
import java.util.Iterator;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import javax.tools.JavaFileObject;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_9)
public class GatherAPIData extends AbstractProcessor {
    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;
    private Messager messager;
    private TreeSet<APIInfo> results;
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
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        results = new TreeSet<>(APIInfo.defaultComparator());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getRootElements()) {
            doDoc(element);
        }

        try (OutputStream os = getOutputStream()) {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            bw.write(String.valueOf(APIInfo.VERSION) + APIInfo.SEP); // header version
            bw.write(srcName + APIInfo.SEP); // source name
            bw.write((base == null ? "" : base) + APIInfo.SEP); // base
            bw.newLine();
            writeResults(results, bw);
            bw.close();
        } catch (IOException e) {
            throw new RuntimeException("write error: " + e.getMessage(), e);
        }

        return true;
    }

    private OutputStream getOutputStream() throws IOException {
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

    private void doDoc(Element element) {
        if (ignore(element)) return;

        if (element.getKind() == ElementKind.CLASS || element.getKind() == ElementKind.INTERFACE) {
            TypeElement typeElement = (TypeElement) element;
            for (Element enclosed : typeElement.getEnclosedElements()) {
                doDoc(enclosed);
            }
        }

        APIInfo info = createInfo(element);
        if (info != null) {
            results.add(info);
        }
    }

    // Sigh. Javadoc doesn't indicate when the compiler generates
    // the values and valueOf enum methods.  The position of the
    // method for these is not always the same as the position of
    // the class, though it often is, so we can't use that.

    private boolean isIgnoredEnumMethod(Element element) {
        if (element.getKind() == ElementKind.METHOD && element.getEnclosingElement().getKind() == ElementKind.ENUM) {
            String name = element.getSimpleName().toString();
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

    private boolean isAbstractClassDefaultConstructor(Element element) {
        return element.getKind() == ElementKind.CONSTRUCTOR
            && element.getEnclosingElement().getModifiers().contains(Modifier.ABSTRACT)
            && ((ExecutableElement) element).getParameters().isEmpty();
    }

    private static final boolean IGNORE_NO_ARG_ABSTRACT_CTOR = false;

    private boolean ignore(Element element) {
        if (element == null) return true;
        if (element.getModifiers().contains(Modifier.PRIVATE) || element.getModifiers().contains(Modifier.DEFAULT)) return true;
        if (element.getKind() == ElementKind.FIELD && element.getModifiers().contains(Modifier.SYNTHETIC)) return true;
        if (element.toString().contains(".misc")) {
            System.out.println("misc: " + element.toString());
            return true;
        }
        if (isIgnoredEnumMethod(element)) {
            return true;
        }

        if (IGNORE_NO_ARG_ABSTRACT_CTOR && isAbstractClassDefaultConstructor(element)) {
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
            for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
                if (annotation.getAnnotationType().toString().equals("internal")) {
                    return true;
                }
            }
        }
        if (pat != null && (element.getKind() == ElementKind.CLASS || element.getKind() == ElementKind.INTERFACE)) {
            if (!pat.matcher(element.getSimpleName().toString()).matches()) {
                return true;
            }
        }

        return false;
    }

    private static void writeResults(Collection<APIInfo> c, BufferedWriter w) {
        for (APIInfo info : c) {
            info.writeln(w);
        }
    }

    private String trimBase(String arg) {
        if (base != null) {
            for (int n = arg.indexOf(base); n != -1; n = arg.indexOf(base, n)) {
                arg = arg.substring(0, n) + arg.substring(n + base.length());
            }
        }
        return arg;
    }

    private APIInfo createInfo(Element element) {
        APIInfo info = new APIInfo();
        if (version) {
            info.includeStatusVersion(true);
        }

        // status
        String[] version = new String[1];
        info.setType(APIInfo.STA, tagStatus(element, version));
        info.setStatusVersion(version[0]);

        // visibility
        if (element.getModifiers().contains(Modifier.PUBLIC)) {
            info.setPublic();
        } else if (element.getModifiers().contains(Modifier.PROTECTED)) {
            info.setProtected();
        } else if (element.getModifiers().contains(Modifier.PRIVATE)) {
            info.setPrivate();
        } else {
            // default is package
        }

        // static
        if (element.getModifiers().contains(Modifier.STATIC)) {
            info.setStatic();
        } else {
            // default is non-static
        }

        // final
        if (element.getModifiers().contains(Modifier.FINAL) && element.getKind() != ElementKind.ENUM) {
            info.setFinal();
        } else {
            // default is non-final
        }

        // type
        if (element.getKind() == ElementKind.FIELD) {
            info.setField();
        } else if (element.getKind() == ElementKind.METHOD) {
            info.setMethod();
        } else if (element.getKind() == ElementKind.CONSTRUCTOR) {
            info.setConstructor();
        } else if (element.getKind() == ElementKind.CLASS || element.getKind() == ElementKind.INTERFACE) {
            if (element.getKind() == ElementKind.ENUM) {
                info.setEnum();
            } else {
                info.setClass();
            }
        } else if (element.getKind() == ElementKind.ENUM_CONSTANT) {
            info.setEnumConstant();
        }

        PackageElement packageElement = elementUtils.getPackageOf(element);
        info.setPackage(trimBase(packageElement.getQualifiedName().toString()));

        String className = (element.getKind() == ElementKind.CLASS || element.getKind() == ElementKind.INTERFACE || element.getEnclosingElement() == null)
                ? ""
                : element.getEnclosingElement().getSimpleName().toString();
        info.setClassName(className);

        String name = element.getSimpleName().toString();
        if (element.getKind() == ElementKind.CONSTRUCTOR) {
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

        if (element.getKind() == ElementKind.FIELD) {
            VariableElement variableElement = (VariableElement) element;
            info.setSignature(trimBase(variableElement.asType().toString()));
        } else if (element.getKind() == ElementKind.CLASS) {
            TypeElement typeElement = (TypeElement) element;

            if (typeElement.getModifiers().contains(Modifier.ABSTRACT)) {
                // interfaces are abstract by default, don't mark them as abstract
                info.setAbstract();
            }

            StringBuilder buf = new StringBuilder();
            if (typeElement.getKind() == ElementKind.CLASS) {
                buf.append("extends ");
                buf.append(typeElement.getSuperclass().toString());
            }
            List<? extends TypeMirror> interfaces = typeElement.getInterfaces();
            if (interfaces != null && !interfaces.isEmpty()) {
                if (buf.length() > 0) {
                    buf.append(" ");
                }
                buf.append("implements");
                for (TypeMirror iface : interfaces) {
                    //todo: fix comma-separator
                    //todo: if (i != 0) {
                    //todo:     buf.append(",");
                    //todo: }
                    buf.append(" ");
                    buf.append(iface.toString());
                }
            }
            info.setSignature(trimBase(buf.toString()));
        } else {
            ExecutableElement executableElement = (ExecutableElement) element;
            if (executableElement.getModifiers().contains(Modifier.SYNCHRONIZED)) {
                info.setSynchronized();
            }

            if (element.getKind() == ElementKind.METHOD) {
                if (executableElement.getModifiers().contains(Modifier.ABSTRACT)) {
                    // Workaround for Javadoc incompatibility between 7 and 8.
                    // isAbstract() returns false for a method in an interface
                    // on Javadoc 7, while Javadoc 8 returns true. Because existing
                    // API signature data files were generated before, we do not
                    // set abstract if a method is in an interface.
                    if (!((TypeElement) executableElement.getEnclosingElement()).getKind().isInterface()) {
                        info.setAbstract();
                    }
                }
                info.setSignature(trimBase(executableElement.getReturnType().toString() + executableElement.toString()));
            } else {
                // constructor
                info.setSignature(trimBase(executableElement.toString()));
            }
        }

        return info;
    }

    private int tagStatus(Element element, String[] version) {
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
                        System.err.println("bad doc: " + element + " both: "
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
                    System.err.println("warning: no tag for " + element);
                    return 0;
                } else if (res == APIInfo.STA_INTERNAL && !deprecatedFlag) {
                    System.err.println("warning: no @deprecated tag for @internal API: " + element);
                }
                return res;
            }
        }

        List<? extends AnnotationMirror> annotations = element.getAnnotationMirrors();
        Result result = new Result();
        String statusVer = "";
        for (AnnotationMirror annotation : annotations) {
            String kind = annotation.getAnnotationType().toString();
            int ix = tagKindIndex(kind);

            switch (ix) {
            case INTERNAL:
                result.set(internal ? APIInfo.STA_INTERNAL : -2); // -2 for legacy compatibility
                statusVer = getStatusVersion(annotation);
                break;

            case DRAFT:
                result.set(APIInfo.STA_DRAFT);
                statusVer = getStatusVersion(annotation);
                break;

            case STABLE:
                result.set(APIInfo.STA_STABLE);
                statusVer = getStatusVersion(annotation);
                break;

            case DEPRECATED:
                result.set(APIInfo.STA_DEPRECATED);
                statusVer = getStatusVersion(annotation);
                break;

            case OBSOLETE:
                result.set(APIInfo.STA_OBSOLETE);
                statusVer = getStatusVersion(annotation);
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

    private String getStatusVersion(AnnotationMirror annotation) {
        String text = annotation.toString();
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

    private static int tagKindIndex(String kind) {
        final String[] tagKinds = {
            "@internal", "@draft", "@stable", "@since", "@deprecated", "@author", "@see",
            "@version", "@param", "@return", "@throws", "@obsolete", "@exception", "@serial"
        };

        for (int i = 0; i < tagKinds.length; ++i) {
            if (kind.equals(tagKinds[i])) {
                return i;
            }
        }
        return UNKNOWN;
    }
}
