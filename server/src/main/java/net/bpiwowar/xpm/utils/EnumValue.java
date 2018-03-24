package net.bpiwowar.xpm.utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface EnumValue {
    String value();

    String help();
}
