package com.quantalabs.jamusync.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * LanguageManager handles multi-language (i18n) support for the app.
 *
 * All the UI text lives in ".properties" files inside the i18n folder:
 *   - messages_en.properties  (English)
 *   - messages_id.properties  (Indonesian)
 *   - messages_jw.properties  (Javanese)
 *
 * A ResourceBundle is Java's built-in tool for this: you give it a language
 * (a Locale) and it loads the matching file. Then getString("some.key") gives
 * back the translated text for that language.
 *
 * Everything here is "static" so any screen can simply call
 * LanguageManager.getString("login.title") without creating an object first.
 */
public class LanguageManager {

    // The shared name of all our files, WITHOUT the "_en" / "_id" / "_jw" part.
    // ResourceBundle adds that part automatically based on the chosen language.
    private static final String BUNDLE_BASE = "com.quantalabs.jamusync.i18n.messages";

    // The language currently in use. We start in English by default.
    private static Locale currentLocale = Locale.of("en");

    // The loaded set of translated texts for the current language.
    private static ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE, currentLocale);

    /**
     * Change the app language. Pass "en", "id", or "jw".
     * This reloads the ResourceBundle so getString() returns the new language.
     */
    public static void setLanguage(String langCode) {
        currentLocale = Locale.of(langCode);
        bundle = ResourceBundle.getBundle(BUNDLE_BASE, currentLocale);
    }

    /**
     * Look up the translated text for a key (e.g. "login.title").
     * If the key is missing we return the key itself instead of crashing,
     * so a forgotten translation is easy to spot on screen.
     */
    public static String getString(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /** Return the Locale (language) currently in use. */
    public static Locale getCurrentLocale() {
        return currentLocale;
    }
}
