package com.github.tjni.captainhook.dagger.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Scope;

/** A type that should be instantiated once per Gradle project. */
@Scope
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface ProjectScope {}
