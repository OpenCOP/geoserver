package org.geoserver.wms.web.publish;

import com.geocent.opencop.db.util.DbUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.layer.LayerDetachableModel;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.data.DataAccessFactory;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;

/**
 * Provides a filtered, sorted view over the catalog layers for adding edit URLs.
 */
@SuppressWarnings("serial")
public class AddEditUrlLayerProvider extends GeoServerDataProvider<LayerInfo> {

  static final Property<LayerInfo> TYPE = new BeanProperty<LayerInfo>("Type",
          "type");
  static final Property<LayerInfo> NAME = new BeanProperty<LayerInfo>("Name",
          "resource.prefixedName");
  static final Property<LayerInfo> TITLE = new BeanProperty<LayerInfo>("Title",
          "resource.title");
  static final Property<LayerInfo> EXISTS = new AbstractProperty<LayerInfo>("Exists") {

    // Looking to see if the layer has an edit_url attribute
    public Boolean getPropertyValue(LayerInfo item) {
      Catalog catalog = GeoServerApplication.get().getCatalog();
      DataStoreInfo dsInfo = null;
      dsInfo = catalog.getDataStore(item.getResource().getStore().getId());
      FeatureTypeInfo ft = catalog.getFeatureTypeByDataStore(dsInfo, item.getName());

      try {
        for (AttributeTypeInfo attr : ft.attributes()) {
          if (DbUtils.EDITURL_FIELD.equalsIgnoreCase(attr.getName())) {
            return true;
          }
        }
      } catch (IOException ex) {
        Logger.getLogger(AddEditUrlLayerProvider.class.getName()).log(Level.SEVERE, null, ex);
      }

      return false;
    }
  };
  static final Property<LayerInfo> ADD = new PropertyPlaceholder<LayerInfo>(
          "");
  static final List<Property<LayerInfo>> PROPERTIES = Arrays.asList(TYPE, NAME,
          TITLE, EXISTS, ADD);

  @Override
  protected List<LayerInfo> getItems() {
    List<LayerInfo> result = new ArrayList<LayerInfo>();
    Catalog catalog = getCatalog();
    ResourcePool resourcePool = catalog.getResourcePool();
    // Get the PostGIS data factory so it can be compared to each layer
    PostgisNGDataStoreFactory postgisFactory = new PostgisNGDataStoreFactory() {};
    String postgisName = postgisFactory.getDisplayName();

    for (LayerInfo layer : catalog.getLayers()) {

      DataAccessFactory factory = null;
      DataStoreInfo dsInfo = null;
      try {
        // Get the DataStore for the current layer
        dsInfo = catalog.getDataStore(layer.getResource().getStore().getId());
        // Some layers dont have DataStores (i.e., raster layers)
        // We dont care about those layers anyway, we only want PostGIS layers
        if (null != dsInfo) {
          factory = resourcePool.getDataStoreFactory(dsInfo);
        }
      } catch (IOException ex) {
        Logger.getLogger(AddEditUrlLayerProvider.class.getName()).log(Level.SEVERE, null, ex);
      }
      // Add the layer to the result list if its factory has the same name
      // as the PostGIS factory
      if (null != factory && postgisName.equals(factory.getDisplayName())) {
        result.add(layer);
      }

    }
    return result;
  }

  @Override
  protected List<Property<LayerInfo>> getProperties() {
    return PROPERTIES;
  }

  @Override
  public IModel newModel(Object object) {
    return new LayerDetachableModel((LayerInfo) object);
  }

  @Override
  protected Comparator<LayerInfo> getComparator(SortParam sort) {
    return super.getComparator(sort);
  }
}
