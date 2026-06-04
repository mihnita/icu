// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.util;

import java.util.Date;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.message2.MessageFormatter;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;

@RunWith(JUnit4.class)
public class DFTest extends CoreTestFmwk {
    @Test
    public void test() {
        Locale locale = Locale.US;
        String tzDefaultId = "Europe/Paris"; // default
        String tzIdOfFormatted = "US/Hawaii"; // for the things to be formatted
        String tzOfFormattersId = "America/New_York"; // for formatters, when explicitly set

        // Save and set the default timezones
        java.util.TimeZone jdkTzBackup = java.util.TimeZone.getDefault();
        ZoneId jdkZidBackup = ZoneId.systemDefault();
        TimeZone icuTzBackup = TimeZone.getDefault();

        TimeZone.setDefault(TimeZone.getTimeZone(tzDefaultId));
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(tzDefaultId));
        TimeZone icuTzCheck = TimeZone.getDefault();

        // Things to format
        // not zoned types
        LocalDateTime ldt =  LocalDateTime.of(2026, Month.SEPTEMBER, 18, 23, 54, 49);
        Date date = new Date(2026 - 1900, 8 /*month, 0 based*/, 18, 23, 54, 49);
        // zoned types
        ZonedDateTime zdt = ZonedDateTime.of(ldt, ZoneId.of(tzIdOfFormatted));
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(tzIdOfFormatted));
        cal.set(2026, Calendar.SEPTEMBER, 18, 23, 54, 49);
        java.util.Calendar jcal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone(tzIdOfFormatted));
        jcal.set(2026, java.util.Calendar.SEPTEMBER, 18, 23, 54, 49);

//        showRawObject(ldt);
//        showRawObject(date);
//        showRawObject(zdt);
//        showRawObject(cal);
//        showRawObject(jcal);

        // Formatters
        String pattern = "MMMM d, y 'at' h:m:s a zzzz";
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMMM d, y 'at' h:m:s a zzzz", locale);
        DateFormat df = DateFormat.getInstanceForSkeleton("y MMMM d jms zzzz", locale);
        java.text.DateFormat jdf = new java.text.SimpleDateFormat(pattern, locale);

        MessageFormat mf = new MessageFormat("{theDate, date, ::yMMMMdjmszzzz}", locale);
        MessageFormatter mf2 = MessageFormatter.builder()
            .setPattern("{$theDate :datetime"
                + " dateFields=year-month-day dateLength=long timePrecision=second"
                + " timeZoneStyle=long}")
            .setLocale(locale)
            .build();

        // The formatting action WITHOUT tz set on formatter
        System.out.println("=== Formatters with default timezone (" + tzDefaultId + ") ========");
        formatAndShow(dtf, df, jdf, mf, mf2, ldt, date, zdt, cal, jcal);

        dtf = dtf.withZone(ZoneId.of(tzOfFormattersId));
        df.setTimeZone(TimeZone.getTimeZone(tzOfFormattersId));
        jdf.setTimeZone(java.util.TimeZone.getTimeZone(tzOfFormattersId));

        mf.setFormatByArgumentName("theDate", df);
        mf2 = MessageFormatter.builder()
            .setPattern("{$theDate :datetime"
                + " dateFields=year-month-day dateLength=long timePrecision=second"
                + " timeZoneStyle=long timeZone=|" + tzOfFormattersId + "|}")
            .setLocale(locale)
            .build();

        // The formatting action WITH tz set on formatter
        System.out.println("=== Formatters with timezone set to " + tzOfFormattersId + " =======");
        formatAndShow(dtf, df, jdf, mf, mf2, ldt, date, zdt, cal, jcal);

        // Restore the default timezones
        TimeZone.setDefault(icuTzBackup);
        java.util.TimeZone.setDefault(jdkTzBackup);
    }

    private void showRawObject(Object obj) {
        System.out.printf("  %-35s : %s%n", obj.getClass().getName(), obj);
    }

    static void formatAndShow(
            DateTimeFormatter dtf, DateFormat df, java.text.DateFormat jdf, MessageFormat mf, MessageFormatter mf2,
            LocalDateTime ldt, java.util.Date date,
            ZonedDateTime zdt, Calendar cal, java.util.Calendar jcal) {
        int count = 0;
        // Locale (non-zoned) types
        System.out.print(countFmt(++count) + ". ✅ jdk DateTimeFormatter(LocalDateTime) : ");
        try {
            System.out.println(dtf.format(ldt));
        } catch (Exception e) {
            System.out.println(e.getClass().getSimpleName() + "(\"" + e.getMessage() + "\")");
        }
        System.out.println(countFmt(++count) + ". ✅ jdk DateFormat(java.util.Date)       : " + jdf.format(date));
        System.out.println(countFmt(++count) + ". icu DateFormat(LocalDateTime)           : " + df.format(ldt));
        System.out.println(countFmt(++count) + ". icu DateFormat(java.util.Date)          : " + df.format(date));
        System.out.println(countFmt(++count) + ". icu MessageFormat (LocalDateTime)       : " + mf.format(Map.of("theDate", ldt)));
        System.out.println(countFmt(++count) + ". icu MessageFormat (java.util.Date)      : " + mf.format(Map.of("theDate", date)));
        System.out.println(countFmt(++count) + ". icu MessageFormat2 (LocalDateTime)      : " + mf2.formatToString(Map.of("theDate", ldt)));
        System.out.println(countFmt(++count) + ". icu MessageFormat2 (java.util.Date)     : " + mf2.formatToString(Map.of("theDate", date)));

        // Zoned types
        System.out.println(countFmt(++count) + ". ✅ jdk DateTimeFormatter(ZonedDateTime) : " + dtf.format(zdt));
        System.out.println(countFmt(++count) + ". icu DateFormat(ZonedDateTime)           : " + df.format(zdt));
        System.out.println(countFmt(++count) + ". icu DateFormat(icu.Calendar)            : " + df.format(cal));
        System.out.println(countFmt(++count) + ". icu DateFormat(jdk.Calendar)            : " + df.format(jcal));

        // MF
        System.out.println(countFmt(++count) + ". icu MessageFormat (ZonedDateTime)       : " + mf.format(Map.of("theDate", zdt)));
        System.out.println(countFmt(++count) + ". icu MessageFormat (icu.Calendar)        : " + mf.format(Map.of("theDate", cal)));
        System.out.println(countFmt(++count) + ". icu MessageFormat  (jdk.Calendar)       : " + mf.format(Map.of("theDate", jcal)));

        System.out.println(countFmt(++count) + ". icu MessageFormat2 (ZonedDateTime)      : " + mf2.formatToString(Map.of("theDate", zdt)));
        System.out.println(countFmt(++count) + ". icu MessageFormat2 (icu.Calendar)       : " + mf2.formatToString(Map.of("theDate", cal)));
        System.out.println(countFmt(++count) + ". icu MessageFormat2 (jdk.Calendar)       : " + mf2.formatToString(Map.of("theDate", jcal)));
    }

    static String countFmt(int count) {
        return String.format(" %3s", count);
    }

    @Test
    public void testMinimal() {
        // Zoned types
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2026, Month.SEPTEMBER.getValue(), 23,
                18, 54, 49, 0, ZoneId.of("US/Hawaii"));
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("US/Hawaii"));
        calendar.set(2026, Calendar.SEPTEMBER, 23,
                18, 54, 49);

        // Formatters
        DateTimeFormatter jdkDateFormatter = DateTimeFormatter.ofPattern("h:m a zzzz", Locale.US);
        DateFormat icuDateFormatter1 = DateFormat.getInstanceForSkeleton("jmzzzz", Locale.US);
        DateFormat icuDateFormatter2 = DateFormat.getInstanceForSkeleton(calendar, "jmzzzz", Locale.US);
        DateFormat icuDateFormatter3 = DateFormat.getInstanceForSkeleton(calendar, "jmzzzz", Locale.US);
        DateFormat icuDateFormatter4 = DateFormat.getInstanceForSkeleton("jmzzzz", Locale.US);

        System.out.println("============");
        System.out.println("* " + jdkDateFormatter.format(zonedDateTime)); // jdk
        System.out.println("1  " + icuDateFormatter1.format(calendar)); // icu
        System.out.println("2  " + icuDateFormatter2.format(calendar)); // icu
        System.out.println("3  " + icuDateFormatter3.format(calendar)); // icu
        System.out.println("4  " + icuDateFormatter4.format(calendar)); // icu
        // 6:54 PM Hawaii-Aleutian Standard Time
        // 6:54 PM Hawaii-Aleutian Standard Time
        System.out.println("1  " + icuDateFormatter1.getTimeZone().getID()); // icu
        System.out.println("2  " + icuDateFormatter2.getTimeZone().getID()); // icu
        System.out.println("3  " + icuDateFormatter3.getTimeZone().getID()); // icu
        System.out.println("4  " + icuDateFormatter4.getTimeZone().getID()); // icu

        // Configure the formatters with a timezone
        jdkDateFormatter = jdkDateFormatter.withZone(ZoneId.of("America/Los_Angeles"));
        icuDateFormatter1.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
//        icuDateFormatter2.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
        icuDateFormatter3.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));

        System.out.println("============");
        System.out.println("* " + jdkDateFormatter.format(zonedDateTime)); // jdk
        System.out.println("1  " + icuDateFormatter1.format(calendar)); // icu
        System.out.println("2  " + icuDateFormatter2.format(calendar)); // icu
        System.out.println("3  " + icuDateFormatter3.format(calendar)); // icu
        System.out.println("4  " + icuDateFormatter4.format(calendar)); // icu
        // 9:54 PM Pacific Daylight Time
        // 6:54 PM Hawaii-Aleutian Standard Time => the tz of the formatter is ignored
        System.out.println("1  " + icuDateFormatter1.getTimeZone().getID()); // icu
        System.out.println("2  " + icuDateFormatter2.getTimeZone().getID()); // icu
        System.out.println("3  " + icuDateFormatter3.getTimeZone().getID()); // icu
        System.out.println("4  " + icuDateFormatter4.getTimeZone().getID()); // icu
    }

    @Test
    public void testBadCalendarSharing() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("US/Hawaii"));
        DateFormat icuDateFormatter1 = DateFormat.getInstanceForSkeleton(calendar, "jmzzzz", Locale.US);
        DateFormat icuDateFormatter2 = DateFormat.getInstanceForSkeleton(calendar, "jmzzzz", Locale.US);

        System.out.println("1  " + icuDateFormatter1.getTimeZone().getID()); // icu
        System.out.println("2  " + icuDateFormatter2.getTimeZone().getID()); // icu
        // US/Hawaii
        // US/Hawaii

        icuDateFormatter2.setTimeZone(TimeZone.getTimeZone("America/New_York"));

        System.out.println("1  " + icuDateFormatter1.getTimeZone().getID()); // icu
        System.out.println("2  " + icuDateFormatter2.getTimeZone().getID()); // icu
        // America/New_York
        // America/New_York
    }

    @Test
    public void testAbsoluteTime() {
        Calendar calendar1 = Calendar.getInstance(TimeZone.getTimeZone("US/Hawaii"));
        calendar1.set(2026, Calendar.SEPTEMBER, 23, 8, 54, 49);
        Calendar calendar2 = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        calendar2.set(2026, Calendar.SEPTEMBER, 23, 11, 54, 49);
        Locale localeIslam = Locale.forLanguageTag("en-US-u-ca-islamic");
        Calendar calendar3 = Calendar.getInstance(TimeZone.getTimeZone("US/Hawaii"), localeIslam);
        calendar3.setTime(calendar2.getTime());

//        DateFormat df = DateFormat.getInstanceForSkeleton("y MMMM d jms zzzz", Locale.US);
//        DateFormat df = DateFormat.getInstanceForSkeleton(calendar2, "y MMMM d jms zzzz", Locale.FRANCE);
        DateFormat df = DateFormat.getInstanceForSkeleton("y MMMM d jms zzzz", Locale.FRANCE);

        System.out.println("cal1     : " + df.format(calendar1));
        System.out.println("cal2     : " + df.format(calendar2));
        System.out.println("cal3     : " + df.format(calendar3));
        System.out.println("cal1.time: " + df.format(calendar1.getTime()));
        System.out.println("cal2.time: " + df.format(calendar2.getTime()));
        System.out.println("cal3.time: " + df.format(calendar3.getTime()));

        System.out.println(calendar1.equals(calendar2)); // false
        System.out.println(calendar1.equals(calendar3)); // false
        System.out.println(calendar2.equals(calendar3)); // false
        System.out.println(calendar1.getTime().equals(calendar2.getTime())); // true
        System.out.println(calendar1.getTime().equals(calendar3.getTime())); // true
        System.out.println(calendar2.getTime().equals(calendar3.getTime())); // true

        df.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
        System.out.println("cal1     : " + df.format(calendar1));
        System.out.println("cal2     : " + df.format(calendar2));
        System.out.println("cal3     : " + df.format(calendar3));
        System.out.println("cal1.time: " + df.format(calendar1.getTime()));
        System.out.println("cal2.time: " + df.format(calendar2.getTime()));
        System.out.println("cal3.time: " + df.format(calendar3.getTime()));
        // calendar.set(2026, Calendar.SEPTEMBER, 23, 18, 54, 49);
    }
}
