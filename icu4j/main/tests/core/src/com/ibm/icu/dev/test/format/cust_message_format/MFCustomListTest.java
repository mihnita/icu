// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
/*
**********************************************************************
* Author: Mihai Nita
* Created: October 20, 2020
* Since: ICU TBD (exploratory)
**********************************************************************
*/
package com.ibm.icu.dev.test.format.cust_message_format;

import java.text.FieldPosition;
import java.text.Format;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.ListFormatter;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class MFCustomListTest extends TestFmwk {

    /*
     * Example: list formatting
     */

    static class CustomListFormat extends CustomFormatBase {
        static final long serialVersionUID = -1;
        private final ListFormatter formatter;

        static class Builder implements MessageFormat.CustomFormatBuilder {
            @Override
            public Format build(ULocale locale, String argName, String style) {
                return new CustomListFormat(locale, argName, style);
            }
        }

        public CustomListFormat(ULocale ulocale, String argName, String style) {
            super(ulocale, argName, style);
            ListFormatter.Style realStyle = ListFormatter.Style.valueOf(style.toUpperCase(Locale.US).trim());
            formatter = ListFormatter.getInstance(ulocale, realStyle);
        }

        @Override
        public final StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj instanceof List) {
                toAppendTo.append(formatter.format((List) obj));
            } else {
                toAppendTo.append(formatter.format(obj));
            }
            return toAppendTo;
        }
    }

    @Test
    public void TestCustomList() {
        HashMap<String, MessageFormat.CustomFormatBuilder> formatterMap = new HashMap<>();
        formatterMap.put("list", new CustomListFormat.Builder());

        ULocale locale = ULocale.forLanguageTag("en");

        int poolCount = 2;
        int restaurantCount = 5;
        int beachCount = 1;
        int golfCount = 0;
        String mainMessage = "{features, select," + " none {This resort has absolutely nothing!}"
                + "other {This all-inclusive resort includes {listOfFeatures, list, STANDARD}.}}";
        String featurePools = "{poolCount, plural, =0 {no pools} one {# pool} other {# pools}}";
        String featureRestaurants = "{restaurantCount, plural, =0 {no restaurants} one {# restaurant} other {# restaurants}}";
        String featureBeaches = "{beachCount, plural, =0 {no beaches} one {# beach} other {# beaches}}";
        String featureGolf = "{golfCount, plural, =0 {no golf courses} one {# golf course} other {# golf courses}}";

        MessageFormat mf = new MessageFormat(mainMessage, locale, formatterMap);

        HashMap<String, Object> args = new HashMap<>();
        List<String> theList = new ArrayList<>();
        args.put("listOfFeatures", theList);
        args.put("poolCount", poolCount);
        args.put("restaurantCount", restaurantCount);
        args.put("beachCount", beachCount);
        args.put("golfCount", golfCount);

        args.put("features", theList.isEmpty() ? "none" : "other");
        assertEquals("List formatting", "This resort has absolutely nothing!", mf.format(args));

        if (poolCount > 0) {
            theList.add(MessageFormat.format(featurePools, args));
        }
        if (restaurantCount > 0) {
            theList.add(MessageFormat.format(featureRestaurants, args));
        }
        if (beachCount > 0) {
            theList.add(MessageFormat.format(featureBeaches, args));
        }
        if (golfCount > 0) {
            theList.add(MessageFormat.format(featureGolf, args));
        }
        args.put("features", theList.isEmpty() ? "none" : "other");

        assertEquals("List formatting", "This all-inclusive resort includes 2 pools, 5 restaurants, and 1 beach.",
                mf.format(args));

        // We want to say "no golf courses"
        theList.add(MessageFormat.format(featureGolf, args));
        assertEquals("List formatting",
                "This all-inclusive resort includes 2 pools, 5 restaurants, 1 beach, and no golf courses.",
                mf.format(args));
    }

    @Test
    public void TestCustomList2() {
        HashMap<String, MessageFormat.CustomFormatBuilder> formatterMap = new HashMap<>();
        formatterMap.put("list", new CustomListFormat.Builder());

        ULocale locale = ULocale.forLanguageTag("en-EG");

        String mainMessage = "Week-end days: {weekEnd, list, UNIT_SHORT}";

        MessageFormat mf = new MessageFormat(mainMessage, locale, formatterMap);
        List<String> theList = new ArrayList<>();

        // Collect all week-end days in a list
        Calendar cal = Calendar.getInstance(locale);
        String[] weekDays = new DateFormatSymbols(locale).getWeekdays();
        for (int i = 1; i < weekDays.length; i++) {
            if (cal.getDayOfWeekType(i) == Calendar.WEEKEND) {
                theList.add(weekDays[i]);
            }
        }

        HashMap<String, Object> args = new HashMap<>();
        args.put("weekEnd", theList);

        assertEquals("List formatting",
            "Week-end days: Friday, Saturday", mf.format(args));
    }
}
