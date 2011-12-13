package org.geotools.image.io.netcdf;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.data.DataSourceException;
import org.geotools.factory.Hints;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridCoverageReader;
import java.util.HashMap;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;

// OpenGIS dependencies
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.parameter.GeneralParameterDescriptor;

// Geotools dependencies
import org.geotools.factory.Hints;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.parameter.DefaultParameterDescriptorGroup;
import org.geotools.parameter.ParameterGroup;

public class NetcdfFormat extends AbstractGridFormat implements Format {

    /** Logger. */
    private final static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geotools.gce.gtopo30");

    /**
     * Creates a new instance of NetcdfFormat.
     * Contains the main information about the NetCDF format.
     */
    public NetcdfFormat() {

        writeParameters = null;
        mInfo = new HashMap();
        mInfo.put("name", "NetCDF");
        mInfo.put("description", "NetCDF Coverage Format");
        mInfo.put("vendor", "GeoShaver");
        mInfo.put("version", "1.0");
        mInfo.put("docURL", "http://ftp.unidata.ucar.edu/software/netcdf-java/v2.2.16/javadoc/index.html");
        readParameters = new ParameterGroup(
                new DefaultParameterDescriptorGroup(mInfo,
                new GeneralParameterDescriptor[]{READ_GRIDGEOMETRY2D}));
    }

    public NetcdfReader getReader(final Object o) {
        return getReader(o, null);
    }

    /**
     * Gets a reader for the netCDF file specified.
     *
     * @param object May be a netCDF file, or an URL for a netCDF file.
     */
    public NetcdfReader getReader(final Object object, final Hints hints) {
        try {
            return new NetcdfReader(object, hints);
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            return null;
        }

    }

    /**
     * Gets a writer for the netCDF file.
     *
     *
     * @param object The source in which we will write.
     */
    public GridCoverageWriter getWriter(Object object) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Specifies if the source is a netCDF file, and by the way is available.
     *
     * @param object The source to test.
     *
     * @todo Not yet implemented (previous implementation was useless).
     */
    public boolean accepts(Object object) {
        return true;
    }

    public GeoToolsWriteParams getDefaultImageIOWriteParameters() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean accepts(Object source, Hints hints) {
        return true;
    }

    @Override
    public GridCoverageWriter getWriter(Object destination, Hints hints) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

