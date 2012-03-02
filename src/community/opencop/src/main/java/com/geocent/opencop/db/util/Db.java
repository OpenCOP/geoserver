package com.geocent.opencop.db.util;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.StoreInfo;

/**
 * Fundamental operations on the OpenCOP database.
 *
 * @author thanthese
 */
public class Db {

  private Db() { throw new AssertionError(); }  // don't instantiate

  /**
   * Execute query on the opencop database. Fn acts like an anonymous
   * function: it's called with the ResultSet.
   *
   * @param query Query to execute.
   * @param fn Callback that takes ResultSet.
   * @return Whatever fn returns.
   */
  public static Object query(StoreInfo storeInfo, String query, ResultSetCallback fn) {
    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;
    Object result = null;
    try {
      conn = getConnection(storeInfo);
      stmt = conn.createStatement();
      rs = stmt.executeQuery(query);
      result = fn.fn(rs);
    } catch (SQLException ex) {
      Logger.getLogger(Db.class.getName()).
              log(Level.SEVERE, null, ex);
    } finally {
      try {
        rs.close();
      } catch (SQLException ex) {
        Logger.getLogger(Db.class.getName()).
                log(Level.SEVERE, null, ex);
      } finally {
        try {
          stmt.close();
        } catch (SQLException ex) {
          Logger.getLogger(Db.class.getName()).
                  log(Level.SEVERE, null, ex);
        } finally {
          try {
            conn.close();
          } catch (SQLException ex) {
            Logger.getLogger(Db.class.getName()).
                    log(Level.SEVERE, null, ex);
          }
        }
      }
    }
    return result;
  }

  /**
   * Create table or somethin'.
   *
   * @param update
   */
  public static void update(StoreInfo storeInfo, String update) {
    Connection conn = null;
    Statement stmt = null;
    try {
      conn = getConnection(storeInfo);
      stmt = conn.createStatement();
      stmt.executeUpdate(update);
    } catch (SQLException ex) {
      Logger.getLogger(Db.class.getName()).
              log(Level.SEVERE, null, ex);
    } finally {
      try {
        stmt.close();
      } catch (SQLException ex) {
        Logger.getLogger(Db.class.getName()).
                log(Level.SEVERE, null, ex);
      } finally {
        try {
          conn.close();
        } catch (SQLException ex) {
          Logger.getLogger(Db.class.getName()).
                  log(Level.SEVERE, null, ex);
        }
      }
    }
  }

  private static Connection getConnection(StoreInfo storeInfo) {
    // Confirm Driver is registered with DriverManager.
    try {
      Class.forName("org.postgresql.Driver");
    } catch (ClassNotFoundException ex) {
      Logger.getLogger(Db.class.getName()).
              log(Level.SEVERE, null, ex);
    }

    Connection conn = null;
    Map<String, Serializable> params = storeInfo.getConnectionParameters();
    try {
      conn = DriverManager.getConnection(
              "jdbc:postgresql://localhost/" + (String) params.get("database"),
              (String) params.get("user"),
              (String) params.get("passwd"));
    } catch (SQLException ex) {
      Logger.getLogger(Db.class.getName()).
              log(Level.SEVERE, null, ex);
    }
    return conn;
  }
}
