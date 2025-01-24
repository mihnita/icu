// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.dev.tool.docs;

import javax.lang.model.element.Element;

import com.sun.source.doctree.DocTree;

public class ICUBugTaglet extends ICUTaglet9 {
    private static ICUTaglet9 singleton;
    private static final String NAME = "bug";

    public ICUBugTaglet() {
        super(NAME, false);
    }

    public String toStringDocTree(DocTree tag, Element element) {
        return null;
    }
}
