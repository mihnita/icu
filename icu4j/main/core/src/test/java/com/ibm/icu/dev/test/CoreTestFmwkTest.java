// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import com.ibm.icu.util.ULocale;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link CoreTestFmwk}, including operation without ICU {@code TimeZone} on classpath.
 */
@RunWith(JUnit4.class)
public class CoreTestFmwkTest {

    @Ignore // Prevent helper subclass from being detected as a standalone JUnit test class
    private static final class DummyCoreTest extends CoreTestFmwk {
        DummyCoreTest(IcuTimeZoneHelper helper) {
            super(helper);
        }
    }

    @Test
    public void testWithIcuTimeZoneAvailable() {
        CoreTestFmwk.IcuTimeZoneHelper helper =
                CoreTestFmwk.IcuTimeZoneHelper.forClassName("com.ibm.icu.util.TimeZone");
        assertNotNull("Expected ICU TimeZone helper when TimeZone is on classpath", helper);

        DummyCoreTest dummy = new DummyCoreTest(helper);
        dummy.testInitialize();
        assertEquals("America/Los_Angeles", helper.getDefaultId());
        dummy.testTeardown();
    }

    @Test
    public void testWithoutIcuTimeZoneAvailable() {
        CoreTestFmwk.IcuTimeZoneHelper missingHelper =
                CoreTestFmwk.IcuTimeZoneHelper.forClassName(
                        "com.ibm.icu.util.NonExistentTimeZoneForTest");
        assertNull("Expected null helper when ICU TimeZone class is absent", missingHelper);

        DummyCoreTest dummy = new DummyCoreTest(missingHelper);
        dummy.testInitialize();
        assertEquals(ULocale.forLocale(Locale.US), ULocale.getDefault());
        assertEquals("America/Los_Angeles", TimeZone.getDefault().getID());
        dummy.testTeardown();

        // Verify ULocale leak detection still triggers even without ICU TimeZone helper
        dummy.testInitialize();
        ULocale saved = ULocale.getDefault();
        try {
            ULocale.setDefault(ULocale.FRANCE);
            assertThrows(AssertionError.class, dummy::testTeardown);
        } finally {
            ULocale.setDefault(saved);
        }
    }

    @Test
    public void testNoStaticBytecodeDependencyOnIcuTimeZone() throws IOException {
        try (InputStream in = CoreTestFmwk.class.getResourceAsStream("CoreTestFmwk.class")) {
            assertNotNull("Could not locate CoreTestFmwk.class bytes", in);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
            }
            String rawLatin1 = new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
            assertFalse(
                    "CoreTestFmwk.class must not contain a direct bytecode descriptor for com/ibm/icu/util/TimeZone",
                    rawLatin1.contains("com/ibm/icu/util/TimeZone"));
        }
    }
}
