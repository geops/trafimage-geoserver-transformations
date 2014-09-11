package org.geoserver.trafimage.transform.process;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.process.ProcessException;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;


abstract class VectorProcess implements GeoServerProcess {
	
	/**
	 * 
	 * @param inputSchema
	 * @param validBinding  the valid JTS geometry class. for example com.vividsolutions.jts.geom.LineString
	 */
	protected void assertInputGeometryType(final SimpleFeatureType inputSchema, Class<?> validBinding) {
		ArrayList<Class<?>> validBindings = new ArrayList<Class<?>>();
		validBindings.add(validBinding);
		assertInputGeometryType(inputSchema, validBindings);
	}

	/**
	 * 
	 * @param inputSchema
	 * @param validBindings the valid JTS geometry classes. for example com.vividsolutions.jts.geom.LineString
	 */
	protected void assertInputGeometryType(final SimpleFeatureType inputSchema, List<Class<?>> validBindings) {
		final GeometryDescriptor geomDescriptor = inputSchema.getGeometryDescriptor();
		final Class<?> geomBinding = geomDescriptor.getType().getBinding();
		
		boolean isValid = false;
		for(Class<?> validBinding: validBindings) {
			if (validBinding == geomBinding) {
				isValid = true;
				break;
			}
		}
		if (!isValid) {
			StringBuilder messageBuilder = new StringBuilder()
				.append("Inputgeometries are not of one of these valid types: ");
			for(int i=0; i<validBindings.size(); i++) {
				if (i>0) {
					messageBuilder.append(", ");
				}
				messageBuilder.append(validBindings.get(i).getName());
			}
			throw new ProcessException(messageBuilder.toString());
		}
	}

}
