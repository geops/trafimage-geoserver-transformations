package org.geoserver.trafimage.transform;

import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.opengis.util.ProgressListener;



@DescribeProcess(title = "AggregateSimilarLinesAsPolygon", description = "Aggregate similar Line Features by their geometry and additional columns. "
		+ "Returns the distict Polygon geometries, the aggregated columns as well as an"
		+ " additional column 'agg_count' holding the number of features in the aggregation"
		+ " ... TODO ...") // TODO offset, width, ...
public class AggregateSimilarLinesAsPolygonsProcess implements GeoServerProcess {

	public AggregateSimilarLinesAsPolygonsProcess() {
		// TODO Auto-generated constructor stub
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
			@DescribeParameter(name = "offsetAttribute", description = "The name attribute of the input collection which contains the value for the offset of the generated polygon."
					+ " The attribute will be included in the aggregation."
					+ " The attribute must be of type integer."
					+ " If nothing is given here, 0 will be assumed. Value is in pixels.") String offsetAttribute,
			@DescribeParameter(name = "widthAttribute", description = "The name attribute of the input collection which contains the value for the width of the generated polygon."
					+ " The attribute will be included in the aggregation."
					+ " The attribute must be of type integer."
					+ " If nothing is given here, 10 will be assumed. Value is in pixels.") String widthAttribute,
			
			 // output image parameters
			@DescribeParameter(name = "outputBBOX", description = "Bounding box for target image extent. Should be set using the env function from the WMS-Parameters.") ReferencedEnvelope outputEnv,
			@DescribeParameter(name = "outputWidth", description = "Target image width in pixels. Should be set using the env function from the WMS-Parameters.", minValue = 1) Integer outputWidth,
			@DescribeParameter(name = "outputHeight", description = "Target image height in pixels. Should be set using the env function from the WMS-Parameters.", minValue = 1) Integer outputHeight,

			ProgressListener monitor
			) throws ProcessException {
			
		return collection;
	}
}
