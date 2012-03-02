package org.geoserver.wms.web.publish;

import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.util.lang.Bytes;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.store.StoreListChoiceRenderer;
import org.geoserver.web.data.store.StoreListModel;
import org.geoserver.wms.web.publish.LegendGraphicAjaxUpdater;
import org.geoserver.wms.web.publish.StyleChoiceRenderer;
import org.geoserver.wms.web.publish.StylesModel;
import org.springframework.security.userdetails.User;

public class UploadShapefilePage extends GeoServerSecuredPage {

  private final FileUploadField file;
  private final TextField<String> username;
  private final TextField<String> password;
  private final TextField<String> url;
  private final TextField<String> layerTitle;
  private final TextField<String> shapefileName;
  private final DropDownChoice stores;
  private final Form<?> form;
  private final Model defaultStyleModel;
  private final User principal;

  public UploadShapefilePage() {
    principal = (User) this.getSession().getAuthentication().getPrincipal();
    
    // Get the base url of this geoserver
    WebRequest request = (WebRequest) getRequest();
    HttpServletRequest httpServletRequest;
    httpServletRequest = ((WebRequest) request).getHttpServletRequest();
    final String baseUrl = ResponseUtils.baseURL(httpServletRequest);

    // create the form
    form = new Form<Void>("form") {

      /**
       * @see org.apache.wicket.markup.html.form.Form#onSubmit()
       */
      @Override
      protected void onSubmit() {

        // display uploaded info
        FileUpload upload = file.getFileUpload();
        if (upload == null) {
          info("No file uploaded");
        } else {
          info("File-Name: " + upload.getClientFileName() + " File-Size: "
                  + Bytes.bytes(upload.getSize()).toString());
          GeoServerRESTPublisher rest = new GeoServerRESTPublisher(
//                  url.getModelObject(), username.getModelObject(), password.getModelObject());
                  baseUrl, principal.getUsername(), principal.getPassword());
          File tmp = null;
          try {
            // Send the zipfile through the rest api
            tmp = upload.writeToTempFile();
            StoreInfo storeInfo = (StoreInfo) stores.getModelObject();
            if( !rest.publishShp(storeInfo.getWorkspace().getName(), storeInfo.getName(), shapefileName.getModelObject(), tmp) ) {
              error("Something went wrong :(");
            } else {
              StyleInfo styleInfo = (StyleInfo) defaultStyleModel.getObject();
              LayerInfo layerInfo = getCatalog().getLayerByName(storeInfo.getWorkspace().getName() + ":" + shapefileName.getModelObject());
              layerInfo.setDefaultStyle(styleInfo);
              ResourceInfo resourceInfo = layerInfo.getResource();
              resourceInfo.setTitle(layerTitle.getModelObject());
              getCatalog().save(resourceInfo);
              layerInfo.setResource(resourceInfo);
              getCatalog().save(layerInfo);
            }
          } catch (IOException ex) {
            Logger.getLogger(UploadShapefilePage.class.getName()).log(Level.SEVERE, null, ex);
          } finally {
            if (null != tmp) {
              tmp.delete();
            }
            upload.closeStreams();
            upload.delete();
          }
        }
      }
    };

//    form.setMaxSize(Bytes.megabytes(1));
    add(form);




    

      // default styleXml chooser. A default styleXml is required
      defaultStyleModel = new Model();
      final DropDownChoice defaultStyle = new DropDownChoice("defaultStyle", defaultStyleModel,
              new StylesModel(), new StyleChoiceRenderer());
      defaultStyle.setOutputMarkupId(true);
      form.add(defaultStyle);

      // Add the Style's legend graphic to the page
      final Image defStyleImg = new Image("defaultStyleLegendGraphic");
      defStyleImg.setOutputMarkupId(true);
      form.add(defStyleImg);

      // Add a legend graphic ajax updater object
      String wmsURL = getRequest().getRelativePathPrefixToContextRoot();
      wmsURL += wmsURL.endsWith("/") ? "wms?" : "/wms?";
      final LegendGraphicAjaxUpdater defaultStyleUpdater;
      defaultStyleUpdater = new LegendGraphicAjaxUpdater(wmsURL, defStyleImg, defaultStyleModel);

      // Add an onChange action to the styleXml drop down that uses the legend
      // ajax updater to change the legend graphic on the page.
      defaultStyle.add(new OnChangeAjaxBehavior() {
        @Override
        protected void onUpdate(AjaxRequestTarget target) {
          defaultStyleUpdater.updateStyleImage(target);
        }
      });


    // create the input fields
    form.add(file = new FileUploadField("file"));
    form.add(username = new TextField<String>("username", new Model<String>()));
    form.add(password = new PasswordTextField("password", new Model<String>()));
    form.add(url = new TextField<String>("url", new Model<String>(baseUrl)));
    form.add(layerTitle = new TextField<String>("layerTitle", new Model<String>()));
    form.add(shapefileName = new TextField<String>("shapefileName", new Model<String>()));

    stores = new DropDownChoice("storesDropDown", new Model(), new StoreListModel(), new StoreListChoiceRenderer());
    stores.setOutputMarkupId(true);
    form.add(stores);

  }
}