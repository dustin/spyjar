// Copyright (c) 2004  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 5C7DB098-34EE-11D9-AAD0-000393CFE6B8

package net.spy.util;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.Iterator;
import java.util.IdentityHashMap;
import java.lang.reflect.Field;

import net.spy.SpyObject;
import net.spy.log.Logger;

/**
 * Dump an object and all of its contents.
 */
public class ObjectDump extends SpyObject {

	private Logger logger=null;

	/**
	 * Get an instance of ObjectDump.
	 */
	public ObjectDump() {
		super();
		logger=getLogger();
	}

	// Get all of the fields available to this class or any of its super
	// classes.
    private void getAllFields(Class c, Set s) throws Exception {
        Field fields[]=c.getDeclaredFields();
        for(int i=0; i<fields.length; i++) {
            fields[i].setAccessible(true);
            s.add(fields[i]);
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
		logger.info("Examining " + path + " - " + o.getClass() + " - " + o);
	}

	/** 
	 * Report a primitive was found.
	 * 
	 * @param path the path to the primitive
	 * @param v the value of the primitive
	 */
	protected void reportPrimitive(String path, Object v) {
		logger.info("Primitive value for " + path + " is " + v);
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
		logger.info("Saw duplicate " + o.getClass() + " instance at "
			+ path + " previously seen at " + path);
	}

	/** 
	 * Report an exception traversing an object.
	 * 
	 * @param path the path of the object
	 * @param o the object
	 * @param e the exception that occurred
	 */
	protected void reportException(String path, Object o, Exception e) {
		logger.info("Problem reading " + o.getClass() + " at " + path
			+ " - " + o, e);
	}

	// Dump this object
    private void dumpObject(Object o, String path, int depth, Map seen) {
        try {
            if(!seen.containsKey(o)) {
                seen.put(o, path);
                Class c=o.getClass();

				reportExamining(path, o);

                Set fields=new HashSet();
                getAllFields(c, fields);
                for(Iterator i=fields.iterator(); i.hasNext(); ) {
                    Field field=(Field)i.next();

                    Class fieldType=field.getType();
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
				reportDuplicate(path, (String)seen.get(o), o);
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
        dumpObject(o, "o", 0, new IdentityHashMap());
    }

}