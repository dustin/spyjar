package net.spy.concurrent;

/**
 * Interface that defines a function that transforms a value of a certain type
 * to a value of another type.
 */
public interface Transformer<F, T> {

	/**
	 * Transform the given value.
	 */
	T transform(F in);
}
