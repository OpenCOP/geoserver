package com.geocent.featuretypebuilder;

import org.geoserver.web.GeoServerSecuredPage;
import org.apache.wicket.markup.html.basic.Label;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CreateFeatureTypePage extends GeoServerSecuredPage {

  public CreateFeatureTypePage() {
    add(new Label("hellolabel", "hello world"));

    Db.query("select default_graphic_size, version from test;", new ResultSetCallback() {
      public void fn(ResultSet rs) throws SQLException {
        while (rs.next()) {
          System.out.print("default_graphic_size");
          System.out.println(rs.getString("default_graphic_size"));
          System.out.print("version");
          System.out.println(rs.getString("version"));
        }
      }
    });
    Db.update("create table test3 (id integer, whatever varchar(20));");
  }

}