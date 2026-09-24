package com.ibm.icu.dev.tools.docs;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

class JoinedFunction implements Comparable<JoinedFunction> {
    public String prototype;
    Function left;
    Function right;

    public String comparableName;

    static JoinedFunction fromLeftFun(Function left) {
        JoinedFunction u = new JoinedFunction();
        u.prototype = left.prototype;
        u.left = left;
        u.right = null;
        u.comparableName = left.comparableName;
        return u;
    }

    static JoinedFunction fromRightFun(Function right) {
        JoinedFunction u = new JoinedFunction();
        u.prototype = right.prototype;
        u.left = null;
        u.right = right;
        u.comparableName = right.comparableName;
        return u;
    }

    static JoinedFunction fromTwoFun(Function left, Function right) {
        if (!left.equals(right)) throw new Error();
        JoinedFunction u = new JoinedFunction();
        u.prototype = left.prototype;
        u.left = left;
        u.right = right;
        u.comparableName = left.comparableName + "+" + right.comparableName;
        return u;
    }

    Element toXml(Document doc) {
        Element ele = doc.createElement("func");
        ele.setAttribute("prototype", StableAPI.formatCode(prototype));
        // ele.setAttribute("leftRefId", leftRefId);

        ele.setAttribute("leftStatus", left != null ? left.status : StableAPI.MISSING);
        // ele.setAttribute("rightRefId", rightRefId);
        ele.setAttribute("rightStatus", right != null ? right.status : StableAPI.MISSING);
        ele.setAttribute("leftVersion", left != null ? left.version : "");
        // ele.setAttribute("rightRefId", rightRefId);
        ele.setAttribute("rightVersion", right != null ? right.version : "");

        // String f = rightRefId.equals(notFound) ? leftRefId : rightRefId;
        // int tail = f.indexOf("_");
        // f = tail != -1 ? f.substring(0, tail) : f;
        // f = f.startsWith("class") ? f.replaceFirst("class","") : f;
        String f = right != null ? right.file : left.file;
        ele.setAttribute("file", f);
        return ele;
    }

    public int compareTo(JoinedFunction o) {
        return comparableName.compareTo(o.comparableName);
    }

    public boolean equals(Function right) {
        return prototype.equals(right.prototype);
    }
}