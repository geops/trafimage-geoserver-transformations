package org.geoserver.trafimage.transform.process;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.geoserver.trafimage.transform.CurveBuilder;
import org.geoserver.trafimage.transform.MapUnits;
import org.geoserver.trafimage.transform.util.MeasuredSimpleFeatureIterator;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

@DescribeProcess(title = "MakeOffsettedLines", description = "Move lines by an offset in pixels."
		+ " Useful for label placement onto these newly generated lines"
		+ " All attributes of the incomming features will be preserved")
public class MakeOffsettedLinesProcess extends VectorProcess implements GeoServerProcess  {

	private static final Logger LOGGER = Logging.getLogger(MakeOffsettedLinesProcess.class);
	
	public MakeOffsettedLinesProcess() {}


	/**
	 * execute the transformation
	 * 
	 * @param collection
	 * @param attributes
	 * @param monitor
	 * @return
	 * @throws ProcessException
	 */
	@DescribeResult(name = "result", description = "collection contaiing the offsetted lines")
	public SimpleFeatureCollection execute(
			// process data
			@DescribeParameter(name = "collection", description = "Input line feature collection") SimpleFeatureCollection collection,
			
			// processing parameters
			@DescribeParameter(name = "offsetInPixels",
					description = "The offset the lines should have in pixels. Negative an positive values contoll the direction of the displacement.",
					defaultValue = "0.0") Double offsetInPixels,

					
			 // --- output image parameters --------------------------------------
			@DescribeParameter(name = "outputBBOX", 
					description = "Bounding box for target image extent. Should be set using the env function from the WMS-Parameters.") ReferencedEnvelope outputEnv,
			@DescribeParameter(name = "outputWidth",
					description = "Target image width in pixels. Should be set using the env function from the WMS-Parameters.", minValue = 1) Integer outputWidth,
			@DescribeParameter(name = "outputHeight",
					description = "Target image height in pixels. Should be set using the env function from the WMS-Parameters.", minValue = 1) Integer outputHeight,			
			
					
			// --- other --------------------------------------		
			@DescribeParameter(name = "enableDurationMeasurement",
					description = "Profiling option to log time durations spend in parts of this transformation to geoservers logfile. "
					+ " This will be logged on the INFO level. "
					+ " The default is Disabled (false).",
					defaultValue = "false") boolean enableDurationMeasurement,
			ProgressListener monitor
			) throws ProcessException {
		
		final SimpleFeatureType inputFeatureType = collection.getSchema();
		
		ArrayList<Class<?>> validBindings = new ArrayList<Class<?>>();
		validBindings.add(LineString.class);
		// TODO: support multilinestrings
		this.assertInputGeometryType(inputFeatureType, validBindings);
		
		
		if (offsetInPixels == 0) {	// nothing to do. taking shortcut
			return collection;
		}
		
		final double offsetInMapUnits = MapUnits.pixelDistanceToMapUnits(outputEnv, outputWidth, outputHeight, offsetInPixels);
		
		final SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(inputFeatureType);
		final ListFeatureCollection outputCollection = new ListFeatureCollection(inputFeatureType);
		final GeometryDescriptor geomDescriptor = inputFeatureType.getGeometryDescriptor();
		final CurveBuilder curveBuilder = new CurveBuilder();
		
		final MeasuredSimpleFeatureIterator featureIt = new MeasuredSimpleFeatureIterator(collection.features());
		featureIt.setMeasuringEnabled(enableDurationMeasurement);
		try {
			while (featureIt.hasNext()) {
				try {
					final SimpleFeature inputFeature = featureIt.next();
					
					// create the new geometry with the offset
					LineString line =  (LineString) inputFeature.getDefaultGeometry();
					LineString offsettedLine = curveBuilder.buildOffsettedLineString(line, offsetInMapUnits);
					featureBuilder.set(geomDescriptor.getName(), offsettedLine);
					
					// copy attributes
					for (final AttributeDescriptor descriptor: inputFeatureType.getAttributeDescriptors()) {
						if (!(descriptor instanceof GeometryDescriptor)) {
							final Object value = inputFeature.getAttribute(descriptor.getName());
							if (!(value instanceof Geometry)) {
								featureBuilder.set(descriptor.getName(), value);
							}
						}
					}
					
					outputCollection.add(featureBuilder.buildFeature(null));
				} catch (IllegalArgumentException e) {
					// possible cause: JTS: Invalid number of points in LineString (found 1 - must be 0 or >= 2)
					LOGGER.warning("Ignoring possible illegal feature: " + e.getMessage());
				}
			}
		} finally {
			featureIt.close();
		}
		
		if (featureIt.isMeasuringEnabled()) {
			LOGGER.info("Spend "+featureIt.getTimeSpendInSeconds()+" seconds on reading "
					+ " and generating offsets for "
					+ outputCollection.size()
					+ " features from the datasource.");
		}
		return outputCollection;
	}
}
