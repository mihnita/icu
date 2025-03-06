package com.ibm.icu.dev.tool.docs;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ExecutableMemberDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.ProgramElementDoc;

public class SpyTools {
    final static private String SPY_FILE = "D:/spy.log";

    static String toSpyString(ProgramElementDoc doc) {
        StringBuilder result = new StringBuilder();
        result.append("ProgramElementDoc { toString:'" + doc.toString() + "'");
        result.append(" name:'" + doc.name() + "'");
        PackageDoc cp = doc.containingPackage();
        if (cp!= null) result.append(" pack.name:'" + cp.name() + "'");
        ClassDoc cc = doc.containingClass();
        if (cc!= null) result.append(" cc.name:'" + cc.name() + "'");
        if (doc.isStatic()) result.append(" isStatic");
        if (doc.isPrivate()) result.append(" isPrivate");
        if (doc.isProtected()) result.append(" isProtected");
        if (doc.isPublic()) result.append(" isPublic");
        if (doc.isEnumConstant()) result.append(" isEnumConstant");
        if (doc.isEnum()) result.append(" isEnum");
        if (doc.isFinal()) result.append(" isFinal");
        if (doc.isMethod()) result.append(" isMethod");
        if (doc.isField()) result.append(" isField");
        if (doc.isConstructor()) result.append(" isConstructor");
        if (doc.isClass()) result.append(" isClass");
        if (doc.isInterface()) result.append(" isInterface");

        if (doc instanceof FieldDoc) {
            FieldDoc fdoc = (FieldDoc) doc;
            result.append(" (FieldDoc).sig:'" + fdoc.type() + "'");
        } else if (doc instanceof ClassDoc) {
            ClassDoc cdoc = (ClassDoc) doc;
            result.append(" (ClassDoc).sig:'...ext...impl...'");
        } else {
            ExecutableMemberDoc emdoc = (ExecutableMemberDoc)doc;
            if (doc instanceof MethodDoc) {
                MethodDoc mdoc = (MethodDoc)doc;
                result.append(" (MethodDoc).sig:'" + mdoc.returnType() + emdoc.signature() + "'");
            } else {
                // constructor
                result.append(" (ExecutableMemberDoc).sig:'" + emdoc.signature() + "'");
            }
        }
        result.append("\n");
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

    static void logToFile(String spyString) {
        try {
            Files.write(Paths.get(SPY_FILE), spyString.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.APPEND, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
