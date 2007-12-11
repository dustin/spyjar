// Copyright (c) 2004  Dustin Sallings <dustin@spy.net>

package net.spy.util;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import net.spy.SpyObject;
import net.spy.log.Logger;

/**
 * Dump an object and all of its contents.
 */
public class ObjectDump extends SpyObject {

	private final Logger logger;

	/**
	 * Get an instance of ObjectDump.
	 */
	public ObjectDump() {
		super();
		logger=getLogger();
	}

	// Get all of the fields available to this class or any of its super
	// classes.
    private void getAllFields(Class<?> c, Set<Field> s) throws Exception {
        Field[] fields=c.getDeclaredFields();
		for(Field field : fields) {
            field.setAccessible(true);
            s.add(field);
        }
        if(c.getSuperclass() != null) {
            getAllFields(c.getSuperclass(), s);
        }
    }

	/**
	 * Report that the given object is being examined.
	 *
	 * @param path the path to the object
	 * @param o the object
	 */
	protected void reportExamining(String path, Object o) {
		logger.info("Examining %s - %s - %s", path, o.getClass(), o);
	}

	/**
	 * Report a primitive was found.
	 *
	 * @param path the path to the primitive
	 * @param v the value of the primitive
	 */
	protected void reportPrimitive(String path, Object v) {
		logger.info("Primitive value for %s is %s", path, v);
	}

	/**
	 * Report a path's value is null.
	 *
	 * @param path the path
	 */
	protected void reportNull(String path) {
		logger.info(path + " is null");
	}

	/**
	 * Report a duplicate object at the given path.
	 *
	 * @param path the path of the duplicate
	 * @param prevPath the first seen path of this object
	 * @param o the object itself
	 */
	protected void reportDuplicate(String path, String prevPath, Object o) {
		logger.info("Saw duplicate %s instance at %s previously seen at %s",
				o.getClass(), path, prevPath);
	}

	/**
	 * Report an exception traversing an object.
	 *
	 * @param path the path of the object
	 * @param o the object
	 * @param e the exception that occurred
	 */
	protected void reportException(String path, Object o, Exception e) {
		logger.info("Problem reading %s at %s - %s", o.getClass(), path, o, e);
	}

	// Dump this object
    private void dumpObject(Object o, String path, int depth,
		Map<Object, String> seen) {
        try {
            if(!seen.containsKey(o)) {
                seen.put(o, path);
                Class<? extends Object> c=o.getClass();

				reportExamining(path, o);

                Set<Field> fields=new HashSet<Field>();
                getAllFields(c, fields);
				for(Field field : fields) {
                    Class<?> fieldType=field.getType();
                    String fieldName=field.getName();

                    String thisPath=path + "." + fieldName;

                    if(fieldType.isPrimitive()) {
						reportPrimitive(thisPath, field.get(o));
                    } else {
                        Object fieldValue=field.get(o);
                        if(fieldValue == null) {
							reportNull(thisPath);
                        } else {
                            dumpObject(fieldValue, thisPath, depth+1, seen);
                        }
                    }
                }
            } else {
				reportDuplicate(path, seen.get(o), o);
            }
        } catch(Exception e) {
			reportException(path, o, e);
        }
    }

    /**
     * Dump the given object.
     */
    public void dumpObject(Object o) {
		if(o == null) {
			throw new NullPointerException("Cannot dump a null object.");
		}
        dumpObject(o, "o", 0, new IdentityHashMap<Object, String>());
    }

}
