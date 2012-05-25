package org.geocent.netcdf;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NCDataCacher {
    /* TODO: Make this build configurable */
    private static String scratchLocation = null;
    private static boolean dirty = false;
    private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static HashMap<CacheKey, File> ncFileMap = new HashMap<CacheKey, File>();

    static {
        /* First lets check if there is a system variable that specifies the "Home" location of our netcdf plugin */
        Map<String, String> env = System.getenv();
        scratchLocation = System.getenv("GEOSERVER_DATA_DIR");
        if (scratchLocation == null || scratchLocation.isEmpty()) {
            System.out.println("WARNING GEOSERVER_DATA_DIR MUST BE SET AND WRITABLE OR YOU WILL HAVE TERRIBLE PERFORMANCE!!! TERRRRRIBLLLEEEE");
        } else {
            File f = new File(scratchLocation);
            if (!f.isDirectory()) {
                System.out.println("Warning, " + scratchLocation + " did not exist, creating it");
                f.mkdirs();
            }
        }
        clearCache();
    }

    public static NCDataEncapsulator getNCData(String parameter, Double elevation, Date time) {
        if (scratchLocation == null)
            return null;

        CacheKey key = new CacheKey(parameter, elevation, time);
        NCDataEncapsulator data = null;
        try {
            lock.readLock().lock();
            File f = ncFileMap.get(key);
            if (f != null) {
                InputStream file = new FileInputStream(f);
                InputStream buffer = new BufferedInputStream(file);
                ObjectInput input = new ObjectInputStream(buffer);
                data = (NCDataEncapsulator) input.readObject();
            }
        } catch (FileNotFoundException e) { /* File not there anymore? Need to remove it from the map.... */
            try {
                lock.writeLock().lock();
                ncFileMap.remove(key);

            } catch (Exception e1) {
                e.printStackTrace();
            } finally {
                lock.writeLock().unlock();
            }
        }

        catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.readLock().unlock();
        }
        return data;
    }

    public static void putNCData(String parameter, Double elevation, Date time, NCDataEncapsulator data) {
        if (scratchLocation == null)
            return;

        ObjectOutput output = null;
        String filename = scratchLocation + "/NCDataFile" + parameter + elevation.toString() + Long.toHexString(time.getTime()) + ".ncDataEncapsulator";
        try {
            OutputStream file = new FileOutputStream(filename);
            OutputStream buffer = new BufferedOutputStream(file);
            output = new ObjectOutputStream(buffer);
            output.writeObject(data);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        } finally {
            try {
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        /* Now that we've written the data object to disk, we'll put it in the hash map. */
        try {
            lock.writeLock().lock();
            ncFileMap.put(new CacheKey(parameter, elevation, time), new File(filename));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void clearCache() {
        if (scratchLocation == null)
            return;
        try {
            lock.writeLock().lock();
            ncFileMap.clear();
            for (File file : new File(scratchLocation).listFiles()) {
                if (file.isFile() && file.getName().contains(".ncDataEncapsulator"))
                    file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
