package org.geocent.netcdf;

import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.media.jai.DataBufferFloat;

import org.geocent.geotools.ParamInformation;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.Envelope;

@SuppressWarnings("unused")
public class NCDataEncapsulator implements Serializable {

    private static final long serialVersionUID = 5274683324298458872L;
    @SuppressWarnings("rawtypes")
    private Float[][] imageArray;
    private boolean flattened;
    private int imageWidth;
    private int imageHeight;
    private double lonSkip;
    private double latSkip;
    private String parameterName;
    private String unit;
    private int pointsPerDegree;
    private ParamInformation paramInfo;
    private double gridLeftLon;
    private double gridRightLon;
    private double gridUpLat;
    private double gridLowLat;
    private double gridLonSpan;
    private double gridLatSpan;
    private LinkedList<Double> latList = new LinkedList<Double>();
    private LinkedList<Double> lonList = new LinkedList<Double>();
    private float highValue = Float.NEGATIVE_INFINITY;
    private float lowValue = Float.POSITIVE_INFINITY;
    private Random test = new Random();

    public NCDataEncapsulator(int pointsPerDegree, String parameterName, String unitOfMeasure, ParamInformation paramInfo) {
        this.pointsPerDegree = pointsPerDegree;
        this.parameterName = parameterName;
        this.unit = unitOfMeasure;
        this.paramInfo = paramInfo;

        flattened = false;

        /*
         * We need to create a buffer for the resulting image. The size of the image is dictated by the WMS request
         */
        imageWidth = Math.abs(this.paramInfo.getDim().getSpan(0));
        imageHeight = Math.abs(this.paramInfo.getDim().getSpan(1));
        imageArray = new Float[imageWidth][imageHeight];

        /*
         * We want to initialize the points to NaN, When these values are being populated, we will look at the "best" files first (the ones with the latest
         * initial time and the highest resolution, that way the "best" data will go into the data encapsulator first, if the NCParser detects that a value has
         * already been added in a specific position, it will skip that position
         */
        for (int i = 0; i < imageWidth; i++) {
            for (int j = 0; j < imageHeight; j++) {
                imageArray[i][j] = Float.NaN;
            }
        }

        /*
         * We need to store the bounding region that we're interested in for this particular request
         */
        Rectangle2D rec = paramInfo.getRequestedEnvelope().toRectangle2D();
        gridLeftLon = rec.getMinX() < -180 ? -180 : rec.getMinX();
        gridRightLon = rec.getMaxX() > 180 ? 180 : rec.getMaxX();
        gridUpLat = rec.getMaxY() > 90 ? 90 : rec.getMaxY();
        gridLowLat = rec.getMinY() < -90 ? -90 : rec.getMinY();
        
        if(Math.abs(gridLeftLon - gridRightLon) < 0.05){
            /*geoserver complains about envelopes being too small, we need to give a bigger envelope*/
            if(gridLeftLon > -179){
                gridLeftLon = gridLeftLon + -1;
            }
            else if(gridRightLon < 179){
                gridRightLon = gridRightLon - 1;
            }
        }
        if(Math.abs(gridUpLat - gridLowLat) < 0.05){
            if(gridLowLat > -89){
                gridLowLat = gridLowLat + -1;
            }
            else if(gridUpLat < 89){
                gridUpLat = gridUpLat + 1;
            }
        }
        
        gridLonSpan = Math.abs((gridRightLon - gridLeftLon)) / imageWidth;
        gridLatSpan = Math.abs(gridUpLat - gridLowLat) / imageHeight;
        
        for (int i = 0; i < imageHeight; i++) {
            latList.add(gridLowLat + gridLatSpan * i);
        }
        
        for (int i = 0; i < imageWidth; i++) {
            lonList.add(gridLeftLon + gridLonSpan * i);
        }
    }

    public List<Double> getDesiredLats() {
        return latList;
    }

    public List<Double> getDesiredLons() {
        return lonList;
    }

    public Envelope getGeneralEnvelope() {
        final Envelope returnedEnv = new Envelope2D(DefaultGeographicCRS.WGS84, gridLeftLon, gridLowLat, (gridRightLon - gridLeftLon), (gridUpLat - gridLowLat));
        return returnedEnv;
    }

    public WritableRaster getWritableRaster() {
        int imageBufferLen = imageWidth * imageHeight;
        float[] imageBuffer = new float[imageBufferLen];

        long numZeros = 0;
        int k = 0;
        for (int height = 0; height < imageArray[0].length; height++) {
            for (int width = 0; width < imageArray.length; width++) {
                imageBuffer[k] = imageArray[width][height];
                k++;
            }
        }

        int[] nBits = { 32 };
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorModel cm = new ComponentColorModel(cs, nBits, false, true, Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);
        SampleModel sm = cm.createCompatibleSampleModel(imageWidth, imageHeight);
        WritableRaster raster = Raster.createWritableRaster(sm, new DataBufferFloat(imageBuffer, imageBuffer.length), new Point(0, 0));
        return raster;
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

    private short getNormalizedValue(Float value) {
        if (value.equals(Float.NaN)) {
            return (short) 0;
        }
        float highMinusLow = highValue - lowValue;
        double y = (value - lowValue) / highMinusLow;
        short returnVal = (short) (Math.round(y * 32767));
        return returnVal;
    }

    public Float[][] getImageArray() {
        return imageArray;
    }

    public void setImageArray(Float[][] imageArray) {
        this.imageArray = imageArray;
    }

    public double getGridLeftLon() {
        return gridLeftLon;
    }

    public void setGridLeftLon(double gridLeftLon) {
        this.gridLeftLon = gridLeftLon;
    }

    public double getGridRightLon() {
        return gridRightLon;
    }

    public void setGridRightLon(double gridRightLon) {
        this.gridRightLon = gridRightLon;
    }

    public double getGridUpLat() {
        return gridUpLat;
    }

    public void setGridUpLat(double gridUpLat) {
        this.gridUpLat = gridUpLat;
    }

    public double getGridLowLat() {
        return gridLowLat;
    }

    public void setGridLowLat(double gridLowLat) {
        this.gridLowLat = gridLowLat;
    }
}
