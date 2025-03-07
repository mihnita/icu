package com.ibm.icu.dev.tool.docs;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

public class SpyTools {
    final static private String SPY_FILE = "/Users/mnita/third_party/icu_work/icu.mihnita.bld/icu4j/spy.log";
    final static private String VISIT_FILE = "/Users/mnita/third_party/icu_work/icu.mihnita.bld/icu4j/visit.log";
//    final static private String SPY_FILE = "./spy.log";

    static String toSpyStringStock(Elements elementUtils, Element doc) {
        StringWriter result = new StringWriter();
        elementUtils.printElements(result, doc);
        result.append("\n");
        return result.toString();
    }

    static String toSpyString(Elements elementUtils, Element doc) {
        StringBuilder result = new StringBuilder();
        result.append("Element { toString:'" + doc.toString() + "'");
        result.append(" name:'" + doc.getSimpleName() + "'"); // name
        PackageElement cp = elementUtils.getPackageOf(doc);
        if (cp!= null) result.append(" pack.name:'" + cp.getQualifiedName() + "'");
        // ClassDoc cc = doc.containingClass();
        Element cc = doc.getEnclosingElement();
        if (cc!= null) result.append(" cc.name:'" + cc.asType().toString() + "'");
        Set<Modifier> modif = doc.getModifiers();
        if (modif.contains(Modifier.STATIC)) result.append(" isStatic");
        if (modif.contains(Modifier.PRIVATE)) result.append(" isPrivate");
        if (modif.contains(Modifier.PROTECTED)) result.append(" isProtected");
        if (modif.contains(Modifier.PUBLIC)) result.append(" isPublic");
        if (modif.contains(Modifier.FINAL)) result.append(" isFinal");
        ElementKind kind = doc.getKind();
        switch (kind) {
            case CLASS:
                result.append(" isClass");
                break;
            case CONSTRUCTOR:
                result.append(" isConstructor");
                break;
            case ENUM:
                result.append(" isEnum");
                break;
            case ENUM_CONSTANT:
                result.append(" isEnumConstant");
                break;
            case FIELD:
                result.append(" isField");
                break;
            case INTERFACE:
                result.append(" isInterface");
                break;
            case METHOD:
                result.append(" isMethod");
                break;
            default:
                result.append(" isUnknown." + kind);
                break;
        }

//        result.append(" (" + doc.getClass() + ").sig:'TODO'");

        if (doc instanceof ExecutableElement) { // MethodSymbol [830]
            ExecutableElement ee = (ExecutableElement) doc;
            result.append(" (ExecutableElement).sig:'" + ee.getReturnType() + "::" + ee.getParameters() + "'");
        } else if (doc instanceof ModuleElement) {
            result.append(" (ModuleElement).sig:'TODO'");
        } else if (doc instanceof PackageElement) { // PackageSymbol [16]
            PackageElement pe = (PackageElement) doc;
            result.append(" (PackageElement).sig:'" + pe.getQualifiedName() + "'");
        } else if (doc instanceof Parameterizable) { // ClassSymbol [90]
            Parameterizable cs = (Parameterizable) doc;
            result.append(" (Parameterizable).sig:'" + cs + "'");
        } else if (doc instanceof QualifiedNameable) {
            result.append(" (QualifiedNameable).sig:'TODO'");
        } else if (doc instanceof TypeElement) {
            result.append(" (TypeElement).sig:'TODO'");
        } else if (doc instanceof TypeParameterElement) {
            result.append(" (TypeParameterElement).sig:'TODO'");
        } else if (doc instanceof VariableElement) { // VarSymbol [309]
            VariableElement ve = (VariableElement) doc;
            result.append(" (VariableElement).sig:'" + ve.getSimpleName() + "'");
        }

//        if (doc instanceof VariableElement) { // Field
//            VariableElement fdoc = (VariableElement) doc;
//            result.append(" (FieldDoc).sig:'" + fdoc.toString() + "'");
//        } else if (doc instanceof ClassDoc) {
//            ClassDoc cdoc = (ClassDoc) doc;
//            result.append(" (ClassDoc).sig:'...ext...impl...'");
//        } else {
//            // ExecutableMemberDoc emdoc = (ExecutableMemberDoc)doc;
//            ExecutableElement emdoc = (emdoc) doc;
//            if (doc instanceof MethodDoc) {
//                MethodDoc mdoc = (MethodDoc)doc;
//                result.append(" (MethodDoc).sig:'" + mdoc.returnType() + emdoc.signature() + "'");
//            } else {
//                // constructor
//                result.append(" (ExecutableMemberDoc).sig:'" + emdoc.signature() + "'");
//            }
//        }
        result.append(" }\n");
        return result.toString();
    }

    static String toSpyString(APIInfo ai, boolean brief) { // Same as APIInfo.writeln
        StringWriter w = new StringWriter();
        try {
            for (int i = 0; i < APIInfo.NUM_TYPES; ++i) {
                String s = ai.get(i, /*brief*/ brief);
                if (s != null) {
                    w.write(s);
                }
                if (/*ai.includeStatusVer &&*/ i == APIInfo.STA) {
                    String ver = ai.getStatusVersion();
                    if (ver.length() > 0) {
                        w.write("@");
                        w.write(ai.getStatusVersion());
                    }
                }
                w.write(APIInfo.SEP);
            }
            w.write("\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return w.toString();
    }

    static String toSpyStringJson(APIInfo ai) { // Same as APIInfo.writeln
        StringBuilder result = new StringBuilder();
        result.append("APIInfo {");
        String s;
        s = ai.get(APIInfo.STA, /*brief*/ false);
        if (s != null && !s.isEmpty()) result.append(" status:'" + s + "'");
        s = ai.getStatusVersion();
        if (s != null && !s.isEmpty()) result.append(" statusVer:'" + s + "'");
        s = ai.get(APIInfo.VIS, /*brief*/ false);
        if (s != null && !s.isEmpty()) result.append(" visibility:'" + s + "'");
        s = ai.get(APIInfo.STK, /*brief*/ false);
        if (s != null && !s.isEmpty()) result.append(" static:'" + s + "'");
        s = ai.get(APIInfo.FIN, /*brief*/ false);
        if (s != null && !s.isEmpty()) result.append(" final:'" + s + "'");
        s = ai.get(APIInfo.SYN, /*brief*/ false);
        if (s != null && !s.isEmpty()) result.append(" syncronized:'" + s + "'");
        s = ai.get(APIInfo.ABS, /*brief*/ false);
        if (s != null && !s.isEmpty()) result.append(" abstract:'" + s + "'");
        s = ai.get(APIInfo.CAT, /*brief*/ false);
        if (s != null && !s.isEmpty()) result.append(" categ:'" + s + "'");
        s = ai.get(APIInfo.PAK, /*brief*/ false);
        if (s != null && !s.isEmpty()) result.append(" package:'" + s + "'");
        s = ai.get(APIInfo.CLS, /*brief*/ false);
        if (s != null && !s.isEmpty()) result.append(" className:'" + s + "'");
        s = ai.get(APIInfo.NAM, /*brief*/ false);
        if (s != null && !s.isEmpty()) result.append(" name:'" + s + "'");
        s = ai.get(APIInfo.SIG, /*brief*/ false);
        if (s != null && !s.isEmpty()) result.append(" signature:'" + s + "'");
        s = ai.get(APIInfo.EXC, /*brief*/ false);
        if (s != null && !s.isEmpty()) result.append(" exceptions:'" + s + "'");
        result.append("}\n");
        return result.toString();
    }

    static void logToFile(String fileName, String spyString) {
        try {
            Files.write(Paths.get(fileName), spyString.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.SYNC, StandardOpenOption.APPEND,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void logToFile(String spyString) {
        logToFile(SPY_FILE, spyString);
    }

    public static void doTheVisit(Element doc, String indent) {
        MyElementVisitor ev = new MyElementVisitor();
        doc.accept(ev, indent);

        indent = indent + "    ";
        for (Element e: doc.getEnclosedElements()) {
            doTheVisit(e, indent);
        }
    }

    private static class MyElementVisitor implements ElementVisitor<Element, String> {
        @Override
        public Element visit(Element e, String indent) {
            // Visits an element.
            SpyTools.logToFile(VISIT_FILE, indent + "visit.visit: " + e + " ::: " + e.getKind() + "\n");
            return e;
        }

        @Override
        public Element visitPackage(PackageElement e, String indent) {
            // Represents a package program element.
            // Provides access to information about the package and its members.
            SpyTools.logToFile(VISIT_FILE, indent + "visit.Package: " + e + " ::: " + e.getKind() + "\n");
            return e;
        }

        @Override
        public Element visitType(TypeElement e, String indent) {
            // Represents a class or interface program element.
            // Provides access to information about the type and its members.
            // Note that an enum type is a kind of class and an annotation type
            // is a kind of interface.
            SpyTools.logToFile(VISIT_FILE, indent + "visit.Type: " + e + " ::: " + e.getKind() + "\n");
            return e;
        }

        @Override
        public Element visitVariable(VariableElement e, String indent) {
            // Represents a field, enum constant, method or constructor parameter,
            // local variable, resource variable, or exception parameter.
            SpyTools.logToFile(VISIT_FILE, indent + "visit.Variable: " + e + " ::: " + e.getKind() + "\n");
            return e;
        }

        @Override
        public Element visitExecutable(ExecutableElement e, String indent) {
            // Represents a method, constructor, or initializer (static or instance)
            // of a class or interface, including annotation type elements.
            SpyTools.logToFile(VISIT_FILE, indent + "visit.Executable: " + e + " ::: " + e.getKind() + "\n");
            return e;
        }

        @Override
        public Element visitTypeParameter(TypeParameterElement e, String indent) {
            // Represents a formal type parameter of a generic class, interface, method,
            // or constructor element. A type parameter declares a TypeVariable.
            SpyTools.logToFile(VISIT_FILE, indent + "visit.TypeParameter: " + e + " ::: " + e.getKind() + "\n");
            return e;
        }

        @Override
        public Element visitUnknown(Element e, String indent) {
            // Visits an unknown kind of element.
            // This can occur if the language evolves and new kinds of elements
            // are added to the Element hierarchy.
            SpyTools.logToFile(VISIT_FILE, indent + "visit.Unknown: " + e + " ::: " + e.getKind() + "\n");
            return e;
        }
    }
}

/*
What a tree navigation (without any filtering) finds:
[     1337] ::: CLASS
[     2314] ::: CONSTRUCTOR
[      294] ::: ENUM
[     1622] ::: ENUM_CONSTANT
[    13240] ::: FIELD
[      147] ::: INTERFACE
[    12172] ::: METHOD
[        7] ::: PACKAGE
    - com.ibm.icu.util
    - com.ibm.icu.math
    - com.ibm.icu.number
    - com.ibm.icu.lang
    - com.ibm.icu.message2
    - com.ibm.icu.text
    - com.ibm.icu.charset 
*/
