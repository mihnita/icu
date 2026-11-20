// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import com.ibm.icu.impl.FormattedStringBuilder;
import com.ibm.icu.impl.SimpleFormatterImpl;
import com.ibm.icu.impl.number.range.PrefixInfixSuffixLengthHelper;
import com.ibm.icu.util.ICUException;
import java.text.Format.Field;

/**
 * The second primary implementation of {@link Modifier}, this one consuming a {@link
 * com.ibm.icu.text.SimpleFormatter} pattern.
 */
public class SimpleModifier implements Modifier {
    private final String compiledPattern;
    private final Field field;
    private final boolean strong;

    // Parameters: used for number range formatting
    private final Parameters parameters;

    /** TODO: This is copied from SimpleFormatterImpl. */
    private static final int ARG_NUM_LIMIT = 0x100;

    /** Creates a modifier that uses the SimpleFormatter string formats. */
    public SimpleModifier(String compiledPattern, Field field, boolean strong) {
        this(compiledPattern, field, strong, null);
    }

    /** Creates a modifier that uses the SimpleFormatter string formats. */
    public SimpleModifier(
            String compiledPattern, Field field, boolean strong, Parameters parameters) {
        assert compiledPattern != null;
        this.compiledPattern = compiledPattern;
        this.field = field;
        this.strong = strong;
        this.parameters = parameters;
    }

    @Override
    public int apply(FormattedStringBuilder output, int leftIndex, int rightIndex) {
        return formatPrefixSuffix(compiledPattern, field, leftIndex, rightIndex, output);
    }

    /**
     * Special case for using FormattedStringBuilder with patterns with 0 or 1 argument.
     *
     * <p>With 1 argument, treat the current contents of the FormattedStringBuilder between start
     * and end as the argument {0}. Insert the extra strings from compiledPattern to surround the
     * argument in the output.
     *
     * <p>With 0 arguments, overwrite the entire contents of the FormattedStringBuilder between
     * start and end.
     *
     * @param compiledPattern Compiled form of a pattern string.
     * @param field Field to use when adding chars to the output.
     * @param start The start index of the argument already in the output string.
     * @param end The end index of the argument already in the output string.
     * @param output Destination for formatted output.
     * @return Net number of characters added to the formatted string.
     */
    public static int formatPrefixSuffix(
            String compiledPattern,
            Field field,
            int start,
            int end,
            FormattedStringBuilder output) {
        int argLimit = SimpleFormatterImpl.getArgumentLimit(compiledPattern);
        if (argLimit == 0) {
            // No arguments in compiled pattern; overwrite the entire segment with our string.
            return output.splice(start, end, compiledPattern, 2, compiledPattern.length(), field);
        } else {
            assert argLimit == 1;
            int suffixOffset;
            int length = 0;
            if (compiledPattern.charAt(1) != '\u0000') {
                int prefixLength = compiledPattern.charAt(1) - ARG_NUM_LIMIT;
                length = output.insert(start, compiledPattern, 2, 2 + prefixLength, field);
                suffixOffset = 3 + prefixLength;
            } else {
                suffixOffset = 2;
            }
            if (suffixOffset < compiledPattern.length()) {
                int suffixLength = compiledPattern.charAt(suffixOffset) - ARG_NUM_LIMIT;
                length +=
                        output.insert(
                                end + length,
                                compiledPattern,
                                1 + suffixOffset,
                                1 + suffixOffset + suffixLength,
                                field);
            }
            return length;
        }
    }

    @Override
    public int getPrefixLength() {
        return SimpleFormatterImpl.getPrefixLength(compiledPattern);
    }

    @Override
    public int getCodePointCount() {
        return SimpleFormatterImpl.getLength(compiledPattern, true);
    }

    @Override
    public boolean isStrong() {
        return strong;
    }

    @Override
    public boolean containsField(Field field) {
        // This method is not currently used.
        assert false;
        return false;
    }

    @Override
    public Parameters getParameters() {
        return parameters;
    }

    @Override
    @SuppressWarnings("ReferenceEquality")
    public boolean strictEquals(Modifier other) {
        if (!(other instanceof SimpleModifier)) {
            return false;
        }
        SimpleModifier _other = (SimpleModifier) other;
        return compiledPattern.equals(_other.compiledPattern)
                && field == _other.field
                && strong == _other.strong;
    }

    /**
     * TODO: Like above, this belongs with the rest of the SimpleFormatterImpl code. I put it here
     * so that the SimpleFormatter uses in FormattedStringBuilder are near each other.
     *
     * <p>Applies the compiled two-argument pattern to the FormattedStringBuilder.
     *
     * <p>This method is optimized for the case where the prefix and suffix are often empty, such as
     * in the range pattern like "{0}-{1}".
     */
    public static void formatTwoArgPattern(
            String compiledPattern,
            FormattedStringBuilder result,
            int index,
            PrefixInfixSuffixLengthHelper h,
            Field field) {
        int argLimit = SimpleFormatterImpl.getArgumentLimit(compiledPattern);
        if (argLimit != 2) {
            throw new ICUException();
        }
        int offset = 1; // offset into compiledPattern
        int length = 0; // chars added to result

        int prefixLength = compiledPattern.charAt(offset);
        offset++;
        if (prefixLength < ARG_NUM_LIMIT) {
            // No prefix
            prefixLength = 0;
        } else {
            prefixLength -= ARG_NUM_LIMIT;
            result.insert(index + length, compiledPattern, offset, offset + prefixLength, field);
            offset += prefixLength;
            length += prefixLength;
            offset++;
        }

        int infixLength = compiledPattern.charAt(offset);
        offset++;
        if (infixLength < ARG_NUM_LIMIT) {
            // No infix
            infixLength = 0;
        } else {
            infixLength -= ARG_NUM_LIMIT;
            result.insert(index + length, compiledPattern, offset, offset + infixLength, field);
            offset += infixLength;
            length += infixLength;
            offset++;
        }

        int suffixLength;
        if (offset == compiledPattern.length()) {
            // No suffix
            suffixLength = 0;
        } else {
            suffixLength = compiledPattern.charAt(offset) - ARG_NUM_LIMIT;
            offset++;
            result.insert(index + length, compiledPattern, offset, offset + suffixLength, field);
            length += suffixLength;
        }

        h.lengthPrefix = prefixLength;
        h.lengthInfix = infixLength;
        h.lengthSuffix = suffixLength;
    }
}
