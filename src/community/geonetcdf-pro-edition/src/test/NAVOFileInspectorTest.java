//package test;
//
//import java.awt.image.BufferedImage;
//import java.awt.image.Raster;
//import java.io.File;
//import java.io.IOException;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.Iterator;
//import java.util.TimeZone;
//
//import javax.imageio.ImageIO;
//import javax.imageio.ImageWriter;
//import javax.imageio.stream.ImageOutputStream;
//
//import org.geocent.netcdf.NCDataCacher;
//import org.geocent.netcdf.NCDataEncapsulator;
//import org.geocent.netcdf.fileParsers.AbstractFileInspector;
//import org.geocent.netcdf.fileParsers.NAVO.FileInspector;
//import org.geotools.geometry.GeneralEnvelope;
//import org.geotools.referencing.CRS;
//import org.junit.Assert;
//import org.junit.Test;
//import org.opengis.geometry.MismatchedDimensionException;
//import org.opengis.referencing.FactoryException;
//import org.opengis.referencing.NoSuchAuthorityCodeException;
//
//public class NAVOFileInspectorTest {
//
//	/*
//	 * This is a simple test that allows me to more easily debug the NAVO file
//	 * inspector, not really a test more than it is a developer tool
//	 */
//
//	@Test
//	public void TestWritableRaster() {
//		NCDataEncapsulator ncData = getNCData("water_temp", -10.0,
//				getDateFromString("10/15/2010 00:00:00"), getWorldEnvelope());
//		Raster wr = ncData.getWritableRaster();
//		// writeImage("TestImageGrayWrite", wr);
//	}
//
//	@Test
//	public void FileCacheTest() {
//		NCDataCacher.clearCache();
//		Assert.assertNull(NCDataCacher.getNCData("water_temp", -10.0,
//				getDateFromString("10/15/2010 00:00:00")));
//		NCDataCacher.putNCData(
//				"water_temp",
//				-10.0,
//				getDateFromString("10/15/2010 00:00:00"),
//				getNCData("water_temp", -10.0,
//						getDateFromString("10/15/2010 00:00:00"),
//						getWorldEnvelope()));
//		Assert.assertNotNull(NCDataCacher.getNCData("water_temp", -10.0,
//				getDateFromString("10/15/2010 00:00:00")));
//	}
//
//	private NCDataEncapsulator getNCData(String parameter, Double elevation,
//			Date time, GeneralEnvelope env) {
//		File f = new File("C:\\development\\data\\ncom");
//		AbstractFileInspector fi = new FileInspector();
//		/* THIS NO LONGER WORKS, EDIT THIS TEST BEFORE YOU TRY USING IT */
//		return fi.parseFiles(f, parameter, elevation, time, null);
//	}
//
//	private GeneralEnvelope getWorldEnvelope() {
//		GeneralEnvelope env = new GeneralEnvelope(new double[] { 0, 0 },
//				new double[] { 0, 0 });
//		env.setRange(0, -180, 180);
//		env.setRange(1, -90, 90);
//		try {
//			env.setCoordinateReferenceSystem(CRS.decode("EPSG:4326"));
//		} catch (MismatchedDimensionException e) {
//			e.printStackTrace();
//		} catch (NoSuchAuthorityCodeException e) {
//			e.printStackTrace();
//		} catch (FactoryException e) {
//			e.printStackTrace();
//		}
//		return env;
//	}
//
//	private Date getDateFromString(String dateString) {
//		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
//		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
//		try {
//			return sdf.parse("10/15/2010 00:00:00");
//		} catch (ParseException e) {
//			e.printStackTrace();
//		}
//		return null;
//	}
//
//	public void writeImage(String fileName, BufferedImage img) {
//		Iterator<ImageWriter> writerIterator = ImageIO
//				.getImageWritersByFormatName("png");
//		ImageWriter writer = writerIterator.next();
//		File f = new File(fileName + ".png");
//		ImageOutputStream ios = null;
//
//		try {
//			ios = ImageIO.createImageOutputStream(f);
//			writer.setOutput(ios);
//			writer.write(img);
//		} catch (IOException e) {
//			e.printStackTrace();
//		} finally {
//			try {
//				ios.flush();
//				ios.close();
//			} catch (Exception e1) {
//				e1.printStackTrace();
//			}
//		}
//	}
//
//}
