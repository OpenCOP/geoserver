package org.geocent.netcdf.fileParsers.NAVO;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.TimeZone;

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
     * The NAVOFileInspector will attempt to find the closest layer and time to match the request, but we have a fudge factor to ensure that data can be
     * returned even if there isn't a perfect match, the parameters for that are defined here
     */

    private static Double MAX_HOURS_OFF = 3.0; /* In hours */
    private static Double MAX_ELEVATION_OFF = 50.0; /* In meters */
    private static int DEFAULT_POINTS_PER_DEGREE = 7;

    @Override
    public synchronized NCDataEncapsulator parseFiles(File rootDirectory, String parameterName, Double heightInMeters, Date time, GeneralEnvelope requestedArea) {

        LinkedList<File> filesToParse = new LinkedList<File>();
        recursiveParse(filesToParse, rootDirectory);
        NCDataEncapsulator data = new NCDataEncapsulator(DEFAULT_POINTS_PER_DEGREE, parameterName, "NOTUSINGTHISYET");

        for (File ncFile : filesToParse) {
            System.out.println("Reading from " + ncFile.getAbsolutePath());
            NetcdfFile ncfile = null;
            try {
                ncfile = NetcdfFile.open(ncFile.getAbsolutePath());
                int timeLayer = getTimeLayerInNCFile(ncfile, time);
                if (timeLayer == -1)
                    continue; /* No need to continue on, we don't have the appropriate time in this file. */
                int elevationLayer = getElevationLayerInNCFile(ncfile, heightInMeters);
                if (elevationLayer == -1)
                    continue; /* The time was correct but we don't have the desired elevation in this file. */

                NetCDFParser ncParser = new NetCDFParser(ncFile.getAbsolutePath(), parameterName, timeLayer, elevationLayer, data);
                ncParser.parseFile();

            } catch (Exception e) {
                System.out.println("COULD NOT OPEN NETCDF FILE " + ncFile.getAbsolutePath() + " " + e.toString());
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

    /*-1 means the layer wasn't found*/
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
        if (positiveDirection != null && positiveDirection.getStringValue().toLowerCase().equals("down"))
            elevation = elevation * -1.0; /* Reverse the sign of elevation, down is up and up is down! */

        try {
            return findClosestIndex(elevationVariable.read(), elevation, MAX_ELEVATION_OFF);
        } catch (IOException e) {
            System.out.println("COULD NOT GET ELEVATION " + e.getMessage());
            return -1;
        }
    }

    /*-1 means the layer wasn't found*/
    private int getTimeLayerInNCFile(NetcdfFile ncfile, Date time) {
        String units = null;

        try {
            Variable timeVariable = getVariableByName(ncfile, "time(");
            if (timeVariable == null)
                return -1;
            Attribute timeUnitsAttribute = getAttributeByName(timeVariable, "units");
            if (timeUnitsAttribute == null)
                return -1;
            units = timeUnitsAttribute.getStringValue();

            Date startTime = getDateFromString(units);
            org.joda.time.DateTime requestedDateTime = new org.joda.time.DateTime(time.getTime());
            org.joda.time.DateTime startDateTime = new org.joda.time.DateTime(startTime.getTime());
            Hours hoursBetween = Hours.hoursBetween(startDateTime, requestedDateTime);
            int numHours = hoursBetween.getHours();
            Array timeArray = timeVariable.read();
            return findClosestIndex(timeArray, (double) numHours, MAX_HOURS_OFF);

        } catch (Exception e) {
            System.out.println("COULD NOT GET DATE " + e.getMessage());
            return -1;
        }
    }

    private Date getDateFromString(String dateString) {
        /*
         * TODO: In my experience NAVO netcdf time values are always "hour since" *some time*, but we need to confirm this with a subject matter expert on navo
         * data
         */
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm"); /* Because I've seen it both with and without seconds! TODO: fix this mess */
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

    /* Luckly the two values I'm interested in are doubles */
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
            /* even the closest value this file has is too far off, return -1 */
            minValueIndex = -1;
        }
        return minValueIndex;
    }
}
