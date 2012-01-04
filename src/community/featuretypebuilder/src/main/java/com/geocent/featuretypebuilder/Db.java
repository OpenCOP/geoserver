package com.geocent.featuretypebuilder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Access the OpenCOP database.
 *
 * @author thanthese
 */
public class Db {

  private static String username = "opencop";
  private static String password = "57levelsofeoc";
  private static String database = "dynamic_feature";

  private Db() { throw new AssertionError(); }  // don't instantiate

  /**
   * Execute query on the opencop database. Fn acts like an anonymous
   * function: it's called with the ResultSet.
   *
   * @param query Query to execute.
   * @param fn Callback that takes ResultSet.
   */
  public static void query(String query, ResultSetCallback fn) {
    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;
    try {
      conn = getConnection();
      stmt = conn.createStatement();
      rs = stmt.executeQuery(query);
      fn.fn(rs);
    } catch (SQLException ex) {
      Logger.getLogger(CreateFeatureTypePage.class.getName()).
              log(Level.SEVERE, null, ex);
    } finally {
      try {
        rs.close();
      } catch (SQLException ex) {
        Logger.getLogger(CreateFeatureTypePage.class.getName()).
                log(Level.SEVERE, null, ex);
      } finally {
        try {
          stmt.close();
        } catch (SQLException ex) {
          Logger.getLogger(CreateFeatureTypePage.class.getName()).
                  log(Level.SEVERE, null, ex);
        } finally {
          try {
            conn.close();
          } catch (SQLException ex) {
            Logger.getLogger(CreateFeatureTypePage.class.getName()).
                    log(Level.SEVERE, null, ex);
          }
        }
      }
    }
  }

  /**
   * Create table or somethin'.
   *
   * @param update
   */
  public static void update(String update) {
    Connection conn = null;
    Statement stmt = null;
    try {
      conn = getConnection();
      stmt = conn.createStatement();
      stmt.executeUpdate(update);
    } catch (SQLException ex) {
      Logger.getLogger(CreateFeatureTypePage.class.getName()).
              log(Level.SEVERE, null, ex);
    } finally {
      try {
        stmt.close();
      } catch (SQLException ex) {
        Logger.getLogger(CreateFeatureTypePage.class.getName()).
                log(Level.SEVERE, null, ex);
      } finally {
        try {
          conn.close();
        } catch (SQLException ex) {
          Logger.getLogger(CreateFeatureTypePage.class.getName()).
                  log(Level.SEVERE, null, ex);
        }
      }
    }
  }

  private static Connection getConnection() {
    // Confirm Driver is registered with DriverManager.
    try {
      Class.forName("org.postgresql.Driver");
    } catch (ClassNotFoundException ex) {
      Logger.getLogger(CreateFeatureTypePage.class.getName()).
              log(Level.SEVERE, null, ex);
    }

    Connection conn = null;
    try {
      conn = DriverManager.getConnection(
              "jdbc:postgresql://localhost/" + database,
              username,
              password);
    } catch (SQLException ex) {
      Logger.getLogger(CreateFeatureTypePage.class.getName()).
              log(Level.SEVERE, null, ex);
    }
    return conn;
  }
}
