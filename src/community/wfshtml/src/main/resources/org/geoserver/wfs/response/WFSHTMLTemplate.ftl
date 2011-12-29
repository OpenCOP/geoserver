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

    <script type="text/javascript">
      var featureJson = ${featureJson};
      var fieldsJson = ${fieldsJson};
      var columnsJson = ${columnsJson};

      var map, mapPanel, app;

      function mapd() {
        console.dir(map);
      }
      function appd() {
        console.dir(app);
      }

      Ext.onReady(function() {
        // Add a renderer to all date columns
        Ext.each(columnsJson, function(column){
          if( column.editor && column.editor.xtype && column.editor.xtype.match(/xdatetime/i) ) {
            column.renderer = Ext.util.Format.dateRenderer('Y-m-d H:i:s');
          }
        });

        // create map instance
        // If you create a map without specifying controls, it creates 
        // with default controls that use images that don't exist.
        // So, I'm manually specifying them so they'll use the correct images.
        map = new OpenLayers.Map({
          controls: [ new OpenLayers.Control.Navigation(),
            new OpenLayers.Control.Attribution(),
            new OpenLayers.Control.PanPanel(),
            new OpenLayers.Control.ZoomPanel() ]
        });
        
        // Add a default base layer
        var baseLayer = new OpenLayers.Layer.WMS(
          "Base Layer",
          "http://vmap0.tiles.osgeo.org/wms/vmap0",
          {layers: 'basic'}
        );

        var saveStrategy = new OpenLayers.Strategy.Save();
        // Alert the user that the WFS-T has succeeded
        saveStrategy.events.register('success', null, function(evt){
          var message = "<h3>Summary</h3><table>";
          Ext.each(evt.response.priv.responseXML.childNodes[0].childNodes[0].childNodes, function(item){
            message += "<tr><td>" + item.localName + ":</td><td>" + item.textContent + "</td></tr>";
          });
          message += "</table>The page will now reload to reflect your changes.";
          Ext.MessageBox.show({
            title: "WFS Transaction Success",
            msg: message,
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
          Ext.each(evt.response.error.exceptionReport.exceptions, function(ex){
            message += "<h3>" + ex.code + "</h3>";
            var texts = ex.texts;
            if( texts.length > 0 ) {
              message += "<ul>";
              Ext.each(texts, function(text){
                message += "<li>" + text + "</li>";
              });
              message += "</ul>";
            }
          });
          Ext.MessageBox.show({
            title: "WFS Transaction Failure",
            msg: message,
            buttons: Ext.MessageBox.OK,
            icon: Ext.MessageBox.ERROR
          });
        });

        // create vector layer for the requested features
        var vectorLayer = new OpenLayers.Layer.Vector("${layerName?js_string}",
        {
          strategies:[saveStrategy],
          protocol: new OpenLayers.Protocol.WFS({
                url: "${wfsUrl}", 
                version: "1.1.0",
                featureType: "${layerName?js_string}", 
                featureNS: "${layerNS?js_string}", 
                srsName: "EPSG:4326", //"EPSG:900913",
                geometryName: ${geometryName},
                maxFeatures: 250
              })
        });
        map.addLayers([baseLayer, vectorLayer]);
        loadData();

        // Loads features from the JSON to the vector layer
        function loadData() {
          var reader = new OpenLayers.Format.GeoJSON();
          var vecs = reader.read(featureJson);
          vectorLayer.addFeatures(vecs);
        }

        // create map panel
        mapPanel = { 
          xtype: "gx_mappanel",
          ref: "map_panel",
          id: "map_panel",
          title: "Map",
          region: "center",
          height: 300,
          //width: 600,
          map: map
        };

        // Either there is geometry and the user should be able to draw it
        // when creating.
        // Or the layer is aspatial and just add a feature to the grid.
        <#if geometryType?? >
        var createButton = new GeoExt.Action({
          text: "Create Feature",
          iconCls: "silk_table_add",
          enableToggle: true,
          map: map,
          control: getDrawControl()
        });
          
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
            return new OpenLayers.Control.DrawFeature(vectorLayer, OpenLayers.Handler[handler]);
          } else {
            return null;
          }
        }

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

            app.feature_table.getSelectionModel().each(function(rec) {
              var feature = rec.getFeature();
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
            vectorLayer.removeAllFeatures();
            modifyButton.toggle(false);
            deleteButton.toggle(false);
            loadData();
          }
        };

        var saveButton = {
          text: "Save Changes",
          iconCls: "silk_table_save",
          handler: saveVectorLayer
        };

        // Editor grid for the requested features
        var feature_table = {
          xtype: "editorgrid",
          ref: "feature_table",
          title: "${layerName?js_string}",
          iconCls: 'silk_table_find',
          region: "north",
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
            undoButton,
            saveButton 
          ]
        }

        // Main viewport for the whole app
        app = new Ext.Viewport({
          layout: "border",
          items: [mapPanel, feature_table]
        });

        // Saves the vector layer by commiting through WFS-T
        function saveVectorLayer() {
          // if feature has changed (and not by insert or delete), change its
          // state to reflect that
          Ext.each(app.feature_table.store.getModifiedRecords(), function(record) {
            var feature = record.getFeature();
            if (!feature.state) {
              feature.state = OpenLayers.State.UPDATE;
            }
            feature.attributes = record.getChanges();
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
        map.zoomToExtent(
          new OpenLayers.Bounds(
            ${minX?c}, 
            ${minY?c},
            ${maxX?c}, 
            ${maxY?c}
          ), 
          true
        );

    }); // end Ext.onReady()

    // Objects with the same keys and values (excluding functions) are equal.
    //   Example: {a: 1, :b: 2} == {a: 1, :b: 2} != {a: 1, b: 2, c: 3}.
    function equalAttributes(objA, objB) {
      // Yes, I feel bad about how hacky this is.  But it seems to work.
      return Ext.encode(objA) === Ext.encode(objB)
    }
    </script>    
  </head>
  <body>
  </body>
</html>