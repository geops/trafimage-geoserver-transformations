package org.geoserver.trafimage.transform;

import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.opengis.util.ProgressListener;

/*
 * Aggregate Features by their geometry and additional columns
 * 
 * 
 * Documentation for Geoserver 2.4.x:
 *    http://docs.geoserver.org/2.4.x/en/developer/programming-guide/wps-services/implementing.html
 */

@DescribeProcess(title = "AggregateSimilarFeatures", description = "Aggregate similar Features by their geometry and additional columns. "
				+ "Returns the distict geometries, the aggregated columns as well as an"
				+ " additional column 'agg_count' holding the number of features in the aggregation")
public class AggregateSimilarFeaturesProcess implements GeoServerProcess  {

	private static final String AGG_COUNT_ATTRIBUTE_NAME = "agg_count";
	
	public AggregateSimilarFeaturesProcess() {
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
			// process data
			@DescribeParameter(name = "collection", description = "Input feature collection") SimpleFeatureCollection collection,
			
			// processing parameters
			@DescribeParameter(name = "attributes", description = "Comma-seperated string of attributes to include in the aggregation") String attributes,

			// other
			@DescribeParameter(name = "enableDurationMeasurement",
					description = "Profiling option to log time durations spend in parts of this transformation to geoservers logfile. "
					+ " This will be logged on the INFO level. "
					+ " The default is Disabled (false).",
					defaultValue = "false") boolean enableDurationMeasurement,
					
			ProgressListener monitor
			) throws ProcessException {
			
		SimpleFeatureAggregator aggregator = new SimpleFeatureAggregator(ParameterHelper.splitAt(attributes, ","));
		aggregator.setMeasuringEnabled(enableDurationMeasurement);
		return aggregator.aggregate(collection, AGG_COUNT_ATTRIBUTE_NAME);
	}
	
}
