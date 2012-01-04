package com.geocent.featuretypebuilder;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A collection of those commonly-used database operations. Shouldn't
 * contain primitive operations.
 *
 * @author thanthese
 */
public class DbUtils {

  private DbUtils() { throw new AssertionError(); }  // don't instantiate

  public static void deleteTable(String tablename) {
    Db.update(String.format("drop table %s;", tablename));
  }

  public static boolean isTableExists(String tablename) {
    String query = String.format(
        "SELECT count(table_name) as count "
        + "FROM information_schema.tables "
        + "WHERE table_schema = 'public' "
        + "AND table_name = '%s';", tablename);
    return (Boolean) Db.query(query, new ResultSetCallback() {
      public Object fn(ResultSet rs) throws SQLException {
        rs.next();
        return rs.getInt("count") == 1;
      }
    });

  }
}