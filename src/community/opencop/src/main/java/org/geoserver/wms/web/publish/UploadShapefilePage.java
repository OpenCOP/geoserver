package org.geoserver.wms.web.publish;

import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.lang.Bytes;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.rest.util.RESTUtils;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.data.store.StoreListChoiceRenderer;
import org.geoserver.web.data.store.PostGISStoreListModel;
import org.restlet.data.MediaType;
import org.springframework.security.userdetails.User;

public class UploadShapefilePage extends AbstractOpenCopPage {

  private final Form form;
  private final DropDownChoice stores;
  private final Model defaultStyleModel;
  private final TextField<String> layerTitle;
  private final FileUploadField file;
  private final CheckBox addEditLinks;
  private final User principal;

  public UploadShapefilePage() {
    // get the current user's principal
    principal = (User) this.getSession().getAuthentication().getPrincipal();

    // create the form
    form = new Form("form");
    add(form);

    // create the datastore picker, only include PostGIS stores
    stores = new DropDownChoice("storesDropDown", new Model(), 
             new PostGISStoreListModel(), new StoreListChoiceRenderer());
    stores.setOutputMarkupId(true);
    stores.setRequired(true);
    form.add(stores);

    // Add the style picker elements
    defaultStyleModel = new Model();
    addStylePicker(form);

    // create the title field
    form.add(layerTitle = new TextField<String>("layerTitle", new Model<String>()));
    
    // create the file upload field
    file = new FileUploadField("file");
    file.setRequired(true);
    form.add(file);

    // create the add edit links checkbox
    form.add(addEditLinks = new CheckBox("addEditLinks", new Model<Boolean>(true)));

    // create the save and cancel buttons
    form.add(new BookmarkablePageLink("cancel", GeoServerHomePage.class));
    SubmitLink saveLink = new SubmitLink("save", form) {
      @Override
      public void onSubmit() {
        submit();
      }
    };
    form.add(saveLink);
    form.setDefaultButton(saveLink);

  }

  private void addStylePicker(Form form) {
    // default styleXml chooser. A default styleXml is required
    final DropDownChoice defaultStyle = new DropDownChoice("defaultStyle", defaultStyleModel,
            new StylesModel(), new StyleChoiceRenderer());
    defaultStyle.setOutputMarkupId(true);
    defaultStyle.setRequired(true);
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
  }

  protected void submit() {
    FileUpload upload = file.getFileUpload();
    
    if (upload == null) {
      error("No file uploaded");
      return;
    }

    // Get the REST publisher object
    GeoServerRESTPublisher rest = new GeoServerRESTPublisher(
            getBaseUrl(), principal.getUsername(), principal.getPassword());

    File fileToUnzip = null;
    File fileToSend = null;
    try {
      // Get workspace and store names
      StoreInfo storeInfo = (StoreInfo) stores.getModelObject();
      String workspace = storeInfo.getWorkspace().getName();
      String store = storeInfo.getName();

      // Create two temp files because the unzip utility deletes the file.
      // Need one to unzip and get the shapefile name and the second to send
      // to the REST service to publish.
      fileToUnzip = upload.writeToTempFile();
      fileToSend = upload.writeToTempFile();

      // Get the name of the shapefile from within the uploaded zip
      String sfname = getShapefileName(fileToUnzip, upload.getContentType(), workspace, store);

      // Send the zipfile through the rest api
      if (null == sfname || !rest.publishShp(workspace, store, sfname, fileToSend)) {
        error("Unable to publish shapefile.");
      } else {    
        info("File-Name: " + upload.getClientFileName() + " File-Size: "
            + Bytes.bytes(upload.getSize()).toString());
        Catalog catalog = getCatalog();
        // Set the title and style for the layer
        updateLayerInfo(catalog, workspace, sfname);
        
        // Only add the edit links if the user wants
        if (addEditLinks.getModelObject()) {
          // Get all the catalog objects needed
          LayerInfo layerInfo = catalog.getLayerByName(workspace + ":" + sfname);
          String layerName = layerInfo.getName();
      
          addEditLinks(storeInfo, layerName);
        }
      }
    } catch (IOException ex) {
      Logger.getLogger(UploadShapefilePage.class.getName()).log(Level.SEVERE, null, ex);
    } finally {
      // Close and delete all the files.
      if (null != fileToSend) {
        fileToSend.delete();
      }
      if (null != fileToUnzip) {
        fileToUnzip.delete();
      }
      try {
        upload.closeStreams();
      } finally {
        upload.delete();
      }
    }
  }
  
  private String getShapefileName(File uploadedFile, String mediaType, String workspaceName, String storeName) {
    File directory = null;
    String name = null;

    // Make sure the uploaded file is a zip file
    if (null == mediaType || !RESTUtils.isZipMediaType( MediaType.valueOf(mediaType) ) ) { 
      error("Uploaded file is not a zip file. MediaType: " + mediaType);
      return null;
    }

    try {
      // Create a temp directory to extract the zip file
      directory = getCatalog().getResourceLoader().findOrCreateDirectory("temp", "shp2pg", workspaceName, storeName);
      // Unzip the uploaded file (side effect of deleting the zip)
      RESTUtils.unzipFile(uploadedFile, directory);

      // Assume the zipfile contains only one shapefile so get the first one
      File primary = findFirstFile(directory, "shp");

      if (null != primary) {
        // get the shapefile's name without the .shp extension
        name = FilenameUtils.getBaseName(primary.getName());
      } else {
        error("Zip file did not contain a '.shp' file.");
      }

    } catch(IOException ex) {
      error("Unable to get shapefile name");
    } finally {
      try {
        // Clean up when done
        FileUtils.deleteDirectory(directory);
      } catch (IOException ex) {
        Logger.getLogger(UploadShapefilePage.class.getName()).log(Level.SEVERE, null, ex);
      }
    }

    return name;
  }

  /**
   * 
   * @param directory
   * @param format
   * @return
   */
  protected File findFirstFile(File directory, String format) {
    Iterator f = FileUtils.listFiles(directory, new String[]{format}, false).iterator();
    if (f.hasNext()) {
      //assume the first
      return (File) f.next();
    }

    return null;
  }

  private void updateLayerInfo(Catalog catalog, String workspace, String layer) {
    // Get the newly created layer
    LayerInfo layerInfo = catalog.getLayerByName(workspace + ":" + layer);
    // Get the chosen style
    StyleInfo styleInfo = (StyleInfo) defaultStyleModel.getObject();
    // Set the default style for the layer
    layerInfo.setDefaultStyle(styleInfo);
    // Set the chosen title for the layer
    ResourceInfo resourceInfo = layerInfo.getResource();
    String title = layerTitle.getModelObject();
    if (null != title) {
      resourceInfo.setTitle(title);
    }
    // Save the layer and resource
    catalog.save(layerInfo);
  }

}