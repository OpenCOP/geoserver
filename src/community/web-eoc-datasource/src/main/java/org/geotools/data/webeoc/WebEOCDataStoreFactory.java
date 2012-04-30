package org.geotools.data.webeoc;

import java.io.IOException;
import java.util.Map;
import org.geotools.data.postgis.PostGISDialect;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.jdbc.JDBCJNDIDataStoreFactory;
import org.geotools.jdbc.SQLDialect;

public class WebEOCDataStoreFactory extends JDBCDataStoreFactory {

    public static final Param EOC_WSDL = new Param("WebEOC WSDL", String.class, "The WSDL address for the WebEOC service.", true);
    public static final Param EOC_USER = new Param("WebEOC User", String.class, "WebEOC User", true);
    public static final Param EOC_PASSWORD = new Param("WebEOC Password", String.class, "WebEOC Password", true);
    public static final Param PORT = new Param("Database Port", Integer.class, "Database Port", true, 5432);
    public static final Param SCHEMA = new Param("Database Schema", String.class, "Database Schema", true, "public");

    public WebEOCDataStoreFactory() {
        super();
    }

    @Override
    protected void setupParameters(Map parameters) {
        parameters.put(EOC_WSDL.key, EOC_WSDL);
        parameters.put(EOC_USER.key, EOC_USER);
        parameters.put(EOC_PASSWORD.key, EOC_PASSWORD);
        parameters.put(DBTYPE.key, DBTYPE);
        parameters.put(SCHEMA.key, SCHEMA);
        parameters.put(PORT.key, PORT);
        parameters.put(MAX_OPEN_PREPARED_STATEMENTS.key, MAX_OPEN_PREPARED_STATEMENTS);
        super.setupParameters(parameters);
    }

    @Override
    public String getDisplayName() {
        return "Web EOC Datastore";
    }

    public String getDescription() {
        return "Web EOC Datastore.  This uses a PostGIS database as caching mechanism.  Credentials for both the Web EOC instance, and write access to a PostGIS database must be provided";
    }

    @Override
    protected String getDatabaseID() {
        return (String) DBTYPE.sample;
    }

    @Override
    protected String getDriverClassName() {
        return "org.postgresql.Driver";
    }

    @Override
    protected SQLDialect createSQLDialect(JDBCDataStore dataStore) {
        return new PostGISDialect(dataStore);
    }

    @Override
    protected String getValidationQuery() {
        return "SELECT now()";
    }

    @Override
    protected String getJDBCUrl(Map params) throws IOException {
        String host = (String) HOST.lookUp(params);
        String db = (String) DATABASE.lookUp(params);
        int port = (Integer) PORT.lookUp(params);
        return "jdbc:postgresql" + "://" + host + ":" + port + "/" + db;
    }

    @Override
    protected JDBCDataStore createDataStoreInternal(JDBCDataStore dataStore, Map params)
            throws IOException {
        return dataStore;
    }
}
