# wfseditor: WFS GetFeature Output Format - Editor

wfseditor is a GeoServer extension that provides a new WFS output format.  This new format is analogous to the WMS application/openlayers format.  The response is an HTML page that contains a GeoExt grid that allows editing feature attributes and an OpenLayers map for manipulating the geometry.  There are also controls for adding and deleting features.  The changes are committed back to GeoServer through WFS-T.


## Implementation notes:
* Tested against Geoserver 2.1.2.  Won't work with 2.2 as implemented since the WFSGetFeatureOutputFormat.write() method signature changes between 2.1 and 2.2.
* This project depends on the geoext extenstion, which just serves the GeoExt libraries out of geoserver.
* The map uses an OpenStreetMap baselayer.  If your layer uses a non-well-known projection, make sure the proj4 definition is included in the projections.js file.
* I couldn't figure out how to get the original request URL, so I write the GeoJSON out to the page and use an OpenLayers.Format.GeoJSON to read in the features.  WFS 1.0 lists coordinates lon lat, but WFS 1.1 lists coordinates lat lon.  I have only been able to make the GeoJSON reader read in the coordinates that are lon lat.  As such, the editor only works properly with a WFS 1.0 request.
* Currently, does not handle a request on multiple layers.  The response will just use the first layer.
