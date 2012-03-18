package org.geocent.geotools;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.geocent.netcdf.NCDataCacher;
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
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
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
    //private Rectangle actualDim = new Rectangle(-180, -90, 179, 89);
    private Rectangle actualDim;
    static GridCoverageFactory gcf = new GridCoverageFactory();
    /*
     * In the future the exact file inspector we use can be deteremined by a
     * metadata file in the netcdf root directory, like if its NRL formatted
     * data, etc. The purpose of the AbstractFileInspector is to abstract that
     * all away and get a buffered image back. For now we assume its all "NAVO"
     * formatted
     */
    private AbstractFileInspector fileInsp = new FileInspector();

    public NetCDFReader(File netcdfRootDirectory, Hints hints) {
        rootDir = netcdfRootDirectory;
        this.coverageName = "NetCDF coverage";

        try {
            this.crs = CRS.decode("EPSG:4326");
        } catch (NoSuchAuthorityCodeException e1) {
            e1.printStackTrace();
        } catch (FactoryException e1) {
            e1.printStackTrace();
        }

        float[] fileBounds = fileInsp.getBounds(rootDir);
        actualDim = new Rectangle((int) fileBounds[0], (int) fileBounds[2], (int) fileBounds[1], (int) fileBounds[3]);

        GeneralEnvelope env = new GeneralEnvelope(new double[]{0, 0}, new double[]{0, 0});
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

        final Envelope geographicArea = new Envelope2D(DefaultGeographicCRS.WGS84, -180, -89, 360, 180);

        /*
         * Our default "in the real world" will the current time and a elevation
         * of 0, but since we have old data, the default for now will be
         * 10-15-2010 since I know we have some stuff for it
         */
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (paramInfo.time == null) {
            try {
                paramInfo.time = sdf.parse("2010-10-15 00:00:00");
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (paramInfo.elevation == null) {
            paramInfo.elevation = 0.0;
        }
        NCDataEncapsulator savedNCData = NCDataCacher.getNCData(paramInfo.parameter, paramInfo.elevation, paramInfo.time);
        if (savedNCData == null) {
            NCDataEncapsulator ncData = fileInsp.parseFiles(rootDir, paramInfo.parameter, paramInfo.elevation, paramInfo.time, paramInfo.requestedEnvelope);
            savedNCData = ncData;
            NCDataCacher.putNCData(paramInfo.parameter, paramInfo.elevation, paramInfo.time, ncData);
        }

        NCDataEncapsulator ncData = savedNCData;

        final GridCoverageFactory factory = new GridCoverageFactory(hints);
        GridCoverage2D coverage = factory.create(rootDir.getName(), ncData.getWritableRaster(), geographicArea);
        return coverage;
    }

    /*
     * paramReader, toNativeCrs and toReferencedEnvelope are heavily based on
     * the ArcSDEGridCoverage2DReaderJAI class in geotools.
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
                if (param.getValue() != null && ((Double) param.getValue()) != 0.0) {
                    elevation = (Double) param.getValue();
                }
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

        parsedParams.requestedEnvelope = reqEnvelope;
        parsedParams.dim = dim;
        parsedParams.overviewPolicy = overviewPolicy;
        parsedParams.time = time;
        parsedParams.elevation = elevation;
        parsedParams.parameter = parameter;

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
}
