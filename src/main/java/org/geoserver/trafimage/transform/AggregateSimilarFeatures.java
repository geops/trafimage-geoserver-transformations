package org.geoserver.trafimage.transform;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Logger;

import org.geoserver.wps.gs.GeoServerProcess;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
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
public class AggregateSimilarFeatures implements GeoServerProcess  {

	private static final Logger LOGGER = Logging.getLogger(AggregateSimilarFeatures.class);
	
	protected static final String AGG_COUNT_ATTRIBUTE_NAME = "agg_count";
	
	public AggregateSimilarFeatures() {
		// TODO Auto-generated constructor stub
	}
	
	@DescribeResult(name = "result", description = "Aggregated feature collection")
	public SimpleFeatureCollection execute(
			// process data
			@DescribeParameter(name = "collection", description = "Input feature collection") SimpleFeatureCollection collection,
			
			// processing parameters
			@DescribeParameter(name = "attributes", description = "Comma-seperated string of attributes to include in the aggregation") String attributes,
			
			 // output image parameters
			@DescribeParameter(name = "outputBBOX", description = "Bounding box for target image extent") ReferencedEnvelope outputEnv,
			@DescribeParameter(name = "outputWidth", description = "Target image width in pixels", minValue = 1) Integer outputWidth,
			@DescribeParameter(name = "outputHeight", description = "Target image height in pixels", minValue = 1) Integer outputHeight,

			ProgressListener monitor
			) throws ProcessException {
		
		LOGGER.finer("Got "+collection.size()+" features incomming");
		
		final SimpleFeatureType inputSchema = collection.getSchema();
		final SimpleFeatureHasher hasher = new SimpleFeatureHasher();
		hasher.setIncludeGeometry(true);
		
		// process the attributes string into a set to elminate duplicates
		for (final String attributeName: this.splitAt(attributes, ",")) {
			if (inputSchema.getDescriptor(attributeName) != null) {
				hasher.addIncludedAttribute(attributeName);
			}
		}

		// build the new featuretype for the output geometries
		final SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("aggregate");
		typeBuilder.setCRS(inputSchema.getCoordinateReferenceSystem());
		
		final GeometryDescriptor geomDescriptor = inputSchema.getGeometryDescriptor();
		typeBuilder.add(geomDescriptor.getName().toString(), geomDescriptor.getType().getClass(), 
				inputSchema.getCoordinateReferenceSystem());
		
		final HashSet<String> attributesSet = hasher.getIncludedAttributes();
		Iterator<String> attributesSetIt = attributesSet.iterator();
		while(attributesSetIt.hasNext()) {
			final String attributeName =  attributesSetIt.next();
			AttributeDescriptor descriptor = inputSchema.getDescriptor(attributeName);
			LOGGER.finer("Adding attribute "+attributeName+" to new SimpleFeatureType");
			typeBuilder.add(attributeName, descriptor.getType().getClass());
		}
		LOGGER.finer("Adding attribute "+AGG_COUNT_ATTRIBUTE_NAME+" to new SimpleFeatureType");
		typeBuilder.add(AGG_COUNT_ATTRIBUTE_NAME, Integer.class);
		
		final SimpleFeatureType outputSchema = typeBuilder.buildFeatureType();
		
		
		// aggregate the features
		SimpleFeatureIterator featureIt = collection.features();
		final HashMap<String, SimpleFeature> featureMap = new HashMap<String,SimpleFeature>();
		
		while (featureIt.hasNext()) {
			final SimpleFeature feature = featureIt.next();
			String hash = null;
			try {
				hash = hasher.getHash(feature);
			} catch (NoSuchAlgorithmException e) {
				throw new ProcessException(e);
			}
			
			if (!featureMap.containsKey(hash)) {
				final SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(outputSchema);
				
				
				// idx =0 is always the geometry. same order as during the creation of the SimpleFeatureBuilder
				Object dgeom = feature.getDefaultGeometry();
				featureBuilder.set(0, dgeom);
				
				
				Iterator<String> attributesSetIt2 = attributesSet.iterator();
				while(attributesSetIt2.hasNext()) {
					final String attributeName =  attributesSetIt2.next();
					final Object attributeValue = feature.getAttribute(attributeName);
					featureBuilder.set(attributeName, attributeValue);
				}
				featureBuilder.set(AGG_COUNT_ATTRIBUTE_NAME, 0);
				final SimpleFeature outputFeature = featureBuilder.buildFeature(feature.getID());
				featureMap.put(hash, outputFeature);
			}
			
			final SimpleFeature outputFeature = featureMap.get(hash);
			outputFeature.setAttribute(AGG_COUNT_ATTRIBUTE_NAME, 
					(Integer)outputFeature.getAttribute(AGG_COUNT_ATTRIBUTE_NAME) + 1);
		}
		
		// build the result collection
		ListFeatureCollection result = new ListFeatureCollection(outputSchema);
		result.addAll(featureMap.values());
		LOGGER.finer("Got "+result.size()+" features outgoing");
		return result;
	}

	
	protected ArrayList<String> splitAt(String input, String seperator) {
		final String[] parts = input.split(seperator);
		final ArrayList<String> result = new ArrayList<String>();
		for (final String part: parts) {
			final String partTrimmed = part.trim();
			if (!partTrimmed.equals("")) {
				result.add(partTrimmed);
			}
		}
		return result;
	}
}
