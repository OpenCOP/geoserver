package com.geocent.featuretypebuilder;

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

}
