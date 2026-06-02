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
        String tzDefaultId = "Europe/Paris"; // for formatters
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
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMMM d, y 'at' h:m:s a zzzz", Locale.US);
        DateFormat df = DateFormat.getInstanceForSkeleton("y MMMM d jms zzzz", Locale.US);
        java.text.DateFormat jdf = new java.text.SimpleDateFormat(pattern, Locale.US);
        
        MessageFormat mf = new MessageFormat("{theDate, date, ::yMMMMdjmszzzz}", Locale.US);
        MessageFormatter mf2 = MessageFormatter.builder()
            .setPattern("{$theDate :datetime"
                + " dateFields=year-month-day dateLength=long timePrecision=second"
                + " timeZoneStyle=long}")
            .setLocale(Locale.US)
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
            .setLocale(Locale.US)
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
        System.out.print(countFmt(++count) + ". \033[92mjdk DateTimeFormatter(LocalDateTime)\033[m : ");
        try {
            System.out.println(dtf.format(ldt));
        } catch (Exception e) {
            System.out.println(e.getClass().getSimpleName() + "(\"" + e.getMessage() + "\")");
        }
        System.out.println(countFmt(++count) + ". \033[92mjdk DateFormat(java.util.Date)\033[m       : " + jdf.format(date));
        System.out.println(countFmt(++count) + ". icu DateFormat(LocalDateTime)        : " + df.format(ldt));
        System.out.println(countFmt(++count) + ". icu DateFormat(java.util.Date)       : " + df.format(date));
        System.out.println(countFmt(++count) + ". icu MessageFormat (LocalDateTime)    : " + mf.format(Map.of("theDate", ldt)));
        System.out.println(countFmt(++count) + ". icu MessageFormat (java.util.Date)   : " + mf.format(Map.of("theDate", date)));
        System.out.println(countFmt(++count) + ". icu MessageFormat2 (LocalDateTime)   : " + mf2.formatToString(Map.of("theDate", ldt)));
        System.out.println(countFmt(++count) + ". icu MessageFormat2 (java.util.Date)  : " + mf2.formatToString(Map.of("theDate", date)));

        // Zoned types
        System.out.println(countFmt(++count) + ". \033[92mjdk DateTimeFormatter(ZonedDateTime)\033[m : " + dtf.format(zdt));
        System.out.println(countFmt(++count) + ". icu DateFormat(ZonedDateTime)        : " + df.format(zdt));
        System.out.println(countFmt(++count) + ". icu DateFormat(icu.Calendar)         : " + df.format(cal));
        System.out.println(countFmt(++count) + ". icu DateFormat(jdk.Calendar)         : " + df.format(jcal));
        
        // MF
        System.out.println(countFmt(++count) + ". icu MessageFormat (ZonedDateTime)    : " + mf.format(Map.of("theDate", zdt)));
        System.out.println(countFmt(++count) + ". icu MessageFormat (icu.Calendar)     : " + mf.format(Map.of("theDate", cal)));
        System.out.println(countFmt(++count) + ". icu MessageFormat  (jdk.Calendar)    : " + mf.format(Map.of("theDate", jcal)));
        
        System.out.println(countFmt(++count) + ". icu MessageFormat2 (ZonedDateTime)   : " + mf2.formatToString(Map.of("theDate", zdt)));
        System.out.println(countFmt(++count) + ". icu MessageFormat2 (icu.Calendar)    : " + mf2.formatToString(Map.of("theDate", cal)));
        System.out.println(countFmt(++count) + ". icu MessageFormat2 (jdk.Calendar)    : " + mf2.formatToString(Map.of("theDate", jcal)));
    }
    
    static String countFmt(int count) {
        return String.format(" %3s", count);
    } 
}
