package com.geocent.featuretypebuilder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.geoserver.catalog.StoreInfo;

/**
 * A collection of those commonly-used database operations. Shouldn't
 * contain primitive operations.
 *
 * @author thanthese
 */
public class DbUtils {

  private DbUtils() { throw new AssertionError(); }  // don't instantiate

  /**
   * Two functions:
   *
   * 1.  "hello world" -> "hello_world"
   * 2.  "1test"       -> "_1test"
   *
   * @param name
   * @return
   */
  public static String fixName(String name) {
    String n = name.toLowerCase().replace(" ", "_");
    boolean startsWithNum = n.length() > 0 && Character.isDigit(n.charAt(0));
    return startsWithNum ? "_" + n : n;
  }

  public static void deleteTable(StoreInfo storeInfo, String tablename) {
    if(isTableExists(storeInfo, tablename)) {
      Db.update(storeInfo, String.format("drop table %s;", tablename));
    }
  }

  public static void createTable(StoreInfo storeInfo, String tablename, List<Row> rows) {
    String update = String.format("create table %s (%s);",
                                    tablename,
                                    prettyRows(rows));
    Db.update(storeInfo, update);
    Logger.getLogger(DbUtils.class.getName())
      .log(Level.INFO, "Executed Query: {0}", update);
  }

  public static void registerGeometryColumn(StoreInfo storeInfo,
                                            String tableName,
                                            String type,
                                            String projection) {
    String queryTemplate =
      "insert into geometry_columns ( "
        + "f_table_catalog, "
        + "f_table_schema, "
        + "f_table_name, "
        + "f_geometry_column, "
        + "coord_dimension, "
        + "srid, "
        + "type "
      + ") values ( '', 'public', '%s', 'the_geom', 2, %s, '%s' );";
    String query = String.format(queryTemplate,
                                          tableName,
                                          projection,
                                          type);
    Db.update(storeInfo, query);
    Logger.getLogger(DbUtils.class.getName())
      .log(Level.INFO, "Executed update: {0}", query);
  }

  /**
   * Return a string representing rows, suitable for dropping in a
   * "create table" expression.
   *
   * @param rows
   * @return
   */
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

  public static boolean isTableExists(StoreInfo storeInfo, String tablename) {
    String query = String.format(
        "SELECT count(table_name) as count "
        + "FROM information_schema.tables "
        + "WHERE table_schema = 'public' "
        + "AND table_name = '%s';", tablename);
    return (Boolean) Db.query(storeInfo, query, new ResultSetCallback() {
      @Override
      public Object fn(ResultSet rs) throws SQLException {
        rs.next();
        return rs.getInt("count") == 1;
      }
    });

  }
}