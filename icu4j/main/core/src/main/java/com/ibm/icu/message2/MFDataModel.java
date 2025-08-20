// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import com.ibm.icu.impl.number.parse.ValidationMatcher;
import com.ibm.icu.text.MessagePatternUtil;
import com.ibm.icu.text.StringPrepParseException;
import jdk.nashorn.api.tree.FunctionExpressionTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This maps closely to the official specification.
 * Since it is not final, we will not add javadoc everywhere.
 *
 * <p>See <a target="github" href="https://github.com/unicode-org/message-format-wg/blob/main/spec/data-model/README.md">the
 * latest description</a>.</p>
 *
 * @internal ICU 72 technology preview
 * @deprecated This API is for technology preview only.
 */
@Deprecated
@SuppressWarnings("javadoc")
public class MFDataModel {

    private MFDataModel() {
        // Prevent instantiation
    }

    // Messages

    /**
     * Provides a common type for {@link PatternMessage} and {@link SelectMessage}.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public interface Message {
        default Type getType() {
            throw new UnsupportedOperationException();
        }
        default PatternMessage getAsPatternMessage() {
            throw new UnsupportedOperationException();
        }
        default SelectMessage getAsSelectMessage() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class PatternMessage implements Message {
        public final List<Declaration> declarations;
        public final Pattern pattern;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public PatternMessage(List<Declaration> declarations, Pattern pattern) {
            this.declarations = List.copyOf(declarations);
            this.pattern = pattern;
        }

        @Override
        public Type getType() {
            return Type.PATTERN_MESSAGE;
        }
        @Override
        public PatternMessage getAsPatternMessage() {
            return this;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class SelectMessage implements Message {
        public final List<Declaration> declarations;
        public final List<Expression> selectors;
        public final List<Variant> variants;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public SelectMessage(
                List<Declaration> declarations,
                List<Expression> selectors,
                List<Variant> variants) {
            this.declarations = List.copyOf(declarations);
            this.selectors = List.copyOf(selectors);
            this.variants = List.copyOf(variants);
        }

        @Override
        public Type getType() {
            return Type.SELECT_MESSAGE;
        }
        @Override
        public SelectMessage getAsSelectMessage() {
            return this;
        }
    }

    /**
     * Provides a common type for {@link InputDeclaration}, and {@link LocalDeclaration}.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public interface Declaration {
        default Type getType() {
            throw new UnsupportedOperationException();
        }
        default InputDeclaration getAsInputDeclaration() {
            throw new UnsupportedOperationException();
        }
        default LocalDeclaration getAsLocalDeclaration() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class InputDeclaration implements Declaration {
        public final String name;
        public final VariableExpression value;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public InputDeclaration(String name, VariableExpression value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public Type getType() {
            return Type.INPUT_DECLARATION;
        }
        @Override
        public InputDeclaration getAsInputDeclaration() {
            return this;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class LocalDeclaration implements Declaration {
        public final String name;
        public final Expression value;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public LocalDeclaration(String name, Expression value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public Type getType() {
            return Type.LOCAL_DECLARATION;
        }
        @Override
        public LocalDeclaration getAsLocalDeclaration() {
            return this;
        }
    }

    /**
     * Provides a common type for the selection keys: {@link Variant},
     * {@link Literal}, or {@link CatchallKey}.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public interface LiteralOrCatchallKey {
        default Type getType() {
            throw new UnsupportedOperationException();
        }
        default Variant getAsVariant() {
            throw new UnsupportedOperationException();
        }
        default Literal getAsLiteral() {
            throw new UnsupportedOperationException();
        }
        default CatchallKey getAsCatchallKey() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class Variant implements LiteralOrCatchallKey {
        public final List<LiteralOrCatchallKey> keys;
        public final Pattern value;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Variant(List<LiteralOrCatchallKey> keys, Pattern value) {
            this.keys = List.copyOf(keys);
            this.value = value;
        }

        @Override
        public Type getType() {
            return Type.VARIANT;
        }
        @Override
        public Variant getAsVariant() {
            return this;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class CatchallKey implements LiteralOrCatchallKey {
        final static String AS_KEY_STRING = "<<::CatchallKey::>>";
        // String value; // Always '*' in MF2

        public static boolean isCatchAll(String key) {
            return AS_KEY_STRING.equals(key);
        }

        @Override
        public Type getType() {
            return Type.CATCH_ALL;
        }
        @Override
        public CatchallKey getAsCatchallKey() {
            return this;
        }
    }

    // Patterns

    // type Pattern = Array<string | Expression | Markup>;
    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class Pattern {
        public final List<PatternPart> parts;

        Pattern() {
            this.parts = new ArrayList<>();
        }
    }

    /**
     * Provides a common type for {@link StringPart} and {@link Expression}.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public interface PatternPart {
        default Type getType() {
            throw new UnsupportedOperationException();
        }
        default StringPart getAsStringPart() {
            throw new UnsupportedOperationException();
        }
        default Expression getAsExpression() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class StringPart implements PatternPart {
        public final String value;

        StringPart(String value) {
            this.value = value;
        }

        @Override
        public Type getType() {
            return Type.STRING_PART;
        }
        @Override
        public StringPart getAsStringPart() {
            return this;
        }
    }

    /**
     * Provides a common type for all kinds of expressions:
     * {@link LiteralExpression}, {@link VariableExpression}, {@link FunctionExpression},
     * {@link Markup}.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public interface Expression extends PatternPart {
        default LiteralExpression getAsLiteralExpression() {
            throw new UnsupportedOperationException();
        }
        default VariableExpression getAsVariableExpression() {
            throw new UnsupportedOperationException();
        }
        default FunctionExpression getAsFunctionExpression() {
            throw new UnsupportedOperationException();
        }
        default Markup getAsMarkup() {
            throw new UnsupportedOperationException();
        }

        default Type getType() {
            return Type.EXPRESSION;
        }
        @Override
        default Expression getAsExpression() {
            return this;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class LiteralExpression implements Expression {
        public final Literal arg;
        public final Function function;
        public final List<Attribute> attributes;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public LiteralExpression(Literal arg, Function function, List<Attribute> attributes) {
            this.arg = arg;
            this.function = function;
            this.attributes = List.copyOf(attributes);
        }

        @Override
        public Type getType() {
            return Type.LITERAL_EXPRESSION;
        }
        @Override
        public LiteralExpression getAsLiteralExpression() {
            return this;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class VariableExpression implements Expression {
        public final VariableRef arg;
        public final Function function;
        public final List<Attribute> attributes;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public VariableExpression(
                VariableRef arg, Function function, List<Attribute> attributes) {
            this.arg = arg;
            this.function = function;
            this.attributes = List.copyOf(attributes);
        }

        @Override
        public Type getType() {
            return Type.VARIABLE_EXPRESSION;
        }
        @Override
        public VariableExpression getAsVariableExpression() {
            return this;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class Function {
        public final String name;
        public final Map<String, Option> options;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Function(String name, Map<String, Option> options) {
            this.name = name;
            this.options = Map.copyOf(options);
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class FunctionExpression implements Expression {
        public final Function function;
        public final List<Attribute> attributes;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public FunctionExpression(Function function, List<Attribute> attributes) {
            this.function = function;
            this.attributes = List.copyOf(attributes);
        }

        @Override
        public Type getType() {
            return Type.FUNCTION_EXPRESSION;
        }
        @Override
        public FunctionExpression getAsFunctionExpression() {
            return this;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class Attribute {
        public final String name;
        public final LiteralOrVariableRef value;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Attribute(String name, LiteralOrVariableRef value) {
            this.name = name;
            this.value = value;
        }
    }

    // Expressions

    /**
     * Provides a common type for {@link Literal} and {@link VariableRef},
     * to represent things like {@code foo} / {@code |foo|} / {@code 1234} (literals)
     * and {@code $foo} ({@code VariableRef}), as argument for placeholders or value in options.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public interface LiteralOrVariableRef {
        default Type getType() {
            throw new UnsupportedOperationException();
        }
        default Literal getAsLiteral() {
            throw new UnsupportedOperationException();
        }
        default VariableRef getAsVariableRef() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class Literal implements LiteralOrVariableRef, LiteralOrCatchallKey {
        public final String value;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Literal(String value) {
            this.value = value;
        }

        @Override
        public Type getType() {
            return Type.LITERAL;
        }
        @Override
        public Literal getAsLiteral() {
            return this;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class VariableRef implements LiteralOrVariableRef {
        public final String name;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public VariableRef(String name) {
            this.name = name;
        }

        @Override
        public Type getType() {
            return Type.VARIABLE_REF;
        }
        @Override
        public VariableRef getAsVariableRef() {
            return this;
        }
    }

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class Option {
        public final String name;
        public final LiteralOrVariableRef value;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Option(String name, LiteralOrVariableRef value) {
            this.name = name;
            this.value = value;
        }
    }

    // Markup

    /**
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class Markup implements Expression {
        enum Kind {
            OPEN,
            CLOSE,
            STANDALONE
        }

        public final Kind kind;
        public final String name;
        public final Map<String, Option> options;
        public final List<Attribute> attributes;

        /**
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Markup(
                Kind kind, String name, Map<String, Option> options, List<Attribute> attributes) {
            this.kind = kind;
            this.name = name;
            this.options = Map.copyOf(options);
            this.attributes = List.copyOf(attributes);
        }

        @Override
        public Type getType() {
            return Type.MARKUP;
        }
        @Override
        public Markup getAsMarkup() {
            return this;
        }
    }

    public enum Type {
        PATTERN_MESSAGE, SELECT_MESSAGE,
        INPUT_DECLARATION, LOCAL_DECLARATION,
        VARIANT, LITERAL, CATCH_ALL, VARIABLE_REF,
        STRING_PART, EXPRESSION,
        LITERAL_EXPRESSION, VARIABLE_EXPRESSION, FUNCTION_EXPRESSION, MARKUP
    }
}
