<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
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

<!-- -->
    <script src="/EOC/ext-3.4.0/adapter/ext/ext-base.js" type="text/javascript"></script>
    <script type="text/javascript" src="http://extjs.cachefly.net/ext-3.4.0/ext-all-debug.js"></script>
 <!--   <script src="/EOC/ext-3.4.0/ext-all.js" type="text/javascript"></script>
 -->   <script src="/EOC/js/lib/OpenLayers.js" type="text/javascript"></script>
    <script src="/EOC/GeoExt/lib/GeoExt.js" type="text/javascript"></script>    
    <link rel="stylesheet" type="text/css" href="/EOC/GeoExt/resources/css/geoext-all-debug.css"></link>
    <link rel="stylesheet" type="text/css" href="/EOC/GeoExt/resources/css/popup.css">
    <link rel="stylesheet" type="text/css" href="/EOC/ext-3.4.0/resources/css/ext-all.css"></link>
    <link rel="stylesheet" type="text/css" href="/EOC/css/silk.css">
    <link rel="stylesheet" type="text/css" href="/EOC/css/geosilk.css">
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
        map = new OpenLayers.Map();
        var wmsLayer = new OpenLayers.Layer.WMS(
          "vmap0",
          "http://vmap0.tiles.osgeo.org/wms/vmap0",
          {layers: 'basic'}
        );

        // create vector layer
        var vectorLayer = new OpenLayers.Layer.Vector("${layerName}");
        map.addLayers([wmsLayer, vectorLayer]);

        var reader = new OpenLayers.Format.GeoJSON();
        var vecs = reader.read(featureJson);
        vectorLayer.addFeatures(vecs);

        // create map panel
        mapPanel = new GeoExt.MapPanel({
          title: "Map",
          region: "south",
          height: 400,
          //width: 600,
          map: map,
          center: new OpenLayers.LonLat(-90, 30),
          zoom: 6
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
              featureType: "${layerName}", 
              featureNS: "${layerNS}", 
              srsName: "EPSG:4326", //"EPSG:900913",
              geometryName: ${geometryName},
              maxFeatures: 250
            })
          })
        });

        var feature_table = {
          xtype: "editorgrid",
          ref: "feature_table",
          title: "${layerName}",
          iconCls: 'silk_table_find',
          region: "center",
          height: 300,
          sm: new GeoExt.grid.FeatureSelectionModel(),
          store: featureStore,
          columns: columnsJson,
          bbar: [{
            text: "Delete",
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
          {
            text: "Save",
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
                var layers = app.map_panel.map.layers;
                for (var i = layers.length - 1; i >= 0; --i) {
                  layers[i].redraw(true);
                }
                app.feature_table.store.reload();
              }
            }
          );
        }

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