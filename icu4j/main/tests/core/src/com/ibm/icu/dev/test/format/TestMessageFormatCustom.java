// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
/*
**********************************************************************
* Author: Mihai Nita
* Created: April 6, 2019
* Since: ICU TBD (exploratory)
**********************************************************************
*/
package com.ibm.icu.dev.test.format;

import java.text.AttributedCharacterIterator;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateIntervalFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SelectFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.DateInterval;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class TestMessageFormatCustom extends TestFmwk {

    static abstract class CustomFormatBase extends Format {
        static final long serialVersionUID = -1;
        final String style;
        final ULocale ulocale;
        final String argName;

        public CustomFormatBase(ULocale ulocale, String argName, String style) {
            this.ulocale = ulocale;
            this.argName = argName;
            this.style = style;
        }

        public final AttributedCharacterIterator formatToCharacterIterator(Object obj) {
            // Not implemented
            return null;
        }

        public final Object parseObject(String source, ParsePosition pos) {
            // Not implemented
            return null;
        }
    }

    /*
     * Example: grammatical case
     * Possibly add support for grammatical case without any deep changes in the internals
     * or the syntax of <code>MessageFormat</code>
     */

    static HashMap<String,String> srCases = new HashMap<>();
    static {
        srCases.put("sr::Larisa::nominative",   "Larisa");
        srCases.put("sr::Larisa::accusative",   "Larisu");
        srCases.put("sr::Larisa::dative",       "Larisi");
        srCases.put("sr::Larisa::instrumental", "Larisom");
        srCases.put("ro::Maria::nominative", "Maria");
        srCases.put("ro::Maria::genitive",   "Mariei");
        srCases.put("ro::Maria::dative",     "Mariei");
    }

    static class GrammarCaseFormatBuilder implements MessageFormat.CustomFormatBuilder {
        public Format build(ULocale locale, String argName, String style) {
            return new GrammarCaseFormat(locale, argName, style);
        }
    }

    static class GrammarCaseFormat extends CustomFormatBase {
        static final long serialVersionUID = -1;
        public GrammarCaseFormat(ULocale ulocale, String argName, String style) {
            super(ulocale, argName, style);
        }

        public final StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String key = ulocale.getLanguage() + "::" + obj + "::" + style;
            String tmp = srCases.get(key);
            toAppendTo.append(tmp != null ? tmp : "" + obj);
            return toAppendTo;
        }
    }

    @Test
    public void TestCustomType() {
        HashMap<String, MessageFormat.CustomFormatBuilder> formatterMap = new HashMap<>();
        formatterMap.put("grammarCase", new GrammarCaseFormatBuilder());

        String[] testStrings = {
            "sr", "Larisa", "nominative",   "Larisa",
            "sr", "Larisa", "accusative",   "Larisu",
            "sr", "Larisa", "dative",       "Larisi",
            "sr", "Larisa", "instrumental", "Larisom",
            "ro", "Maria",  "nominative",   "Maria",
            "ro", "Maria",  "genitive",     "Mariei",
            "ro", "Maria",  "dative",       "Mariei",
            "ro", "Maria",  "accusative",   "Maria",
            "es", "Maria",  "nominative",   "Maria",
            "es", "Maria",  "genitive",     "Maria",
            "es", "Maria",  "dative",       "Maria",
            "es", "Maria",  "accusative",   "Maria",
        };

        HashMap<String, Object> args = new HashMap<>();

        for (int i = 0; i < testStrings.length; i += 4) {
            String localeName = testStrings[i];
            String nameIn = testStrings[i + 1];
            String caseName = testStrings[i + 2];
            String expected = testStrings[i + 3];

            ULocale locale = ULocale.forLanguageTag(localeName);
            MessageFormat mf = new MessageFormat(
                "{name,grammarCase," + caseName + "}", locale, formatterMap);

            args.put("name", nameIn);
            assertEquals("custom format("+ nameIn + ", " + caseName + ")",
                expected, mf.format(args));
        }
    }

    /*
     * Example: user defined class & formatter
     * Formatting a user defined class (<code>Name</code>) using an <code>argStyle</code>
     * (<code>formal</code> / <code>casual</code>)
     * So we can do <code>{...,politeness,formal}</code>, very similar to the way we
     * do <code>{...,date,long}</code>
     */

    static class Name {
        final String firstName;
        final String lastName;

        public Name(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public String toString() {
            return firstName + " " + lastName;
        }
    }

    static class PoliteNameFormat extends CustomFormatBase {
        static final long serialVersionUID = -1;

        static class PoliteNameFormatBuilder implements MessageFormat.CustomFormatBuilder {
            public Format build(ULocale locale, String argName, String style) {
                return new PoliteNameFormat(locale, argName, style);
            }
        }

        public PoliteNameFormat(ULocale ulocale, String argName, String style) {
            super(ulocale, argName, style);
        }

        public final StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj instanceof Name) {
                Name name = (Name) obj;
                if ("formal".equals(style)) {
                     // This would of course should not be hardcoded.
                     // Should come from the name, might be translated, might depend on gender,
                     // and the position / spaces are locale-sensitive (think "Tanaka San")
                     // But that is not the point here, it can be refined later.
                    toAppendTo.append("Mr. " + name.lastName);
                } else {
                    toAppendTo.append(name.firstName);
                }
            } else {
                toAppendTo.append("" + obj);
            }
            return toAppendTo;
        }
    }

    @Test
    public void TestCustomTypeNames() {
        HashMap<String, MessageFormat.CustomFormatBuilder> formatterMap = new HashMap<>();
        formatterMap.put("politeness", new PoliteNameFormat.PoliteNameFormatBuilder());

        HashMap<String, Object> args = new HashMap<>();

        ULocale locale = ULocale.forLanguageTag("ja");

        MessageFormat mfFormal = new MessageFormat(
            "Hello {jaName,politeness,formal}, hello {enName,politeness,formal}",
            locale, formatterMap);
        MessageFormat mfCasual = new MessageFormat(
            "Hello {jaName,politeness,casual}, hello {enName,politeness,casual}",
            locale, formatterMap);

        Name name = new Name("Taro", "Yamada");
        args.put("jaName", name);
        args.put("enName", "John"); // not a Name object

        assertEquals("polite format("+ name + ",formal)",
            "Hello Mr. Yamada, hello John", mfFormal.format(args));

        assertEquals("polite format("+ name + ",casual)",
            "Hello Taro, hello John", mfCasual.format(args));
    }

    static class FirstLastNameFormat extends CustomFormatBase {
        static final long serialVersionUID = -1;

        static class Builder implements MessageFormat.CustomFormatBuilder {
            public Format build(ULocale locale, String argName, String style) {
                return new FirstLastNameFormat(locale, argName, style);
            }
        }

        public FirstLastNameFormat(ULocale ulocale, String argName, String style) {
            super(ulocale, argName, style);
        }

        public final StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj instanceof Name) {
                Name name = (Name) obj;
                if ("last".equals(style)) {
                    toAppendTo.append(name.lastName);
                } else {
                    toAppendTo.append(name.firstName);
                }
            } else {
                toAppendTo.append("" + obj);
            }
            return toAppendTo;
        }
    }

    /*
     * Example: works in nested messages
     * Showing that it works "deep", inside a <code>select</code> or <code>plural</code>
     * If I use {today, date, ::yMd} then
     *   DateFormat df = SimpleDateFormat.getInstanceForSkeleton("yMMMd", locale)
     *   setFormatByArgumentName("today", new DateFormat)
     * then the parameter outside the select if affected, but the ones inside are not.
     */

    static class DeepDateFormat extends CustomFormatBase {
        static final long serialVersionUID = -1;
        private final SimpleDateFormat df;

        static class Builder implements MessageFormat.CustomFormatBuilder {
            public Format build(ULocale locale, String argName, String style) {
                return new DeepDateFormat(locale, argName, style);
            }
        }

        public DeepDateFormat(ULocale ulocale, String argName, String style) {
            super(ulocale, argName, style);
            df = style.startsWith("::")
                ? (SimpleDateFormat) SimpleDateFormat.getInstanceForSkeleton(style.substring(2), ULocale.US)
                : new SimpleDateFormat(style, ULocale.US);
        }

        public final StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            toAppendTo.append("<<");
            df.format(obj, toAppendTo, pos);
            toAppendTo.append(">>");
            return toAppendTo;
        }
    }

    @Test
    public void TestCustomTypeNamesRecurse() {
        HashMap<String, MessageFormat.CustomFormatBuilder> formatterMap = new HashMap<>();
        formatterMap.put("firstLastName", new FirstLastNameFormat.Builder());
        formatterMap.put("deepDateFormat", new DeepDateFormat.Builder());

        ULocale locale = ULocale.US;

        Calendar cal = new GregorianCalendar(2019, Calendar.OCTOBER, 25);
        Date today = cal.getTime();
        MessageFormat mf = new MessageFormat(
            "Hello {gender, select,"
            + " female {Ms. {name,firstLastName,last}, today is {today, deepDateFormat, ::yMd}}"
            + "   male {Mr. {name,firstLastName,last}, today is {today, deepDateFormat, ::yMd}}"
            + "  other {{name,firstLastName,first}, today is {today, deepDateFormat, ::yMd}}"
            + "} or {today, deepDateFormat,::yMd}!",
            locale, formatterMap);

        HashMap<String, Object> args = new HashMap<>();

        args.put("today", today);

        args.put("name", new Name("Steve", "Johnson"));
        args.put("gender", "male");
        assertEquals("recursive",
            "Hello Mr. Johnson, today is <<10/25/2019>> or <<10/25/2019>>!",
            mf.format(args));

        args.put("name", new Name("Jane", "Johnson"));
        args.put("gender", "female");
        assertEquals("recursive",
            "Hello Ms. Johnson, today is <<10/25/2019>> or <<10/25/2019>>!",
            mf.format(args));

        args.put("name", new Name("Anonymous", "Johnson"));
        args.put("gender", "other");
        assertEquals("recursive",
            "Hello Anonymous, today is <<10/25/2019>> or <<10/25/2019>>!",
            mf.format(args));
    }

    /*
     * Example: implementing a selector (<code>complexArg</code>)
     * Showing that it is also easy to implement a selector kind of <code>argType</code>.
     * This implements a <code>{..., gender, ...}</code> syntax working similar to <code>select</code>
     */

    static class GenderFormat extends SelectFormat {
        static final long serialVersionUID = -1;

        static class Builder implements MessageFormat.CustomFormatBuilder {
            public Format build(ULocale locale, String argName, String style) {
                return new GenderFormat(locale, argName, style);
            }
        }

        public GenderFormat(ULocale ulocale, String argName, String style) {
            super(style);
        }
    }

    @Test
    public void TestCustomTypeComplexArgument() {
        HashMap<String, MessageFormat.CustomFormatBuilder> formatterMap = new HashMap<>();
        formatterMap.put("gender", new GenderFormat.Builder());
        ULocale locale = ULocale.US;

        MessageFormat mf = new MessageFormat(
            "{host_gender, gender,"
            + " female {She invited you}"
            + "   male {He invited you}"
            + "  other {They invited you}"
            + "}",
            locale, formatterMap);

        HashMap<String, Object> args = new HashMap<>();

        args.put("host_gender", "female");
        assertEquals("gender", "She invited you", mf.format(args));

        args.put("host_gender", "male");
        assertEquals("gender", "He invited you", mf.format(args));

        args.put("host_gender", "other");
        assertEquals("gender", "They invited you", mf.format(args));

        args.put("host_gender", "something");
        assertEquals("gender", "They invited you", mf.format(args));
    }

    static class XRefFormat extends CustomFormatBase {
        static final long serialVersionUID = -1;
        private final Properties prop;

        static class XRefFormatBuilder implements MessageFormat.CustomFormatBuilder {
            private final Properties prop;

            public XRefFormatBuilder(Properties prop) {
                this.prop = prop;
            }

            public Format build(ULocale locale, String argName, String style) {
                return new XRefFormat(locale, argName, style, prop);
            }
        }

        public XRefFormat(ULocale ulocale, String argName, String style, Properties prop) {
            super(ulocale, argName, style);
            this.prop = prop;
        }

        public final StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String argValue = (String) prop.get(argName + "." + style);
            if (argValue == null) {
                argValue = (String) prop.get(argName);
            }
            if (argValue == null) {
                String show = (obj == null) ? "{" + argName + "}" : Objects.toString(obj);
                if (show.isEmpty())
                    show = "{" + argName + "}";
                toAppendTo.append(show);
            } else {
                toAppendTo.append(argValue);
            }
            return toAppendTo;
        }
    }

    @Test
    public void TestCustomTypeXRef() {
        // Resources. This to be (or course) replaced with whatever the "resource manager"
        // is on the hosting platform. For instance Android would use Resources.getString, etc.
        Properties prop = new Properties();
        // prop.setProperty("brandName.nominative", "Konto Firefox");
        prop.setProperty("brandName", "Konto Firefox");
        prop.setProperty("brandName.genitive", "Konta Firefox");
        prop.setProperty("brandName.accusative", "Kontem Firefox");

        HashMap<String, MessageFormat.CustomFormatBuilder> formatterMap = new HashMap<>();
        formatterMap.put("xref", new XRefFormat.XRefFormatBuilder(prop));

        HashMap<String, Object> args = new HashMap<>();
        args.put("company", "unknown company"); // Used as "ultimate fallback

        ULocale locale = ULocale.forLanguageTag("pl");

        MessageFormat mfXref = new MessageFormat("Zaloguj do {brandName,xref,genitive}",
                locale, formatterMap);
        assertEquals("Cross-reference to a sting in resources",
            "Zaloguj do Konta Firefox", mfXref.format(args));

        mfXref = new MessageFormat("Zaloguj do {brandName,xref,dative}", // dative does not exist, fallback
                locale, formatterMap);
        assertEquals("Cross-reference to a sting in resources",
            "Zaloguj do Konto Firefox", mfXref.format(args));

        // TBD what we really want to do when we can't find the string in resources
        mfXref = new MessageFormat("Zaloguj do {company,xref,dative}", // company does not exist, has fallback
                locale, formatterMap);
        assertEquals("Cross-reference to a sting in resources",
            "Zaloguj do unknown company", mfXref.format(args));

        // The current result is not great. This is what we really want?
        mfXref = new MessageFormat("Zaloguj do {entity,xref,dative}", // entity does not exist
                locale, formatterMap);
        assertEquals("Cross-reference to a sting in resources",
            "Zaloguj do {entity}", mfXref.format(args));
    }

    static class UseTheNameFormat extends CustomFormatBase {
        static final long serialVersionUID = -1;

        static class UseTheNameFormatBuilder implements MessageFormat.CustomFormatBuilder {
            public Format build(ULocale locale, String argName, String style) {
                return new UseTheNameFormat(locale, argName, style);
            }
        }

        public UseTheNameFormat(ULocale ulocale, String argName, String style) {
            super(ulocale, argName, style);
        }

        public final StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (argName.startsWith("N")) { // number
                Double val = Double.valueOf(argName.substring(1).replace("_", "."));
                NumberFormat nf = NumberFormat.getInstance(ulocale);
                toAppendTo.append(nf.format(val));
            } else if (argName.startsWith("D")) { // very primitive date parsing, iso
                String [] parts = argName.substring(1).split("T", 2);
                String date = "";
                String time = "";
                if (parts.length == 2) {
                    date = parts[0];
                    time = parts[1];
                } else {
                    if (parts[0].length() == 8) { // date
                        date = parts[0];
                    } else if (parts[0].length() == 6) { // time
                        time = parts[0];
                    }
                }
                Date now = new Date();
                if (!date.isEmpty()) {
                    int year = Integer.valueOf(date.substring(0, 4));
                    int month = Integer.valueOf(date.substring(4, 6));
                    int day = Integer.valueOf(date.substring(6, 8));
                    now.setYear(year - 1900);
                    now.setMonth(month - 1);
                    now.setDate(day);
                }
                if (!time.isEmpty()) {
                    int hour = Integer.valueOf(date.substring(0, 2));
                    int minute = Integer.valueOf(date.substring(2, 4));
                    int second = Integer.valueOf(date.substring(4, 6));
                    now.setHours(hour);
                    now.setMinutes(minute);
                    now.setSeconds(second);
                }
                DateFormat df = DateFormat.getInstanceForSkeleton(style, ulocale);
                toAppendTo.append(df.format(now));
            }
            return toAppendTo;
        }
    }

    @Test
    public void TestCustomTypeNameDependent() {
        HashMap<String, MessageFormat.CustomFormatBuilder> formatterMap = new HashMap<>();
        formatterMap.put("inline", new UseTheNameFormat.UseTheNameFormatBuilder());

        HashMap<String, Object> args = new HashMap<>();

        ULocale locale = ULocale.forLanguageTag("en-IN");

        MessageFormat mfXref = new MessageFormat("This expires on {D20200923T214215,inline,yMMMdjm}",
                locale, formatterMap);
        assertEquals("Cross-reference to a sting in resources",
            "This expires on 23 Sep 2020, 8:20 pm", mfXref.format(args));

        mfXref = new MessageFormat("The population is {N123456789_9876,inline,{x:'a', b:'xxrr'}}",
                locale, formatterMap);
        assertEquals("Cross-reference to a sting in resources",
                "The population is 12,34,56,789.988", mfXref.format(args));
    }

    static class DTIntervalFormat extends CustomFormatBase {
        static final long serialVersionUID = -1;
        private final DateIntervalFormat dif;

        static class DTIntervalBuilder implements MessageFormat.CustomFormatBuilder {

            public DTIntervalBuilder() {
            }

            public Format build(ULocale locale, String argName, String style) {
                return new DTIntervalFormat(locale, argName, style);
            }
        }

        public DTIntervalFormat(ULocale ulocale, String argName, String style) {
            super(ulocale, argName, style);
            dif = DateIntervalFormat.getInstance(style, ulocale);
        }

        public final StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            // ICU axpects the obj to be DateInterval.
            // If it is not, it throws. So there is no point to test & throw here.
            dif.format(obj, toAppendTo, pos);
            return toAppendTo;
        }
    }

    @Test
    public void TestDateTimeInterval() {
        HashMap<String, MessageFormat.CustomFormatBuilder> formatterMap = new HashMap<>();
        formatterMap.put("interval", new DTIntervalFormat.DTIntervalBuilder());

        long from = new GregorianCalendar(2020, Calendar.SEPTEMBER, 12).getTimeInMillis();
        long toSameMonth = new GregorianCalendar(2020, Calendar.SEPTEMBER, 21).getTimeInMillis();
        long toSameYear = new GregorianCalendar(2020, Calendar.NOVEMBER, 13).getTimeInMillis();
        long toDifferentYear = new GregorianCalendar(2021, Calendar.FEBRUARY, 2).getTimeInMillis();

        ULocale locale = ULocale.forLanguageTag("en");

        MessageFormat mf = new MessageFormat("Offer valid {validDates, interval, ::dMMMMy}",
                locale, formatterMap);

        HashMap<String, Object> args = new HashMap<>();

        args.put("validDates", new DateInterval(from, from)); // same day
        assertEquals("Interval formatting",
            "Offer valid September 12, 2020", mf.format(args));

        args.put("validDates", new DateInterval(from, toSameMonth));
        assertEquals("Interval formatting",
            "Offer valid September 12 – 21, 2020", mf.format(args));

        args.put("validDates", new DateInterval(from, toSameYear));
        assertEquals("Interval formatting",
            "Offer valid September 12 – November 13, 2020", mf.format(args));

        args.put("validDates", new DateInterval(from, toDifferentYear));

        assertEquals("Interval formatting",
            "Offer valid September 12, 2020 – February 2, 2021", mf.format(args));
    }
}
