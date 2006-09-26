// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 569EAE91-6590-492C-8158-32B8670E6D6F

package net.spy.factory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies an alternate cache key for a given field.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheKey {
	/**
	 * The name under which this is cached.
	 */
	String name();
}
