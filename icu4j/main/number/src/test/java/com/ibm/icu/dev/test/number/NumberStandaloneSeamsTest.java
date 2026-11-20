// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.number;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.ibm.icu.impl.CurrencyData.CurrencySpacingInfo.SpacingPattern;
import com.ibm.icu.impl.FormattedStringBuilder;
import com.ibm.icu.impl.SimpleFormatterImpl;
import com.ibm.icu.impl.number.SimpleModifier;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Verifies standalone {@code number} operation without {@code rbnf} or {@code datetime} on the
 * classpath.
 */
@RunWith(JUnit4.class)
public class NumberStandaloneSeamsTest {

    @Test
    @SuppressWarnings("EnumOrdinal")
    public void testSpacingPatternConstantsMatchDecimalFormatSymbols() {
        assertEquals(
                DecimalFormatSymbols.CURRENCY_SPC_CURRENCY_MATCH,
                SpacingPattern.CURRENCY_MATCH.ordinal());
        assertEquals(
                DecimalFormatSymbols.CURRENCY_SPC_SURROUNDING_MATCH,
                SpacingPattern.SURROUNDING_MATCH.ordinal());
        assertEquals(
                DecimalFormatSymbols.CURRENCY_SPC_INSERT, SpacingPattern.INSERT_BETWEEN.ordinal());
    }

    @Test
    public void testSimpleModifierFormatPrefixSuffix() {
        FormattedStringBuilder fsb = new FormattedStringBuilder();
        fsb.append("42", NumberFormat.Field.INTEGER);
        String oneArg =
                SimpleFormatterImpl.compileToStringMinMaxArguments(
                        "approx. {0} kg", new StringBuilder(), 0, 1);
        SimpleModifier.formatPrefixSuffix(oneArg, null, 0, fsb.length(), fsb);
        assertEquals("approx. 42 kg", fsb.toString());

        String zeroArg =
                SimpleFormatterImpl.compileToStringMinMaxArguments(
                        "pair", new StringBuilder(), 0, 1);
        SimpleModifier.formatPrefixSuffix(zeroArg, null, 0, fsb.length(), fsb);
        assertEquals("pair", fsb.toString());
    }

    @Test
    public void testAlgorithmicNumberingSystemFallsBackToDecimalFormatWithoutRbnf()
            throws IOException {
        assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("com.ibm.icu.text.RuleBasedNumberFormat"));

        // With rbnf absent, algorithmic numbering system locales must fall back cleanly to
        // DecimalFormat rather than throwing ClassNotFoundException.
        NumberFormat nf = NumberFormat.getInstance(new ULocale("he@numbers=hebr"));
        assertTrue(nf instanceof DecimalFormat);
        assertEquals("123", nf.format(123));

        try (InputStream in = NumberFormat.class.getResourceAsStream("NumberFormat.class")) {
            assertNotNull(in);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
            }
            String raw = new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
            assertFalse(raw.contains("Lcom/ibm/icu/text/RuleBasedNumberFormat;"));
        }
    }
}
