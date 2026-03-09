package com.das.skillmatrix.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogActivity {

    // The action name, e.g. "CREATE_CAREER", "DELETE_TEAM", "LOGIN"
    String action();

    // The entity type, e.g. "CAREER", "DEPARTMENT", "TEAM", "USER"
    String entityType();
}