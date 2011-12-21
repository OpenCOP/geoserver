<html>
  <head>
    <title>WFS GetFeature Response</title>
<!-- 
    <script type="text/javascript" src="http://extjs.cachefly.net/ext-4.0.0/adapter/ext/ext-base.js"></script>
    <script type="text/javascript" src="http://extjs.cachefly.net/ext-4.0.0/ext-all-debug.js"></script>
    <link rel="stylesheet" type="text/css" href="http://extjs.cachefly.net/ext-4.0.0/resources/css/ext-all.css" />
    <link rel="stylesheet" type="text/css" href="http://extjs.cachefly.net/ext-4.0.0/examples/shared/examples.css" />
    <script src="http://www.openlayers.org/api/2.10/OpenLayers.js"></script>
    <script type="text/javascript" src="http://api.geoext.org/1.0/script/GeoExt.js"></script>
 -->

    <!-- Ext includes -->
    <script src="/geoserver/lib/ext-3.4.0/adapter/ext/ext-base.js" type="text/javascript"></script>
<!--    <script type="text/javascript" src="http://extjs.cachefly.net/ext-3.4.0/ext-all-debug.js"></script>-->
    <script src="/geoserver/lib/ext-3.4.0/ext-all.js" type="text/javascript"></script>
    <link rel="stylesheet" type="text/css" href="/geoserver/lib/ext-3.4.0/resources/css/ext-all.css" />
 
    <!-- OpenLayers includes -->
    <script src="/geoserver/lib/openlayers/OpenLayers.js" type="text/javascript"></script>
<!--    <link rel="stylesheet" type="text/css" href="/EOC/theme/default/style.css"/>-->

    <!-- GeoExt includes -->
    <script src="/geoserver/lib/GeoExt/lib/GeoExt.js" type="text/javascript"></script>    
    <link rel="stylesheet" type="text/css" href="/geoserver/lib/GeoExt/resources/css/geoext-all-debug.css" />
    <link rel="stylesheet" type="text/css" href="/geoserver/lib/GeoExt/resources/css/popup.css" />
    <link rel="stylesheet" type="text/css" href="/geoserver/lib/css/silk.css">
    <link rel="stylesheet" type="text/css" href="/geoserver/lib/css/geosilk.css">
<!-- -->


    <script type="text/javascript">
      var featureJson = ${featureJson};
      var fieldsJson = ${fieldsJson};
      var columnsJson = ${columnsJson};

      var featureStore, map, mapPanel, app;

      function mapd() {
        console.dir(map);
      }
      function stored() {
        console.dir(featureStore);
      }

      Ext.onReady(function() {
        // create map instance
        // If you create a map without specifying controls, it creates 
        // with default controls that use images that don't exist.
        // So, I'm manually specifying them so they'll use the correct images.
        map = new OpenLayers.Map({
          controls: [new OpenLayers.Control.Navigation(),
            new OpenLayers.Control.Attribution(),
            new OpenLayers.Control.PanPanel(),
            new OpenLayers.Control.ZoomPanel() ]
        });
        
        var baseLayer = new OpenLayers.Layer.WMS(
          "Base Layer",
          "http://vmap0.tiles.osgeo.org/wms/vmap0",
          {layers: 'basic'}
        );

        // create vector layer
        var vectorLayer = new OpenLayers.Layer.Vector("${layerName?js_string}");
        map.addLayers([baseLayer, vectorLayer]);
        loadData();

        function loadData() {
          var reader = new OpenLayers.Format.GeoJSON();
          var vecs = reader.read(featureJson);
          vectorLayer.addFeatures(vecs);
        }

        // create map panel
        mapPanel = new GeoExt.MapPanel({
          title: "Map",
          region: "center",
          height: 300,
          //width: 600,
          map: map
        });

        // create feature store, binding it to the vector layer
        featureStore = new GeoExt.data.FeatureStore({
          layer: vectorLayer,
          fields: fieldsJson,
          autoLoad: false,

          featureFilter: new OpenLayers.Filter({
            evaluate: function(feature) {
              // Don't want deleted features showing up in the store.
              return feature.state != OpenLayers.State.DELETE
            }
          }),

          proxy: new GeoExt.data.ProtocolProxy({
            protocol: new OpenLayers.Protocol.WFS({
              url: "${wfsUrl}", 
              version: "1.1.0",
              featureType: "${layerName?js_string}", 
              featureNS: "${layerNS?js_string}", 
              srsName: "EPSG:4326", //"EPSG:900913",
              geometryName: ${geometryName},
              maxFeatures: 250
            })
          })
        });

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
                var feature = new OpenLayers.Feature.Vector();
                feature.state = OpenLayers.State.INSERT;
                vectorLayer.addFeatures([feature]);
              }};
        </#if>

        // Select Model links the grid and the map together.
        // Defining it explicitly so the modifyFeature control can use its
        // selectFeature control.
        var modControl = new OpenLayers.Control.ModifyFeature(vectorLayer,{standalone:true});
        map.addControl(modControl);
        var selModel = new GeoExt.grid.FeatureSelectionModel();
        
        var editButton = new Ext.Button({
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

        var feature_table = {
          xtype: "editorgrid",
          ref: "feature_table",
          title: "${layerName?js_string}",
          iconCls: 'silk_table_find',
          region: "north",
          height: 300,
          sm: selModel,
          store: featureStore,
          columns: columnsJson,
          bbar: [
            createButton,
          {
            text: "Delete Feature",
            iconCls: "silk_table_delete",
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
          },
            editButton,
          {
            text: "Undo Changes",
            iconCls: "silk_arrow_undo",
            handler: function() {
              vectorLayer.removeAllFeatures();
              editButton.toggle(false);
              loadData();
            }
          },
          {
            text: "Save Changes",
            iconCls: "silk_table_save",
            handler: saveVectorLayer
          }]
        }


        // init app
        app = new Ext.Viewport({
          layout: "border",
          items: [mapPanel, feature_table]
        });

        function saveVectorLayer() {

          // if feature has changed (and not by update or delete), change its
          // state to reflect that
          Ext.each(vectorLayer.features, function(n) {
            if (!n.state && !equalAttributes(n.data, n.attributes)) {
              n.state = OpenLayers.State.UPDATE;
            }
          })

          // commit vector layer via WFS-T
          app.feature_table.store.proxy.protocol.commit(
            vectorLayer.features,
            {
              callback: function() {
                // refresh everything the user sees
                window.location.reload();
              }
            }
          );
        }

        selModel.selectControl.onSelect = function(feature) {
          if(editButton.pressed) {
            modControl.selectFeature(feature);
          } else {
            if( feature.layer.selectedFeatures.length == 1 ) {
              editButton.enable();
            } else {
              editButton.disable();
            }
          }
        };        
        selModel.selectControl.onUnselect = function(feature) {
          if(editButton.pressed) {
            modControl.unselectFeature(feature);
          } else {
            if( feature.layer.selectedFeatures.length == 1 ) {
              editButton.enable();
            } else {
              editButton.disable();
            }
          }
        };

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