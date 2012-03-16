package org.geoserver.wms.web.publish;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;

public class AddEditUrlPage extends AbstractOpenCopPage {

  AddEditUrlLayerProvider provider = new AddEditUrlLayerProvider();
  GeoServerTablePanel<LayerInfo> table;
//    final CatalogIconFactory icons = CatalogIconFactory.get();

  public AddEditUrlPage() {

    final CatalogIconFactory icons = CatalogIconFactory.get();
    table = new GeoServerTablePanel<LayerInfo>("table", provider) {

      @Override
      protected Component getComponentForProperty(String id, IModel itemModel,
              Property<LayerInfo> property) {
        if (property == AddEditUrlLayerProvider.TYPE) {

          // Get the layer type (point, line, raster, etc.) icon
          Fragment f = new Fragment(id, "iconFragment", AddEditUrlPage.this);
          f.add(new Image("layerIcon", icons.getSpecificLayerIcon((LayerInfo) itemModel.getObject())));
          return f;
          
        } else if (property == AddEditUrlLayerProvider.NAME) {
          return new Label(id, property.getModel(itemModel));
        } else if (property == AddEditUrlLayerProvider.TITLE) {
          return new Label(id, property.getModel(itemModel));
        } else if (property == AddEditUrlLayerProvider.EXISTS) {
          
          // return a check mark icon if the layer has an edit_url field
          Fragment f = new Fragment(id, "existsFragment", AddEditUrlPage.this);
          boolean exists = (Boolean)property.getPropertyValue((LayerInfo) itemModel.getObject());
          if (exists) {
            f.add(new Image("existsIcon", icons.getEnabledIcon()));
          } else {
            f.add(new Image("existsIcon"));
          }
          return f;
          
        } else if (property == AddEditUrlLayerProvider.ADD) {

          // Create the link that will do the database work to add
          // edit_url column and rule
          Fragment f = new Fragment(id, "addEditUrlFragment", AddEditUrlPage.this);
          f.add(new Link("addEditUrlLink", itemModel) {

            public void onClick() {
              // Get all the catalog objects needed
              LayerInfo layer = (LayerInfo) getModelObject();
              StoreInfo store = layer.getResource().getStore();
              String layerName = layer.getName();

              addEditLinks(store, layerName);
              info("Added edit URLs to layer: " + layerName);
            }
          });
          return f;
        }

        throw new IllegalArgumentException("Don't know a property named " + property.getName());
      }
    };
    
    table.setOutputMarkupId(true);
    add(table);

  }
}