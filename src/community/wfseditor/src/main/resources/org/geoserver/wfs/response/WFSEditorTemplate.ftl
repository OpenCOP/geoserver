<html>
  <head>
    <title>WFS GetFeature Response</title>

    <script src="/geoserver/lib/openlayers/OpenLayers.js" type="text/javascript"></script>
    <script src="/geoserver/lib/ext-3.4.0/adapter/ext/ext-base.js" type="text/javascript"></script>
<!--    <script type="text/javascript" src="http://extjs.cachefly.net/ext-3.4.0/ext-all-debug.js"></script>-->
    <script src="/geoserver/lib/ext-3.4.0/ext-all.js" type="text/javascript"></script>
    <script src="/geoserver/lib/ext-3.4.0/ext-datetime.js" type="text/javascript"></script>
    <script src="/geoserver/lib/GeoExt/GeoExt.js" type="text/javascript"></script>    

    <link rel="stylesheet" type="text/css" href="/geoserver/lib/GeoExt/resources/css/geoext-all-debug.css" />
    <link rel="stylesheet" type="text/css" href="/geoserver/lib/GeoExt/resources/css/popup.css" />
    <link rel="stylesheet" type="text/css" href="/geoserver/lib/ext-3.4.0/resources/css/ext-all.css" />
    <link rel="stylesheet" type="text/css" href="/geoserver/lib/css/silk.css">
    <link rel="stylesheet" type="text/css" href="/geoserver/lib/css/geosilk.css">

    <script src="/geoserver/lib/openlayers/proj4js-compressed.js" type="text/javascript"></script>
    <!-- Projection definitions -->
    <script src="/geoserver/lib/openlayers/defs/projections.js" type="text/javascript"></script>
    
    <script type="text/javascript">
      var featureJson = ${featureJson};
      var fieldsJson = ${fieldsJson};
      var columnsJson = ${columnsJson};

      var map, app, mapProjection, vectorProjection, vectorLayer;

      Ext.onReady(function() {
        // Add a renderer to all date columns
        Ext.each(columnsJson, function(column){
          if( column.editor && column.editor.xtype && column.editor.xtype.match(/xdatetime/i) ) {
            column.renderer = Ext.util.Format.dateRenderer('Y-m-d H:i:s');
          }
        });

        /*** PROJECTIONS ***/
        // Set the map and vector layer projections
        setProjections();
        
        /*** STRATEGY ***/
        // Build the save strategy for the vector layer
        var saveStrategy = new OpenLayers.Strategy.Save();
        registerSaveStrategyEvents(saveStrategy);

        /*** LAYERS ***/
        // Add a default base layer
        var baseLayer = new OpenLayers.Layer.OSM();
        
        // create vector layer for the requested features
        vectorLayer = buildVectorLayer(saveStrategy);
        
        /*** MAP ***/
        // Build the map object
        map = buildMap();
        map.addLayers([baseLayer, vectorLayer]);
        
        // Load the features from the JSON into the vector layer
        loadDataFromJson();

        /*** CONTROLS ***/
        // Either there is geometry and the user should be able to draw it
        // when creating.
        <#if geometryType?? >
        function getDrawControl() {
          var gt = "${(geometryType!"")?js_string}";
          var handler = null;
          if( gt.match(/Line/i) ) {
            handler = "Path";
          } else if( gt.match(/Polygon/i) ) {
            handler = "Polygon";
          } else if( gt.match(/Point/i) ) {
            var handler = "Point";
          }
          if( null != handler ) {
            return new OpenLayers.Control.DrawFeature(vectorLayer, 
              OpenLayers.Handler[handler],
              {
                multi: (gt.match(/Multi/i)) ? true : false
              });
          } else {
            return null;
          }
        }

        var drawControl = getDrawControl();
        drawControl.featureAdded = function(feature) {
          selModel.unlock();
          selModel.selectControl.select(feature);
          createButton.toggle(false);
        };
        map.addControls([drawControl]);
        
        var createButton = new Ext.Button({
          text: "Create Feature",
          iconCls: "silk_table_add",
          enableToggle: true,
          toggleHandler: function(button, state) {
            if( true === state ) {
              modifyButton.toggle(false);
              selModel.selectControl.unselectAll();
              selModel.lock();
              drawControl.activate();
            } else {
              drawControl.deactivate();
              selModel.unlock();
            }
          }
        });

        // Or the layer is aspatial and just add a feature to the grid.
        <#else>
        var createButton = {
          text: "Create Feature",
          iconCls: "silk_table_add",
          handler: function() {
  //            selModel.selectControl.unselectAll();
            var feature = new OpenLayers.Feature.Vector();
            feature.state = OpenLayers.State.INSERT;
            vectorLayer.addFeatures([feature]);
            selModel.selectControl.select(feature);
          }
        };
        </#if>

        var deleteButton = new Ext.Button({
          text: "Delete Feature",
          iconCls: "silk_table_delete",
          disabled: true,
          handler: function() {
            // Turn off the modifyButton
            modifyButton.toggle(false);

            app.featureTable.getSelectionModel().each(function(rec) {
              var feature = rec.getFeature();
                          
              // There shouldn't be any features selected since the delete will
              // remove all selected features.  Calling unselect() to make
              // sure the onUnselect handler is called.
              selModel.selectControl.unselect(feature);
      
              // TODO: I believe this could be a better implementation.
              // Try it out when you get time.
//              if(feature.fid == undefined) {
//                vectorLayer.destroyFeatures([feature]);
//              } else {
//                feature.state = OpenLayers.State.DELETE;
//                vectorLayer.events.triggerEvent("afterfeaturemodified", {feature: feature});
//                feature.renderIntent = "select";
//                vectorLayer.drawFeature(feature);
//              }

              vectorLayer.removeFeatures([feature]);
              if (feature.state != OpenLayers.State.INSERT) {
                // Set the state to DELETE
                feature.state = OpenLayers.State.DELETE;
                // add the deleted feature back to the layer (will not render)
                vectorLayer.addFeatures([feature]);
              }
            });

          }
        });

        // Select Model links the grid and the map together.
        // Defining it explicitly so the modifyFeature control can use its
        // selectFeature control.
        var modControl = new OpenLayers.Control.ModifyFeature(vectorLayer,{standalone:true});
        map.addControl(modControl);
        var selModel = new GeoExt.grid.FeatureSelectionModel();
        
        var modifyButton = new Ext.Button({
          text: "Move Feature",
          iconCls: "silk_table_edit",
          enableToggle: true,
          disabled: true,
          toggleHandler: function(button, state) {
            if( true === state ) {
              var feature = selModel.selectControl.layer.selectedFeatures[0];
              if( feature && undefined !== feature && null !== feature ) {
                modControl.selectFeature(feature);
              }
              selModel.selectControl.multipleKey = null;
              selModel.singleSelect = true;
            } else {
              modControl.unselectFeature();
              selModel.selectControl.multipleKey = "ctrlKey";
              selModel.singleSelect = false;
            }
          }
        });

        var undoButton = {
          text: "Undo Changes",
          iconCls: "silk_arrow_undo",
          handler: function() {
            // Turn off all Buttons
            createButton.toggle(false);
            modifyButton.toggle(false);
            // Remove all the features from the vector layer
            selModel.selectControl.unselectAll();
            vectorLayer.removeAllFeatures();
            // Load the original data back into the vector layer
            loadDataFromJson();
          }
        };

        var saveButton = {
          text: "Save Changes",
          iconCls: "silk_table_save",
          handler: saveVectorLayer
        };

        var csvButton = {
          text: "Download CSV",
          iconCls: "silk_arrow_down",
          handler: function() {
            window.open("${csvLink?js_string}");
          }
        }

        var shapefileButton = {
          text: "Download Shapefile",
          iconCls: "silk_arrow_down",
          handler: function() {
            window.open("${shapefileLink?js_string}");
          }
        }

        /*** PANELS ***/
        // Editor grid for the requested features
        var featureTable = {
          xtype: "editorgrid",
          ref: "featureTable",
          title: "${layerName?js_string}",
          iconCls: 'silk_table_find',
          region: "center",
          height: 300,
          sm: selModel,
          store: new GeoExt.data.FeatureStore({
            layer: vectorLayer,
            fields: fieldsJson,
            autoLoad: false,

            featureFilter: new OpenLayers.Filter({
              evaluate: function(feature) {
                // Don't want deleted features showing up in the store.
                return feature.state != OpenLayers.State.DELETE
              }
            })
          }),
          columns: columnsJson,
          bbar: [
            createButton,
            deleteButton,
            modifyButton,
            '-',
            undoButton,
            saveButton,
            '->',
            csvButton,
            shapefileButton
          ]
        }

        // create map panel
        var mapPanel = { 
          xtype: "gx_mappanel",
          ref: "map_panel",
          id: "map_panel",
          title: "Map",
          region: "south",
          height: document.body.clientHeight - 300,
          split: true,
          collapsible: true,
          collapsed: null == vectorProjection,
          map: map
        };

        /*** VIEWPORT ***/
        // Main viewport for the whole app
        app = new Ext.Viewport({
          layout: "border",
          items: [featureTable, mapPanel]
        });

        // Saves the vector layer by commiting through WFS-T
        function saveVectorLayer() {
          // if feature has changed (and not by insert or delete), change its
          // state to reflect that
          Ext.each(app.featureTable.store.getModifiedRecords(), function(record) {
            var feature = record.getFeature();
            if (!feature.state) {
              feature.state = OpenLayers.State.UPDATE;
            }
            feature.attributes = record.getChanges();
            
            // So apparently GeoExt has a state in its data for each record.
            // This should match up to feature.state (OpenLayers state).
            // This collides quite nicely with an actually attribute named state.
            // Not sure what to do about it right now, so just remove it from the
            // attributes so that it doesn't break the transaction.
            delete feature.attributes.state;
          });

          // commit vector layer via WFS-T
          saveStrategy.save();
        }

        // Add select and unselect handlers to the select control to tell
        // the modify control which feature to modify.
        selModel.selectControl.onSelect = function(feature) {
          if(modifyButton.pressed) {
            modControl.selectFeature(feature);
          } else {
            enableModifyButton(feature);
          }
          enableDeleteButton(feature);
        };        
        selModel.selectControl.onUnselect = function(feature) {
          if(modifyButton.pressed) {
            modControl.unselectFeature(feature);
          } else {
            enableModifyButton(feature);
          }
          enableDeleteButton(feature);
        };

        function enableModifyButton(feature) {
          // Set the modify button enabled/disabled
          if( feature.layer.selectedFeatures.length == 1 ) {
            modifyButton.enable();
          } else {
            modifyButton.disable();
          }
        }

        function enableDeleteButton(feature) {
          // Set the delete button enabled/disabled
          if( feature.layer.selectedFeatures.length > 0 ) {
            deleteButton.enable();
          } else {
            deleteButton.disable();
          }
        }

        // Putting this down here, seems to need to happen after something.
        // So its after everything.
        var initialExtent = vectorLayer.getDataExtent();
        if( null !== initialExtent ) {
          map.zoomToExtent(initialExtent, true);
        }

    }); // end Ext.onReady()

    /*** PROJECTIONS ***/
    /*
     * Creates the projection objects for the map and the vector layer.
     */
    function setProjections() {
      mapProjection = new OpenLayers.Projection("EPSG:900913");
      <#if geometryProjection??>
        vectorProjection = new OpenLayers.Projection("${(geometryProjection!"")?js_string}");
      <#else> // most likely aspatial
        vectorProjection = null;
      </#if>
    }

    /*** STRATEGY ***/
    /*
     * Builds and registers callback functions for the savestrategy's success
     * and fail events.
     */
    function registerSaveStrategyEvents(saveStrategy) {
      // Alert the user that the WFS-T has succeeded
      saveStrategy.events.register('success', null, function(evt){
        var message = "<h3>Summary</h3><table>";
        Ext.each(evt.response.priv.responseXML.documentElement.firstChild.childNodes, function(item){
          message += "<tr><td>" + 
            ((null != item.localName) ? item.localName : item.baseName) + 
            ":</td><td>" + 
            ((null != item.textContent) ? item.textContent : item.text) + 
            "</td></tr>";
        });
        message += "</table>The page will now reload to reflect your changes.";
        Ext.MessageBox.show({
          title: "WFS Transaction Success",
          msg: message,
          minWidth: 500,
          buttons: Ext.MessageBox.OK,
          fn: function(btn) {
            window.location.reload();
          },
          icon: Ext.MessageBox.INFO
        });
      });

      // Alert the user that the WFS-T has failed
      saveStrategy.events.register('fail', null, function(evt){
        var message = "";
        if( null != evt.response.error.exceptionReport ) {
          Ext.each(evt.response.error.exceptionReport.exceptions, function(ex){
            message += "<h3>" + ex.code + "</h3>";
            var texts = ex.texts;
            if( texts.length > 0 ) {
              message += "<ul>";
              Ext.each(texts, function(text){
                message += "<li>" + text.substring(0, 250) + "</li>";
              });
              message += "</ul>";
            }
          });
        } else {
          message = evt.response.priv.responseText;
        }
        
        Ext.MessageBox.show({
          title: "WFS Transaction Failure",
          msg: message,
          minWidth: 500,
          buttons: Ext.MessageBox.OK,
          icon: Ext.MessageBox.ERROR
        });
      });
    }

    /*** LAYERS ***/
    /*
     * Builds the vector layer that contains the requested features.
     */
    function buildVectorLayer(saveStrategy) {
      return new OpenLayers.Layer.Vector("${layerName?js_string}",
        {
          strategies:[saveStrategy],
          projection: vectorProjection,
          protocol: new OpenLayers.Protocol.WFS({
            url: "${wfsUrl}", 
            version: "1.1.0",
            featureType: "${layerName?js_string}", 
            featureNS: "${layerNS?js_string}", 
            srsName: (null != vectorProjection) ? vectorProjection.getCode() : null, 
            geometryName: ${geometryName},
            maxFeatures: 250
          })
        });
    }

    /*
     * Loads features from the JSON to the vector layer
     */
    function loadDataFromJson() {
      var reader = new OpenLayers.Format.GeoJSON({
        internalProjection: mapProjection,
        externalProjection: vectorProjection
      });
      var vecs = reader.read(featureJson);
      vectorLayer.addFeatures(vecs);
    }

    /*** MAP ***/
    /*
     * Builds the map object.
     */
    function buildMap() {
      // create map instance
      return new OpenLayers.Map({
        projection: mapProjection.getCode(),
        displayProjection: vectorProjection,
        // If you create a map without specifying controls, it creates 
        // with default controls that use images that don't exist.
        // So, I'm manually specifying them so they'll use the correct images.
        controls: [ new OpenLayers.Control.Navigation(),
          new OpenLayers.Control.Attribution(),
          new OpenLayers.Control.PanPanel(),
          new OpenLayers.Control.ZoomPanel(),
          new OpenLayers.Control.MousePosition() ]
      });
    }
    </script>    
  </head>
  <body>
  </body>
</html>
