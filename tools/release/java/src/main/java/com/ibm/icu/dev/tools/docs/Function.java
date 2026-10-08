package com.ibm.icu.dev.tools.docs;

import java.util.StringJoiner;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

class Function implements Comparable<Function> {
    public String prototype;
    public String id;
    public String status;
    public String version;
    public String file;
    public String comparableName;
    public String comparablePrototype;

    public boolean equals(Function right) {
        if (right == null) {
            return false;
        }
        if (comparablePrototype == right.comparablePrototype) {
            return true;
        }
        if (comparablePrototype == null) {
            return false;
        }
        return comparablePrototype.equals(right.comparablePrototype);
    }

    static Function fromXml(Node n) {
        Function f = new Function();
        f.prototype = getAttr(n, "prototype");

        if ("yes".equals(getAttr(n, "static")) && !f.prototype.contains("static")) {
            f.prototype = "static ".concat(f.prototype);
        }

        f.id = getAttr(n, "id");

        String status1 = getAttr(n, "status");
        String status2 = getAttr(n, "status2");
        f.status = "";
        if (status1 != null) f.status = f.status + status1;
        if (status2 != null) f.status = f.status + status2;

        String version1 = StableAPI.trimICU(getAttr(n, "version"));
        String version2 = StableAPI.trimICU(getAttr(n, "version2"));
        f.version = "";
        if (version1 != null) f.version = f.version + version1;
        if (version2 != null) f.version = f.version + version2;

        f.file = getAttr(n, "file");
        f.purifyPrototype();
        f.simplifyPrototype();

        f.comparablePrototype = f.prototype;
        // Modify the prototype here, but don't display it to the user. ( Char16Ptr -->
        // char16_t* etc )
        for (int i = 0; i < ALIAS_LIST.length; i += 2) {
            f.comparablePrototype =
                    f.comparablePrototype.replaceAll(ALIAS_LIST[i + 0], ALIAS_LIST[i + 1]);
        }

        if (f.file == null) {
            f.file = "{null}";
        } else {
            f.file = Function.getBasename(f.file);
        }
        f.comparableName = f.comparableName();
        return f;
    }


    static String getAttr(Node node, String attrName) {
        if (node.getAttributes() == null && node.getNodeType() == 3) {
            // return "(text node 3)";
            return "(Node: " + node.toString() + " )";
            // return
            // node.getFirstChild().getAttributes().getNamedItem(attrName).getNodeValue();
        }

        try {
            return node.getAttributes().getNamedItem(attrName).getNodeValue();
        } catch (NullPointerException npe) {
            if (node.getAttributes() == null) {
                throw new InternalError(
                        "[no attributes Can't get attr "
                                + attrName
                                + " out of node "
                                + node.getNodeName()
                                + ":"
                                + node.getNodeType()
                                + ":"
                                + node.getNodeValue()
                                + "@"
                                + node.getTextContent());
            } else if (node.getAttributes().getNamedItem(attrName) == null) {
                return null;
                // throw new InternalError("No attribute named: "+attrName);
            } else {
                System.err.println("Can't get attr " + attrName + ": " + npe.toString());
            }
            npe.printStackTrace();
            throw new InternalError("Can't get attr " + attrName);
        }
    }

    static String getAttr(NamedNodeMap attrList, String attrName) {
        return attrList.getNamedItem(attrName).getNodeValue();
    }

    /**
     * Convert string to basename.
     *
     * @param str
     * @return
     */
    private static String getBasename(String str) {
        int i = str.lastIndexOf("/");
        str = i == -1 ? str : str.substring(i + 1);
        return str;
    }

    private static final String REPL_LIST[] = {
        "[ ]*\\([ ]*void[ ]*\\)",
        "() ", // (void) => ()
        // No spaces preceding commas.
        "[ ]*,",
        ", ",
        // No spaces preceding '*'.
        "[ ]*\\*[ ]*",
        "* ",
        // No spaces in " = 0".
        "[ ]*=[ ]*0[ ]*$",
        "=0 ",
        // Multiple spaces collapse to single.
        "[ ]{2,}",
        " ",
        "\n",
        " "
    };

    /** these are noted as deltas. */
    private static final String SIMPLIFY_LIST[] = {
        // TODO: notify about this difference, separately
        "[ ]*=[ ]*0[ ]*$", "",
        // remove U_NOEXCEPT (this was fixed in Doxyfile, but fixing here so it is retroactive)
        "[ ]*U_NOEXCEPT", "",
        "[ ]*noexcept", "",
        // remove U_OVERRIDE and override
        "[ ]*(override|U_OVERRIDE)", "",

        // Simplify possibly-covariant functions to void*
        "^([^\\* ]+)\\*(.*)::(clone|safeClone|cloneAsThawed|freeze|createBufferClone)\\((.*)",
                "void*$2::$3($4",
        // remove trailing spaces.
        "\\s+$", "",
        // Bug in processing of uspoof.h
        "^U_NAMESPACE_END ", "",
        "\\bUBool\\b", "bool"
    };

    /**
     * This list is applied only for comparisons. The resulting string is NOT shown to the user.
     * These should be ignored as far as changes go. func(UChar) === func(char16_t)
     */
    private static final String ALIAS_LIST[] = {
        "UChar", "char16_t", "ConstChar16Ptr", "const char16_t*", "Char16Ptr", "char16_t*",
    };

    // refer to 'umachine.h'
    private static final String STATUS_LIST[] = {
        "U_CAPI",
        "U_STABLE",
        "U_DRAFT",
        "U_DEPRECATED",
        "U_OBSOLETE",
        "U_INTERNAL",
        "virtual",
        "U_EXPORT2",
        "U_I18N_API",
        "U_COMMON_API",
        "U_LIFETIME_BOUND"
    };

    /**
     * Special cases:
     *
     * <p>Remove the status attribute embedded in the C prototype
     *
     * <p>Remove the virtual keyword in Cpp prototype
     */
    private void purifyPrototype() {
        for (int i = 0; i < STATUS_LIST.length; i++) {
            String s = STATUS_LIST[i];
            prototype = prototype.replaceAll(s, "");
            prototype = prototype.trim();
        }

        for (int i = 0; i < REPL_LIST.length; i += 2) {
            prototype = prototype.replaceAll(REPL_LIST[i + 0], REPL_LIST[i + 1]);
        }

        prototype = prototype.trim();

        // Now, remove parameter names!
        StringBuffer out = new StringBuffer();
        StringBuffer in = new StringBuffer(prototype);
        int openParen = in.indexOf("(");
        int closeParen = in.lastIndexOf(")");

        if (openParen == -1 || closeParen == -1) return; // exit, malformed?
        if (openParen + 1 == closeParen) return; // exit: ()

        out.append(in, 0, openParen + 1); // prelude

        for (int left = openParen + 1; left < closeParen; ) {
            int right = in.indexOf(",", left + 1); // right edge
            if (right >= closeParen || right == -1) right = closeParen; // found last comma

            // System.err.println("Considering " + left + " / " + right + " - " + closeParen
            // + " : " + in.substring(left, right));

            if (left == right) continue;

            // find variable name
            int rightCh = right - 1;
            if (rightCh == left) { // 1 ch- break
                out.append(in, left, right);
                continue;
            }
            // eat whitespace at right
            int nameEndCh = rightCh;
            while (nameEndCh > left && Character.isWhitespace(in.charAt(nameEndCh))) {
                nameEndCh--;
            }
            int nameStartCh = nameEndCh;
            while (nameStartCh > left
                    && Character.isJavaIdentifierPart(in.charAt(nameStartCh))) {
                nameStartCh--;
            }

            // now, did we find something to skip?
            if (nameStartCh > left && nameEndCh > nameStartCh) {
                out.append(in, left, nameStartCh + 1);
            } else {
                // pass through
                out.append(in, left, right);
            }

            left = right;
        }

        out.append(in, closeParen, in.length()); // postlude

        // Delete any doubled whitespace.
        for (int p = 1; p < out.length(); p++) {
            char prev = out.charAt(p - 1);
            if (Character.isWhitespace(prev)) {
                while (out.length() > p && (Character.isWhitespace(out.charAt(p)))) {
                    out.deleteCharAt(p);
                }
                if (out.length() > p) {
                    // any trailings to delete?
                    char curr = out.charAt(p);
                    if (curr == ','
                            || curr == ')'
                            || curr == '*'
                            || curr == '&') { // delete spaces before these.
                        out.deleteCharAt(--p);
                        continue;
                    }
                }
            }
        }

        // System.err.println(prototype+" -> " + out.toString());
        prototype = out.toString();
    }

    private void simplifyPrototype() {
        if (prototype.startsWith("#define")) {
            return;
        }
        final String prototype0 = prototype;
        for (int i = 0; i < SIMPLIFY_LIST.length; i += 2) {
            prototype = prototype.replaceAll(SIMPLIFY_LIST[i + 0], SIMPLIFY_LIST[i + 1]);
        }
        if (!prototype0.equals(prototype)) {
            StableAPI.addSimplification(prototype0, prototype);
        }
    }

    /**
     * @Override
     */
    public int compareTo(Function o) {
        return comparableName.compareTo(((Function) o).comparableName);
    }

    public String comparableName() {
        return file + "|" + comparablePrototype + "|" + status + "|" + version + "|" + id;
    }

    @Override
    public String toString() {
        return comparableName;
    }

    static Function fromComparableName(String str) {
        if (str == null) {
            return null;
        }
        String[] parts = str.split("\\|", -1);
        if (parts.length < 5) {
            System.out.println(parts.length + " ::: " + str); 
            return null;
        }
        StringJoiner cp = new StringJoiner("|");
        for (int i = 1; i < parts.length - 3; i++) {
            cp.add(parts[i]);
        }
        Function result = new Function();
        result.file = parts[0];
        result.comparablePrototype = cp.toString(); 
        result.status = parts[parts.length - 3];
        result.version = parts[parts.length - 2];
        result.id = parts[parts.length - 1];
        return result;
    }
}