package org.geoserver.wms.web.publish;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import com.geocent.opencop.db.util.DbUtils;

public class AddEditUrlPage extends GeoServerSecuredPage {

  AddEditUrlLayerProvider provider = new AddEditUrlLayerProvider();
  GeoServerTablePanel<LayerInfo> table;

  public AddEditUrlPage() {

    final CatalogIconFactory icons = CatalogIconFactory.get();
    table = new GeoServerTablePanel<LayerInfo>("table", provider) {

      @Override
      protected Component getComponentForProperty(String id, IModel itemModel,
              Property<LayerInfo> property) {
        if (property == AddEditUrlLayerProvider.TYPE) {
          Fragment f = new Fragment(id, "iconFragment", AddEditUrlPage.this);
          f.add(new Image("layerIcon", icons.getSpecificLayerIcon((LayerInfo) itemModel.getObject())));
          return f;
        } else if (property == AddEditUrlLayerProvider.WORKSPACE) {
          return new Label(id, property.getModel(itemModel));
        } else if (property == AddEditUrlLayerProvider.STORE) {
          return new Label(id, property.getModel(itemModel));
        } else if (property == AddEditUrlLayerProvider.NAME) {
          return new Label(id, property.getModel(itemModel));
        } else if (property == AddEditUrlLayerProvider.ADD) {
          Fragment f = new Fragment(id, "addEditUrlFragment", AddEditUrlPage.this);
          f.add(new Link("addEditUrlLink", itemModel) {

            public void onClick() {
              LayerInfo layer = (LayerInfo) getModelObject();

//                      DataStore ds = null;
                      DataStoreInfo dsInfo = null;
//                      try {
              // basic checks
                        dsInfo = getCatalog().getDataStore(layer.getResource().getStore().getId());
//                        ds = (DataStore) dsInfo.getDataStore(null);
//                        SimpleFeatureType featureType = ds.getSchema(layer.getName());
//                        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
//                        builder.init(featureType);
//                        builder.add("edit_url", String.class);
//
//                        ds.updateSchema(layer.getName(), builder.buildFeatureType());
              StoreInfo store = layer.getResource().getStore();
              String layerName = layer.getName();
              
              DbUtils.addEditUrlColumn(store, layerName);
              DbUtils.setEditUrls(store, layerName);
              DbUtils.createEditUrlRule(store, layerName);

              GeoServerApplication app = (GeoServerApplication) getApplication();

//              getCatalog().getFeatureTypeByDataStore(dsInfo, layerName);
                
                FeatureTypeInfo ft = getCatalog().getFeatureTypeByDataStore(dsInfo, layerName);
                app.getCatalog().getResourcePool().clear(ft);
                app.getCatalog().getResourcePool().clear(ft.getStore());
              getCatalog().save(layer);
              
              info("Added edit URLs to layer: " + layerName);
//                      } catch (IOException ex) {
//                        Logger.getLogger(AddEditUrlPage.class.getName()).log(Level.SEVERE, null, ex);
//                      }

              // do something here...
              System.out.println("This button works!");
            }
          });
          return f;
        }

        throw new IllegalArgumentException("Don't know a property named " + property.getName());
      }

      @Override
      protected void onSelectionUpdate(AjaxRequestTarget target) {
      }
    };
    
    table.setOutputMarkupId(true);
    add(table);

  }
}