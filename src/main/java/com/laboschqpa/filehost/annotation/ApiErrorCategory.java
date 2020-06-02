package com.laboschqpa.filehost.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorCategory {
    String value();
}
