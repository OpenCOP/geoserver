package org.geoserver.wfs.response;

/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.GetFeatureType;
import org.apache.commons.lang.StringEscapeUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.xml.GML3OutputFormat;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;

/**
 * WFS output format for a GetFeature operation in which the outputFormat is "csv".
 * The refence specification for this format can be found in this RFC:
 * http://www.rfc-editor.org/rfc/rfc4180.txt
 *
 * @author Justin Deoliveira, OpenGeo, jdeolive@opengeo.org
 * @author Sebastian Benthall, OpenGeo, seb@opengeo.org
 * @author Andrea Aime, OpenGeo
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
  private GML3OutputFormat gmlFormatter;

  public HTMLOutputFormat(GeoServer gs) {
    //this is the name of your output format, it is the string
    // that will be used when requesting the format in a 
    // GEtFeature request: 
    // ie ;.../geoserver/wfs?request=getfeature&outputFormat=myOutputFormat
    super(gs, "html");
  }

  public HTMLOutputFormat(GeoServer gs, GeoJSONOutputFormat json, GML3OutputFormat gml) {
    this(gs);
    jsonFormatter = json;
    gmlFormatter = gml;
  }

  /**
   * @return "text/csv";
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
   * 2.1
   * @param featureCollection
   * @param output
   * @param getFeature
   * @throws IOException
   * @throws ServiceException 
   */
  protected void write(FeatureCollectionType featureCollection,
          OutputStream output, Operation getFeature) throws IOException,
          ServiceException {

    Template template = cfg.getTemplate("WFSHTMLTemplate.ftl");
    template.setOutputEncoding("UTF-8");
    HashMap<String, Object> map = new HashMap<String, Object>();

    // Get the layer name
//    if (featureCollection.getFeature().size() == 1) {
      SimpleFeatureCollection features = (SimpleFeatureCollection) featureCollection.getFeature().get(0);
      SimpleFeatureType featureType = features.getSchema();
      map.put("layerName", featureType.getName().getLocalPart());
      map.put("layerNS", featureType.getName().getNamespaceURI());
//    } else {
//      map.put("layerName", "GeoServer Layers");
//    }

    // Get the features JSON
    ByteArrayOutputStream featureJsonStream = new ByteArrayOutputStream();
    jsonFormatter.write(featureCollection, featureJsonStream, getFeature);
    map.put("getFeatureJson", featureJsonStream.toString());
    
    // Get the features GML
    ByteArrayOutputStream featureGmlStream = new ByteArrayOutputStream();
    gmlFormatter.write(featureCollection, featureGmlStream, getFeature);
    String getFeatureGml = featureGmlStream.toString(); //StringEscapeUtils.escapeHtml(featureGmlStream.toString());
//    map.put("getFeatureGml", getFeatureGml);

//            GetFeatureRequest request = GetFeatureRequest.adapt(getFeature.getParameters()[0]);

    // get the fields and columns JSONs
    List<AttributeType> attributes = featureType.getTypes();
            
    StringWriter fieldStringWriter = new StringWriter();
    GeoJSONBuilder fieldJsonBuilder = new GeoJSONBuilder(fieldStringWriter);
    
    StringWriter columnStringWriter = new StringWriter();
    GeoJSONBuilder columnJsonBuilder = new GeoJSONBuilder(columnStringWriter);

    fieldJsonBuilder.array();
    columnJsonBuilder.array();

    for( AttributeType attribute : attributes ) {
      if( "the_geom".equalsIgnoreCase(attribute.getName().toString()) ){
        continue;
      }
              
      // add this attribute to the fields json
      fieldJsonBuilder.object();
      fieldJsonBuilder.key("name").value(attribute.getName());
      fieldJsonBuilder.key("type").value(attribute.getBinding().getSimpleName());
      fieldJsonBuilder.endObject();

      // add this attribute to the columns json
      columnJsonBuilder.object();
      columnJsonBuilder.key("dataIndex").value(attribute.getName());
      columnJsonBuilder.key("header").value(attribute.getName());
      columnJsonBuilder.key("sortable").value(true);
      
      String type = attribute.getBinding().getSimpleName();
      String gridXtype = "gridcolumn";
      String editorXtype = "textfield";
      
      if( !"string".equalsIgnoreCase(type) ) {
        gridXtype = "numbercolumn";
        editorXtype = "numberfield";
      }

      columnJsonBuilder.key("xtype").value(gridXtype);
      columnJsonBuilder.key("editor").object().key("xtype").value(editorXtype).endObject();
      
      columnJsonBuilder.endObject();
      
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
              ((GetFeatureType)getFeature.getParameters()[0]).getBaseUrl(), 
              "wfs", 
              null, 
              URLType.SERVICE)
            );
    

    try {
      //            ByteArrayOutputStream buff = new ByteArrayOutputStream();
      template.process(map, new OutputStreamWriter(output, Charset.forName("UTF-8")));
      //create a writer
      //    BufferedWriter w = new BufferedWriter(new OutputStreamWriter(output));
      //
      //    w.flush();
    } catch (TemplateException ex) {
      throw new ServiceException(ex);
    }
  }

  /**
   * 2.2
   * @see WFSGetFeatureOutputFormat#write(Object, OutputStream, Operation)
   */
  @Override
  protected void write(FeatureCollectionResponse featureCollection,
          OutputStream output, Operation getFeature) throws IOException,
          ServiceException {
    //write out content here

    //create a writer
    BufferedWriter w = new BufferedWriter(new OutputStreamWriter(output));

    w.write("Booyah");

//        //get the feature collection
//        SimpleFeatureCollection fc = 
//            (SimpleFeatureCollection) featureCollection.getFeature().get(0);


//        //write out the header
//        SimpleFeatureType ft = fc.getSchema();
//        w.write("FID,");
//        for ( int i = 0; i < ft.getAttributeCount(); i++ ) {
//            AttributeDescriptor ad = ft.getDescriptor( i );
//            w.write( prepCSVField(ad.getLocalName()) );
//               
//            if ( i < ft.getAttributeCount()-1 ) {
//               w.write( "," );
//            }
//        }
//        // by RFC each line is terminated by CRLF
//        w.write( "\r\n" );
//        
//        // prepare the formatter for numbers
//        NumberFormat coordFormatter = NumberFormat.getInstance(Locale.US);
//        coordFormatter.setMaximumFractionDigits(getInfo().getGeoServer().getGlobal().getNumDecimals());
//        coordFormatter.setGroupingUsed(false);
//           
//        //write out the features
//        SimpleFeatureIterator i = fc.features();
//        try {
//            while( i.hasNext() ) {
//                SimpleFeature f = i.next();
//                // dump fid
//                w.write(prepCSVField(f.getID()));
//                w.write(",");
//                // dump attributes
//                for ( int j = 0; j < f.getAttributeCount(); j++ ) {
//                    Object att = f.getAttribute( j );
//                    if ( att != null ) {
//                        String value = null;
//                        if(att instanceof Number) {
//                            // don't allow scientific notation in the output, as OpenOffice won't 
//                            // recognize that as a number 
//                            value = coordFormatter.format(att);
//                        } else if(att instanceof Date) {
//                            // serialize dates in ISO format
//                            if(att instanceof java.sql.Date)
//                                value = DateUtil.serializeSqlDate((java.sql.Date) att);
//                            else if(att instanceof java.sql.Time)
//                                value = DateUtil.serializeSqlTime((java.sql.Time) att);
//                            else
//                                value = DateUtil.serializeDateTime((Date) att);
//                        } else {
//                            // everything else we just "toString"
//                            value = att.toString();
//                        }
//                        w.write( prepCSVField(value) );
//                    }
//                    if ( j < f.getAttributeCount()-1 ) {
//                        w.write(",");    
//                    }
//                }
//                // by RFC each line is terminated by CRLF
//                w.write( "\r\n" );
//            }
//        } finally {
//            fc.close( i );
//        }

    w.flush();
  }

  /*
   * The CSV "spec" explains that fields with certain properties must be
   * delimited by double quotes, and also that double quotes within fields
   * must be escaped.  This method takes a field and returns one that
   * obeys the CSV spec.
   */
//    private String prepCSVField(String field){
//    	// "embedded double-quote characters must be represented by a pair of double-quote characters."
//    	String mod = field.replaceAll("\"", "\"\"");
//    	
//    	/*
//    	 * Enclose string in double quotes if it contains double quotes, commas, or newlines
//    	 */
//    	if(mod.matches(".*(\"|\n|,).*")){
//    		mod = "\"" + mod + "\"";
//    	}
//    	
//		return mod;
//    	
//    }
  @Override
  public String getCapabilitiesElementName() {
    return "HTML";
  }
}
