package org.geocent.netcdf.fileParsers.NAVO;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geocent.netcdf.NCDataEncapsulator;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class NetCDFParser {

    private String file;
    private String parameterName;
    private NCDataEncapsulator data;
    private int tau;
    private int layer;

    public NetCDFParser(String file, String parameterName, int tau, int layer, NCDataEncapsulator data) {
        this.file = file;
        this.parameterName = parameterName;
        this.tau = tau;
        this.layer = layer;
        this.data = data;
    }

    /**
     * * Returns a float array: {Minimum Longitude, Maximum Longitude, Minimum
     * Latitude, Maximum Latitude}**
     */
    public static float[] getBoundsForFile(String file) {
        NetcdfFile ncfile = null;
        /*
         * Sane defaults incase something goes wrong
         */
        float[] minAndMaxLon = new float[]{-180, 180};
        float[] minAndMaxLat = new float[]{-90, 90};

        try {
            ncfile = NetcdfFile.open(file);
            Variable lon = ncfile.findVariable("lon");
            Variable lat = ncfile.findVariable("lat");
            Array lonArray = lon.read();
            Array latArray = lat.read();


            /*
             * TODO: I'm including some stupid error correction because some of
             * these files have longitudes greater than 180 sometimes, that
             * probably means something, but I'm too busy to find out what
             */
            minAndMaxLon = getMinAndMaxFromArray(lonArray);
            if (minAndMaxLon[0] < -180) {
                minAndMaxLon[0] = -180;
            }
            if (minAndMaxLon[1] > 180) {
                minAndMaxLon[1] = 180;
            }


            minAndMaxLat = getMinAndMaxFromArray(latArray);
            if (minAndMaxLat[0] < -90) {
                minAndMaxLat[0] = -90;
            }
            if (minAndMaxLat[1] > 90) {
                minAndMaxLat[1] = 90;
            }

        } catch (Exception e) {
        } finally {
            try {
                ncfile.close();
            } catch (IOException ex) {
                Logger.getLogger(NetCDFParser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return new float[]{minAndMaxLon[0], minAndMaxLon[1], minAndMaxLat[0], minAndMaxLat[1]};
    }

    public void parseFile() {
        NetcdfFile ncfile = null;

        try {
            ncfile = NetcdfFile.open(file);
            Variable lon = ncfile.findVariable("lon");
            Variable lat = ncfile.findVariable("lat");


            Array lonArray = lon.read();
            Array latArray = lat.read();


            for (Variable v : ncfile.getVariables()) {
                // System.out.println(v.getShortName());
                if (v.getShortName().equals(parameterName)) {
                    int[] origin = new int[v.getDimensions().size()];
                    int[] range = new int[v.getDimensions().size()];
                    int lonIndex = getIndexForDimensionName(v, "lon");
                    int latIndex = getIndexForDimensionName(v, "lat");
                    int timeIndex = getIndexForDimensionName(v, "time");
                    int depthIndex = getIndexForDimensionName(v, "depth");
                    float scaleFactor = 1.0f;
                    float addOffset = 0.0f;
                    float missingValue = Float.NaN;
                    float fillValue = Float.NaN;
                    List<Attribute> attributeList = v.getAttributes();
                    for (Attribute a : attributeList) {
                        if (a.getName().equals("scale_factor")) {
                            scaleFactor = a.getNumericValue().floatValue();
                        }
                        if (a.getName().equals("add_offset")) {
                            addOffset = a.getNumericValue().floatValue();
                        }
                        if (a.getName().equals("_FillValue")) {
                            fillValue = a.getNumericValue().floatValue();
                        }
                        if (a.getName().equals("missing_value")) {
                            missingValue = a.getNumericValue().floatValue();
                        }
                    }

                    /*
                     * Layer and tau need to remain constant, but they might not
                     * be in the file, so we need to check
                     */
                    if (timeIndex != -1) {
                        range[timeIndex] = tau;
                        origin[timeIndex] = tau;
                    }
                    if (depthIndex != -1) {
                        range[depthIndex] = layer;
                        origin[depthIndex] = layer;
                    }
                    range[lonIndex] = (int) lon.getSize();
                    range[latIndex] = (int) lat.getSize();
                    int[] currentIndex = null;
                    try {
                        String indexString = getFortranStyleIndexString(origin, range);
                        Array a = v.read(indexString).reduce(); /*
                         * Reduce down to a 2d array, we know we will only be
                         * looking at 1 time and 1 elevation at a time, so it
                         * must be 2d.
                         */
                        /*
                         * Since we have reduced the dimensionality of our data,
                         * we need to figure out which dimension is the lat and
                         * which is the lon; I do not think it will reorder the
                         * dimensions, so if the lat index came before the lon
                         * in the shape array, it should be the first dimension
                         * (i.e 0), and the lon dimension will be 1
                         */
                        int reducedLatIndex = latIndex > lonIndex ? 1 : 0;
                        int reducedLonIndex = latIndex > lonIndex ? 0 : 1;
                        IndexIterator ii = a.getIndexIterator();

                        while (ii.hasNext()) {
                            float value = ii.getFloatNext();
                            if (value == missingValue || value == fillValue) {
                                continue;
                            }
                            currentIndex = ii.getCurrentCounter();
                            float adjustedValue = (value * scaleFactor) + addOffset;
                            data.setImagePoint(latArray.getFloat(currentIndex[reducedLatIndex]), lonArray.getFloat(currentIndex[reducedLonIndex]),
                                    adjustedValue);
                        }
                    } catch (Exception e) {
                        System.out.println(String.format("Caught exception, current index is %d,%d, lat is %f lon is %f", currentIndex[0], currentIndex[1],
                                latArray.getFloat(currentIndex[latIndex]), lonArray.getFloat(currentIndex[lonIndex])));
                        e.printStackTrace();
                    }
                }
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            if (null != ncfile) {
                try {
                    ncfile.close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    /*
     * This function determines the indexes of the lat and lon dimension so we
     * can put them in the correct place in the range array.
     */
    private int getIndexForDimensionName(Variable v, String dimensionName) {
        int index = 0;
        int i = 0;
        boolean dimensionFound = false;
        List<Dimension> dims = v.getDimensions();
        for (Dimension d : dims) {
            if (d.getName().equals(dimensionName)) {
                index = i;
                dimensionFound = true;
                break;
            }
            i++;
        }
        if (!dimensionFound) {
            return -1;
        }
        return index;
    }

    /*
     * This is always going to be used on 4d data, where we want to keep two
     * dimensions (time and depth) constant, and a range for lat and lon
     */
    private String getFortranStyleIndexString(int[] origin, int[] range) {
        String indexFormat = "%d, %d, %d:%d, %d:%d";
        String filledOutIndex = String.format(indexFormat, origin[0], origin[1], origin[2], range[2] - 1, origin[3], range[3] - 1);
        return filledOutIndex;
    }

    private static float[] getMinAndMaxFromArray(Array a) {
        float max = Float.MIN_VALUE;
        float min = Float.MAX_VALUE;
        IndexIterator ii = a.getIndexIterator();
        while (ii.hasNext()) {
            float f = ii.getFloatNext();
            if (f < min) {
                min = f;
            }
            if (f > max) {
                max = f;
            }
        }
        return new float[]{min, max};
    }
}
