package com.geocent.featuretypebuilder;

import org.geoserver.web.GeoServerSecuredPage;
import org.apache.wicket.markup.html.basic.Label;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.value.ValueMap;

public class CreateFeatureTypePage extends GeoServerSecuredPage {

  public CreateFeatureTypePage() {
    add(new FeatureTypeForm("featureTypeForm"));
  }

  public final class FeatureTypeForm extends Form<ValueMap> {

    private final List<String> TYPES = Arrays.asList(new String[] {
      "integer",
      "varchar(20)",
      "varchar(500)",
      "text",
      "boolean",
      "geometry",
      "point"});

    List<Row> rows = new ArrayList(Arrays.asList(new Row[] {
      new Row("id", "integer"),
      new Row("version", "integer"),
      new Row("description", "varchar(500)"),
      new Row("the_geom", "geometry")
    }));

    public FeatureTypeForm(final String id) {
      super(id, new CompoundPropertyModel<ValueMap>(new ValueMap()));  // no validation

      add(new TextField<String>("layername").setType(String.class));
      add(new TextField<String>("namespace").setType(String.class));
      add(new TextField<String>("style").setType(String.class));
      add(new HiddenField<String>("serialized-fields")
                .setType(String.class)
                .setOutputMarkupId(true));
      add(buildAttrsTable(rows));
    }

    private ListView buildAttrsTable(List<Row> rows) {
      return new ListView("schema", rows) {
        @Override
        protected void populateItem(ListItem item) {
          Row row = (Row) item.getModelObject();
          item.add(new TextField("name", new PropertyModel<String>(row, "name")));
          item.add(new DropDownChoice<String>(
                            "type",
                            new PropertyModel<String>(row, "type"),
                            TYPES));
        }
      };
    }

    @Override
    public final void onSubmit() {
      ValueMap values = getModelObject();

      String layername = values.getString("layername");
      String namespace = values.getString("namespace");
      String style = values.getString("style");
      List<Row> rows = parseSerialization(values.getString("serialized-fields"));

      System.out.println("layername = " + layername);
      System.out.println("namespace = " + namespace);
      System.out.println("style = " + style);
      System.out.println("rows = " + rows);

      DbUtils.createTable(layername, rows);

      info("Layer created.");
    }

    private List<Row> parseSerialization(String serialization) {

      // parses:
      //   name1|type1|name2|type2|...
      // into groups of
      //   (name1, type1), (name2, type2), ...
      String patternStr = "([a-zA-Z0-9_ ]*)\\|([a-zA-Z0-9_() ]*)\\|?";

      Pattern pattern = Pattern.compile(patternStr);
      Matcher matcher = pattern.matcher(serialization);

      List<Row> rows = new ArrayList<Row>();
      while (matcher.find()) {
        if(matcher.groupCount() >= 2) {
          String name = matcher.group(1);
          String type = matcher.group(2);
          rows.add(new Row(name, type));
        }
      }
      return rows;
    }
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