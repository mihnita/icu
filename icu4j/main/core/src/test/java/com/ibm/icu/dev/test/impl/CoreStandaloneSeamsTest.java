// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import com.ibm.icu.impl.CurrencyData;
import com.ibm.icu.impl.CurrencyData.CurrencySpacingInfo.SpacingPattern;
import com.ibm.icu.impl.SimpleFormatterImpl;
import com.ibm.icu.util.ULocale;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Verifies standalone {@code core} operation and zero dependencies on {@code number}, {@code rbnf},
 * and {@code datetime}.
 */
@RunWith(JUnit4.class)
public class CoreStandaloneSeamsTest {

    @Test
    @SuppressWarnings("EnumOrdinal")
    public void testCoreStandaloneWithoutHigherModules() {
        for (String absentClass :
                new String[] {
                    "com.ibm.icu.text.NumberFormat",
                    "com.ibm.icu.text.DecimalFormatSymbols",
                    "com.ibm.icu.impl.FormattedStringBuilder",
                    "com.ibm.icu.text.RuleBasedNumberFormat",
                    "com.ibm.icu.text.DateFormat"
                }) {
            assertThrows(ClassNotFoundException.class, () -> Class.forName(absentClass));
        }

        assertEquals(0, SpacingPattern.CURRENCY_MATCH.ordinal());
        assertEquals(1, SpacingPattern.SURROUNDING_MATCH.ordinal());
        assertEquals(2, SpacingPattern.INSERT_BETWEEN.ordinal());
        assertNotNull(CurrencyData.provider.getInstance(ULocale.US, true));

        String compiled =
                SimpleFormatterImpl.compileToStringMinMaxArguments(
                        "{1} - {0}", new StringBuilder(), 2, 2);
        assertEquals("B - A", SimpleFormatterImpl.formatCompiledPattern(compiled, "A", "B"));

        // Verify timezoneTypes.res alias in keyTypeData.res resolves in standalone core
        ULocale tzLoc = ULocale.forLanguageTag("en-u-tz-uslax");
        assertEquals("America/Los_Angeles", tzLoc.getKeywordValue("timezone"));
    }

    @Test
    public void testNoBytecodeReferencesToNumberFromCoreSeams() throws IOException {
        for (Class<?> cls : new Class<?>[] {CurrencyData.class, SimpleFormatterImpl.class}) {
            String res = cls.getSimpleName() + ".class";
            try (InputStream in = cls.getResourceAsStream(res)) {
                assertNotNull(in);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int r;
                while ((r = in.read(buf)) != -1) {
                    out.write(buf, 0, r);
                }
                String raw = new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
                assertFalse(raw.contains("com/ibm/icu/text/DecimalFormatSymbols"));
                assertFalse(raw.contains("com/ibm/icu/impl/FormattedStringBuilder"));
            }
        }
    }
}
