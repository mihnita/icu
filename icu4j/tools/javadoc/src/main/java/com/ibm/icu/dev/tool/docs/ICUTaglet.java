// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
*******************************************************************************
* Copyright (C) 2002-2016 International Business Machines Corporation   *
* and others. All Rights Reserved.                                            *
*******************************************************************************
*/

package com.ibm.icu.dev.tool.docs;

import java.text.BreakIterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.doctree.UnknownInlineTagTree;
import com.sun.source.util.SimpleDocTreeVisitor;
import jdk.javadoc.doclet.Taglet;

public abstract class ICUTaglet implements Taglet {
    private static String ICU_LABEL = "<strong><font color=red>[icu]</font></strong>";
    private static final String STATUS = "<dt><b>Status:</b></dt>";

	protected final String name;
    protected final boolean isInline;

    public static void register(Map<String, Taglet> taglets) {
        ICUInternalTaglet.register(taglets);
        ICUDraftTaglet.register(taglets);
        ICUStableTaglet.register(taglets);
        ICUProvisionalTaglet.register(taglets);
        ICUObsoleteTaglet.register(taglets);
        ICUIgnoreTaglet.register(taglets);
        ICUNewTaglet.register(taglets);
        ICUNoteTaglet.register(taglets);
        ICUEnhancedTaglet.register(taglets);
        ICUDiscouragedTaglet.register(taglets);
    }

    protected ICUTaglet(String name, boolean isInline) {
        this.name = name;
        this.isInline = isInline;
    }

    @Override
	public boolean isInlineTag() {
        return isInline;
    }

    @Override
	public String getName() {
        return name;
    }

	@Override
	public Set<Location> getAllowedLocations() {
		return Set.of(
				Location.CONSTRUCTOR, // In the documentation for a constructor.
				Location.FIELD, // In the documentation for a field.
				Location.METHOD, // In the documentation for a method.
				Location.MODULE, // In the documentation for a module.
				Location.OVERVIEW, // In an Overview document.
				Location.PACKAGE, // In the documentation for a package.
				Location.TYPE // In the documentation for a class, interface or enum.
		);
	}

    @Override
	public String toString(List<? extends DocTree> tags, Element element) {
    	int tagsLength = tags.size();
    	if (!isInlineTag() && tags != null) {
            if (tagsLength > 1) {
                String msg = "Should not have more than one ICU tag per element:\n";
                for (int i = 0; i < tagsLength; ++i) {
                    msg += "  [" + i + "] " + tags.get(i) + "\n";
                }
                throw new IllegalStateException(msg);
            } else if (tagsLength > 0) {
                return toString(getText(tags.get(0)), element);
            }
        }
        return null;
    }

    protected abstract String toString(String text, Element element);

    private final static SimpleDocTreeVisitor<String, Void> VISITOR = new SimpleDocTreeVisitor<>() {
        @Override
        public String visitUnknownBlockTag(UnknownBlockTagTree node, Void p) {
            for (DocTree dt : node.getContent()) {
                return dt.accept(this, null);
            }
            return "";
        }

        @Override
        public String visitUnknownInlineTag(UnknownInlineTagTree node, Void p) {
            for (DocTree dt : node.getContent()) {
                return dt.accept(this, null);
            }
            return "";
        }

        @Override
        public String visitText(TextTree node, Void p) {
            return node.getBody();
        }

        @Override
        protected String defaultAction(DocTree node, Void p) {
            return "";
        }
    };
    
    static String getText(DocTree doc) {
    	return doc.accept(VISITOR, null).trim();
    }

    public static class ICUDiscouragedTaglet extends ICUTaglet {
        private static final String NAME = "discouraged";

        public static void register(Map<String, Taglet> taglets) {
            taglets.put(NAME, new ICUDiscouragedTaglet());
        }

        private ICUDiscouragedTaglet() {
            super(NAME, false);
        }

        @Override
		public String toString(String text, Element element) {
            if (text.length() == 0) {
                System.err.println("Error: empty discouraged tag ");
            } 
            return "<dt><b><font color=red>Discouraged:</font></b></dt><dd>" + text + "</dd>";
        }
    }

    public static class ICUInternalTaglet extends ICUTaglet {
        private static final String NAME = "internal";

        public static void register(Map<String, Taglet> taglets) {
            taglets.put(NAME, new ICUInternalTaglet());
        }

        private ICUInternalTaglet() {
            super(NAME, false);
        }

        @Override
		public String toString(String text, Element element) {
            if (text.toLowerCase(Locale.US).indexOf("technology preview") >= 0) {
                return STATUS + "<dd><em>Technology Preview</em>. <font color='red'>" +
                    "This API is still in the early stages of development. Use at your own risk.</font></dd>";
            }
            return STATUS + "<dd><em>Internal</em>. <font color='red'>" +
                "This API is <em>ICU internal only</em>.</font></dd>";
        }
    }

    public static class ICUDraftTaglet extends ICUTaglet {
        private static final String NAME = "draft";

        public static void register(Map<String, Taglet> taglets) {
            taglets.put(NAME, new ICUDraftTaglet());
        }

        private ICUDraftTaglet() {
            super(NAME, false);
        }

        @Override
		public String toString(String text, Element element) {
            if (text.isEmpty()) {
                System.err.println("Warning: empty draft tag");
            }
            return STATUS + "<dd>Draft " + text + ".</dd>";
        }
    }

    public static class ICUStableTaglet extends ICUTaglet {
        private static final String NAME = "stable";

        public static void register(Map<String, Taglet> taglets) {
            taglets.put(NAME, new ICUStableTaglet());
        }

        private ICUStableTaglet() {
            super(NAME, false);
        }

        @Override
		public String toString(String text, Element element) {
            if (text.length() > 0) {
                return STATUS + "<dd>Stable " + text + ".</dd>";
            } else {
                return STATUS + "<dd>Stable.</dd>";
            }
        }
    }

    public static class ICUProvisionalTaglet extends ICUTaglet {
        private static final String NAME = "provisional";

        public static void register(Map<String, Taglet> taglets) {
            taglets.remove(NAME); // override standard deprecated taglet
            taglets.put(NAME, new ICUProvisionalTaglet());
        }

        private ICUProvisionalTaglet() {
            super(NAME, false);
        }

        @Override
		public String toString(String text, Element element) {
            return null;
        }
    }

    public static class ICUObsoleteTaglet extends ICUTaglet {
        private static final String NAME = "obsolete";

        public static void register(Map<String, Taglet> taglets) {
            taglets.put(NAME, new ICUObsoleteTaglet());
        }

        private ICUObsoleteTaglet() {
            super(NAME, false);
        }

        @Override
		public String toString(String text, Element element) {
            BreakIterator bi = BreakIterator.getSentenceInstance(Locale.US);
            bi.setText(text);
            int first = bi.first();
            int next = bi.next();
            if (text.length() == 0) {
                first = next = 0;
            }
            return STATUS + "<dd><em>Obsolete.</em> <font color='red'>Will be removed in " +
                text.substring(first, next) + "</font>. " + text.substring(next) + "</dd>";
        }
    }

    public static class ICUIgnoreTaglet extends ICUTaglet {
        private static ICUTaglet singleton = new ICUIgnoreTaglet();

        public static void register(Map<String, Taglet> taglets) {
            taglets.put("bug", singleton);
            taglets.put("test", singleton);
            taglets.put("summary", singleton);
        }

        private ICUIgnoreTaglet() {
            super(".ignore", false);
        }

        @Override
		public String toString(String text, Element element) {
            return null;
        }
    }

    /**
     * This taglet should be used in the first line of the class description of classes
     * that are enhancements of JDK classes that similar names and APIs.  The text should
     * provide the full package and name of the JDK class.  A period should follow the
     * tag.  This puts an 'icu enhancement' message into the first line of the class docs,
     * where it will also appear in the class summary.
     *
     * <p>Following this tag (and period), ideally in the first paragraph, the '@icu' tag
     * should be used with the text '_label_' to generate the standard boilerplate about
     * how that tag is used in the class docs.  See {@link ICUNewTaglet}.
     *
     * <p>This cumbersome process is necessary because the javadoc code that handles
     * taglets doesn't look at punctuation in the substitution text to determine when to
     * end the first line, it looks in the original javadoc comment.  So we need a tag to
     * identify the related java class, then a period, then another tag.
     */
     public static class ICUEnhancedTaglet extends ICUTaglet {
        private static final String NAME = "icuenhanced";

        public static void register(Map<String, Taglet> taglets) {
            taglets.put(NAME, new ICUEnhancedTaglet());
        }

        private ICUEnhancedTaglet() {
            super(NAME, true);
        }

        @Override
		public String toString(String text, Element element) {
        	ElementKind kind = element.getKind();
            boolean isClassDoc = (kind == ElementKind.CLASS) || (kind == ElementKind.INTERFACE);
            if (isClassDoc && !text.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                return sb.append("<strong><font color=red>[icu enhancement]</font></strong> ")
                    .append("ICU's replacement for <code>")
                    .append(text)
                    .append("</code>")
                    .toString();
            }
            return "";
        }
    }

    /**
     * This taglet should be used in the first line of any icu-specific members in a class
     * that is an enhancement of a JDK class (see {@link ICUEnhancedTaglet}). It generates
     * the '[icu]' marker followed by the &lt;strong&gt; text, if any.  This does not
     * start or end a paragraph or provide additional leading or trailing punctuation such
     * as spaces or periods.
     *
     * <p>Note: if the text is '_usage_' (without quotes) this spits out a boilerplate
     * message describing the meaning of the '[icu]' tag.  This should be done in the
     * first paragraph of the class docs of any class containing '@icu' tags.
     */
    public static class ICUNewTaglet extends ICUTaglet {
        private static final String NAME = "icu";

        public static void register(Map<String, Taglet> taglets) {
            taglets.put(NAME, new ICUNewTaglet());
        }

        private ICUNewTaglet() {
            super(NAME, true);
        }

        @Override
		public String toString(String text, Element element) {
            StringBuilder sb = new StringBuilder();
            if ("_usage_".equals(text)) {
                return sb.append(" Methods, fields, and other functionality specific to ICU ")
                    .append("are labeled '" + ICU_LABEL + "'.</p>")
                    .toString();
            }

            sb.append("<strong><font color=red>[icu]</font>");
            if (text.length() > 0) {
                sb.append(" ").append(text);
            }
            sb.append("</strong>");
            return sb.toString();
        }
    }

    /**
     * This taglet should be used in class or member documentation, after the first line,
     * where the behavior of the ICU method or class has notable differences from its JDK
     * counterpart. It starts a new paragraph and generates an '[icu] Note:' header.
     */
    public static class ICUNoteTaglet extends ICUTaglet {
        private static final String NAME = "icunote";

        public static void register(Map<String, Taglet> taglets) {
            taglets.put(NAME, new ICUNoteTaglet());
        }

        private ICUNoteTaglet() {
            super(NAME, true);
        }

        @Override
		public String toString(String text, Element element) {
            return "<p><strong><font color=red>[icu]</font> Note:</strong> ";
        }
    }
}
