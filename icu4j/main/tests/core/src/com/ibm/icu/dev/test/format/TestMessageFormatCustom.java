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
import java.text.AttributedString;
import java.text.ChoiceFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.MessagePattern;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SelectFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class TestMessageFormatCustom extends TestFmwk {

    static abstract class CustomFormatBase extends Format {
        static final long serialVersionUID = -1;
        final String style;
        final ULocale ulocale;

        public CustomFormatBase(ULocale ulocale, String style) {
            this.ulocale = ulocale;
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
        public Format build(ULocale locale, String style) {
            return new GrammarCaseFormat(locale, style);
        }
    }

    static class GrammarCaseFormat extends CustomFormatBase {
        static final long serialVersionUID = -1;
        public GrammarCaseFormat(ULocale ulocale, String style) {
            super(ulocale, style);
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
            public Format build(ULocale locale, String style) {
                return new PoliteNameFormat(locale, style);
            }
        }

        public PoliteNameFormat(ULocale ulocale, String style) {
            super(ulocale, style);
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
            public Format build(ULocale locale, String style) {
                return new FirstLastNameFormat(locale, style);
            }
        }

        public FirstLastNameFormat(ULocale ulocale, String style) {
            super(ulocale, style);
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
            public Format build(ULocale locale, String style) {
                return new DeepDateFormat(locale, style);
            }
        }

        public DeepDateFormat(ULocale ulocale, String style) {
            super(ulocale, style);
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
            public Format build(ULocale locale, String style) {
                return new GenderFormat(locale, style);
            }
        }

        public GenderFormat(ULocale ulocale, String style) {
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
}
