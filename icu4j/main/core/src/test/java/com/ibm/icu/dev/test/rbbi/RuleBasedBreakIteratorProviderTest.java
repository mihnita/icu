// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.rbbi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.ibm.icu.impl.breakiter.LanguageBreakEngine;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.RuleBasedBreakIterator;
import com.ibm.icu.util.ULocale;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Verifies standalone RBBI operation in {@code core} when dictionary/LSTM break engines in {@code
 * segmenter_dict} are not on the classpath.
 */
@RunWith(JUnit4.class)
public class RuleBasedBreakIteratorProviderTest {

    @Test
    public void testStandaloneWordBreakAndTitleCaseWithoutDictionaryModule() {
        assertNull(
                "Expected null factory when provider class is absent",
                LanguageBreakEngine.loadFactory(
                        "com.ibm.icu.impl.breakiter.DictionaryBreakEngineProvider"));

        assertEquals("Hello World", UCharacter.toTitleCase(ULocale.ENGLISH, "hello world", null));

        // Dictionary scripts (e.g. Thai, CJK) must fall back cleanly to UnhandledBreakEngine
        // without throwing ClassNotFoundException when segmenter_dict is absent.
        BreakIterator wordIter = BreakIterator.getWordInstance(new ULocale("th"));
        wordIter.setText("ภาษาไทย hello");
        int first = wordIter.first();
        assertEquals(0, first);
        int end = wordIter.next();
        while (end != BreakIterator.DONE) {
            first = end;
            end = wordIter.next();
        }
        assertEquals("ภาษาไทย hello".length(), first);
    }

    @Test
    public void testNoStaticBytecodeReferencesToConcreteDictionaryEngines() throws IOException {
        try (InputStream in =
                RuleBasedBreakIterator.class.getResourceAsStream("RuleBasedBreakIterator.class")) {
            assertNotNull("Could not locate RuleBasedBreakIterator.class", in);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
            }
            String rawLatin1 = new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
            for (String forbidden :
                    new String[] {
                        "BurmeseBreakEngine",
                        "CjkBreakEngine",
                        "KhmerBreakEngine",
                        "LSTMBreakEngine",
                        "LaoBreakEngine",
                        "ThaiBreakEngine"
                    }) {
                assertFalse(
                        "RuleBasedBreakIterator.class must not directly reference " + forbidden,
                        rawLatin1.contains("com/ibm/icu/impl/breakiter/" + forbidden));
            }
        }
    }
}
