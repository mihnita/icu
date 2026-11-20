// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.format;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.ibm.icu.text.ConstrainedFieldPosition;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.FormattedValue;
import com.ibm.icu.text.NumberFormat;
import java.math.BigDecimal;
import java.text.Format.Field;
import org.junit.Test;

/**
 * @author sffc
 */
public class FormattedValueTest {
    @Test
    public void testBasic() {
        ConstrainedFieldPosition cfpos = new ConstrainedFieldPosition();
        assertAllPartsEqual("basic", cfpos, 15, null, null, 0, 0, 0L);
    }

    @Test
    public void testSetters() {
        ConstrainedFieldPosition cfpos = new ConstrainedFieldPosition();

        cfpos.constrainField(NumberFormat.Field.COMPACT);
        assertAllPartsEqual("setters 1", cfpos, 10, NumberFormat.Field.COMPACT, null, 0, 0, 0L);

        cfpos.constrainFieldAndValue(NumberFormat.Field.COMPACT, 42);
        assertAllPartsEqual("setters 1.2", cfpos, 8, NumberFormat.Field.COMPACT, 42, 0, 0, 0L);

        cfpos.constrainClass(NumberFormat.Field.class);
        assertAllPartsEqual("setters 1.5", cfpos, 11, null, null, 0, 0, 0L);

        cfpos.setInt64IterationContext(42424242424242L);
        assertAllPartsEqual("setters 2", cfpos, 11, null, null, 0, 0, 42424242424242L);

        cfpos.setState(NumberFormat.Field.COMPACT, BigDecimal.ONE, 5, 10);
        assertAllPartsEqual(
                "setters 3",
                cfpos,
                11,
                NumberFormat.Field.COMPACT,
                BigDecimal.ONE,
                5,
                10,
                42424242424242L);

        cfpos.reset();
        assertAllPartsEqual("setters 4", cfpos, 15, null, null, 0, 0, 0L);
    }

    @Test
    public void testIllegalArgumentException() {
        ConstrainedFieldPosition cfpos = new ConstrainedFieldPosition();
        try {
            cfpos.matchesField(null, null);
            fail("Expected an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    private void assertAllPartsEqual(
            String messagePrefix,
            ConstrainedFieldPosition cfpos,
            int matching,
            Field field,
            Object value,
            int start,
            int limit,
            long context) {
        assertEquals(messagePrefix + ": field", field, cfpos.getField());
        assertEquals(messagePrefix + ": field value", value, cfpos.getFieldValue());
        assertEquals(messagePrefix + ": start", start, cfpos.getStart());
        assertEquals(messagePrefix + ": limit", limit, cfpos.getLimit());
        assertEquals(messagePrefix + ": context", context, cfpos.getInt64IterationContext());

        assertEquals(
                messagePrefix + ": integer field",
                ((matching & 1) != 0),
                cfpos.matchesField(NumberFormat.Field.INTEGER, null));
        assertEquals(
                messagePrefix + ": compact field",
                ((matching & 2) != 0),
                cfpos.matchesField(NumberFormat.Field.COMPACT, null));
        assertEquals(
                messagePrefix + ": date field",
                ((matching & 4) != 0),
                cfpos.matchesField(DateFormat.Field.AM_PM, null));
        assertEquals(
                messagePrefix + ": compact field with value",
                ((matching & 8) != 0),
                cfpos.matchesField(NumberFormat.Field.COMPACT, 42));
    }

    public static void checkFormattedValue(
            String message,
            FormattedValue fv,
            String expectedString,
            Object[][] expectedFieldPositions) {
        FormattedValueTestUtil.checkFormattedValue(
                message, fv, expectedString, expectedFieldPositions);
    }

    public static void checkFormattedValue(
            String message,
            FormattedValue fv,
            String expectedString,
            Object[][] expectedFieldPositions,
            boolean skipAttributedCharacterIterator) {
        FormattedValueTestUtil.checkFormattedValue(
                message,
                fv,
                expectedString,
                expectedFieldPositions,
                skipAttributedCharacterIterator);
    }

    public static void assertCharSequenceEquals(CharSequence a, CharSequence b) {
        FormattedValueTestUtil.assertCharSequenceEquals(a, b);
    }
}
