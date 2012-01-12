package org.geoserver.uploadshapefileui;

import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.lang.Bytes;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.store.StoreListChoiceRenderer;
import org.geoserver.web.data.store.StoreListModel;

public class UploadShapefilePage extends GeoServerSecuredPage {

  private final FileUploadField file;
  private final TextField<String> username;
  private final TextField<String> password;
  private final TextField<String> url;
  private final TextField<String> layername;
  private final DropDownChoice stores;
  private final Form<?> form;

  public UploadShapefilePage() {

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
                  url.getModelObject(), username.getModelObject(), password.getModelObject());
          File tmp = null;
          try {
            // Send the zipfile through the rest api
            tmp = upload.writeToTempFile();
            StoreInfo storeInfo = (StoreInfo) stores.getModelObject();
            rest.publishShp(storeInfo.getWorkspace().getName(), storeInfo.getName(), layername.getModelObject(), tmp);
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

    // create the input fields
    form.add(file = new FileUploadField("file"));
    form.add(username = new TextField<String>("username", new Model<String>()));
    form.add(password = new PasswordTextField("password", new Model<String>()));
    form.add(url = new TextField<String>("url", new Model<String>()));
    form.add(layername = new TextField<String>("layername", new Model<String>()));

    stores = new DropDownChoice("storesDropDown", new Model(), new StoreListModel(), new StoreListChoiceRenderer());
    stores.setOutputMarkupId(true);
    form.add(stores);

  }
}