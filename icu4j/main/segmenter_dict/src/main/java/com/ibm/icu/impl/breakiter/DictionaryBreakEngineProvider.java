// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.impl.breakiter;

import com.ibm.icu.lang.UScript;
import java.io.IOException;
import java.util.MissingResourceException;

/**
 * Dynamically loaded factory for dictionary and ML/LSTM {@link LanguageBreakEngine} instances.
 *
 * @internal
 */
public final class DictionaryBreakEngineProvider implements LanguageBreakEngine.Factory {
    public DictionaryBreakEngineProvider() {}

    @Override
    public LanguageBreakEngine create(int script) throws IOException {
        switch (script) {
            case UScript.THAI:
                try {
                    return LSTMBreakEngine.create(script, LSTMBreakEngine.createData(script));
                } catch (MissingResourceException e) {
                    return new ThaiBreakEngine();
                }
            case UScript.LAO:
                return new LaoBreakEngine();
            case UScript.MYANMAR:
                try {
                    return LSTMBreakEngine.create(script, LSTMBreakEngine.createData(script));
                } catch (MissingResourceException e) {
                    return new BurmeseBreakEngine();
                }
            case UScript.KHMER:
                return new KhmerBreakEngine();
            case UScript.HAN:
                return new CjkBreakEngine(false);
            case UScript.HANGUL:
                return new CjkBreakEngine(true);
            default:
                return null;
        }
    }
}
