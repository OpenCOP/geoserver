package org.geocent.geotools;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.geocent.netcdf.NCDataEncapsulator;
import org.geocent.netcdf.fileParsers.AbstractFileInspector;
import org.geocent.netcdf.fileParsers.NAVO.FileInspector;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.data.DataSourceException;
import org.geotools.factory.Hints;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

public class NetCDFReader extends AbstractGridCoverage2DReader implements GridCoverageReader {

    private File rootDir;
    private Rectangle actualDim;
    static GridCoverageFactory gcf = new GridCoverageFactory();
    /*
     * In the future the exact file inspector we use can be deteremined by a metadata file in the netcdf root directory, like if its NRL formatted data, etc.
     * The purpose of the AbstractFileInspector is to abstract that all away and get a buffered image back. For now we assume its all "NAVO" formatted
     */
    private AbstractFileInspector fileInsp;

    String timeAttribute;

    String elevationAttribute;

    public NetCDFReader(File netcdfRootDirectory, Hints hints) {
        rootDir = netcdfRootDirectory;
        fileInsp = new FileInspector(rootDir);
        this.coverageName = "NetCDF coverage";

        try {
            this.crs = CRS.decode("EPSG:4326");
        } catch (NoSuchAuthorityCodeException e1) {
            e1.printStackTrace();
        } catch (FactoryException e1) {
            e1.printStackTrace();
        }

        float[] fileBounds = fileInsp.getBounds();
        /*Rectangle dimensions added as: LEFT LON, BOTTOM LAT, RIGHT LON, UPPER LAT*/
        actualDim = new Rectangle((int) fileBounds[0], (int) fileBounds[2], (int) fileBounds[1], (int) fileBounds[3]);

        GeneralEnvelope env = new GeneralEnvelope(new double[] { 0, 0 }, new double[] { 0, 0 });
        env.setRange(0, fileBounds[0], fileBounds[1]);
        env.setRange(1, fileBounds[2], fileBounds[3]);
        env.setCoordinateReferenceSystem(this.crs);
        this.originalGridRange = new GridEnvelope2D(actualDim);
        this.originalEnvelope = env;

        try {
            this.highestRes = getResolution(this.originalEnvelope, actualDim, this.crs);
        } catch (DataSourceException e) {
            e.printStackTrace();
        }
        this.numOverviews = 0;
    }

    @Override
    public Format getFormat() {
        return new NetCDFFormat();
    }

    @Override
    public GridCoverage2D read(GeneralParameterValue[] params) throws IllegalArgumentException, IOException {

        ParamInformation paramInfo = paramReader(params);
        if (params == null) {
            throw new IllegalArgumentException("Params must not be null");
        }
        /*
         * TODO: Our default "in the real world" will the current time and a elevation of 0
         */
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (paramInfo.getTime() == null) {
            try {
                paramInfo.setTime(sdf.parse("2012-05-17 00:00:00"));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if (paramInfo.getElevation() == null) {
            paramInfo.setElevation(0.0);
        }
        NCDataEncapsulator ncData = fileInsp.parseFiles(paramInfo.getParameter(), paramInfo.getElevation(), paramInfo.getTime(), paramInfo);

        final GridCoverageFactory factory = new GridCoverageFactory(hints);
        GridCoverage2D coverage = factory.create(rootDir.getName(), ncData.getWritableRaster(), ncData.getGeneralEnvelope());
        return coverage;
    }

    /*
     * paramReader, toNativeCrs and toReferencedEnvelope are heavily based on the ArcSDEGridCoverage2DReaderJAI class in geotools.
     */
    @SuppressWarnings("rawtypes")
    private ParamInformation paramReader(GeneralParameterValue[] params) {
        ParamInformation parsedParams = new ParamInformation();

        if (params == null) {
            throw new IllegalArgumentException("No GeneralParameterValue given to read operation");
        }

        GeneralEnvelope reqEnvelope = null;
        GridEnvelope dim = null;
        OverviewPolicy overviewPolicy = null;
        Double elevation = null;
        Date time = null;
        String parameter = null;

        for (int i = 0; i < params.length; i++) {
            final ParameterValue<?> param = (ParameterValue<?>) params[i];
            final String name = param.getDescriptor().getName().getCode();
            if (name.equals("TIME")) {
                if (param.getValue() != null) {
                    List timeList = (List) param.getValue();
                    if (!timeList.isEmpty() && timeList.get(0) instanceof Date) {
                        time = (Date) timeList.get(0);
                    }
                }
            }

            if (name.equals("ELEVATION")) {
                final Object value = param.getValue();
                if (value != null)
                    elevation = (Double) ((List<?>) value).get(0);
            }

            if (name.equals(NetCDFFormat.PARAMETER.getName().toString())) {
                parameter = (String) param.getValue();
            }

            if (name.equals(AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString())) {
                final GridGeometry2D gg = (GridGeometry2D) param.getValue();
                reqEnvelope = new GeneralEnvelope((Envelope) gg.getEnvelope2D());

                final GeneralEnvelope coverageEnvelope = getOriginalEnvelope();
                CoordinateReferenceSystem nativeCrs = coverageEnvelope.getCoordinateReferenceSystem();
                CoordinateReferenceSystem requestCrs = reqEnvelope.getCoordinateReferenceSystem();
                if (!CRS.equalsIgnoreMetadata(nativeCrs, requestCrs)) {
                    ReferencedEnvelope nativeCrsEnv;
                    nativeCrsEnv = toNativeCrs(reqEnvelope, nativeCrs);
                    reqEnvelope = new GeneralEnvelope(nativeCrsEnv);
                }

                dim = gg.getGridRange2D();
                continue;
            }
            if (name.equals(AbstractGridFormat.OVERVIEW_POLICY.getName().toString())) {
                overviewPolicy = (OverviewPolicy) param.getValue();
                continue;
            }
        }

        if (reqEnvelope == null && dim == null) {
            reqEnvelope = getOriginalEnvelope();
            dim = getOriginalGridRange();
        }

        if (reqEnvelope == null) {
            reqEnvelope = getOriginalEnvelope();
        }
        if (dim == null) {
            final GeneralEnvelope adjustedGRange;
            try {
                MathTransform gridToWorld = getOriginalGridToWorld(PixelInCell.CELL_CENTER);
                MathTransform worldToGrid = gridToWorld.inverse();
                adjustedGRange = CRS.transform(worldToGrid, reqEnvelope);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            int xmin = (int) Math.floor(adjustedGRange.getMinimum(0));
            int ymin = (int) Math.floor(adjustedGRange.getMinimum(1));
            int xmax = (int) Math.ceil(adjustedGRange.getMaximum(0));
            int ymax = (int) Math.ceil(adjustedGRange.getMaximum(1));
            dim = new GridEnvelope2D(xmin, ymin, xmax - xmin, ymax - ymin);
        }

        if (!reqEnvelope.intersects(getOriginalEnvelope(), true)) {
            throw new IllegalArgumentException("The requested extend does not overlap the coverage extent: " + getOriginalEnvelope());
        }

        if (overviewPolicy == null) {
            overviewPolicy = OverviewPolicy.NEAREST;
        }

        parsedParams.setRequestedEnvelope(reqEnvelope);
        parsedParams.setDim(dim);
        parsedParams.setOverviewPolicy(overviewPolicy);
        parsedParams.setTime(time);
        parsedParams.setElevation(elevation);
        parsedParams.setParameter(parameter);

        return parsedParams;

    }

    private static ReferencedEnvelope toNativeCrs(final GeneralEnvelope requestedEnvelope, final CoordinateReferenceSystem nativeCRS)
            throws IllegalArgumentException {

        ReferencedEnvelope reqEnv = toReferencedEnvelope(requestedEnvelope);

        if (!CRS.equalsIgnoreMetadata(nativeCRS, reqEnv.getCoordinateReferenceSystem())) {
            try {
                reqEnv = reqEnv.transform(nativeCRS, true);
            } catch (FactoryException fe) {
                throw new IllegalArgumentException("Unable to find a reprojection from requested " + "coordsys to native coordsys for this request", fe);
            } catch (TransformException te) {
                throw new IllegalArgumentException("Unable to perform reprojection from requested " + "coordsys to native coordsys for this request", te);
            }
        }
        return reqEnv;
    }

    private static ReferencedEnvelope toReferencedEnvelope(GeneralEnvelope envelope) {
        double minx = envelope.getMinimum(0);
        double maxx = envelope.getMaximum(0);
        double miny = envelope.getMinimum(1);
        double maxy = envelope.getMaximum(1);
        CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        ReferencedEnvelope refEnv = new ReferencedEnvelope(minx, maxx, miny, maxy, crs);
        return refEnv;
    }

    /*
     * These two methods provide the support for Time and Elevation. Check out ImageMosaicReader from GeoTools-8 imagemosaic datasource module for more
     * inspiration.
     */
    @Override
    public String[] getMetadataNames() {
        final String[] parentNames = super.getMetadataNames();
        final List<String> metadataNames = new ArrayList<String>();
        metadataNames.add(TIME_DOMAIN);
        metadataNames.add(HAS_TIME_DOMAIN);
        metadataNames.add(TIME_DOMAIN_MINIMUM);
        metadataNames.add(TIME_DOMAIN_MAXIMUM);
        metadataNames.add(TIME_DOMAIN_RESOLUTION);
        metadataNames.add(ELEVATION_DOMAIN);
        metadataNames.add(ELEVATION_DOMAIN_MINIMUM);
        metadataNames.add(ELEVATION_DOMAIN_MAXIMUM);
        metadataNames.add(HAS_ELEVATION_DOMAIN);
        metadataNames.add(ELEVATION_DOMAIN_RESOLUTION);
        if (parentNames != null)
            metadataNames.addAll(Arrays.asList(parentNames));
        return metadataNames.toArray(new String[metadataNames.size()]);
    }

    @Override
    public String getMetadataValue(final String name) {
        final String superValue = super.getMetadataValue(name);
        if (superValue != null)
            return superValue;

        /*
         * TODO: This is a huge assumption, our data will NOT always have time and elevation information, we are just assuming it will always be 4d data for
         * simplicitiy, in the future this must be fixed!
         */
        if (name.equalsIgnoreCase(HAS_ELEVATION_DOMAIN))
            return String.valueOf(true);

        if (name.equalsIgnoreCase(HAS_TIME_DOMAIN))
            return String.valueOf(true);

        /* Get the time string */
        if (name.equalsIgnoreCase("time_domain")) {
            return fileInsp.getTimeString();
        }
        /* Get the elevation string */
        if (name.equalsIgnoreCase("elevation_domain")) {
            return fileInsp.getElevationString();
        }
        return superValue;
    }
}
