package com.geocent.featuretypebuilder;

import org.geoserver.web.GeoServerSecuredPage;
import org.apache.wicket.markup.html.basic.Label;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.value.ValueMap;
import org.geoserver.catalog.StoreInfo;

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

    ListView lv = null;

    public FeatureTypeForm(final String id) {
      super(id, new CompoundPropertyModel<ValueMap>(new ValueMap()));  // no validation

      List<Row> rows = new ArrayList(Arrays.asList(new Row[] {
        new Row("id", "integer"),
        new Row("version", "integer"),
        new Row("description", "varchar(500)"),
        new Row("the_geom", "geometry")
      }));

      lv = new ListView("schema", rows) {
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

      add(new TextField<String>("layername").setType(String.class));
      add(new DropDownChoice(
                    "storesDropDown",
                    new Model(),
                    new StoreListModel(),
                    new StoreListChoiceRenderer()).setOutputMarkupId(true));
      add(new TextField<String>("style").setType(String.class));
      add(new HiddenField<String>("serialized-fields")
                .setType(String.class)
                .setOutputMarkupId(true));
      add(lv);
    }

    @Override
    public final void onSubmit() {
      ValueMap values = getModelObject();
      String layername = DbUtils.fixName(values.getString("layername"));
      String store = values.getString("storesDropDown");

      System.out.println("values = " + values.toString());
      System.out.println("store = " + store);
      System.out.println("lv = " + lv);
      System.out.println("lv.getModelObject() = " + lv.getModelObject());

      String style = values.getString("style");
      List<Row> newRows = parseSerialization(values.getString("serialized-fields"));

      // refresh attrs model
      List lvModel = lv.getModelObject();
      lvModel.clear();
      for (Row row: newRows) {
        System.out.println("row = " + row);
        lvModel.add(row);
      }
      System.out.println("--- now ---");
      System.out.println("lvModel = " + lvModel);

      // guard: valid layer/table name
      if(layername.isEmpty()) {
        error("Layer name not given");
        return;
      }

      // guard: valid rows
      for (Row row : newRows) {
        if(!row.isValid()) {
          error(String.format("Row with name '%s' and type '%s' is not valid.",
                  row.getName(),
                  row.getType()));
          return;
        }
      }

      // guard: table already exists
      if(DbUtils.isTableExists(layername)) {
        error(String.format("Table of name '%s' already exists", layername));
        return;
      }

      // create table
      DbUtils.createTable(layername, newRows);

      // confirm table creation
      if(DbUtils.isTableExists(layername)) {
        info(String.format("Layer '%s' created.", layername));
        return;
      } else {
        error(String.format("Layer '%s' not created.", layername));
      }
    }

    /**
     * Parses: name1|type1|name2|type2|...
     * @param serialization
     * @return
     */
    private List<Row> parseSerialization(String serialization) {
      String patternStr = "([a-zA-Z0-9_ ]*)\\|([a-zA-Z0-9_() ]*)\\|?";
      Pattern pattern = Pattern.compile(patternStr);
      Matcher matcher = pattern.matcher(serialization);
      List<Row> rws = new ArrayList<Row>();
      while (matcher.find()) {
        if(matcher.groupCount() >= 2) {
          String name = matcher.group(1);
          String type = matcher.group(2);
          rws.add(new Row(name, type));
        }
      }
      return rws;
    }

    // Class is moved to a public class in GeoServer 2.2.
    // Repeating it here for compatibility.
    final class StoreListModel extends LoadableDetachableModel {

      @Override
      protected Object load() {
        List<StoreInfo> stores = getCatalog().getStores(StoreInfo.class);
        stores = new ArrayList<StoreInfo>(stores);
        Collections.sort(stores, new Comparator<StoreInfo>() {

          public int compare(StoreInfo o1, StoreInfo o2) {
            if (o1.getWorkspace().equals(o2.getWorkspace())) {
              return o1.getName().compareTo(o2.getName());
            }
            return o1.getWorkspace().getName().compareTo(o2.getWorkspace().getName());
          }
        });
        return stores;
      }
    }

    // Class is moved to a public class in GeoServer 2.2.
    // Repeating it here for compatibility.
    final class StoreListChoiceRenderer implements IChoiceRenderer {

      public Object getDisplayValue(Object store) {
        StoreInfo info = (StoreInfo) store;
        return new StringBuilder(info.getWorkspace().getName()).append(':').append(
                info.getName());
      }

      public String getIdValue(Object store, int arg1) {
        return ((StoreInfo) store).getId();
      }
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