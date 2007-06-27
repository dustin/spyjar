// Copyright (c) 2006 Dustin Sallings <dustin@spy.net<
// arch-tag: 08E82D35-62F8-4715-A2AA-A1BE72D38A7C

package net.spy.util;

import java.io.Closeable;
import java.sql.Connection;

import net.spy.log.Logger;
import net.spy.log.LoggerFactory;

/**
 * CloseUtil exists to provide a safe means to close anything closeable.
 * This prevents exceptions from being thrown from within finally blocks while
 * still providing logging of exceptions that occur during close.  Exceptions
 * during the close will be logged using the spy logging infrastructure, but
 * will not be propagated up the stack.
 */
public final class CloseUtil {

	private static Logger logger=LoggerFactory.getLogger(CloseUtil.class);

    /**
     * Close a closeable.
     */
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                logger.info("Unable to close %s", closeable, e);
            }
        }
    }

    /**
     * Close a JDBC connection.
     */
    public static void close(Connection closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                logger.info("Unable to close %s", closeable, e);
            }
        }
    }

}
