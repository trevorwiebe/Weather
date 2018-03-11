package com.trevorwiebe.weather.utils;

import java.util.Locale;

/**
 * Created by thisi on 1/27/2018.
 */

public class GetDeviceLocale {
    public static GetDeviceLocale Imperial = new GetDeviceLocale();
    public static GetDeviceLocale Metric = new GetDeviceLocale();

    public static GetDeviceLocale getDefault() {
        return getFrom(Locale.getDefault());
    }

    public static GetDeviceLocale getFrom(Locale locale) {
        String countryCode = locale.getCountry();
        if ("US".equals(countryCode)) return Imperial; // USA
        if ("LR".equals(countryCode)) return Imperial; // liberia
        if ("MM".equals(countryCode)) return Imperial; // burma
        return Metric;
    }
}
