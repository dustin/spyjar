// Copyright (c) 2007 Dustin Sallings <dustin@spy.net>
package net.spy.concurrent;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import net.spy.util.RingBuffer;

/**
 * Execution exception thrown from a RetryableCallable get() when a failure
 * has occurred.  This contains at most, the ten most recent exceptions.
 */
public class CompositeExecutorException extends ExecutionException {

	private static final int MAX_EXCEPTIONS = 10;

	private final Collection<ExecutionException> exceptions
		=new RingBuffer<ExecutionException>(MAX_EXCEPTIONS);

	public CompositeExecutorException(ExecutionException e) {
		super("Too many failures");
		exceptions.add(e);
	}

	public CompositeExecutorException(
			Collection<? extends ExecutionException> e) {
		super("Too many failures");
		exceptions.addAll(e);
	}

	/**
	 * Add an exception to this composite executor exception.
	 */
	void addException(ExecutionException e) {
		exceptions.add(e);
	}

	/**
	 * Get the execution exceptions that led up to this
	 * CompositeExecutorException.
	 */
	public Collection<ExecutionException> getExceptions() {
		return exceptions;
	}

	@Override
	public void printStackTrace(PrintStream p) {
		super.printStackTrace(p);
		for(ExecutionException e : exceptions) {
			try {
				p.write("Also caused by: ".getBytes());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace(p);
		}
	}

	@Override
	public void printStackTrace(PrintWriter p) {
		super.printStackTrace(p);
		for(ExecutionException e : exceptions) {
			p.print("Also caused by: ");
			e.printStackTrace(p);
		}
	}
}
