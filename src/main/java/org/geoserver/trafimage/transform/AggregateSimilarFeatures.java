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
	
	/**
	 * build the new featuretype for the output geometries
	 * 
	 * @param inputSchema
	 * @param attributesSet
	 * @return
	 */
	protected SimpleFeatureType buildOutputFeatureType(final SimpleFeatureType inputSchema, final HashSet<String> attributesSet, final String aggregateAttributeName) {
		
		final SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("aggregate");
		typeBuilder.setCRS(inputSchema.getCoordinateReferenceSystem());
		
		final GeometryDescriptor geomDescriptor = inputSchema.getGeometryDescriptor();
		typeBuilder.add(geomDescriptor.getName().toString(), 
				geomDescriptor.getType().getClass(), 
				inputSchema.getCoordinateReferenceSystem());

		Iterator<String> attributesSetIt = attributesSet.iterator();
		while(attributesSetIt.hasNext()) {
			final String attributeName =  attributesSetIt.next();
			AttributeDescriptor descriptor = inputSchema.getDescriptor(attributeName);
			LOGGER.finer("Adding attribute "+attributeName+" to new SimpleFeatureType");
			typeBuilder.add(attributeName, descriptor.getType().getClass());
		}
		LOGGER.finer("Adding attribute "+aggregateAttributeName+" to new SimpleFeatureType");
		
		// column to store the counts
		typeBuilder.add(aggregateAttributeName, Integer.class);
		
		return typeBuilder.buildFeatureType();
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

			ProgressListener monitor
			) throws ProcessException {
			
		return aggregate(collection, this.splitAt(attributes, ","), AGG_COUNT_ATTRIBUTE_NAME);
	}
	
	
	/**
	 * 
	 * @param collection
	 * @param arrayList
	 * @return
	 */
	protected SimpleFeatureCollection aggregate(final SimpleFeatureCollection collection, final ArrayList<String> arrayList, final String aggregateAttributeName) {
		final SimpleFeatureType inputSchema = collection.getSchema();
		final SimpleFeatureHasher hasher = new SimpleFeatureHasher();
		hasher.setIncludeGeometry(true);
		
		// process the attributes string into a set to eliminate duplicates
		for (final String attributeName: arrayList) {
			if (inputSchema.getDescriptor(attributeName) != null) {
				hasher.addIncludedAttribute(attributeName);
			}
		}

		// build the new featuretype for the output geometries
		final HashSet<String> attributesSet = hasher.getIncludedAttributes();
		final SimpleFeatureType outputSchema = this.buildOutputFeatureType(inputSchema, attributesSet, aggregateAttributeName);
		
		// aggregate the features
		final SimpleFeatureIterator featureIt = collection.features();
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
				final Object geometry = feature.getDefaultGeometry();
				featureBuilder.set(0, geometry);
				
				final Iterator<String> attributesSetIt2 = attributesSet.iterator();
				while(attributesSetIt2.hasNext()) {
					final String attributeName =  attributesSetIt2.next();
					final Object attributeValue = feature.getAttribute(attributeName);
					featureBuilder.set(attributeName, attributeValue);
				}
				featureBuilder.set(aggregateAttributeName, 0);
				final SimpleFeature outputFeature = featureBuilder.buildFeature(feature.getID());
				featureMap.put(hash, outputFeature);
			}
			
			final SimpleFeature outputFeature = featureMap.get(hash);
			outputFeature.setAttribute(aggregateAttributeName, 
					(Integer)outputFeature.getAttribute(aggregateAttributeName) + 1);
		}
		
		// build the result collection
		final ListFeatureCollection result = new ListFeatureCollection(outputSchema);
		result.addAll(featureMap.values());
		LOGGER.finer("Aggregated "+collection.size()+" incoming features to "
					+result.size()+" outgoing features");
		return result;
	}
	
	/**
	 * Split a string at a separator and return the unique trimmed entries 
	 * from the string.
	 *  
	 * @param input
	 * @param seperator
	 * @return
	 */
	protected ArrayList<String> splitAt(final String input, final String seperator) {
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
