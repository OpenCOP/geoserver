package org.geocent.geotools;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.factory.Hints;
import org.geotools.parameter.DefaultParameterDescriptor;
import org.geotools.parameter.DefaultParameterDescriptorGroup;
import org.geotools.parameter.ParameterGroup;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.filter.Filter;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;

public class NetCDFFormat extends AbstractGridFormat implements Format {

    private static String UNSUPPORTED_WRITING_OPERATION_MESSAGE = "Writing operations are not implemented";
    private static String CANNOT_CREATE_READER_MESSAGE = "Cannot create Netcdf Reader without a directory of netcdf files";

    /* The WMS time component */
    public static final ParameterDescriptor<List> TIME = new DefaultParameterDescriptor<List>("TIME", List.class, null, null);
    /* The WMS elevation component */
    public static final ParameterDescriptor<Double> ELEVATION = new DefaultParameterDescriptor<Double>("ELEVATION", Double.class, null, 0.0);
    /* The CQL filter contents */
    public static final ParameterDescriptor<Filter> FILTER = new DefaultParameterDescriptor<Filter>("Filter", Filter.class, null, null);


    public NetCDFFormat() {
        writeParameters = null;
        mInfo = new HashMap<String, String>();
        mInfo.put("name", "NetCDF");
        mInfo.put("description", "NetCDF reader");
        mInfo.put("vendor", "Geocent");
        mInfo.put("version", "0.1");
        readParameters = new ParameterGroup(new DefaultParameterDescriptorGroup(mInfo, new GeneralParameterDescriptor[] { READ_GRIDGEOMETRY2D, ELEVATION, TIME,
                FILTER }));

    }

    @Override
    public boolean accepts(Object arg0, Hints arg1) {
        return true;
    }

    @Override
    public GeoToolsWriteParams getDefaultImageIOWriteParameters() {
        throw new UnsupportedOperationException(UNSUPPORTED_WRITING_OPERATION_MESSAGE);
    }

    @Override
    public AbstractGridCoverage2DReader getReader(Object arg0) {
        return getReader(arg0, null);
    }

    @Override
    public AbstractGridCoverage2DReader getReader(Object arg0, Hints arg1) {
        if (arg0 instanceof File) {
            File path = (File) arg0;
            return new NetCDFReader(path, arg1);
        } else {
            throw new UnsupportedOperationException(CANNOT_CREATE_READER_MESSAGE);
        }
    }

    @Override
    public GridCoverageWriter getWriter(Object arg0) {
        throw new UnsupportedOperationException(UNSUPPORTED_WRITING_OPERATION_MESSAGE);
    }

    @Override
    public GridCoverageWriter getWriter(Object arg0, Hints arg1) {
        throw new UnsupportedOperationException(UNSUPPORTED_WRITING_OPERATION_MESSAGE);
    }
}
