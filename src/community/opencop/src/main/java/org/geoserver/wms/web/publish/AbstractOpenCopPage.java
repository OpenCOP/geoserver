package org.geoserver.wms.web.publish;

import com.geocent.opencop.db.util.DbUtils;
import javax.servlet.http.HttpServletRequest;
import org.apache.wicket.protocol.http.WebRequest;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.web.GeoServerSecuredPage;

/**
 * Some common functions for the OpenCOP pages.
 *
 * @author yancy
 */
public abstract class AbstractOpenCopPage extends GeoServerSecuredPage {

  // Get the base url of this geoserver
  protected String getBaseUrl() {
    WebRequest request = (WebRequest) getRequest();
    HttpServletRequest httpServletRequest;
    httpServletRequest = ((WebRequest) request).getHttpServletRequest();
    return ResponseUtils.baseURL(httpServletRequest);
  }

  /*
   * Update an existing PostGIS layer to include the edit_url column.
   */
  protected void addEditLinks(StoreInfo store, String layerName) {
    Catalog catalog = getCatalog();
    DataStoreInfo dsInfo = catalog.getDataStore(store.getId());

    // Do the database work
    DbUtils.addEditUrlColumn(store, layerName);
    DbUtils.setEditUrls(store, layerName);
    DbUtils.createEditUrlRule(store, layerName);

    // Refresh the in memory catalog so GeoServer knows about the new field
    FeatureTypeInfo ft = catalog.getFeatureTypeByDataStore(dsInfo, layerName);
    catalog.getResourcePool().clear(ft);
    catalog.getResourcePool().clear(ft.getStore());
  }
}
