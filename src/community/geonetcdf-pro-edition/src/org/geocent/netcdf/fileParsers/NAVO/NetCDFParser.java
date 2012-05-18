package org.geocent.netcdf.fileParsers.NAVO;

import java.io.IOException;
import java.util.LinkedList;
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
     * * Returns a float array: {Minimum Longitude, Maximum Longitude, Minimum Latitude, Maximum Latitude}**
     */
    public static float[] getBoundsForFile(String file) {
        NetcdfFile ncfile = null;
        /*
         * Sane defaults incase something goes wrong
         */
        float[] minAndMaxLon = new float[] { -180, 180 };
        float[] minAndMaxLat = new float[] { -90, 90 };

        try {
            ncfile = NetcdfFile.open(file);
            Variable lon = ncfile.findVariable("lon");
            Variable lat = ncfile.findVariable("lat");
            Array lonArray = lon.read();
            Array latArray = lat.read();

            /*
             * TODO: I'm including some stupid error correction because some of these files have longitudes greater than 180 sometimes, that probably means
             * something, but I'm too busy to find out what
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

        return new float[] { minAndMaxLon[0], minAndMaxLon[1], minAndMaxLat[0], minAndMaxLat[1] };
    }

    public void parseFile() {
        NetcdfFile ncfile = null;

        try {
            ncfile = NetcdfFile.open(file);
            Variable lon = ncfile.findVariable("lon");
            Variable lat = ncfile.findVariable("lat");

            Array lonArray = lon.read();
            Array latArray = lat.read();
            Variable v = null;

            for (Variable t : ncfile.getVariables()) {
                if (t.getShortName().equals(parameterName)) {
                    /* We have found the variable we are looking for */
                    v = t;
                    break;
                }
            }

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
             * Layer and tau need to remain constant, but they might not be in the file, so we need to check
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
            try {
                List<Double> latList = data.getDesiredLats();
                List<Double> lonList = data.getDesiredLons();

                for (int i = 0; i < latList.size(); i++) {
                    range[latIndex] = getNearestCoordIndex(latArray, latList.get(i));
                    if (range[latIndex] == -1)
                        continue;
                    for (int j = 0; j < lonList.size(); j++) {
                        if (data.getImageArray()[j][(latList.size() - 1) - i] == Float.NaN) {
                            continue;
                        }
                        range[lonIndex] = getNearestCoordIndex(lonArray, lonList.get(j));
                        if (range[lonIndex] == -1)
                            continue;

                        String request = String.format("%d, %d, %d, %d", range[0], range[1], range[2], range[3]);
                        // System.out.println(request);
                        // System.out.println(String.format("Getting point lat %f  lon %f from netcdf file to satisfy point lat %f  lon %f",latArray.getFloat(range[latIndex]),
                        // lonArray.getFloat(range[lonIndex]), latList.get(i), lonList.get(j)));
                        Array a = v.read(request).reduce();
                        /* Array should be a 1 dimensional array with a single entry */
                        float f = a.getFloat(0);
                        float adjustedValue = (f * scaleFactor) + addOffset;

                        /* TODO: Check to see if missing values and fill values are checked before or after they are adjusted */
                        if (f == missingValue || f == fillValue || adjustedValue == fillValue || adjustedValue == missingValue)
                            continue;
                        //System.out.println(String.format("PUTTING DATA IN FOR POINT %d %d IN DA FILE, THE VALUE IS %f", j, (latList.size() - 1) - i, adjustedValue));
                        data.getImageArray()[j][(latList.size() - 1) - i] = adjustedValue;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
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
     * This function determines the indexes of the lat and lon dimension so we can put them in the correct place in the range array.
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
     * This is always going to be used on 4d data, where we want to keep two dimensions (time and depth) constant, and a range for lat and lon
     */
    private String getFortranStyleIndexString(int[] origin, int[] range) {
        String indexFormat = "%d, %d, %d:%d, %d:%d";
        String filledOutIndex = String.format(indexFormat, origin[0], origin[1], origin[2], range[2] - 1, origin[3], range[3] - 1);
        return filledOutIndex;
    }

    private static float[] getMinAndMaxFromArray(Array a) {
        Float max = null;
        Float min = null;
        IndexIterator ii = a.getIndexIterator();
        while (ii.hasNext()) {
            float f = ii.getFloatNext();
            if (min == null || f < min) {
                min = f;
            }
            if (max == null || f > max) {
                max = f;
            }
        }
        return new float[] { min, max };
    }

    private static int getNearestCoordIndex(Array coordArray, double desiredCoord) {
        if (coordArray.getRank() != 1) {
            System.out.println("ERROR passed a non-1D array into a function that wasn't expecting it");
            return 0;
        }
        double initialPoint = coordArray.getDouble(0);
        /*
         * if the desired Coordinate is less than the initial coordinate, and the coordinate array is always sorted so that the values increase, then the
         * desired value cant be in the array
         */
        if (desiredCoord < initialPoint) {
            if (Math.abs(desiredCoord - initialPoint) < 0.5) {
                /* its within .5 degrees, this is "good enough" for now, TODO: make this configurable in the future */
                return 0;
            } else {
                return -1; 
            }
        }
        double spacing = Math.abs(coordArray.getDouble(0) - coordArray.getDouble(1));
        int index = (int) Math.round((desiredCoord - initialPoint) / spacing);
        if (index >= coordArray.getShape()[0]) {
            /* the file does not have the point we want */
            return -1;
        }
        double actualPoint = 0;
        try {
            actualPoint = coordArray.getDouble(index);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        if (Math.abs(desiredCoord - actualPoint) > spacing * 2) {
            System.out
                    .println("WARNING: it looks like this file has a point we need but it was not at the predicted spot, searching through avaliable coordinates, performance will suffer if this happens a lot");
            return getNearestCoordIndexBruteForce(coordArray, desiredCoord);
        } else {
            return index;
        }
    }

    private static int getNearestCoordIndexBruteForce(Array coordArray, double desiredCoord) {
        IndexIterator ii = coordArray.getIndexIterator();
        double err = Double.MAX_VALUE;
        int i = 0;
        while (ii.hasNext()) {
            double c = ii.getDoubleNext();
            if (Math.abs(desiredCoord - c) < err) {
                err = Math.abs(desiredCoord - c);
                i = ii.getIntCurrent();
            }
        }

        if (err > 0.5)
            /* The file does not have a point close enough to what we need */
            return -1;
        else
            return i;
    }
}
