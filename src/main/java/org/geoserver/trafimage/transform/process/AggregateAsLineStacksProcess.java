package org.geoserver.trafimage.transform.process;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import org.geoserver.trafimage.transform.CurveBuilder;
import org.geoserver.trafimage.transform.MapUnits;
import org.geoserver.trafimage.transform.SimpleFeatureAggregator;
import org.geoserver.trafimage.transform.SimpleFeatureHasher;
import org.geoserver.trafimage.transform.SimpleFeatureHelper;
import org.geoserver.trafimage.transform.script.AggregateAsLineStacksScript;
import org.geoserver.trafimage.transform.script.ScriptException;
import org.geoserver.trafimage.transform.util.DebugIO;
import org.geoserver.trafimage.transform.util.MeasuredSimpleFeatureIterator;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.LineString;

@DescribeProcess(title = "AggregateAsLineStacks", description = "AggregateAsLineStacks")
public class AggregateAsLineStacksProcess extends VectorProcess implements GeoServerProcess {

	protected class FeatureOrderComparator implements Comparator<SimpleFeature> {

		private String orderAttributeName;
		
		public int compare(SimpleFeature f0, SimpleFeature f1) {
			int orderF0 = this.getOrderValue(f0);
			int orderF1 = this.getOrderValue(f1);
			return orderF0 - orderF1;
		}
		
		private int getOrderValue(final SimpleFeature feature) {
			if (this.orderAttributeName == null || this.orderAttributeName.equals("")) {
				return 0;
			}
			Object value = feature.getAttribute(this.orderAttributeName);
			if (value == null) {
				return 0;
			}
			int orderValue = Integer.parseInt(value.toString());
			return orderValue;
		}
		
		public void setOrderAttributeName(final String orderAttributeName) {
			this.orderAttributeName = orderAttributeName;
		}
	}
	
	private static final String AGG_COUNT_ATTRIBUTE_NAME = "agg_count";
	private static final String WIDTH_ATTRIBUTE_NAME = "line_width";
	
	private static final Logger LOGGER = Logging.getLogger(AggregateAsLineStacksProcess.class);
	
	public AggregateAsLineStacksProcess() {
	}
	
	/**
	 * 
	 * @param lineIn
	 * @param outputSchema
	 * @param offsetInMapUnits
	 * @return
	 */
	protected SimpleFeature buildOffsettedLine(final SimpleFeature lineIn, final SimpleFeatureBuilder outputFeatureBuilder, final SimpleFeatureType outputSchema, double offsetInMapUnits, double widthInPixels) {
		outputFeatureBuilder.reset();
		
		final LineString line = (LineString) lineIn.getDefaultGeometry();
		
		// generate the offset
		CurveBuilder curveBuilder = new CurveBuilder();
		final LineString offsettedLine = curveBuilder.buildOffsettedLineString(line, offsetInMapUnits);
		
		// write the geometry
		GeometryDescriptor geometryDescriptor = outputSchema.getGeometryDescriptor();
		outputFeatureBuilder.set(geometryDescriptor.getName(), offsettedLine);
		
		// clone the original features attributes
		final Collection<PropertyDescriptor> descriptors = outputSchema.getDescriptors();
		for (PropertyDescriptor descriptor: descriptors) {
			if (!(descriptor instanceof GeometryDescriptor)) {
				Object value = lineIn.getAttribute(descriptor.getName());
				if (value != null) {
					outputFeatureBuilder.set(descriptor.getName(), value);
				}
			}
		}
		
		// set the width
		outputFeatureBuilder.set(WIDTH_ATTRIBUTE_NAME, widthInPixels);
		SimpleFeature featureOut = outputFeatureBuilder.buildFeature(null);
		return featureOut;
	}
	
	/**
	 * build the new featuretype for the output geometries
	 * 
	 * @param inputSchema
	 * @return
	 */
	protected SimpleFeatureType buildOutputFeatureType(final SimpleFeatureType inputSchema, final String widthAttributeName) {
		
		final SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("stacked");
		typeBuilder.setCRS(inputSchema.getCoordinateReferenceSystem());
		
		final GeometryDescriptor geomDescriptor = inputSchema.getGeometryDescriptor();
		typeBuilder.add(geomDescriptor.getName().toString(), 
				geomDescriptor.getType().getClass(), 
				inputSchema.getCoordinateReferenceSystem());

		// clone all other columns
		final Collection<PropertyDescriptor> descriptors = inputSchema.getDescriptors();
		for (PropertyDescriptor descriptor: descriptors) {
			if (!(descriptor instanceof GeometryDescriptor)) {
				LOGGER.finer("Adding descriptor "+descriptor.getName().toString()+" to new SimpleFeatureType");
				typeBuilder.add(descriptor.getName().toString(), descriptor.getType().getClass());
			}
		}
		
		// column to store the widths
		LOGGER.finer("Adding attribute "+widthAttributeName+" to new SimpleFeatureType");
		typeBuilder.add(widthAttributeName, Double.class);
		
		return typeBuilder.buildFeatureType();
	}
	
	/**
	 * 
	 * @param collection
	 * @return
	 */
	protected HashMap<Integer, List<SimpleFeature>> createStacksOfSimilarGeometries(SimpleFeatureCollection collection, boolean enableDurationMeasurement) {
		final SimpleFeatureHasher hasher = new SimpleFeatureHasher();
		hasher.setMeasuringEnabled(enableDurationMeasurement);
		hasher.setIncludeGeometry(true);
		hasher.setIncludedAttributes(new HashSet<String>());

		HashMap<Integer, List<SimpleFeature>> stacks = new HashMap<Integer, List<SimpleFeature>>();
		final MeasuredSimpleFeatureIterator featureIt = new MeasuredSimpleFeatureIterator(collection.features());
		try {
			while (featureIt.hasNext()) {
				final SimpleFeature feature = featureIt.next();
				final int hash = hasher.getHash(feature);
				
				List<SimpleFeature> stackList = stacks.get(hash);
				if (stackList == null) {
					stackList = new ArrayList<SimpleFeature>();
					stacks.put(hash, stackList);
				}
				stackList.add(feature);
			}
		} finally {
			featureIt.close();
		}
		
		if (hasher.isMeasuringEnabled()) {
			LOGGER.info("Spend "+hasher.getTimeSpendInSeconds()+" seconds on just creating feature hashes to order line stacks.");
		}
		return stacks;
	}
	
	
	/**
	 * execute the transformation
	 * 
	 * @param collection
	 * @param attributes
	 * @param monitor
	 * @return
	 * @throws ProcessException
	 */
	@DescribeResult(name = "result", description = "Aggregated feature collection")
	public SimpleFeatureCollection execute(
					// -- process data -------------------------
					@DescribeParameter(name = "collection", 
						description = "Input feature collection") SimpleFeatureCollection collection,
					
					// -- processing parameters -----------------------------
					@DescribeParameter(name = "attributes", 
							description = "Comma-seperated string of attributes to include in the aggregation") String attributes,
					@DescribeParameter(name = "orderAttribute", 
							description = "The name attribute of the input collection which contains the value for the ordering of the line stacks."
							+ " The attribute will be included in the aggregation."
							+ " The attribute must be of type integer."
							+ " The smaller the value is, the closer the feature will be placed to the orignal line.", 
							defaultValue = "") String orderAttributeName,
					@DescribeParameter(name = "invertSidesAttribute", 
							description = "The name attribute of the input collection which contains the boolean value for inverting the sides on which the stacks are drawn."
							+ " True means the sides will be inverted, Null and False will not changes the placement of the stacks."
							+ " The attribute will be included in the aggregation."
							+ " The attribute must be of type boolean.",
							defaultValue = "") String invertSidesAttributeName,
					@DescribeParameter(name = "minLineWidth",
							description = "The minimum width of a line in pixels.",
							defaultValue = "8") Integer minLineWidth,
					@DescribeParameter(name = "maxLineWidth",
							description = "The maximum width of a line in pixels.",
							defaultValue = "80") Integer maxLineWidth,
					@DescribeParameter(name = "drawOnBothSides",
							description = "Draw the stacks on both sides of the line."
							+ " Default: True",
							defaultValue = "true") boolean drawOnBothSides,
					@DescribeParameter(name = "spacingBetweenStackEntries",
							description = "The spacing between lines in a stack as well as to the original line itself. Default is 0",
							defaultValue = "0") Integer spacingBetweenStackEntries,
							
				    // --- javascript related parameters 
					@DescribeParameter(name = "renderScript",
							description="A javascript script to control the rendering of the features."
							+" Bypasses minLineWidth and maxLineWidth."
							+" For a more detailed documentation check the README.",
							defaultValue = "") String renderScript,
					@DescribeParameter(name = "scriptCustomVariable1",
							description="A value which will be exposed in the script as a global variable with the name customVariable1."
							+" The variable will only be available after the initial evaluation of the script, so it should only be used from the defined functions. Otherwise an \"undefined\" error will be raised."
							+" Useful to pass SLD and WMS parameters to the script. For example \"wms_scale_denominator\"",
							defaultValue = "") String scriptCustomVariable1,
					@DescribeParameter(name = "scriptCustomVariable2",
							description="A value which will be exposed in the script as a global variable with the name customVariable2."
							+" The variable will only be available after the initial evaluation of the script, so it should only be used from the defined functions. Otherwise an \"undefined\" error will be raised."
							+" Useful to pass SLD and WMS parameters to the script. For example \"wms_scale_denominator\"",
							defaultValue = "") String scriptCustomVariable2,		
							
					
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
					@DescribeParameter(name = "debugSqlFile", 
							description = "Name of the file to write SQL insert statements of the generated polygons to."
							+ " Other attributes will not be written."
							+ "Leave unset to deactivate.",	
							defaultValue = "") String debugSqlFile,
					ProgressListener monitor
			) throws ProcessException {
		
		final SimpleFeatureType inputFeatureType = collection.getSchema();
		this.assertInputGeometryType(inputFeatureType, LineString.class);
		
		if (minLineWidth<1) {
			throw new ProcessException("minLineWidth has to be a positive value bigger than 0, but currently is "+minLineWidth);
		}
		if (maxLineWidth<1) {
			throw new ProcessException("maxLineWidth has to be a positive value bigger than 1, but currently is "+maxLineWidth);
		}
		if (spacingBetweenStackEntries<0) {
			throw new ProcessException("spacingBetweenStackEntries has to be a positive value or 0, but currently is "+spacingBetweenStackEntries);
		}
		
		AggregateAsLineStacksScript scriptRunner = null;
		
		try {
			
			if (renderScript != null && !renderScript.trim().equals("")) {
				LOGGER.info("creating scriptRunner");
				try {
					scriptRunner = new AggregateAsLineStacksScript(renderScript);
					scriptRunner.registerVariable("customVariable1", scriptCustomVariable1);
					scriptRunner.registerVariable("customVariable2", scriptCustomVariable2);
				} catch (ScriptException e) {
					throw new ProcessException(e.getMessage(), e);
				}
			}

			// create a full list of attributes to aggregate by
			final ArrayList<String> aggregationAttributes = ParameterHelper.splitAt(attributes, ",");
			if (orderAttributeName != null && !orderAttributeName.equals("")) {
				aggregationAttributes.add(orderAttributeName);
			}
			if (invertSidesAttributeName != null && !invertSidesAttributeName.equals("")) {
				aggregationAttributes.add(invertSidesAttributeName);
			}
			
			monitor.started();
			
			// aggregate the features as simple lines for further processing
			final SimpleFeatureAggregator aggregator = new SimpleFeatureAggregator(aggregationAttributes);
			aggregator.setMeasuringEnabled(enableDurationMeasurement);
			final SimpleFeatureCollection aggLinesCollection = aggregator.aggregate(collection, AGG_COUNT_ATTRIBUTE_NAME);
			
			// create hashes again, this time only respecting the geometry itself to allow grouping similar geometries to
			// calculate the feature stacking
			final HashMap<Integer, List<SimpleFeature>> stacks = this.createStacksOfSimilarGeometries(aggLinesCollection, enableDurationMeasurement);
			
			SimpleFeatureType outputSchema = buildOutputFeatureType(aggLinesCollection.getSchema(), WIDTH_ATTRIBUTE_NAME);
			final ListFeatureCollection outputCollection = new ListFeatureCollection(outputSchema);
			final SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(outputSchema);
			
			// build the offsetted lines
			final FeatureOrderComparator comparator = new FeatureOrderComparator();
			comparator.setOrderAttributeName(orderAttributeName);
			for (List<SimpleFeature> stackFeatures: stacks.values()) {
				Collections.sort(stackFeatures, comparator);
				double stackOffsetInPixels = (double)spacingBetweenStackEntries;
				for(final SimpleFeature feature: stackFeatures) {
					try {
						// side inversion
						final double inversionValue = getInversionValue(feature, invertSidesAttributeName);	
						
						// find the width of the line
						final int aggCount = Integer.parseInt(feature.getAttribute(AGG_COUNT_ATTRIBUTE_NAME).toString());
						int featureWidthInPixels = 0;
						
						if (scriptRunner != null) {
							try {
								double featureLength = 0.0;
								Object geom = feature.getDefaultGeometry();
								if (geom != null) {
									LineString lineString = (LineString)geom;
									featureLength = lineString.getLength();
								}
								featureWidthInPixels = scriptRunner.getFeatureWidth(featureLength, aggCount);
							} catch (ScriptException e) {
								throw new ProcessException(e);
							}
						} else {
							featureWidthInPixels = Math.min( Math.max(minLineWidth, aggCount), maxLineWidth);
						}
						
						double offsetMapUnits = MapUnits.pixelDistanceToMapUnits(outputEnv, outputWidth, outputHeight, stackOffsetInPixels);
						double featureWidthInMapUnits = MapUnits.pixelDistanceToMapUnits(outputEnv, outputWidth, outputHeight, featureWidthInPixels);
						
						if (drawOnBothSides) {
							offsetMapUnits = offsetMapUnits + 
									(featureWidthInMapUnits 
											/ 2.0 /* on two sides*/ 
											/ 2.0 /* middle of the line to be drawn */
									);
						} else {
							offsetMapUnits = offsetMapUnits + 
									(featureWidthInMapUnits 
											/ 2.0 /* middle of the line to be drawn */
									);
						}
						
						/*
						if (aggCount>2) {
							LOGGER.info(
									"aggCount: "+aggCount
									+"; spacingBetweenStackEntries:"+(double)spacingBetweenStackEntries
									+"; maxLineWidth: "+maxLineWidth
									+"; minLineWidth: "+minLineWidth
									+"; featureWidthInPixels: "+featureWidthInPixels+"px"
									+"; featureWidthInMapUnits: "+featureWidthInMapUnits
									+"; stackOffsetInPixels: "+stackOffsetInPixels
									+"; offsetMapUnits: "+offsetMapUnits
									+"; klasse_color: "+feature.getAttribute("klasse_color").toString()
									);
						}
						*/
						
						if (drawOnBothSides) {
							final SimpleFeature line1 = this.buildOffsettedLine(feature, featureBuilder, outputSchema, 
									offsetMapUnits * inversionValue, featureWidthInPixels / 2.0);
							final SimpleFeature line2 = this.buildOffsettedLine(feature, featureBuilder, outputSchema, 
									offsetMapUnits * inversionValue * -1.0, featureWidthInPixels / 2.0);
							
							outputCollection.add(line1);
							outputCollection.add(line2);
							
							stackOffsetInPixels = stackOffsetInPixels + (featureWidthInPixels / 2.0) + (double)spacingBetweenStackEntries;
						} else {
							final SimpleFeature line1 = this.buildOffsettedLine(feature, featureBuilder, outputSchema, 
									offsetMapUnits * inversionValue, featureWidthInPixels);
							outputCollection.add(line1);
							
							stackOffsetInPixels = stackOffsetInPixels + featureWidthInPixels + (double)spacingBetweenStackEntries;
						}
					} catch (IllegalArgumentException e) {
						// possible cause: JTS: Invalid number of points in LineString (found 1 - must be 0 or >= 2)
						LOGGER.warning("Ignoring possible illegal feature: " + e.getMessage());
					}
				}
			}
			
			monitor.complete();
			
			if (!debugSqlFile.equals("")) {
				LOGGER.warning("Writing debugSqlFile to "+debugSqlFile+". This should only be activated for debugging purposes.");
				DebugIO.dumpCollectionToSQLFile(outputCollection, debugSqlFile, "stacked_lines");
			}
			
			LOGGER.info("Returning a collection with "+outputCollection.size()+" features");
			
			return outputCollection;
		} finally {
			if (scriptRunner != null) {
				scriptRunner.terminate();
			}
		}
		
	}
	
	
	/**
	 * get a value which inverts the side on which the stack is drawn based on the 
	 * value of the invertSidesAttribute of the feature
	 * @param feature
	 * @param invertSidesAttributeName
	 * @return
	 */
	private double getInversionValue(SimpleFeature feature, String invertSidesAttributeName) {
		if (SimpleFeatureHelper.getBooleanValue(feature, invertSidesAttributeName, false)) {
			return -1.0;
		}
		return 1.0;
	}
}
