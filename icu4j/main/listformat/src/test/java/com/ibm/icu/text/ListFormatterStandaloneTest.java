// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.ibm.icu.dev.test.format.FormattedValueTestUtil;
import com.ibm.icu.text.ListFormatter.FormattedList;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for standalone {@link ListFormatter} field spans, offsets, and module isolation. */
@RunWith(JUnit4.class)
public class ListFormatterStandaloneTest {

    @Test
    public void testStandaloneClasspathIsolation() {
        for (String absentClass :
                new String[] {
                    "com.ibm.icu.text.NumberFormat",
                    "com.ibm.icu.impl.FormattedStringBuilder",
                    "com.ibm.icu.impl.FormattedValueStringBuilderImpl"
                }) {
            try {
                Class.forName(absentClass);
                fail("Expected " + absentClass + " to be absent from standalone listformat");
            } catch (ClassNotFoundException expected) {
                // Expected in standalone listformat module
            }
        }
    }

    @Test
    public void testOutOfOrderAndWrappedPatternFields() {
        ListFormatter formatter =
                new ListFormatter(
                        "[{1} after {0}]",
                        "({1} after first {0})", "<{1} after {0}>", "[{1} last after {0}]");

        FormattedList twoItems = formatter.formatToValue("Alpha", "Beta");
        FormattedValueTestUtil.checkFormattedValue(
                "out-of-order 2 items",
                twoItems,
                "[Beta after Alpha]",
                new Object[][] {
                    {ListFormatter.Field.LITERAL, 0, 1},
                    {ListFormatter.SpanField.LIST_SPAN, 1, 5, 1},
                    {ListFormatter.Field.ELEMENT, 1, 5},
                    {ListFormatter.Field.LITERAL, 5, 12},
                    {ListFormatter.SpanField.LIST_SPAN, 12, 17, 0},
                    {ListFormatter.Field.ELEMENT, 12, 17},
                    {ListFormatter.Field.LITERAL, 17, 18},
                });

        FormattedList threeItems = formatter.formatToValue("A", "B", "C");
        // Step 1 (start): "(B after first A)"
        // Step 2 (end):   "[C last after (B after first A)]"
        FormattedValueTestUtil.checkFormattedValue(
                "out-of-order 3 items with coalesced literals",
                threeItems,
                "[C last after (B after first A)]",
                new Object[][] {
                    {ListFormatter.Field.LITERAL, 0, 1},
                    {ListFormatter.SpanField.LIST_SPAN, 1, 2, 2},
                    {ListFormatter.Field.ELEMENT, 1, 2},
                    {ListFormatter.Field.LITERAL, 2, 15},
                    {ListFormatter.SpanField.LIST_SPAN, 15, 16, 1},
                    {ListFormatter.Field.ELEMENT, 15, 16},
                    {ListFormatter.Field.LITERAL, 16, 29},
                    {ListFormatter.SpanField.LIST_SPAN, 29, 30, 0},
                    {ListFormatter.Field.ELEMENT, 29, 30},
                    {ListFormatter.Field.LITERAL, 30, 32},
                });
    }

    @Test
    public void testEmptyAndBlankElementSpans() {
        ListFormatter fmt = ListFormatter.getInstance(com.ibm.icu.util.ULocale.ENGLISH);

        FormattedList zeroItems = fmt.formatToValue(Collections.emptyList());
        FormattedValueTestUtil.checkFormattedValue("empty list", zeroItems, "", new Object[][] {});

        FormattedList singleEmpty = fmt.formatToValue("");
        FormattedValueTestUtil.checkFormattedValue(
                "single empty element", singleEmpty, "", new Object[][] {});

        FormattedList middleEmpty = fmt.formatToValue("A", "", "C");
        FormattedValueTestUtil.checkFormattedValue(
                "middle empty element",
                middleEmpty,
                "A, , and C",
                new Object[][] {
                    {ListFormatter.SpanField.LIST_SPAN, 0, 1, 0},
                    {ListFormatter.Field.ELEMENT, 0, 1},
                    {ListFormatter.Field.LITERAL, 1, 9},
                    {ListFormatter.SpanField.LIST_SPAN, 9, 10, 2},
                    {ListFormatter.Field.ELEMENT, 9, 10},
                });
    }

    @Test
    public void testFormattedListBuilderOffsetsAndAppendTo() {
        ListFormatter fmt =
                new ListFormatter("[{1} after {0}]", "{0}; {1}", "{0}, {1}", "{0}, and {1}");

        ListFormatter.FormattedListBuilder withFields =
                fmt.formatImpl(Arrays.asList("Alpha", "Beta"), true);
        assertEquals("[Beta after Alpha]", withFields.toString());
        assertEquals(-1, withFields.getOffset(-1));
        assertEquals(12, withFields.getOffset(0));
        assertEquals(1, withFields.getOffset(1));
        assertEquals(-1, withFields.getOffset(2));

        StringBuilder dest = new StringBuilder("prefix:");
        withFields.appendTo(dest);
        assertEquals("prefix:[Beta after Alpha]", dest.toString());

        FormattedList fv = withFields.toValue();
        StringBuilder fvDest = new StringBuilder("val:");
        fv.appendTo(fvDest);
        assertEquals("val:[Beta after Alpha]", fvDest.toString());

        ConstrainedFieldPosition cfpos = new ConstrainedFieldPosition();
        cfpos.constrainFieldAndValue(ListFormatter.SpanField.LIST_SPAN, 0);
        assertTrue(fv.nextPosition(cfpos));
        assertEquals(12, cfpos.getStart());
        assertEquals(17, cfpos.getLimit());
        assertFalse(fv.nextPosition(cfpos));

        ListFormatter.FormattedListBuilder withoutFields =
                fmt.formatImpl(Arrays.asList("Alpha", "Beta"), false);
        assertEquals("[Beta after Alpha]", withoutFields.toString());
        assertEquals(-1, withoutFields.getOffset(0));
    }
}
