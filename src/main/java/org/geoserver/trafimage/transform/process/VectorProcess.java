/* Copyright (c) 2014 geOps - www.geops.de. All rights reserved.
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
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
				.append("Inputgeometries are of type <")
				.append(geomBinding.getName())
				.append("> and not of one of these valid types: ");
			for(int i=0; i<validBindings.size(); i++) {
				if (i>0) {
					messageBuilder.append(", ");
				}
				messageBuilder
					.append("<")
					.append(validBindings.get(i).getName())
					.append(">");
			}
			throw new ProcessException(messageBuilder.toString());
		}
	}

}
