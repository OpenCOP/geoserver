package com.geocent.featuretypebuilder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;

/**
 * A collection of those commonly-used database operations. Shouldn't
 * contain primitive operations.
 *
 * @author thanthese
 */
public class DbUtils {

  private DbUtils() { throw new AssertionError(); }  // don't instantiate

  public static void deleteTable(String tablename) {
    if(isTableExists(tablename)) {
      Db.update(String.format("drop table %s;", tablename));
    }
  }

  public static void createTable(String tablename, List<Row> rows) {
    String update = String.format("create table %s (%s);",
                                    tablename,
                                    prettyRows(rows));
    Db.update(update);
    Logger.getLogger(DbUtils.class.getName())
      .log(Level.INFO, "Executed Query: {0}", update);
  }

  private static String prettyRows(List<Row> rows) {
    List<String> rs = (List<String>) CollectionUtils.collect(rows, new Transformer() {
      @Override
      public Object transform(Object row) {
        Row r = (Row) row;
        return r.getName() + " " + r.getType();
      }
    });
    return StringUtils.join(rs.toArray(), ", ");
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