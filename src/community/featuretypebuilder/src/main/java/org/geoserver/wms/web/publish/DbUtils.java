package org.geoserver.wms.web.publish;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.web.GeoServerApplication;

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

  /**
   * An anonymous function that returns the given field from the first
   * row of a resultset. Userful for extracting, for example, the count
   * from a "count(*)" query.
   */
  public static class First implements ResultSetCallback {
    private String fieldName;
    public First(String fieldName) { this.fieldName = fieldName; }
    public Object fn(ResultSet rs) throws SQLException {
      rs.next();
      return rs.getObject(fieldName);
    }
  }

  public static boolean isGeometryColumnRegistered(StoreInfo storeInfo, String tableName) {
    String query = String.format(
            "SELECT count(*) "
            + "FROM geometry_columns "
            + "WHERE f_table_name = '%s';", tableName);
    return (Long) Db.query(storeInfo, query, new First("count")) > 0;
  }

  public static boolean isTableExists(StoreInfo storeInfo, String tablename) {
    String query = String.format(
        "SELECT count(*) "
        + "FROM information_schema.tables "
        + "WHERE table_schema = 'public' "
        + "AND table_name = '%s';", tablename);
    return (Long) Db.query(storeInfo, query, new First("count")) > 0;

  }

  /**
   * Create the rule that sets the edit_url for every row on tableName
   * after insert.
   *
   * Assumes:
   * 1.  Table exists
   * 2.  Table has edit_url field
   * 3.  Table has fid field as id field.
   *
   * @param si
   * @param tableName
   * @param domain e.g.: "demo.geocent.com"
   */
  public static void createEditUrlRule(StoreInfo storeInfo, String tableName) {
    String updateTemplate = "create or replace rule \"edit_url_%s\" "
            + "as on insert to \"%s\" "
            + "do also update \"%s\" "
            + "set edit_url = "
              + "'<a href=\"http://%s/geoserver/wfs"
                + "?request=GetFeature"
                + "&version=1.0.0"
                + "&outputFormat=html"
                + "&typeName=%s:%s"
                + "&FEATUREID=' || fid || "
              + "'\" target=\"_blank\" style=\"color:red;\">Edit</a>';";
    String update = String.format(updateTemplate,
            tableName,
            tableName,
            tableName,
            getDomain(),
            storeInfo.getWorkspace().getName(),
            tableName);
    Db.update(storeInfo, update);
    Logger.getLogger(DbUtils.class.getName())
      .log(Level.INFO, "Created rule: {0}", update);
  }

  /**
   * Where is the program running? demo.geocent.com? localhost?
   *
   * @return
   */
  private static String getDomain() {
    StoreInfo storeInfo = getStore("opencop", "opencop");
    String query = "select value from config where component='db' and name='domain';";
    return (String) Db.query(storeInfo, query, new First("value"));
  }

  private static StoreInfo getStore(final String workspaceName, final String storeName) {
    List<StoreInfo> storeInfos = GeoServerApplication.get().getCatalog().getStores(StoreInfo.class);
    return (StoreInfo) CollectionUtils.find(storeInfos, new Predicate() {
      @Override
      public boolean evaluate(Object object) {
        StoreInfo si = (StoreInfo) object;
        return si.getWorkspace().getName().equals(workspaceName) &&
                si.getName().equals(storeName);
      }
    });
  }
}