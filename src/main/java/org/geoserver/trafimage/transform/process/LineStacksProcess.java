package org.geoserver.trafimage.transform.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.geoserver.trafimage.transform.FeatureOrderComparator;
import org.geoserver.trafimage.transform.MapUnits;
import org.geoserver.trafimage.transform.SimpleFeatureHasher;
import org.geoserver.trafimage.transform.script.LineStacksScript;
import org.geoserver.trafimage.transform.script.ScriptException;
import org.geoserver.trafimage.transform.util.DebugIO;
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
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.LineString;

@DescribeProcess(title = "LineStacksProcess", description = "LineStacksProcess")
public class LineStacksProcess extends AbstractStackProcess implements GeoServerProcess {

	private static final String WIDTH_ATTRIBUTE_NAME = "line_width";
	
	private static final Logger LOGGER = Logging.getLogger(LineStacksProcess.class);
	
	public LineStacksProcess() {
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
	@DescribeResult(name = "result", description = "feature collection")
	public SimpleFeatureCollection execute(
					// -- process data -------------------------
					@DescribeParameter(name = "collection", 
						description = "Input feature collection") SimpleFeatureCollection collection,
					
					// -- processing parameters -----------------------------
					@DescribeParameter(name = "orderAttribute", 
							description = "The name attribute of the input collection which contains the value for the ordering of the line stacks."
							+ " The attribute must be of type integer."
							+ " The smaller the value is, the closer the feature will be placed to the orignal line.", 
							defaultValue = "") String orderAttributeName,
					@DescribeParameter(name = "invertSidesAttribute", 
							description = "The name attribute of the input collection which contains the boolean value for inverting the sides on which the stacks are drawn."
							+ " True means the sides will be inverted, Null and False will not changes the placement of the stacks."
							+ " The attribute will be included in the aggregation."
							+ " The attribute must be of type boolean.",
							defaultValue = "") String invertSidesAttributeName,
					@DescribeParameter(name = "lineWidth",
							description = "The minimum width of a line in pixels.",
							defaultValue = "8") Integer lineWidth,
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
		
		if (lineWidth<1) {
			throw new ProcessException("lineWidth has to be a positive value bigger than 0, but currently is "+lineWidth);
		}
		if (spacingBetweenStackEntries<0) {
			throw new ProcessException("spacingBetweenStackEntries has to be a positive value or 0, but currently is "+spacingBetweenStackEntries);
		}
		
		SimpleFeatureType outputSchema = this.buildOutputFeatureType(inputFeatureType, WIDTH_ATTRIBUTE_NAME);
		final ListFeatureCollection outputCollection = new ListFeatureCollection(outputSchema);
		
		LineStacksScript scriptRunner = null;
		try {
			
			if (renderScript != null && !renderScript.trim().equals("")) {
				LOGGER.info("creating scriptRunner");
				try {
					scriptRunner = new LineStacksScript(renderScript);
					scriptRunner.registerVariable("customVariable1", scriptCustomVariable1);
					scriptRunner.registerVariable("customVariable2", scriptCustomVariable2);
				} catch (ScriptException e) {
					throw new ProcessException(e.getMessage(), e);
				}
			}

			monitor.started();
			
			// create hashes to find similar geometries
			final MeasuredSimpleFeatureIterator featureIt = new MeasuredSimpleFeatureIterator(collection.features());
			featureIt.setMeasuringEnabled(enableDurationMeasurement);
			try {
			    SimpleFeatureHasher hasher = new SimpleFeatureHasher();
			    hasher.setIncludeGeometry(true);
			    final HashMap<Integer, ArrayList<SimpleFeature>> stacks = new HashMap<Integer, ArrayList<SimpleFeature>>();

				while (featureIt.hasNext()) {
					final SimpleFeature feature = featureIt.next();
					final int hash = hasher.getHash(feature);
					
					if (!stacks.containsKey(hash)) {
						stacks.put(hash, new ArrayList<SimpleFeature>());
					}
					stacks.get(hash).add(feature);
				}


                if (enableDurationMeasurement) {
                    LOGGER.info("Spend "+featureIt.getTimeSpendInSeconds()+" seconds on just reading "
                            + collection.size()
                            + " features from the datasource.");
                }

                final SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(outputSchema);


                // build the offsetted lines
                final FeatureOrderComparator comparator = new FeatureOrderComparator();
                comparator.setOrderAttributeName(orderAttributeName);
                for (List<SimpleFeature> stackFeatures: stacks.values()) {
                    Collections.sort(stackFeatures, comparator);
                    HashMap<Double, Double> stackOffsetInPixels = new HashMap<Double, Double>();
                    for(final SimpleFeature feature: stackFeatures) {
                        try {		
                            // find the width of the line
                            int featureWidthInPixels = lineWidth;
                            if (scriptRunner != null) {
                                try {
                                    double featureLength = 0.0;
                                    Object geom = feature.getDefaultGeometry();
                                    if (geom != null) {
                                        LineString lineString = (LineString)geom;
                                        featureLength = lineString.getLength();
                                    }
                                    featureWidthInPixels = scriptRunner.getFeatureWidth(featureLength);
                                } catch (ScriptException e) {
                                    throw new ProcessException(e);
                                }
                            }

                            double inversionValue = getInversionValue(feature, invertSidesAttributeName);
                            double stackOffsetInPixelsSide = stackOffsetInPixels.containsKey(inversionValue) ?
                                    stackOffsetInPixels.get(inversionValue) : (double)spacingBetweenStackEntries;
                            
                            double baseOffsetMapUnits = MapUnits.pixelDistanceToMapUnits(outputEnv, outputWidth, outputHeight, stackOffsetInPixelsSide);
                            double featureWidthInMapUnits = MapUnits.pixelDistanceToMapUnits(outputEnv, outputWidth, outputHeight, featureWidthInPixels);
                            double offsetMapUnits = calculateOffsetInMapUnits(baseOffsetMapUnits, featureWidthInMapUnits,  drawOnBothSides);
                            
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

                            stackOffsetInPixelsSide = addDrawableLines(outputCollection, featureBuilder, feature,
                                    outputSchema, offsetMapUnits, featureWidthInPixels, stackOffsetInPixelsSide, spacingBetweenStackEntries,
                                    drawOnBothSides, invertSidesAttributeName, WIDTH_ATTRIBUTE_NAME);

                            stackOffsetInPixels.put(inversionValue, stackOffsetInPixelsSide);
                            
                        } catch (IllegalArgumentException e) {
                            // possible cause: JTS: Invalid number of points in LineString (found 1 - must be 0 or >= 2)
                            LOGGER.warning("Ignoring possible illegal feature: " + e.getMessage());
                        }
                    }
                }

			} finally {
				featureIt.close(); // closes the underlying database query, ...  
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
	

}
