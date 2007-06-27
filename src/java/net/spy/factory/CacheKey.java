// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.factory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies an alternate cache key for a given field.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheKey {
	/**
	 * The name under which this is cached.
	 */
	String name();

	CacheType type() default CacheType.SINGLE;
}
