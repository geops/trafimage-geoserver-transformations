package org.geoserver.trafimage.transform.process;

import java.util.Collection;
import java.util.logging.Logger;

import org.geoserver.trafimage.transform.CurveBuilder;
import org.geoserver.trafimage.transform.SimpleFeatureHelper;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.PropertyDescriptor;

import com.vividsolutions.jts.geom.LineString;


abstract class AbstractStackProcess extends VectorProcess {
	
	private static final Logger LOGGER = Logging.getLogger(AbstractStackProcess.class);
	
	/**
	 * 
	 * @param lineIn
	 * @param outputSchema
	 * @param offsetInMapUnits
	 * @return
	 */
	private SimpleFeature buildGenericOffsettedLine(final SimpleFeature lineIn, final SimpleFeatureBuilder outputFeatureBuilder, final SimpleFeatureType outputSchema, double offsetInMapUnits, double widthInPixels, String widthAttributeName) {
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
		outputFeatureBuilder.set(widthAttributeName, widthInPixels);
		SimpleFeature featureOut = outputFeatureBuilder.buildFeature(null);
		return featureOut;
	}
	
	
	/**
	 * create the new drawable lines and add them to the output collection
	 *
	 * TODO: needs refactoring to reduce the number of arguments
	 *
	 * @param outputCollection
	 * @param featureBuilder
	 * @param originalFeature
	 * @param outputSchema
	 * @param offsetMapUnits
	 * @param featureWidthInPixels
	 * @param stackOffsetInPixels
	 * @param spacingBetweenStackEntries
	 * @param drawOnBothSides
	 * @param invertSidesAttributeName
	 * @param widthAttributeName
	 * @return the new stackOffsetInPixels
	 */
	protected double addDrawableLines(ListFeatureCollection outputCollection, SimpleFeatureBuilder featureBuilder, SimpleFeature originalFeature, 
			SimpleFeatureType outputSchema,
			double offsetMapUnits, double featureWidthInPixels, double stackOffsetInPixels, int spacingBetweenStackEntries,
			boolean drawOnBothSides, String invertSidesAttributeName, String widthAttributeName) {
		// side inversion
		final double inversionValue = getInversionValue(originalFeature, invertSidesAttributeName);	

		if (drawOnBothSides) {
			final SimpleFeature line1 = this.buildGenericOffsettedLine(originalFeature, featureBuilder, outputSchema, 
					offsetMapUnits * inversionValue, featureWidthInPixels / 2.0, widthAttributeName);
			final SimpleFeature line2 = this.buildGenericOffsettedLine(originalFeature, featureBuilder, outputSchema, 
					offsetMapUnits * inversionValue * -1.0, featureWidthInPixels / 2.0, widthAttributeName);
			
			outputCollection.add(line1);
			outputCollection.add(line2);
			
			stackOffsetInPixels = stackOffsetInPixels + (featureWidthInPixels / 2.0) + spacingBetweenStackEntries;
		} else {
			final SimpleFeature line1 = this.buildGenericOffsettedLine(originalFeature, featureBuilder, outputSchema, 
					offsetMapUnits * inversionValue, featureWidthInPixels, widthAttributeName);
			outputCollection.add(line1);
			
			stackOffsetInPixels = stackOffsetInPixels + featureWidthInPixels + spacingBetweenStackEntries;
		}
		return stackOffsetInPixels;
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
	 * @param baseOffsetMapUnits
	 * @param featureWidthInMapUnits
	 * @param drawOnBothSides
	 * @return
	 */
	protected double calculateOffsetInMapUnits(double baseOffsetMapUnits, double featureWidthInMapUnits, boolean drawOnBothSides) {
		if (drawOnBothSides) {
			return baseOffsetMapUnits + 
					(featureWidthInMapUnits 
							/ 2.0 /* on two sides*/ 
							/ 2.0 /* middle of the line to be drawn */
					);
		} else {
			return baseOffsetMapUnits + 
					(featureWidthInMapUnits 
							/ 2.0 /* middle of the line to be drawn */
					);
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
