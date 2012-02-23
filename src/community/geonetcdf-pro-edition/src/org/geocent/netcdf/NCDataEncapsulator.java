package org.geocent.netcdf;

import java.awt.Color;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.DataBufferFloat;

@SuppressWarnings("unused")
public class NCDataEncapsulator implements Serializable {

    private static final long serialVersionUID = 5274683324298458872L;
    @SuppressWarnings("rawtypes")
    private LinkedList[][] imageArray;
    private float[][] flattenedImageArray;
    private boolean flattened;
    private int imageWidth;
    private int imageHeight;
    private String parameterName;
    private String unit;
    private Color[] colorValues;
    private static Color transparent = new Color(0, 0, 0, 255);
    private int pointsPerDegree;
    private float highValue = Float.NEGATIVE_INFINITY;
    private float lowValue = Float.POSITIVE_INFINITY;
    private Random test = new Random();

    public NCDataEncapsulator(int pointsPerDegree, String parameterName, String unitOfMeasure) {
        this.pointsPerDegree = pointsPerDegree;
        this.parameterName = parameterName;
        this.unit = unitOfMeasure;
        /*
         * Very wasteful at this point because most files are not world wide, but this makes merging easier
         */
        flattened = false;
        imageWidth = 360 * pointsPerDegree;
        imageHeight = 180 * pointsPerDegree;
        imageArray = new LinkedList[imageWidth][imageHeight];
        flattenedImageArray = new float[imageWidth][imageHeight];
        colorValues = createColorArray();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setImagePoint(float lat, float lon, float value) {
        int latIndex = 0;
        int lonIndex = 0;
        try {
            /*
             * TODO Coordinate must not be negative (needs to be in 0-360 or 0-180 format). This is such a stupid way of doing this, please think of a better
             * way.
             */

            /* We want the point -180 lon and 90 lat to be point 0,0 in the raster rectangle, so we'll need to flip the sign of the lat */
            lat = lat * -1.0f;

            if (lat < 0)
                lat = 90 - Math.abs(lat);
            else
                lat = lat + 90;

            if (lon < 0)
                lon = 180 - Math.abs(lon);
            else
                lon = lon + 180;

            lat = lat + 1;

            /*
             * I don't know what the deal with this data is, sometimes we have values greater than you'd expect... if we detect that I'm just going to throw it
             * out
             */
            if (lat < 0 || lat > 180 || lon < 0 || lon > 360)
                return;

            lonIndex = roundToNearestIndex(lon);
            latIndex = roundToNearestIndex(lat);

            if (imageArray[lonIndex][latIndex] != null)
                imageArray[lonIndex][latIndex].add(value);
            else {
                imageArray[lonIndex][latIndex] = new LinkedList();
                imageArray[lonIndex][latIndex].add(value);
            }
        } catch (Exception e) {
            /* Do nothing, I will deal with these edge cases later. */
        }
    }

    private int roundToNearestIndex(float coordinate) {

        float decimalPoint = coordinate - ((float) ((int) coordinate));
        float minDiff = 1.0f;
        int minIndex = 0;
        for (int i = 0; i < pointsPerDegree; i++) {
            float diff = Math.abs((i * 1.0f) / pointsPerDegree - decimalPoint);
            if (diff < minDiff) {
                minDiff = diff;
                minIndex = i;
            }
        }
        int returnPoint = (((int) coordinate) * pointsPerDegree + minIndex); /*-1 to convert it to 0 based*/
        if (returnPoint < 0)
            return 0; /* TODO: make this work right */
        return returnPoint;
    }

    @SuppressWarnings({ "rawtypes" })
    public void flatten() {
        if (flattened)
            return; /* We have already flattened this object */
        for (int i = 0; i < imageArray.length; i++) {
            for (int j = 0; j < imageArray[i].length; j++) {
                BigDecimal biggie = new BigDecimal("0");

                LinkedList numbers = imageArray[i][j];
                /*
                 * We didn't have any data at this particular point, so we'll fill it with NaN which will translate to a transparent value
                 */
                if (numbers == null) {
                    flattenedImageArray[i][j] = Float.NaN;
                    continue;
                }

                long points = numbers.size();
                for (Object f : numbers) {
                    /*
                     * there is a limitation with making typed linked lists in 2d arrays, I can assure you these are all floats.
                     */
                    biggie = biggie.add(new BigDecimal((Float) f));
                }
                float averageValue = biggie.divide(new BigDecimal(points), 2, BigDecimal.ROUND_HALF_UP).floatValue();
                numbers.clear();
                flattenedImageArray[i][j] = averageValue;

                if (averageValue < lowValue)
                    lowValue = averageValue;
                if (averageValue > highValue)
                    highValue = averageValue;

            }
        }
        flattened = true;
        imageArray = null; /* This object is pretty huge so we need to null it out so it won't waste space */
    }

    public WritableRaster getWritableRaster() {

        if (!flattened)
            flatten();
        int imageBufferLen = imageWidth * imageHeight;
        float[] imageBuffer = new float[imageBufferLen];

        long numZeros = 0;
        int k = 0;
        for (int height = 0; height < flattenedImageArray[0].length; height++) {
            for (int width = 0; width < flattenedImageArray.length; width++) {
                imageBuffer[k] = flattenedImageArray[width][height];
                k++;
            }
        }
        System.out.println("The image is " + numZeros / (imageBuffer.length * 1.0) + " percent zero");

        int[] nBits = { 32 };
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorModel cm = new ComponentColorModel(cs, nBits, false, true, Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);
        SampleModel sm = cm.createCompatibleSampleModel(imageWidth, imageHeight);
        WritableRaster raster = Raster.createWritableRaster(sm, new DataBufferFloat(imageBuffer, imageBuffer.length), new Point(0, 0));
        return raster;
    }

    /* Returns a buffered ARGB image with a custom color value map */
    public BufferedImage getBufferedImage() {
        if (!flattened)
            flatten();
        BufferedImage img = new BufferedImage(flattenedImageArray.length, flattenedImageArray[0].length, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < flattenedImageArray.length; i++) {
            for (int j = 0; j < flattenedImageArray[i].length; j++) {
                img.setRGB(i, j, getNormalizedColorValue(flattenedImageArray[i][j]));
            }
        }

        return img;
    }

    private BufferedImage flipImage(BufferedImage img) {
        /*
         * TODO: The x,y coordiante system I was using to put the data into the 2d array assumes 0,0 is at the left bottom, when buffered image needs 0,0 at the
         * left top. Since I don't feel like refactoring that now I'm just going to flip the image. I would like to correct this in the future as it seems like
         * a pretty huge waste of memory...
         */
        AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
        tx.translate(0, -img.getHeight(null));
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        img = op.filter(img, null);
        return img;
    }

    public void writeImage(String fileName) {
        if (!flattened)
            flatten();

        Iterator<ImageWriter> writerIterator = ImageIO.getImageWritersByFormatName("png");
        ImageWriter writer = writerIterator.next();
        File f = new File(fileName + ".png");
        ImageOutputStream ios = null;

        BufferedImage img = getBufferedImage();

        try {
            ios = ImageIO.createImageOutputStream(f);
            writer.setOutput(ios);
            writer.write(img);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                ios.flush();
                ios.close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    private int getNormalizedColorValue(Float value) {
        if (value.equals(Float.NaN)) {
            return transparent.getRGB();
        }
        float highMinusLow = highValue - lowValue;
        double y = (value - lowValue) / highMinusLow;
        Color colorValue = colorValues[(int) Math.round(y * (colorValues.length - 1))];
        return colorValue.getRGB();
    }

    private short getNormalizedValue(Float value) {
        if (value.equals(Float.NaN)) {
            return (short) 0;
        }
        float highMinusLow = highValue - lowValue;
        double y = (value - lowValue) / highMinusLow;
        short returnVal = (short) (Math.round(y * 32767));
        return returnVal;
    }

    private Color[] createColorArray() {
        /* Is there a more elegant way to do this? Probably. */
        Color[] colors = new Color[1276];

        for (int i = 0; i < 256; i++) {
            colors[i] = new Color(0, 0, i, 255);
        }
        for (int i = 1; i < 256; i++) {
            colors[256 + i - 1] = new Color(0, i, 255, 255);
        }
        int j = 0;
        for (int i = 254; i >= 0; i--) {
            colors[511 + j] = new Color(0, 255, i, 255);
            j++;
        }
        for (int i = 0; i < 256; i++) {
            colors[765 + i] = new Color(i, 255, 0, 255);
        }
        j = 0;
        for (int i = 254; i >= 0; i--) {
            colors[1021 + j] = new Color(255, i, 0, 255);
            j++;
        }
        return colors;
    }

}
