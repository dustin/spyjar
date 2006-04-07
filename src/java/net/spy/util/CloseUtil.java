// Copyright (c) 2006 Dustin Sallings <dustin@spy.net<
// arch-tag: 08E82D35-62F8-4715-A2AA-A1BE72D38A7C

package net.spy.util;

import java.io.Closeable;
import java.sql.Connection;

import net.spy.db.DBSPLike;
import net.spy.log.Logger;
import net.spy.log.LoggerFactory;

/**
 * Object closer.
 */
public class CloseUtil {

	private static Logger logger=LoggerFactory.getLogger(CloseUtil.class);

    /**
     * Close a closeable.
     */
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                logger.info("Unable to close " + closeable, e);
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
                logger.info("Unable to close " + closeable, e);
            }
        }
    }

    /**
     * Close a DBSP instance.
     */
    public static void close(DBSPLike closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                logger.info("Unable to close " + closeable, e);
            }
        }
    }

}
