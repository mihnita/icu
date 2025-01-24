// © 2025 and later: Unicode, Inc. and others.

package com.ibm.icu.dev.tool.docs;

import javax.lang.model.element.Element;

import com.sun.source.doctree.DocTree;

public class ICUTestTaglet extends ICUTaglet9 {
    private static ICUTaglet9 singleton;
    private static final String NAME = "test";

    public ICUTestTaglet() {
        super(NAME, false);
    }

    public String toStringDocTree(DocTree tag, Element element) {
        return null;
    }
}
