package org.geocent.netcdf.fileParsers.NAVO;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.geocent.geotools.ParamInformation;
import org.geocent.netcdf.NCDataEncapsulator;
import org.geocent.netcdf.fileParsers.AbstractFileInspector;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.xml.xsi.XSISimpleTypes.DateTime;
import org.joda.time.Hours;

import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

@SuppressWarnings("unused")
public class FileInspector extends AbstractFileInspector {

    /*
     * The NAVOFileInspector will attempt to find the closest layer and time to match the request, but we have a error factor to ensure that data can be
     * returned even if there isn't a perfect match, the parameters for that are defined here
     */
    private static Double MAX_HOURS_OFF = 3.0; /*
                                                * In hours
                                                */

    private static Double MAX_ELEVATION_OFF = 50.0; /*
                                                     * In meters
                                                     */

    /* TODO: remove, this is no longer being used */
    private static int DEFAULT_POINTS_PER_DEGREE = 7;

    public FileInspector(File rootDir) {
        super(rootDir);
    }

    @Override
    public synchronized float[] getBounds() {
        LinkedList<File> filesToParse = new LinkedList<File>();
        recursiveParse(filesToParse, rootDirectory);

        /*
         * I'm going to put some sane defaults here incase something goes wrong, we will check to see if its our first run through when looping over files, if
         * it is the first run through, the reported bounds are used as the default
         */
        float[] minAndMaxLonAndLat = new float[] { -180f, 180f, -90f, 90f };
        boolean firstFile = true;

        for (File ncFile : filesToParse) {
            System.out.println("Getting Bounds For " + ncFile.getAbsolutePath());
            float[] maxBounds = NetCDFParser.getBoundsForFile(ncFile.getAbsolutePath());

            /*
             * TODO: this could probably stand some cleaning up in the future, sorry if its unclear (its stupid, sorry)
             */

            /*
             * Min longitude compare
             */
            if (maxBounds[0] < minAndMaxLonAndLat[0] || firstFile) {
                minAndMaxLonAndLat[0] = maxBounds[0];
            }
            /*
             * Max longitude compare
             */
            if (maxBounds[1] > minAndMaxLonAndLat[1] || firstFile) {
                minAndMaxLonAndLat[1] = maxBounds[1];
            }
            /*
             * Min latitude compare
             */
            if (maxBounds[2] < minAndMaxLonAndLat[2] || firstFile) {
                minAndMaxLonAndLat[2] = maxBounds[2];
            }
            /*
             * Max latitude compare
             */
            if (maxBounds[3] > minAndMaxLonAndLat[3] || firstFile) {
                minAndMaxLonAndLat[3] = maxBounds[3];
            }
            firstFile = false; /*
                                * We have seen the first file, all of the comparsions after this will need to be real
                                */
        }
        return minAndMaxLonAndLat;
    }

    @Override
    public synchronized NCDataEncapsulator parseFiles(String parameterName, Double heightInMeters, Date time, ParamInformation paramInfo) {

        LinkedList<File> filesToParse = new LinkedList<File>();
        recursiveParse(filesToParse, rootDirectory);
        NCDataEncapsulator data = new NCDataEncapsulator(DEFAULT_POINTS_PER_DEGREE, parameterName, "NOTUSINGTHISYET", paramInfo);

        for (File ncFile : filesToParse) {
            NetcdfFile ncfile = null;
            try {
                ncfile = NetcdfFile.open(ncFile.getAbsolutePath());
                int timeLayer = getTimeLayerInNCFile(ncfile, time);
                if (timeLayer == -1) {
                    continue; /*
                               * No need to continue on, we don't have the appropriate time in this file.
                               */
                }
                int elevationLayer = getElevationLayerInNCFile(ncfile, heightInMeters);
                if (elevationLayer == -1) {
                    continue; /*
                               * The time was correct but we don't have the desired elevation in this file.
                               */
                }

                NetCDFParser ncParser = new NetCDFParser(ncFile.getAbsolutePath(), parameterName, timeLayer, elevationLayer, data);
                ncParser.parseFile();

            } catch (Exception e) {
                System.out.println("COULD NOT OPEN NETCDF FILE " + ncFile.getAbsolutePath() + " " + e.toString());
                e.printStackTrace();
            } finally {
                try {
                    ncfile.close();
                } catch (IOException e1) {
                    System.out.println("EXCEPTION CAUGHT TRYING TO CLOSE NETCDF FILE " + e1.getMessage());
                }
            }
        }
        return data;
    }

    /*
     * -1 means the layer wasn't found
     */
    private int getElevationLayerInNCFile(NetcdfFile ncfile, double elevation) {
        String units = null;
        Variable elevationVariable = getVariableByName(ncfile, "depth(");
        Attribute elevationUnits = getAttributeByName(elevationVariable, "units");
        if (!elevationUnits.getStringValue().toLowerCase().equals("meter")) {
            System.out.println("NCFILE " + ncfile.getTitle() + " HAS A DEPTH DIMENSION IN NOT METERS (" + elevationUnits.getName()
                    + ") WE ONLY SUPPORT METERS CURRENTLY");
            return -1;
        }
        Attribute positiveDirection = getAttributeByName(elevationVariable, "positive");
        if (positiveDirection != null && positiveDirection.getStringValue().toLowerCase().equals("down")) {
            elevation = elevation * -1.0; /*
                                           * Reverse the sign of elevation, down is up and up is down!
                                           */
        }

        try {
            int i =  findClosestIndex(elevationVariable.read(), elevation, MAX_ELEVATION_OFF);
            if(i == -1){
                System.out.println("COULD NOT FIND REQUESTED ELEVATION IN FILE " + ncfile.getTitle() + " REQUESTED ELEVATION WAS  " + elevation);
            }
            return i;
        } catch (IOException e) {
            System.out.println("COULD NOT GET ELEVATION " + e.getMessage());
            return -1;
        }
    }

    /*
     * -1 means the layer wasn't found
     */
    private int getTimeLayerInNCFile(NetcdfFile ncfile, Date time) {
        String units = null;

        try {
            Variable timeVariable = getVariableByName(ncfile, "time(");
            if (timeVariable == null) {
                System.out.println("COULD NOT FIND REQUESTED DATE IN FILE " + ncfile.getTitle() + " REQUESTED TIME WAS  " + time.toString());
                return -1;
            }
            Attribute timeUnitsAttribute = getAttributeByName(timeVariable, "units");
            if (timeUnitsAttribute == null) {
                System.out.println("COULD NOT FIND REQUESTED DATE IN FILE " + ncfile.getTitle() + " REQUESTED TIME WAS  " + time.toString());
                return -1;
            }
            units = timeUnitsAttribute.getStringValue();

            Date startTime = getDateFromString(units);
            org.joda.time.DateTime requestedDateTime = new org.joda.time.DateTime(time.getTime());
            org.joda.time.DateTime startDateTime = new org.joda.time.DateTime(startTime.getTime());
            Hours hoursBetween = Hours.hoursBetween(startDateTime, requestedDateTime);
            int numHours = hoursBetween.getHours();
            Array timeArray = timeVariable.read();
            int i =  findClosestIndex(timeArray, (double) numHours, MAX_HOURS_OFF);
            if(i == -1){
                System.out.println("COULD NOT FIND REQUESTED DATE IN FILE " + ncfile.getTitle() + " REQUESTED TIME WAS  " + time.toString());
            }
            return i;

        } catch (Exception e) {
            System.out.println("COULD NOT GET DATE " + e.getMessage());
            return -1;
        }
    }

    @Override
    public String getElevationString() {
        /*
         * TODO: a lot of code duplication here between getTimeString and getElevation string but I'm in a hurry, try to consolidate some common logic in the
         * future. Also a lot of duplication in TimeLayerInNCFile and ElevationLayerInNCFile
         */
        LinkedList<File> filesToParse = new LinkedList<File>();
        recursiveParse(filesToParse, rootDirectory);
        HashSet<String> elevationStrings = new HashSet<String>();
        for (File ncFile : filesToParse) {
            NetcdfFile ncfile;
            try {
                ncfile = NetcdfFile.open(ncFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            Variable elevationVariable = getVariableByName(ncfile, "depth(");
            Attribute elevationUnits = getAttributeByName(elevationVariable, "units");
            if (!elevationUnits.getStringValue().toLowerCase().equals("meter")) {
                System.out.println("NCFILE " + ncfile.getTitle() + " HAS A DEPTH DIMENSION IN NOT METERS (" + elevationUnits.getName()
                        + ") WE ONLY SUPPORT METERS CURRENTLY");
                continue;
            }

            int elevationModifier = 1;
            Attribute positiveDirection = getAttributeByName(elevationVariable, "positive");
            if (positiveDirection != null && positiveDirection.getStringValue().toLowerCase().equals("down")) {
                elevationModifier = -1; /* We're going to flip it and reverse it */
            }
            try {
                IndexIterator ii = elevationVariable.read().getIndexIterator();
                while (ii.hasNext()) {
                    double elevation = ii.getDoubleNext();
                    elevationStrings.add(Double.toString(elevation * elevationModifier));
                }
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
        }
        return returnSortedListFromCollection(elevationStrings);
    }

    /*
     * This function is designed to get a geoserver formatted time string (some times seperated by commas, for now, this will probably change in the future) for
     * the getCapabilities information
     */
    @Override
    public String getTimeString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        LinkedList<File> filesToParse = new LinkedList<File>();
        recursiveParse(filesToParse, rootDirectory);
        HashSet<String> timeStrings = new HashSet<String>();
        for (File ncFile : filesToParse) {
            NetcdfFile ncfile;
            try {
                ncfile = NetcdfFile.open(ncFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            Variable timeVariable = getVariableByName(ncfile, "time(");
            if (timeVariable == null) {
                System.out.println("TIME VARIBLE FOR FILE " + ncFile.getAbsolutePath() + " WAS NULL I GUESS IT DOESNT HAVE TIME IN IT");
                continue;
            }
            Attribute timeUnitsAttribute = getAttributeByName(timeVariable, "units");

            Date startTime = getDateFromString(timeUnitsAttribute.getStringValue());
            try {
                Array timeArray = timeVariable.read();
                IndexIterator ii = timeArray.getIndexIterator();
                while (ii.hasNext()) {
                    double time = ii.getDoubleNext();
                    /*
                     * The value here is hours since 2000-01-01 00:00:00, which is stored in startTime, we will use some joda magic to add this number of hours
                     * and see what date we get
                     */
                    org.joda.time.DateTime startDateTime = new org.joda.time.DateTime(startTime.getTime());
                    org.joda.time.DateTime actualTime = startDateTime.plusHours((int) Math.round(time));
                    timeStrings.add(sdf.format(actualTime.toDate()));
                }
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
        }
        return returnSortedListFromCollection(timeStrings);
    }

    private String returnSortedListFromCollection(Collection<String> strings) {
        LinkedList<String> sortableList = new LinkedList<String>();
        StringBuilder sb = new StringBuilder();
        for (String s : strings) {
            sortableList.add(s);
        }
        Collections.sort(sortableList);
        boolean first = true;
        for (String s : sortableList) {
            if (first) {
                sb.append(s);
                first = false;
            } else {
                sb.append(",").append(s);
            }
        }
        return sb.toString();
    }

    private Date getDateFromString(String dateString) {
        /*
         * TODO: In my experience NAVO netcdf time values are always "hour since" *some time*, but we need to confirm this with a subject matter expert on navo
         * data
         */
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm"); /*
                                                                           * Because I've seen it both with and without seconds ! TODO: fix this mess
                                                                           */
        sdf2.setTimeZone(TimeZone.getTimeZone("GMT"));
        dateString = dateString.replaceAll("\\p{Alpha}", "").trim();
        Date d = null;
        try {
            return sdf.parse(dateString);
        } catch (Exception e) {
            try {
                return sdf2.parse(dateString);
            } catch (ParseException e1) {
                e1.printStackTrace();
            }
        }
        return new Date(0);
    }

    private Variable getVariableByName(NetcdfFile ncFile, String variable) {
        Variable timeVariable = null;
        for (Variable var : ncFile.getVariables()) {
            if (var.getNameAndDimensions().toLowerCase().contains(variable)) {
                timeVariable = var;
                break;
            }
        }
        return timeVariable;
    }

    private Attribute getAttributeByName(Variable var, String attribute) {
        Attribute attr = null;
        for (Attribute a : var.getAttributes()) {
            if (a.getName().toLowerCase().equals(attribute)) {
                attr = a;
                break;
            }
        }
        return attr;
    }

    /*
     * Luckly the two values I'm interested in are doubles
     */
    private int findClosestIndex(Array a, Double val, Double maxError) {
        IndexIterator ii = a.getIndexIterator();
        double minValueDiff = Double.MAX_VALUE;
        int minValueIndex = -1;
        while (ii.hasNext()) {
            Double currentVal = ii.getDoubleNext();
            int currentPos = ii.getCurrentCounter()[0];
            if (minValueDiff > Math.abs(currentVal - val)) {
                minValueDiff = Math.abs(currentVal - val);
                minValueIndex = currentPos;
            }
        }

        if (minValueDiff > maxError) {
            /*
             * even the closest value this file has is too far off, return -1
             */
            minValueIndex = -1;
        }
        return minValueIndex;
    }
}
