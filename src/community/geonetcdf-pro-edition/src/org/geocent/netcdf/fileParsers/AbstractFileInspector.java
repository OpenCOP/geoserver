package org.geocent.netcdf.fileParsers;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;

import org.geocent.netcdf.NCDataEncapsulator;
import org.geotools.geometry.GeneralEnvelope;

import ucar.nc2.NetcdfFile;

abstract public class AbstractFileInspector {
    abstract public NCDataEncapsulator parseFiles(File rootDirectory, String parameterName, Double heightInMeters, Date time, GeneralEnvelope requestedArea);

    /*
     * In the future we need to have an inventory of known files in the system and what parameters (at the least) they contain, for now we will search for the
     * data we need in every nc file under the root directory
     */
    protected static void recursiveParse(LinkedList<File> files, File f) {
        if (f.isDirectory()) {
            for (File file : f.listFiles()) {
                recursiveParse(files, file);
            }
        } else {
            try {
                if (NetcdfFile.canOpen(f.getAbsolutePath())) {
                    files.add(f);
                }
            } catch (IOException e) {
                /* Guess we couldn't open it? Not sure if we even want to put something here. */
            }
        }
    }
}
