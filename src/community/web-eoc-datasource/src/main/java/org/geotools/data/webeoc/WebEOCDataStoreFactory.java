package org.geotools.data.webeoc;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import org.geotools.data.DataAccessFactory.Param;
import org.geotools.data.Parameter;
import org.geotools.data.postgis.PostGISDialect;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.jdbc.JDBCJNDIDataStoreFactory;
import org.geotools.jdbc.SQLDialect;

public class WebEOCDataStoreFactory extends PostgisNGDataStoreFactory {

    public static final Param WEBEOC_WSDL = new Param("WebEOC WSDL", String.class, "The WSDL address for the WebEOC service.", true);
    public static final Param WEBEOC_POSITION = new Param("WebEOC Position", String.class, "WebEOC Position", true);
    public static final Param WEBEOC_USER = new Param("WebEOC User", String.class, "WebEOC User", true);
    public static final Param WEBEOC_PASSWORD = new Param("WebEOC Password", String.class, "WebEOC Password", true, null, Collections
                    .singletonMap(Parameter.IS_PASSWORD, Boolean.TRUE));

    public WebEOCDataStoreFactory() {
        super();
    }

    @Override
    protected void setupParameters(Map parameters) {
        parameters.put(WEBEOC_WSDL.key, WEBEOC_WSDL);
        parameters.put(WEBEOC_POSITION.key, WEBEOC_POSITION);
        parameters.put(WEBEOC_USER.key, WEBEOC_USER);
        parameters.put(WEBEOC_PASSWORD.key, WEBEOC_PASSWORD);
        super.setupParameters(parameters);
    }

    @Override
    public String getDisplayName() {
        return "Web EOC Datastore";
    }

    public String getDescription() {
        return "Web EOC Datastore.  This uses a PostGIS database as caching mechanism.  Credentials for both the Web EOC instance, and write access to a PostGIS database must be provided";
    }
}