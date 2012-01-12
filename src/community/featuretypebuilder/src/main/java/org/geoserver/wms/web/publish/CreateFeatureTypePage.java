package org.geoserver.wms.web.publish;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import java.io.IOException;
import org.geoserver.web.GeoServerSecuredPage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.value.ValueMap;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.data.store.StoreListChoiceRenderer;
import org.geoserver.web.data.store.StoreListModel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class CreateFeatureTypePage extends GeoServerSecuredPage {
  static final CoordinateReferenceSystem WGS84;

  static {
    try {
      WGS84 = CRS.decode("EPSG:4326");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public CreateFeatureTypePage() {
    add(new FeatureTypeForm("featureTypeForm"));
  }

  public final class FeatureTypeForm extends Form<ValueMap> {

    private final List<String> TYPES = Arrays.asList(new String[]{
              "integer",
              "varchar(5)",
              "varchar(20)",
              "varchar(500)",
              "text",
              "boolean",
              "LINE",
              "POLYGON",
              "POINT"});

    private Map<String, Class> BINDINGS = new TreeMap<String, Class>();
    private Map<String, Integer> LENGTHS = new TreeMap<String, Integer>();

    List<Row> defaultRows = new ArrayList(Arrays.asList(new Row[]{
              new Row("version", "integer"),
              new Row("description", "varchar(500)"),
              new Row("edit_url", "varchar(500)"),
              new Row("the_geom", "POINT")}));

    ListView lv = null;  // represents attrs list in form
    DropDownChoice stores = null;
    Model defaultStyleModel = null;

    public FeatureTypeForm(final String id) {
      super(id, new CompoundPropertyModel<ValueMap>(new ValueMap()));  // no validation

      BINDINGS.put("integer", Integer.class);
      BINDINGS.put("varchar(5)", String.class);
      BINDINGS.put("varchar(20)", String.class);
      BINDINGS.put("varchar(500)", String.class);
      BINDINGS.put("text", String.class);
      BINDINGS.put("boolean", Boolean.class);
      BINDINGS.put("LINE", LineString.class);
      BINDINGS.put("POLYGON", Polygon.class);
      BINDINGS.put("POINT", Point.class);

      LENGTHS.put("integer", 0);
      LENGTHS.put("varchar(5)", 5);
      LENGTHS.put("varchar(20)", 20);
      LENGTHS.put("varchar(500)", 500);
      LENGTHS.put("text", 0);
      LENGTHS.put("boolean", 0);
      LENGTHS.put("LINE", 0);
      LENGTHS.put("POLYGON", 0);
      LENGTHS.put("POINT", 0);

      add(new TextField<String>("layername").setType(String.class));
      add(stores = (DropDownChoice) new DropDownChoice(
              "storesDropDown",
              new Model(),
              new StoreListModel(),
              new StoreListChoiceRenderer()).setOutputMarkupId(true));
      add(new TextField<String>("style").setType(String.class));
      add(new HiddenField<String>("serialized-fields").setType(String.class).setOutputMarkupId(true));
      add(lv = new ListView("schema", defaultRows) {
        @Override
        protected void populateItem(ListItem item) {
          Row row = (Row) item.getModelObject();
          item.add(new TextField("name", new PropertyModel<String>(row, "name")));
          item.add(new DropDownChoice<String>(
                  "type",
                  new PropertyModel<String>(row, "type"),
                  TYPES));
        }
      });

      // default style chooser. A default style is required
      defaultStyleModel = new Model();
      final DropDownChoice defaultStyle = new DropDownChoice("defaultStyle", defaultStyleModel,
              new StylesModel(), new StyleChoiceRenderer());
      defaultStyle.setRequired(true);
      defaultStyle.setOutputMarkupId(true);
      add(defaultStyle);

      // Add the Style's legend graphic to the page
      final Image defStyleImg = new Image("defaultStyleLegendGraphic");
      defStyleImg.setOutputMarkupId(true);
      add(defStyleImg);

      // Add a legend graphic ajax updater object
      String wmsURL = getRequest().getRelativePathPrefixToContextRoot();
      wmsURL += wmsURL.endsWith("/") ? "wms?" : "/wms?";
      final LegendGraphicAjaxUpdater defaultStyleUpdater;
      defaultStyleUpdater = new LegendGraphicAjaxUpdater(wmsURL, defStyleImg, defaultStyleModel);

      // Add an onChange action to the style drop down that uses the legend
      // ajax updater to change the legend graphic on the page.
      defaultStyle.add(new OnChangeAjaxBehavior() {
        @Override
        protected void onUpdate(AjaxRequestTarget target) {
          defaultStyleUpdater.updateStyleImage(target);
        }
      });
    }

    @Override
    public final void onSubmit() {

      // grab values from form
      ValueMap values = getModelObject();
      String layername = DbUtils.fixName(values.getString("layername"));
      StoreInfo storeInfo = (StoreInfo) stores.getModelObject();
      StyleInfo styleInfo = (StyleInfo) defaultStyleModel.getObject();
      List<Row> rows = parseSerialization(values.getString("serialized-fields"));

      // refresh attrs model
      List lvModel = lv.getModelObject();
      lvModel.clear();
      for (Row row : rows) {
        lvModel.add(row);
      }

      // guard: user selected a store type
      if (storeInfo == null || storeInfo.getType() == null) {
        error("You must select a data store.");
        return;
      }

      // guard: correct store type
      if (!storeInfo.getType().equals("PostGIS")) {
        error(String.format("Store type '%s' is invalid.  Store must be of type 'PostGIS'.",
                storeInfo.getType()));
        return;
      }

      // guard: valid layer/table name
      if (layername.isEmpty()) {
        error("Layer name not given");
        return;
      }

      // guard: all rows are valid
      for (Row row : rows) {
        if (!row.isValid()) {
          error(String.format("Row with name '%s' and type '%s' is not valid.",
                  row.getName(),
                  row.getType()));
          return;
        }
      }

      // guard: all row names are unique
      if (!isAllNamesUnique(rows)) {
        error("Duplicate attribute names are not allowed.");
        return;
      }

      // guard: doesn't already exist
      if (DbUtils.isTableExists(storeInfo, layername)) {
        error(String.format("Table of name '%s' already exists", layername));
        return;
      }

      // guard: if the_geom exists...
      Row the_geom = getTheGeom(rows);
      if(the_geom != null) {

        // guard: geometry_columns needs to exist
        if(!DbUtils.isTableExists(storeInfo, "geometry_columns")) {
          error("The table 'geometry_columns' doesn't exist on this data store.");
          return;
        }

        // guard: the_geom must have a geometry type
        if(!the_geom.isGeometryType()) {
          error("Field 'the_geom' must have a geometry type.");
          return;
        }

        // guard: the geometry column needs to not already be registered
        if(DbUtils.isGeometryColumnRegistered(storeInfo, layername)) {
          error(String.format(
            "A geometry column has already been registered for table '%s'."
            , layername));
          return;
        }
      }

      // create table
      DataStore ds = null;
      DataStoreInfo dsInfo = null;
      try {
        // basic checks
        dsInfo = getCatalog().getDataStore(storeInfo.getId());
        ds = (DataStore) dsInfo.getDataStore(null);
        // Check if the layername already exists in the datastore
        if (Arrays.asList(ds.getTypeNames()).contains(layername)) {
          error(new ParamResourceModel("duplicateTypeName", this, dsInfo.getName(),
                  layername).getString());
          return;
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      try {
        // Convert the rows to a SimpleFeatureType
        SimpleFeatureType featureType = buildFeatureType(rows, layername);
        // Persist the SimpleFeatureType to the datastore
        ds.createSchema(featureType);

        CatalogBuilder builder = new CatalogBuilder(getCatalog());
        builder.setStore(dsInfo);

        // Build the geoserver feature type object
        FeatureTypeInfo fti = builder.buildFeatureType(getFeatureSource(ds, layername));
        // Set the bounding boxes to makes things happy
        ReferencedEnvelope world = new ReferencedEnvelope(-180, 180, -90, 90, WGS84);
        fti.setLatLonBoundingBox(world);
        fti.setNativeBoundingBox(world);

        // Build the geoserver layer object
        LayerInfo layerInfo = builder.buildLayer(fti);
        // Set the default style to the one selected by the user
        layerInfo.setDefaultStyle(styleInfo);

        // Create rule
        DbUtils.createEditUrlRule(storeInfo, layername);

        // Redirect user to the layer edit page.
        // At this point the layer is not persisted. Only after the user clicks
        // save on the edit page.
        setResponsePage(new ResourceConfigurationPage(layerInfo, true));
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Failed to create feature type", e);
        error(new ParamResourceModel("creationFailure", this, e.getMessage()).getString());
      }

      // confirm table creation
      if (!DbUtils.isTableExists(storeInfo, layername)) {
        error(String.format("Layer '%s' not created.", layername));
        return;
      }

      // confirm geometry registered
      if(the_geom != null && !DbUtils.isGeometryColumnRegistered(storeInfo, layername)) {
        error("The layer was created, but the geometry column was not registered properly.");
        return;
      }

      info(String.format("Layer '%s' created.", layername));
    }

    private Row getTheGeom(List<Row> rows) {
      return (Row) CollectionUtils.find(rows, new Predicate() {
        @Override
        public boolean evaluate(Object object) {
          return ((Row) object).getName().equals("the_geom");
        }
      });

    }

    FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource(DataStore ds, String name)
            throws IOException {
      try {
        return ds.getFeatureSource(name);
      } catch (IOException e) {
        // maybe it's Oracle?
        try {
          return ds.getFeatureSource(name.toUpperCase());
        } catch (Exception ora) {
          // nope, the reason was another one
          throw e;
        }
      }
    }

    SimpleFeatureType buildFeatureType(List<Row> rows, String name) {
      SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
      for (Row row : rows) {
        Integer length = LENGTHS.get(row.getType());
        Class binding = BINDINGS.get(row.getType());
        if (length > 0) {
          builder.length(length);
        }
        if (Geometry.class.isAssignableFrom(binding)) {

          try {
            builder.add(row.getName(), binding, WGS84);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        } else {
          builder.add(row.getName(), binding);
        }
      }
      builder.setName(name);
      return builder.buildFeatureType();
    }

    private boolean isAllNamesUnique(List<Row> rows) {
      List<Row> names = (List<Row>) CollectionUtils.collect(rows, new Transformer() {
        @Override
        public Object transform(Object row) {
          return ((Row) row).getName();
        }
      });
      return names.size() == new HashSet(names).size();
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
      List<Row> rows = new ArrayList<Row>();
      while (matcher.find()) {
        if (matcher.groupCount() >= 2) {
          String name = matcher.group(1);
          String type = matcher.group(2);
          rows.add(new Row(name, type));
        }
      }
      return rows;
    }
  }
}