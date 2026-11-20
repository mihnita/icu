// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.format;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedNumberFormat;
import com.ibm.icu.util.ULocale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Verifies that {@link NumberFormat#getInstance(ULocale)} delegates algorithmic numbering systems
 * to {@link RuleBasedNumberFormat} when the {@code rbnf} module is present on the classpath.
 */
@RunWith(JUnit4.class)
public class NumberFormatAlgorithmicSeamTest {

    @Test
    public void testAlgorithmicNumberingSystemsReturnRuleBasedNumberFormat() {
        NumberFormat hebr = NumberFormat.getInstance(new ULocale("he@numbers=hebr"));
        assertTrue(hebr instanceof RuleBasedNumberFormat);
        assertFalse(hebr.format(5786).isEmpty());

        NumberFormat hant = NumberFormat.getInstance(new ULocale("zh@numbers=hant"));
        assertTrue(hant instanceof RuleBasedNumberFormat);
        assertFalse(hant.format(1234).isEmpty());
    }
}
