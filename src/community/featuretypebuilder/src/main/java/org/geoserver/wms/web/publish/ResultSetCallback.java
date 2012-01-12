package org.geoserver.wms.web.publish;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author thanthese
 */
public interface ResultSetCallback {
    Object fn(ResultSet rs) throws SQLException;
}