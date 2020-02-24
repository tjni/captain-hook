package com.github.tjni.captainhook.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotates an element whose visibility is relaxed for testing.
 *
 * <p>The idea for this annotation comes from the Guava library.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface VisibleForTesting {}
