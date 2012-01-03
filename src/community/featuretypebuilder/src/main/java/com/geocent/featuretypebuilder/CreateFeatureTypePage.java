package com.geocent.featuretypebuilder;

//import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.GeoServerSecuredPage;
import org.apache.wicket.markup.html.basic.Label;

public class CreateFeatureTypePage extends GeoServerSecuredPage {

  public CreateFeatureTypePage() {
    add(new Label("hellolabel", "hello world"));
  }
}
