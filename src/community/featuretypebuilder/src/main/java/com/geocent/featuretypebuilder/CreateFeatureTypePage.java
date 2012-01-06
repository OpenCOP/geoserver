package com.geocent.featuretypebuilder;

import org.geoserver.web.GeoServerSecuredPage;
import org.apache.wicket.markup.html.basic.Label;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.PropertyModel;


public class CreateFeatureTypePage extends GeoServerSecuredPage {

  public CreateFeatureTypePage() {

    Form form = new Form("form");
    add(form);

    List<Row> rows = Arrays.asList(new Row[] {
      new Row("id", "integer"),
      new Row("version", "integer"),
      new Row("description", "varchar 500"),
      new Row("the_geom", "Geometry")
    });

    form.add(new ListView("schema", rows) {
      protected void populateItem(ListItem item) {
        Row row = (Row) item.getModelObject();
        item.add(new TextField("name", new PropertyModel(row, "name")));
        item.add(new TextField("type", new PropertyModel(row, "type")));
      }
    });
  }

  private void demoFunctionality() {
    add(new Label("hellolabel", "hello world"));

    Db.query("select default_graphic_size, version from test;", new ResultSetCallback() {
      public Object fn(ResultSet rs) throws SQLException {
        while (rs.next()) {
          System.out.print("default_graphic_size");
          System.out.println(rs.getString("default_graphic_size"));
          System.out.print("version");
          System.out.println(rs.getString("version"));
        }
        return null;
      }
    });
//    Db.update("create table test3 (id integer, whatever varchar(20));");
    DbUtils.deleteTable("test2");
    System.out.println("table exists");
    System.out.println(DbUtils.isTableExists("test"));
    System.out.println(DbUtils.isTableExists("test3"));
  }

}