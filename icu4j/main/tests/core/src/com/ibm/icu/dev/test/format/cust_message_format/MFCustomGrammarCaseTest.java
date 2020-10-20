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
import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class MFCustomGrammarCaseTest extends TestFmwk {

    /*
     * Example: grammatical case
     * Possibly add support for grammatical case without any deep changes in the internals
     * or the syntax of <code>MessageFormat</code>
     */

    static HashMap<String, String> srCases = new HashMap<>();

    static {
        srCases.put("sr::Larisa::nominative",   "Larisa");
        srCases.put("sr::Larisa::accusative",   "Larisu");
        srCases.put("sr::Larisa::dative",       "Larisi");
        srCases.put("sr::Larisa::instrumental", "Larisom");
        srCases.put("ro::Maria::nominative", "Maria");
        srCases.put("ro::Maria::genitive",   "Mariei");
        srCases.put("ro::Maria::dative",     "Mariei");
    }

    static class GrammarCaseFormat extends CustomFormatBase {
        static final long serialVersionUID = -1;

        static class Builder implements MessageFormat.CustomFormatBuilder {
            @Override
            public Format build(ULocale locale, String argName, String style) {
                return new GrammarCaseFormat(locale, argName, style);
            }
        }

        public GrammarCaseFormat(ULocale ulocale, String argName, String style) {
            super(ulocale, argName, style);
        }

        @Override
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
        formatterMap.put("grammarCase", new GrammarCaseFormat.Builder());

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
}
