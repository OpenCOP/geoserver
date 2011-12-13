package org.geotools.image.io.netcdf;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageReadParam;

import org.geotools.coverage.Category;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.data.DataSourceException;
import org.geotools.factory.Hints;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.NumberRange;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.geometry.Envelope;


import java.awt.image.RenderedImage;
import javax.imageio.ImageReader;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.io.GridCoverageReader;
import org.geotools.geometry.Envelope2D;
import org.geotools.parameter.Parameter;
import org.geotools.util.Range;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;

public class NetcdfReader extends AbstractGridCoverage2DReader implements GridCoverageReader{

    /**
     * The entry to log messages during the process.
     */
    private static final Logger LOGGER = Logger.getLogger(NetcdfReader.class.toString());
    /**
     * The reader Spi for netCDF images.
     */
    private NetcdfImageReader.Spi readerSpi;
    /** The {@link URL} that points to the file to use. */
    private URL urlToUse;

    /**
     * Constructs a reader for a netCDF file.
     *
     * @param format The default netcdf format.
     * @param input The netcdf file or url for this file.
     * @param hints Null in this implementation.
     * @throws DataSourceException
     */
    public NetcdfReader(Object source, Hints hints) throws IOException {


        if (source == null) {
            throw new DataSourceException(
                    "NetcdfReader:No source set to read this coverage.");
        }
        if (source instanceof File) {
            urlToUse = ((File) source).toURL();
        } else if (source instanceof URL) {
            // we only allow files
            urlToUse = (URL) source;
        } else if (source instanceof String) {
            try {
                // is it a filename?
                urlToUse = new File((String) source).toURL();
            } catch (MalformedURLException e) {
                // is it a URL
                urlToUse = new URL((String) source);
            }
        } else {
            throw new IllegalArgumentException("Illegal input argument!");
        }

        if (hints != null) {
            this.hints.add(hints);
        }
        this.source = source;

        // ///////////////////////////////////////////////////////////
        //
        // decoding source
        //
        // ///////////////////////////////////////////////////////////
        final String filename;

        try {
            filename = URLDecoder.decode(urlToUse.getFile(), "UTF-8");
        } catch (UnsupportedEncodingException use) {
            MalformedURLException exception = new MalformedURLException(
                    new StringBuffer("Unable to decode ").append(urlToUse).append(" cause ").append(use.getMessage()).toString());
            exception.initCause(exception);
            throw exception;
        }

        boolean recognized = false;
        boolean extUpperCase = false;

        if (filename.endsWith(".nc")) {
            recognized = true;
        }

        if (!recognized) {
            throw new IOException(
                    "Unrecognized file (file extension doesn't match)");
        }

        //hardcoded
        //can we do this other way?
        this.coverageName = "TMP";

        final Rectangle actualDim = new Rectangle( -82, 8, 42, 30);
        this.originalGridRange = new GridEnvelope2D(actualDim);
//        originalGridRange = new GeneralGridRange(new Rectangle(0, 0, 360, 180));

        // ///////////////////////////////////////////////////////////
        //
        // Build the coordinate system and the envelope
        //
        // ///////////////////////////////////////////////////////////
        final Object tempCRS = this.hints.get(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM);
        if (tempCRS != null) {
            this.crs = (CoordinateReferenceSystem) tempCRS;
            LOGGER.log(Level.WARNING, new StringBuffer(
                    "Using forced coordinate reference system ").append(
                    crs.toWKT()).toString());
        } else {
            crs = initCRS();
        }
        this.originalEnvelope = getBounds(crs);

        // /////////////////////////////////////////////////////////////////////
        //
        // Compute source Resolution
        //
        // /////////////////////////////////////////////////////////////////////
        highestRes = getResolution(originalEnvelope, new Rectangle( -82, 8, 42, 30), crs);
        numOverviews = 0;
        overViewResolutions = null;
    }

    /**
     * @see org.opengis.coverage.grid.GridCoverageReader#read(org.opengis.parameter.GeneralParameterValue[])
     */
    @Override
    public GridCoverage2D read(
            final GeneralParameterValue[] params)
            throws java.lang.IllegalArgumentException, java.io.IOException {
        // /////////////////////////////////////////////////////////////////////
        //
        // do we have paramters to use for reading from the specified source
        //
        // /////////////////////////////////////////////////////////////////////
        GeneralEnvelope requestedEnvelope = null;
        Rectangle dim = null;
        if (params != null) {
            // /////////////////////////////////////////////////////////////////////
            //
            // Checking params
            //
            // /////////////////////////////////////////////////////////////////////
            if (params != null) {
                Parameter param;
                final int length = params.length;
                for (int i = 0; i < length; i++) {
                    param = (Parameter) params[i];

                    if (param.getDescriptor().getName().getCode().equals(
                            AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString())) {
                        final GridGeometry2D gg = (GridGeometry2D) param.getValue();
                        requestedEnvelope = new GeneralEnvelope((Envelope) gg.getEnvelope2D());
                        dim = gg.getGridRange2D().getBounds();
                    }
                }
            }
        }

        // /////////////////////////////////////////////////////////////////////
        //
        // Building the required coverage
        //
        // /////////////////////////////////////////////////////////////////////
        return getGridCoverage(requestedEnvelope, dim);
    }

    
     public GridCoverage2D getGridCoverage(GeneralEnvelope requestedEnvelope, Rectangle dim) throws IllegalArgumentException, IOException {
        try {
            // Experimental; will be replaced by something more generic soon (work in progress)
            //readerSpi = (NetcdfImageReader.Spi) Class.forName("fr.geomatys.image.io.netcdf.TemperatureReaderSpi").newInstance();
            readerSpi = new NetcdfImageReader.Spi();
        } catch (Exception e) {
            throw new IOException(e.toString());
        }
        //final ImageReader reader = readerSpi.createReaderInstance(null);
        final ImageReader reader = readerSpi.createReaderInstance();
        reader.setInput(source);

        final ImageReadParam param = reader.getDefaultReadParam();
//        param.setSourceRegion(dim);
//        final Rectangle region = new Rectangle(0, 0, dim.getWidth(), dim.getHeight());
        final Rectangle region = new Rectangle(0, 0, 337, 241);
        param.setSourceRegion(region);

        //still limited to 1 depth level
        int[] depthLevels = new int[]{0};
        param.setSourceBands(depthLevels);

//        RenderedImage image = reader.read(0, param);
        BufferedImage image = reader.read(0, param);

        //manually set this metadata once again
        //final Envelope geographicArea = new Envelope2D(DefaultGeographicCRS.WGS84,);
//        final Envelope geographicArea = new Envelope2D(DefaultGeographicCRS.WGS84, -180, -89, 360, 180);
        final Envelope geographicArea = new Envelope2D(DefaultGeographicCRS.WGS84, -98, 8, 42, 30);
//        final NumberRange temperatureRange = new NumberRange(7000f, 10000f);
        final NumberRange sampleValueRange = new NumberRange(new Range(Float.class, 0f, 12000f));
        final NumberRange temperatureRange = new NumberRange(new Range(Float.class, 0f, 30f));
//        final Color[] colorPalette = new Color[]{Color.RED, Color.GREEN, Color.BLUE};
        final Color[] colorPalette = new Color[]{Color.BLUE.brighter(), Color.YELLOW.brighter(), Color.RED.darker()};
        final Color transparent = new Color(0, 0,0,0);
        

        GridSampleDimension temperature = new GridSampleDimension("Temperature", new Category[]{
            new Category("Nodata", transparent, Float.NaN),
            //new Category("Temperature", colorPalette, sampleValueRange, temperatureRange)
            new Category("Bubba", colorPalette, sampleValueRange, .0010, 20.0)
//            new Category("Bubba", colorPalette, temperatureRange, .00001, .0000001)
        }, javax.measure.unit.SI.CELSIUS);
        
        GridSampleDimension[] gridSampleDimensionArray = new GridSampleDimension[] { temperature }; 
        
        
        
//        GridSampleDimension temperature = new GridSampleDimension("Temperature", new Category[]{
//            Category.NODATA,
//            new Category("Temperature", colorPalette, sampleValueRange, temperatureRange)
//        }, javax.measure.unit.SI.KELVIN);
        
        
        // Necessary because our data are already geophysics.
        //temperature = temperature.geophysics(false);

        /*
         * Now create the coverage.
         */
//        final GridCoverageFactory factory = new GridCoverageFactory(hints);
//        GridCoverage2D coverage = factory.create("Temperature", image, geographicArea,
//                new GridSampleDimension[]{
//            temperature
//        // If we had more bands, we would enumerate them here.
//        }, null, null);

        final GridCoverageFactory factory = new GridCoverageFactory(hints);

//        GridCoverage2D coverage = factory.create("Temperature", image, geographicArea);
        GridCoverage2D coverage = factory.create("Temperature", image.getRaster(), geographicArea, gridSampleDimensionArray); 

      
        // return the coverage
        
        return coverage;
    }
    
    
    /*
    private GridCoverage2D getGridCoverage(GeneralEnvelope requestedEnvelope,
            Rectangle dim) throws IOException {
        int hrWidth = originalGridRange.getLength(0);
        int hrHeight = originalGridRange.getLength(1);

        // /////////////////////////////////////////////////////////////////////
        //
        // Setting subsampling factors with some checkings
        // 1) the subsampling factors cannot be zero
        // 2) the subsampling factors cannot be such that the w or h are zero
        //
        // /////////////////////////////////////////////////////////////////////
        final ImageReadParam readP = new ImageReadParam();
        final Integer imageChoice;
        try {
            imageChoice = setReadParams(readP, requestedEnvelope, dim);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
            return null;
        } catch (TransformException e) {
            LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
            return null;
        }

        
        
        // /////////////////////////////////////////////////////////////////////
        //
        // Statistics
        //
        // /////////////////////////////////////////////////////////////////////
        final int max = stats.getMax();
        final int min = stats.getMin();

        // /////////////////////////////////////////////////////////////////////
        //
        // Preparing to load
        //
        // /////////////////////////////////////////////////////////////////////
        // trying to create a channel to the file to read
        final String filePath = URLDecoder.decode(this.demURL.getFile(),
                "UTF-8");
        final ImageInputStream iis = ImageIO.createImageOutputStream(new File(
                filePath));
        if (header.getByteOrder().compareToIgnoreCase("M") == 0) {
            iis.setByteOrder(ByteOrder.BIG_ENDIAN);
        } else {
            iis.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        }

        // Prepare temporaray colorModel and sample model, needed to build the
        // RawImageInputStream
        final ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false, false,
                Transparency.OPAQUE, DataBuffer.TYPE_SHORT);
        final SampleModel sm = cm.createCompatibleSampleModel(hrWidth, hrHeight);
        final ImageTypeSpecifier its = new ImageTypeSpecifier(cm, sm);
        // Finally, build the image input stream
        final RawImageInputStream raw = new RawImageInputStream(iis, its,
                new long[]{0}, new Dimension[]{new Dimension(hrWidth,
            hrHeight)
        });

        // building the final image layout
        final Dimension tileSize = ImageUtilities.toTileSize(new Dimension(
                hrWidth, hrHeight));
        final ImageLayout il = new ImageLayout(0, 0, hrWidth / readP.getSourceXSubsampling(), hrHeight / readP.getSourceYSubsampling(), 0, 0, (int) tileSize.getWidth(), (int) tileSize.getHeight(), sm, cm);

        // First operator: read the image
        final RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT,
                il);
        final ParameterBlock pbjImageRead = new ParameterBlock();
        pbjImageRead.add(raw);
        pbjImageRead.add(imageChoice);
        pbjImageRead.add(Boolean.FALSE);
        pbjImageRead.add(Boolean.FALSE);
        pbjImageRead.add(Boolean.FALSE);
        pbjImageRead.add(null);
        pbjImageRead.add(null);
        pbjImageRead.add(readP);
        pbjImageRead.add(imageIOSPI.createReaderInstance());
        RenderedOp image = JAI.create("ImageRead", pbjImageRead, hints);

        // sample dimension for this coverage
        final GridSampleDimension band = getSampleDimension(max, min);

        // setting metadata
        final Map metadata = new HashMap();
        metadata.put("maximum", new Double(stats.getMax()));
        metadata.put("minimum", new Double(stats.getMin()));
        metadata.put("mean", new Double(stats.getAverage()));
        metadata.put("std_dev", new Double(stats.getStdDev()));
        metadata.put("nodata", new Double(-9999.0));

        // /////////////////////////////////////////////////////////////////////
        //
        // Creating coverage
        //
        // /////////////////////////////////////////////////////////////////////
        // cleaning name
        String coverageName = (new File(this.coverageName)).getName();
        final int extension = coverageName.lastIndexOf(".");
        if (extension != -1) {
            String ext = coverageName.substring(extension + 1);

            if ((dmext.compareToIgnoreCase(ext) == 0) || (dhext.compareToIgnoreCase(ext) == 0) || (srext.compareToIgnoreCase(ext) == 0) || (shext.compareToIgnoreCase(ext) == 0) || (stext.compareToIgnoreCase(ext) == 0)) {
                coverageName = coverageName.substring(0, extension);
            }
        }

        // return the coverage
        return (GridCoverage2D) coverageFactory.create(coverageName, image,
                new GeneralEnvelope(originalEnvelope),
                new GridSampleDimension[]{band}, null, metadata);
        
    }
    */
    

    /**
     * Gets the bounding box of this datasource using the default speed of this
     * datasource as set by the implementer.
     * 
     * @param lonFirst
     * 
     * @return The bounding box of the datasource or null if unknown and too
     *         expensive for the method to calculate.
     * 
     * @throws IOException
     * 
     */
    private GeneralEnvelope getBounds(CoordinateReferenceSystem crs)
            throws IOException {
        GeneralEnvelope env = new GeneralEnvelope(new double[]{0, 0},
                new double[]{0, 0});

        /*
        // preparing data for the envelope
        final double xULC = header.getULXMap();
        final double yULC = header.getULYMap();
        final double xDim = header.getXDim();// dx
        final double yDim = header.getYDim();// dy
        final int imageWidth = header.getNCols();
        final int imageHeight = header.getNRows();
        final double longMin;
        final double latMax;
        final double longMax;
        final double latMin;
        longMin = xULC - xDim / 2.0;
        latMax = yULC + yDim / 2.0;
        longMax = longMin + imageWidth * xDim;
        latMin = latMax - imageHeight * yDim;
         */

        //hardcoded for now
        //should get this from metadata store
//        final double longMin = -180;
//        final double longMax = 180;
//        final double latMin = -89;
//        final double latMax = 90;
        
        final double longMin = -124;
        final double longMax = -82;
        
// -82, 8, 42, 30
        final double latMin = 8;
        final double latMax = 38;

        // longitude
        env.setRange(0, longMin, longMax);
        // latitude
        env.setRange(1, latMin, latMax);

        env.setCoordinateReferenceSystem(crs);

        return env;
    }

    /**
     * This method is responsible for the creation of the CRS for this GTOPO30.
     * The possible options are two, EPSG:4326 and POlar Stereographc. In case
     * an error occurs the default CRS is chosen.
     * 
     * @return CoordinateReferenceSystem a CRS for this coverage.
     * @throws IOException
     * @throws FactoryException
     */
    private CoordinateReferenceSystem initCRS() {

        final CoordinateReferenceSystem crs = AbstractGridFormat.getDefaultCRS();
        return crs;
    }


    /**
     * Gets information about the netCDF format.
     */
    @Override
    public Format getFormat() {
        return new NetcdfFormat();
    }

    /**
     * Get the names of metadata. Not implemented in this project.
     */
//    public String[] getMetadataNames() {
//        throw new UnsupportedOperationException("Not implemented.");
//    }
//
//    /**
//     * Get the metadata value for a specified fields. Not implemented in this project.
//     */
//    public String getMetadataValue(String string) {
//        throw new UnsupportedOperationException("Not implemented.");
//    }

    /**
     * Not implemented.
     */
    public String[] listSubNames() {
        return null;
    }

    /**
     * Not implemented.
     */
    public String getCurrentSubname() {
        return null;
    }

    /**
     * Not implemented.
     */
    public boolean hasMoreGridCoverages() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    /**
     * Not implemented.
     */
    public void skip() {
        throw new UnsupportedOperationException("Only one NetCDF image supported.");
    }

    /**
     * Desallocate the input stream. If in IOException is caught, this implementation will retry.
     */
    public void dispose() {
        while (inStream != null) {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public void setInput(Object input, boolean seekForwardOnly) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getNumImages(boolean allowSearch) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getName(int index) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public CoordinateReferenceSystem getCoordinateReferenceSystem(int index) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Envelope getEnvelope(int index) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public GridEnvelope getGridRange(int index) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public MathTransform getMathTransform(int index) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public GridSampleDimension[] getSampleDimensions(int index) throws IOException {
        final NumberRange sampleValueRange = new NumberRange(5000f, 15000f);
//        final Color[] colorPalette = new Color[]{Color.RED, Color.GREEN, Color.BLUE};
        final Color[] colorPalette = new Color[]{Color.BLACK, Color.WHITE};

        GridSampleDimension temperature = new GridSampleDimension("Temperature", new Category[]{
            new Category("Nodata", Color.YELLOW, -30000),
            //new Category("Temperature", colorPalette, sampleValueRange, temperatureRange)
            new Category("Bubba", colorPalette, sampleValueRange, .0010, 20.0)
        }, javax.measure.unit.SI.CELSIUS);
        return new GridSampleDimension[] {
            temperature };
    
    }

    public GridCoverage getGridCoverage(int index) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void reset() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

