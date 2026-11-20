// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.format;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.ibm.icu.impl.RelativeDateFormat;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.TimeUnitFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeUnit;
import com.ibm.icu.util.TimeUnitAmount;
import com.ibm.icu.util.ULocale;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Verifies the {@code SimpleFormatterImpl}-based replacements for legacy {@code MessageFormat}
 * callers in {@code RelativeDateFormat}, {@code SimpleDateFormat}, and {@code TimeUnitFormat}.
 */
@RunWith(JUnit4.class)
public class SimpleFormatterSeamsTest {

    @Test
    public void testRelativeDateTimeFormattingAndSerialization()
            throws IOException, ClassNotFoundException {
        DateFormat df =
                DateFormat.getDateTimeInstance(
                        DateFormat.RELATIVE_LONG, DateFormat.SHORT, ULocale.ENGLISH);
        assertTrue(df instanceof RelativeDateFormat);

        Calendar nowCal = Calendar.getInstance(ULocale.ENGLISH);
        String todayFormatted = df.format(nowCal);
        assertTrue(
                "Expected relative day text in: " + todayFormatted,
                todayFormatted.toLowerCase(Locale.ROOT).contains("today"));

        // Distant date falls back to non-relative date pattern combined with time pattern
        Calendar distantCal = Calendar.getInstance(ULocale.ENGLISH);
        distantCal.setTimeInMillis(0L);
        String distantFormatted = df.format(distantCal);
        assertTrue(distantFormatted.contains("1970") || distantFormatted.contains("1969"));

        // Verify Java serialization round-trip re-initializes transient fCombinedPattern
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(df);
        }
        DateFormat deserialized;
        try (ObjectInputStream ois =
                new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            deserialized = (DateFormat) ois.readObject();
        }
        assertEquals(distantFormatted, deserialized.format(distantCal));
        assertEquals(todayFormatted, deserialized.format(nowCal));
    }

    @Test
    public void testSimpleDateFormatNumericLeapMonthRoundTrip() throws ParseException {
        for (String locId :
                new String[] {
                    "zh@calendar=chinese",
                    "zh_Hant@calendar=chinese",
                    "fr@calendar=chinese",
                    "en@calendar=dangi",
                    "ko@calendar=dangi"
                }) {
            ULocale loc = new ULocale(locId);
            SimpleDateFormat sdf = new SimpleDateFormat("G-y-M-d", loc);
            Calendar cal = Calendar.getInstance(loc);
            cal.clear();
            // 2012 (ren-chen, cycle 78 year 29) had a leap 4th month in Chinese / 3rd in Dangi
            cal.set(Calendar.ERA, 78);
            cal.set(Calendar.YEAR, 29);
            cal.set(Calendar.MONTH, locId.contains("dangi") ? 2 : 3);
            cal.set(Calendar.IS_LEAP_MONTH, 1);
            cal.set(Calendar.DATE, 2);

            String formattedLeap = sdf.format(cal.getTime());
            Calendar parsedCal = Calendar.getInstance(loc);
            parsedCal.clear();
            ParsePosition pos = new ParsePosition(0);
            sdf.parse(formattedLeap, parsedCal, pos);
            assertEquals(
                    "Full string consumed for " + locId, formattedLeap.length(), pos.getIndex());
            assertEquals("Leap month flag for " + locId, 1, parsedCal.get(Calendar.IS_LEAP_MONTH));
            assertEquals(
                    "Month index for " + locId,
                    cal.get(Calendar.MONTH),
                    parsedCal.get(Calendar.MONTH));

            // Non-leap month in the same year must parse with IS_LEAP_MONTH == 0
            cal.set(Calendar.IS_LEAP_MONTH, 0);
            String formattedNormal = sdf.format(cal.getTime());
            parsedCal.clear();
            pos.setIndex(0);
            sdf.parse(formattedNormal, parsedCal, pos);
            assertEquals(formattedNormal.length(), pos.getIndex());
            assertEquals(0, parsedCal.get(Calendar.IS_LEAP_MONTH));

            // Custom NumberFormat on SimpleDateFormat must also be honored for numeric leap months
            NumberFormat customNf = NumberFormat.getIntegerInstance(ULocale.ENGLISH);
            sdf.setNumberFormat(customNf);
            cal.set(Calendar.IS_LEAP_MONTH, 1);
            String customFormatted = sdf.format(cal.getTime());
            parsedCal.clear();
            pos.setIndex(0);
            sdf.parse(customFormatted, parsedCal, pos);
            assertEquals(customFormatted.length(), pos.getIndex());
            assertEquals(1, parsedCal.get(Calendar.IS_LEAP_MONTH));
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTimeUnitFormatParseEdgeCases() throws ParseException {
        TimeUnitFormat enFmt = new TimeUnitFormat(ULocale.ENGLISH, TimeUnitFormat.FULL_NAME);

        // Offset parsing with prefix text
        ParsePosition pos = new ParsePosition(6);
        TimeUnitAmount parsed = enFmt.parseObject("Wait: 2.5 hours!", pos);
        assertNotNull(parsed);
        assertEquals(TimeUnit.HOUR, parsed.getTimeUnit());
        assertEquals(2.5, parsed.getNumber().doubleValue(), 1e-9);
        assertEquals("Wait: 2.5 hours".length(), pos.getIndex());
        assertEquals(-1, pos.getErrorIndex());

        // Literal "{0}" placeholder must be rejected
        pos = new ParsePosition(0);
        assertNull(enFmt.parseObject("{0} hours", pos));
        assertEquals(0, pos.getIndex());

        // Empty and null input handling
        pos = new ParsePosition(0);
        assertNull(enFmt.parseObject("", pos));
        assertEquals(0, pos.getErrorIndex());

        pos = new ParsePosition(0);
        assertNull(enFmt.parseObject(null, pos));
        assertEquals(0, pos.getErrorIndex());

        // 0-argument dual unit pattern in Arabic
        TimeUnitFormat arFmt = new TimeUnitFormat(new ULocale("ar"), TimeUnitFormat.FULL_NAME);
        TimeUnitAmount twoSeconds = new TimeUnitAmount(2, TimeUnit.SECOND);
        String arFormatted = arFmt.format(twoSeconds);
        TimeUnitAmount arParsed = (TimeUnitAmount) arFmt.parseObject(arFormatted);
        assertEquals(twoSeconds, arParsed);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testNoMessageFormatDependencyInCore() throws IOException {
        assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("com.ibm.icu.text.MessageFormat"));

        for (Class<?> cls :
                new Class<?>[] {
                    RelativeDateFormat.class, SimpleDateFormat.class, TimeUnitFormat.class
                }) {
            String resourceName = cls.getSimpleName() + ".class";
            try (InputStream in = cls.getResourceAsStream(resourceName)) {
                assertNotNull("Missing class resource " + resourceName, in);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int r;
                while ((r = in.read(buf)) != -1) {
                    out.write(buf, 0, r);
                }
                String rawLatin1 = new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
                assertFalse(
                        cls.getName() + " must not reference com/ibm/icu/text/MessageFormat",
                        rawLatin1.contains("com/ibm/icu/text/MessageFormat"));
            }
        }
    }
}
