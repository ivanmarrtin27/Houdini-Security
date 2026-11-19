package com.houdinisecurity.ui;

import java.util.Locale;
import java.util.ResourceBundle;

public final class I18n {

    private ResourceBundle bundle;

    public I18n() {
        setLocale(Locale.forLanguageTag("es"));
    }

    public void setLocale(Locale locale) {
        this.bundle = ResourceBundle.getBundle("i18n.messages", locale);
    }

    public String t(String key) {
        if (bundle.containsKey(key)) {
            return bundle.getString(key);
        }
        return key;
    }

    public String t(String key, Object... args) {
        return String.format(t(key), args);
    }

    public ResourceBundle bundle() {
        return bundle;
    }
}
