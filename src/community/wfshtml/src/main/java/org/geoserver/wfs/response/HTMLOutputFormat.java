package org.geoserver.wfs.response;

import static org.geoserver.ows.util.ResponseUtils.buildURL;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.GetFeatureType;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

/**
 * WFS output format for a GetFeature operation in which the outputFormat is "html".
 */
public class HTMLOutputFormat extends WFSGetFeatureOutputFormat {

  /**
   * static freemaker configuration
   */
  private static Configuration cfg;

  static {
    cfg = new Configuration();
    cfg.setClassForTemplateLoading(HTMLOutputFormat.class, "");
    BeansWrapper bw = new BeansWrapper();
    bw.setExposureLevel(BeansWrapper.EXPOSE_PROPERTIES_ONLY);
    cfg.setObjectWrapper(bw);
  }
  private GeoJSONOutputFormat jsonFormatter;
  private Map<String, String> typeConversion;

  public HTMLOutputFormat(GeoServer gs) {
    //this is the name of your output format, it is the string
    // that will be used when requesting the format in a 
    // GEtFeature request: 
    // ie ;.../geoserver/wfs?request=getfeature&outputFormat=myOutputFormat
    super(gs, "html");

    // Map to convert Java types to Ext types
    typeConversion = new TreeMap<String, String>();
    typeConversion.put("Integer", "int");
    typeConversion.put("Short", "int");
    typeConversion.put("Long", "int");
    typeConversion.put("String", "string");
    typeConversion.put("Timestamp", "date");
    typeConversion.put("Float", "float");
    typeConversion.put("Double", "float");
    typeConversion.put("Boolean", "boolean");
  }

  public HTMLOutputFormat(GeoServer gs, GeoJSONOutputFormat json) {
    this(gs);
    jsonFormatter = json;
  }

  /**
   * @return "text/html";
   */
  @Override
  public String getMimeType(Object value, Operation operation)
          throws ServiceException {
    // won't allow browsers to open it directly, but that's the mime
    // state in the RFC
    return "text/html";
  }

  @Override
  public String getPreferredDisposition(Object value, Operation operation) {
    return DISPOSITION_ATTACH;
  }

  @Override
  public String getAttachmentFileName(Object value, Operation operation) {
    GetFeatureRequest request = GetFeatureRequest.adapt(operation.getParameters()[0]);
    String outputFileName = request.getQueries().get(0).getTypeNames().get(0).getLocalPart();
    return outputFileName + ".html";
  }

  /**
   * Version of write function used in GeoServer 2.1
   * 
   * @param featureCollection
   * @param output
   * @param getFeature
   * @throws IOException
   * @throws ServiceException 
   */
  protected void write(FeatureCollectionType featureCollection,
          OutputStream output, Operation getFeature) throws IOException,
          ServiceException, 
          TransformException, 
          FactoryException {

    Template template = cfg.getTemplate("WFSHTMLTemplate.ftl");
    template.setOutputEncoding("UTF-8");
    HashMap<String, Object> map = new HashMap<String, Object>();

    // Get the layer name
//    if (featureCollection.getFeature().size() == 1) {
    // Only getting Layer Name and such from the first layer (if more they are silently ignored)
    SimpleFeatureCollection features = (SimpleFeatureCollection) featureCollection.getFeature().get(0);
    SimpleFeatureType featureType = features.getSchema();
    map.put("layerName", featureType.getName().getLocalPart());
    map.put("layerNS", featureType.getName().getNamespaceURI());
    ReferencedEnvelope coord = features.getBounds().transform(DefaultGeographicCRS.WGS84, true);
    map.put("minX", coord.getMinX());
    map.put("minY", coord.getMinY());
    map.put("maxX", coord.getMaxX());
    map.put("maxY", coord.getMaxY());
//    } else {
//      map.put("layerName", "GeoServer Layers");
//    }
//      featureType.getGeometryDescriptor().getName().getLocalPart()

    // Get the geometry column's name and type
    map.put("geometryName", getGeometryColumnName(featureType));
    map.put("geometryType", getGeometryColumnType(featureType));

    // Get the features JSON
    map.put("featureJson", getFeaturesAsJson(featureCollection, getFeature));

    // get the fields and columns JSONs
    StringWriter fieldStringWriter = new StringWriter();
    GeoJSONBuilder fieldJsonBuilder = new GeoJSONBuilder(fieldStringWriter);

    StringWriter columnStringWriter = new StringWriter();
    GeoJSONBuilder columnJsonBuilder = new GeoJSONBuilder(columnStringWriter);

    fieldJsonBuilder.array();
    columnJsonBuilder.array();

    for (AttributeType attribute : featureType.getTypes()) {
      // TODO: change this check from the_geom to the binding class's name
      if ("the_geom".equalsIgnoreCase(attribute.getName().toString())) {
        continue;
      }

      // add this attribute to the fields json
      addAttributeToFieldsJson(fieldJsonBuilder, attribute);

      // add this attribute to the columns json
      addAttributeToColumnsJson(columnJsonBuilder, attribute);
    }

    fieldJsonBuilder.endArray();
    columnJsonBuilder.endArray();

    fieldStringWriter.flush();
    map.put("fieldsJson", fieldStringWriter.toString());
    columnStringWriter.flush();
    map.put("columnsJson", columnStringWriter.toString());

    // Get the wfs url
    map.put("wfsUrl",
            buildURL(
            ((GetFeatureType) getFeature.getParameters()[0]).getBaseUrl(),
            "wfs",
            null,
            URLType.SERVICE));

    try {
      template.process(map, new OutputStreamWriter(output, Charset.forName("UTF-8")));
    } catch (TemplateException ex) {
      throw new ServiceException(ex);
    }
  }

  /**
   * Version of write function used in GeoServer 2.2
   * 
   * @param featureCollection
   * @param output
   * @param getFeature
   * @throws IOException
   * @throws ServiceException 
   */
  @Override
  protected void write(FeatureCollectionResponse featureCollection,
          OutputStream output, Operation getFeature) throws IOException,
          ServiceException {
    //write out content here

    //create a writer
    BufferedWriter w = new BufferedWriter(new OutputStreamWriter(output));

    w.write("Test");
    w.flush();
  }

  /*
   * returns a String that is a JSON of the featureCollection.
   */
  private String getFeaturesAsJson(FeatureCollectionType featureCollection, Operation getFeature) throws IOException {
    ByteArrayOutputStream featureJsonStream = new ByteArrayOutputStream();
    jsonFormatter.write(featureCollection, featureJsonStream, getFeature);
    featureJsonStream.flush();
    return featureJsonStream.toString();
  }

  /*
   * Adds the attribute as an Ext Field to the JSON builder.
   */
  private GeoJSONBuilder addAttributeToFieldsJson(GeoJSONBuilder builder, AttributeType attribute) {
    builder.object();
    builder.key("name").value(attribute.getName());
    String type = getExtDataType(attribute.getBinding().getSimpleName());
    builder.key("type").value(type);

    // Add a format for dates
    if("date".equalsIgnoreCase(type)) {
      builder.key("dateFormat").value("Y-m-d H:i:s.u");
    }

    builder.endObject();
    return builder;
  }

  /*
   * Adds the attribute as an Ext ColumnModel to the JSON builder.
   * 
   * TODO: Needs to handle all types.
   */
  private GeoJSONBuilder addAttributeToColumnsJson(GeoJSONBuilder builder, AttributeType attribute) {
    builder.object();
    builder.key("dataIndex").value(attribute.getName());
    builder.key("header").value(attribute.getName());
    builder.key("sortable").value(true);

    String type = getExtDataType(attribute.getBinding().getSimpleName());
    String gridXtype = "numbercolumn";
    String editorXtype = "numberfield";

    if ("date".equalsIgnoreCase(type)) {
      gridXtype = "gridcolumn";
      editorXtype = "xdatetime";
      builder.key("width").value(160);
    } else if ("string".equalsIgnoreCase(type)) {
      gridXtype = "gridcolumn";
      editorXtype = "textfield";
    } else if ("boolean".equalsIgnoreCase(type)) {
      gridXtype = "booleancolumn";
      editorXtype = "checkbox";
    }

//    builder.key("xtype").value(gridXtype);
    builder.key("editor").object().key("xtype").value(editorXtype);

    if ("date".equalsIgnoreCase(type)) {
      builder.key("dtSeparator").value("T");
      builder.key("hiddenFormat").value("Y-m-d\\TH:i:s");
      builder.key("timeWidth").value(70);
      builder.key("timeFormat").value("H:i:s");
      builder.key("timeConfig").object().key("altFormats").value("H:i:s").key("allowBlank").value(true).endObject();
      builder.key("dateFormat").value("Y-m-d");
      builder.key("dateConfig").object().key("altFormats").value("Y-m-d").key("allowBlank").value(true).endObject();
    }
    
    builder.endObject(); // end editor

    builder.endObject();
    return builder;
  }

  private String getExtDataType(String javaType) {
    return typeConversion.get(javaType);
  }

  private boolean isGeometryAttribute(AttributeType attribute) {
    return false;
  }

  /*
   * returns String that is geometry column's name quoted or null
   * Used by OpenLayers' WFS Protocol
   */
  private String getGeometryColumnName(SimpleFeatureType featureType) {
    if (null != featureType.getGeometryDescriptor()) {
      return '"' + featureType.getGeometryDescriptor().getName().getLocalPart() + '"';
    } else {
      return "null";
    }
  }
  
  private String getGeometryColumnType(SimpleFeatureType featureType) {
    if (null != featureType.getGeometryDescriptor()) {
      return featureType.getGeometryDescriptor().getType().getBinding().getSimpleName();
    } else {
      return null;
    }
  }

  @Override
  public String getCapabilitiesElementName() {
    return "HTML";
  }
}
