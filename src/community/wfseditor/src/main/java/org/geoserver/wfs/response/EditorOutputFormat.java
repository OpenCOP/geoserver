package org.geoserver.wfs.response;

import static org.geoserver.ows.util.ResponseUtils.buildURL;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Set;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.GetFeatureType;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.referencing.NamedIdentifier;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * WFS output format for a GetFeature operation in which the outputFormat is "editor".
 */
public class EditorOutputFormat extends WFSGetFeatureOutputFormat {

  /**
   * static freemaker configuration
   */
  private static Configuration cfg;

  static {
    cfg = new Configuration();
    cfg.setClassForTemplateLoading(EditorOutputFormat.class, "");
    BeansWrapper bw = new BeansWrapper();
    bw.setExposureLevel(BeansWrapper.EXPOSE_PROPERTIES_ONLY);
    cfg.setObjectWrapper(bw);
  }
  private GeoJSONOutputFormat jsonFormatter;

  public EditorOutputFormat(GeoServer gs) {
    //this is the name of your output format, it is the string
    // that will be used when requesting the format in a 
    // GEtFeature request: 
    // ie ;.../geoserver/wfs?request=getfeature&outputFormat=myOutputFormat
    super(gs, "editor");
  }

  public EditorOutputFormat(GeoServer gs, GeoJSONOutputFormat json) {
    this(gs);
    jsonFormatter = json;
  }

  /**
   * @return "text/html";
   */
  @Override
  public String getMimeType(Object value, Operation operation)
          throws ServiceException {
    return "text/html";
  }

//  @Override
//  public String getPreferredDisposition(Object value, Operation operation) {
//    return DISPOSITION_ATTACH;
//  }
//
//  @Override
//  public String getAttachmentFileName(Object value, Operation operation) {
//    GetFeatureRequest request = GetFeatureRequest.adapt(operation.getParameters()[0]);
//    String outputFileName = request.getQueries().get(0).getTypeNames().get(0).getLocalPart();
//    return outputFileName + ".html";
//  }

  /**
   * Version of write function used in GeoServer 2.1
   * 
   * @param featureCollection
   * @param output
   * @param getFeature
   * @throws IOException
   * @throws ServiceException 
   */
  @Override
  protected void write(FeatureCollectionType featureCollection,
          OutputStream output, Operation getFeature) throws IOException,
          ServiceException {

    Template template = cfg.getTemplate("WFSEditorTemplate.ftl");
    template.setOutputEncoding("UTF-8");
    HashMap<String, Object> map = new HashMap<String, Object>();

    // Get the layer name
//    if (featureCollection.getFeature().size() == 1) {
    // Only getting Layer Name and such from the first layer (if more they are silently ignored)
    SimpleFeatureCollection features = (SimpleFeatureCollection) featureCollection.getFeature().get(0);
    SimpleFeatureType featureType = features.getSchema();
    map.put("layerName", featureType.getName().getLocalPart());
    map.put("layerNS", featureType.getName().getNamespaceURI());
//    } else {
//      map.put("layerName", "GeoServer Layers");
//    }
//      featureType.getGeometryDescriptor().getName().getLocalPart()

    // Get the geometry column's name and type
    GeometryDescriptor geometryDescriptor = featureType.getGeometryDescriptor();
    map.put("geometryName", getGeometryColumnName(geometryDescriptor));
    map.put("geometryType", getGeometryColumnType(geometryDescriptor));
    map.put("geometryProjection", getGeometryColumnProjection(geometryDescriptor));

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
      if (isGeometryAttribute(geometryDescriptor, attribute)) {
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
//  @Override
//  protected void write(FeatureCollectionResponse featureCollection,
//          OutputStream output, Operation getFeature) throws IOException,
//          ServiceException {
//    //write out content here
//
//    //create a writer
//    BufferedWriter w = new BufferedWriter(new OutputStreamWriter(output));
//
//    w.write("Test");
//    w.flush();
//  }

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
    return ExtTypes.valueOf(javaType).getExtType();
  }

  private boolean isGeometryAttribute(GeometryDescriptor geometryDescriptor, 
          AttributeType attribute) {
    if (null != geometryDescriptor && null != geometryDescriptor.getType()) {
      return geometryDescriptor.getType().equals(attribute);
    }
    return false;
  }

  /*
   * returns String that is geometry column's name quoted or null quoted
   * Used by OpenLayers' WFS Protocol
   */
  private String getGeometryColumnName(GeometryDescriptor geometryDescriptor) {
    if (null != geometryDescriptor) {
      return '"' + geometryDescriptor.getName().getLocalPart() + '"';
    } else {
      return "null";
    }
  }
  
  private String getGeometryColumnType(GeometryDescriptor geometryDescriptor) {
    if (null != geometryDescriptor) {
      return geometryDescriptor.getType().getBinding().getSimpleName();
    } else {
      return null;
    }
  }
  
  /**
   * Returns the geometry's projection in the format needed by OpenLayers (e.g., "EPSG:4326").
   * 
   * @param featureType
   * @return 
   */
  private String getGeometryColumnProjection(GeometryDescriptor geometryDescriptor) {
    // This implementation is based off of GeoJSONOutputFormat.
    // For some reason crs.getName() is different than the first crs.getIdentifiers()
    // even though they return the same class and there is only one in crs.getIdentifiers()
    if (null != geometryDescriptor) {               
      CoordinateReferenceSystem crs = geometryDescriptor.getCoordinateReferenceSystem();
    
      if (null != crs) {
        Set<ReferenceIdentifier> ids = crs.getIdentifiers();
    
        // WKT defined crs might not have identifiers at all
        if(ids != null && ids.size() > 0) {
            NamedIdentifier id = (NamedIdentifier) ids.iterator().next();
            return id.getCodeSpace().toUpperCase() + ":" + id.getCode();
        }
      }
    } 
    
    return null;
  }

  @Override
  public String getCapabilitiesElementName() {
    return "editor";
  }

  /*
   * Provides a conversion from Java types to Ext types.
   */
  private enum ExtTypes {
    Integer("int"),
    Short("int"),
    Long("int"),
    String("string"),
    Timestamp("date"),
    Float("float"),
    Double("float"),
    Boolean("boolean");

    private String extType;

    private ExtTypes(String t) {
      this.extType = t;
    }

    public String getExtType() {
      return this.extType;
    }
  }
}
