package org.geoserver.trafimage.transform;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.geoserver.trafimage.transform.util.DebugIO;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
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
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;



@DescribeProcess(title = "AggregateSimilarLinesAsPolygon", description = "Aggregate similar Line Features by their geometry and additional columns. "
		+ "Returns the distict Polygon geometries, the aggregated columns, an"
		+ " additional column 'agg_count' holding the number of features in the aggregation"
		+ " and the additional column 'polygon_width' which contains the width of the polygon in pixels")
public class AggregateSimilarLinesAsPolygonsProcess implements GeoServerProcess {

	private static final String AGG_COUNT_ATTRIBUTE_NAME = "agg_count";
	private static final String POLYGON_WIDTH_ATTRIBUTE_NAME = "polygon_width";
	
	private static final Logger LOGGER = Logging.getLogger(AggregateSimilarLinesAsPolygonsProcess.class);
	
	public AggregateSimilarLinesAsPolygonsProcess() {
	}

	/**
	 * return a FeatureType like the inputFeatureType just with Polygon as geometrytype
	 * 
	 * @param inputFeatureType
	 * @return
	 */
	private SimpleFeatureType buildPolygonFeatureType(final SimpleFeatureType inputFeatureType) {
		final SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("polyonAggregate");
		typeBuilder.setCRS(inputFeatureType.getCoordinateReferenceSystem());
		
		final GeometryDescriptor geomDescriptor = inputFeatureType.getGeometryDescriptor();
		typeBuilder.add(geomDescriptor.getName().toString(), Polygon.class, 
				inputFeatureType.getCoordinateReferenceSystem());

		for (final AttributeDescriptor descriptor: inputFeatureType.getAttributeDescriptors()) {
			if (!(descriptor instanceof GeometryDescriptor)) {
				LOGGER.fine("Adding attribute "+descriptor.getName().toString()+" to new SimpleFeatureType");
				typeBuilder.add(descriptor);
			}
		}
		
		typeBuilder.add(POLYGON_WIDTH_ATTRIBUTE_NAME, Double.class);
		return typeBuilder.buildFeatureType();
	}
	
	/**
	 * 
	 * @param inputSchema
	 */
	private void checkInputGeometryType(final SimpleFeatureType inputSchema) {
		GeometryDescriptor geomDescriptor = inputSchema.getGeometryDescriptor();
		if (geomDescriptor.getType().getBinding() != LineString.class) {
			throw new ProcessException("Inputgeometries are not Linestrings");
		}
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
	@DescribeResult(name = "result", description = "Aggregated feature collection containing polygons."
			+ "The outputlayer will contain the geometry, all attibutes included in the aggregation,"
		    + " the attribute agg_count containing the number of features aggregated in a polygon,"
			+ " and the attriute polygon_width which contains the width of a polygon in pixels")
	public SimpleFeatureCollection execute(
			// -- process data -------------------------
			@DescribeParameter(name = "collection", 
					description = "Input feature collection") SimpleFeatureCollection collection,
			
					
			// -- processing parameters -----------------------------
			@DescribeParameter(name = "attributes", 
					description = "Comma-seperated string of attributes to include in the aggregation") String attributes,
			@DescribeParameter(name = "offsetAttribute", 
					description = "The name attribute of the input collection which contains the value for the offset of the generated polygon."
					+ " The attribute will be included in the aggregation."
					+ " The attribute must be of type integer."
					+ " If nothing is given here, polygons will be centered on the line. Value is in pixels.", 
					defaultValue = "") String offsetAttributeName,
			@DescribeParameter(name = "widthScalingAlgorithm", 
					description = "The scaling algorithm to use for the polygon width."
					+ " Possible values are: linear, logarithmic", 
					defaultValue = "linear") String widthScalingAlgorithm,
			@DescribeParameter(name = "maxPolygonWidth",
					description = "The maximum width of a polygon in pixels.",
					defaultValue = "20") Integer maxPolygonWidth,
			@DescribeParameter(name = "enableArtifactRemoval",
					description = "Attempt to remove rendering artifacts in polygons."
					+ " This is a very expensive operation and will only run in a acceptable time when there are just a few features.",
					defaultValue = "false") boolean enableArtifactRemoval,
			@DescribeParameter(name = "maxPolygonWidthFeatureCount",
					description = "The number of features which have to be in an aggregation for the polygon to be drawn with <maxPolygonWidth>."
					+ " This parameter is optional and allows overriding the default. The default is the number of features in the"
					+ " largest aggregation. While this leads to an even spread of line width, it also results in growing/shrinking polygons"
					+ " when the map extents is modified and other aggregations enter or exit the view."
					+ " This, of course, will result in all values above this parameter being squashed at the <maxPolygonWidth>."
					+ " Value has to be an positive Integer larger than 0.",
					defaultValue = "0") Integer maxPolygonWidthFeatureCount,
	/*			
			@DescribeParameter(name = "widthAttribute", 
					description = "The name attribute of the input collection which contains the value for the width of the generated polygon."
					+ " The attribute will be included in the aggregation."
					+ " The attribute must be of type integer."
					+ " If nothing is given here, 10 will be assumed. Value is in pixels.", 
					defaultValue = "") String widthAttributeName,
	*/
					
			 // --- output image parameters --------------------------------------
			@DescribeParameter(name = "outputBBOX", 
					description = "Bounding box for target image extent. Should be set using the env function from the WMS-Parameters.") ReferencedEnvelope outputEnv,
			@DescribeParameter(name = "outputWidth",
					description = "Target image width in pixels. Should be set using the env function from the WMS-Parameters.", minValue = 1) Integer outputWidth,
			@DescribeParameter(name = "outputHeight",
					description = "Target image height in pixels. Should be set using the env function from the WMS-Parameters.", minValue = 1) Integer outputHeight,
					
					
			// --- other --------------------------------------
			@DescribeParameter(name = "debugSqlFile", 
					description = "Name of the file to write SQL insert statements of the generated polygons to."
					+ " Other attributes will not be written."
					+ "Leave unset to deactivate.",	
					defaultValue = "") String debugSqlFile,
			@DescribeParameter(name = "enableDurationMeasurement",
					description = "Profiling option to log time durations spend in parts of this transformation to geoservers logfile. "
					+ " This will be logged on the INFO level. "
					+ " The default is Disabled (false).",
					defaultValue = "false") boolean enableDurationMeasurement,
					
			ProgressListener monitor
			) throws ProcessException {
					
		final SimpleFeatureType inputFeatureType = collection.getSchema();
		checkInputGeometryType(inputFeatureType);
		
		if (maxPolygonWidthFeatureCount<0) {
			throw new ProcessException("maxPolygonWithFeatureCount has to be a positive value, but currently is "+maxPolygonWidthFeatureCount);
		}
		
		// choose the drawing configuration
		PolygonDrawingAlgorithm drawingAlgo = this.getPolygonDrawingAlgorithm(widthScalingAlgorithm);
		drawingAlgo.setOffsetAttributeName(offsetAttributeName);
		//drawingAlgo.setWidthAttributeName(widthAttributeName);
		drawingAlgo.setMaxPolygonWidth(maxPolygonWidth);
		
		// create a full list of attributes to aggregate by
		final ArrayList<String> aggregationAttributes = ParameterHelper.splitAt(attributes, ",");
		aggregationAttributes.addAll(drawingAlgo.getAdditionalAggregationAttributes()); // to have unique values and preserve 
																						  // the attribute during aggregation
		monitor.started();
		monitor.progress(0.0f);
		
		// aggregate the features as simple lines for further processing
		final SimpleFeatureAggregator aggregator = new SimpleFeatureAggregator(aggregationAttributes);
		aggregator.setMeasuringEnabled(enableDurationMeasurement);
		final SimpleFeatureCollection aggLinesCollection = aggregator.aggregate(collection, AGG_COUNT_ATTRIBUTE_NAME);
		if (maxPolygonWidthFeatureCount>0) {
			drawingAlgo.setMaxPolygonWidthFeatureCount(maxPolygonWidthFeatureCount);
		} else {
			drawingAlgo.setMaxPolygonWidthFeatureCount(aggregator.getAggregationStatistics().numMaxEntriesInAggregate);
		}
		drawingAlgo.setAggCountAttributeName(AGG_COUNT_ATTRIBUTE_NAME);
		
		final SimpleFeatureType outputFeatureType = buildPolygonFeatureType(inputFeatureType);
		final ListFeatureCollection outputCollection = new ListFeatureCollection(outputFeatureType);
		
		final LineToPolygonConverter lineToPolygon = new LineToPolygonConverter();
		lineToPolygon.setCenterOnLine(drawingAlgo.getCenterOnLine());
		lineToPolygon.setEnableArtifactRemoval(enableArtifactRemoval);
		lineToPolygon.setMeasuringEnabled(enableDurationMeasurement);
		
		// build polygons
		final SimpleFeatureIterator aggLinesIt = aggLinesCollection.features();
		final int aggLinesCount = aggLinesCollection.size();
		final SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(outputFeatureType);
		try {
			int aggLineI = 0;
			while (aggLinesIt.hasNext()) {
				final SimpleFeature aggLine = aggLinesIt.next();
				
				monitor.progress((float)aggLineI/(float)aggLinesCount);
				
				final double widthPx = drawingAlgo.getPolygonWidth(aggLine);
				final double offsetPx = drawingAlgo.getPolygonOffset(aggLine);
				
				lineToPolygon.setWidth(MapUnits.pixelDistanceToMapUnits(outputEnv, outputWidth, outputHeight, widthPx));
				lineToPolygon.setOffset(MapUnits.pixelDistanceToMapUnits(outputEnv, outputWidth, outputHeight, offsetPx));
				
				// the geometry is field number 0. see buildPolygonFeatureType
				Object lineGeometry = aggLine.getDefaultGeometry();
				if (lineGeometry != null) {
					if (!(lineGeometry instanceof LineString)) {
						throw new ProcessException("Input geometries must be of the type LineString, is: "+lineGeometry.getClass().getName());
					}
					featureBuilder.set(0, lineToPolygon.convert((LineString) lineGeometry));
				}
				
				// copy attributes
				for (final AttributeDescriptor descriptor: inputFeatureType.getAttributeDescriptors()) {
					if (!(descriptor instanceof GeometryDescriptor)) {
						final Object value = aggLine.getAttribute(descriptor.getName());
						if (!(value instanceof Geometry)) {
							featureBuilder.set(descriptor.getName(), value);
						}
					}
				}
				featureBuilder.set(POLYGON_WIDTH_ATTRIBUTE_NAME, widthPx);
				outputCollection.add(featureBuilder.buildFeature(aggLine.getID()));
				aggLineI++;
			}
		} finally {
			aggLinesIt.close();
		}
		
		if (lineToPolygon.isMeasuringEnabled()) {
			LOGGER.info("Spend "+lineToPolygon.getTimeSpendInSeconds()+" seconds on just converting lines to polygons.");
		}
		
		// sort the features so no wider polygon covers a smaller polygon. This may not be respected by the 
		// renderer
		final SimpleFeatureCollection sortedOutputCollection = this.sortCollection(outputCollection);

		if (!debugSqlFile.equals("")) {
			LOGGER.warning("Writing debugSqlFile to "+debugSqlFile+". This should only be activated for debugging purposes.");
			DebugIO.dumpCollectionToSQLFile(sortedOutputCollection, debugSqlFile, "aggregated_polygons");
		}
		
		monitor.progress(1.0f);
		monitor.complete();

		LOGGER.fine("Returning "+sortedOutputCollection.size()+" polygons");
		
		return sortedOutputCollection;
	}
	
	/**
	 * 
	 * @param name
	 * @return
	 */
	private PolygonDrawingAlgorithm getPolygonDrawingAlgorithm(final String algorithmName) {
		if (algorithmName.equals("linear") || algorithmName.equals("lin")) { 
			return new LinearPolygonDrawingAlgorithm();
		} else if (algorithmName.equals("logarithmic") || algorithmName.equals("log")) {
			return new LogarithmicPolygonDrawingAlgorithm();
		} else {
			throw new ProcessException("Unknown scaling algorithm for widthScaling: "+algorithmName);
		}
	}

	
	/**
	 * sort the features so no wider polygon covers a smaller polygon.
	 * 
	 * @param inCollection
	 * @return
	 */
	private SimpleFeatureCollection sortCollection(final SimpleFeatureCollection inCollection) {
		// seems as the sorting does not work with doubles, so this is sorting the indirect way by using the agg_count
		// instead
		SortBy sortBy = CommonFactoryFinder.getFilterFactory().sort(AGG_COUNT_ATTRIBUTE_NAME,
                SortOrder.DESCENDING);
		SimpleFeatureCollection sortedOutputCollection = inCollection.sort(sortBy);
		return sortedOutputCollection;
	}
	
}
