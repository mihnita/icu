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

import java.text.AttributedCharacterIterator;
import java.text.Format;
import java.text.ParsePosition;

import com.ibm.icu.util.ULocale;

// Convenience class to reduce implementation effort for new formatters
abstract class CustomFormatBase extends Format {
    static final long serialVersionUID = -1;
    final String style;
    final ULocale ulocale;
    final String argName;

    public CustomFormatBase(ULocale ulocale, String argName, String style) {
        this.ulocale = ulocale;
        this.argName = argName;
        this.style = style;
    }

    @Override
    public final AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        // Not implemented
        return null;
    }

    @Override
    public final Object parseObject(String source, ParsePosition pos) {
        // Not implemented
        return null;
    }
}