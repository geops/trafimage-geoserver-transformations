package org.geoserver.trafimage.transform;

import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.process.ProcessException;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;

import com.vividsolutions.jts.geom.LineString;

abstract class VectorLineProcess implements GeoServerProcess {
	
	/**
	 * 
	 * @param inputSchema
	 */
	protected void checkInputGeometryType(final SimpleFeatureType inputSchema) {
		GeometryDescriptor geomDescriptor = inputSchema.getGeometryDescriptor();
		if (geomDescriptor.getType().getBinding() != LineString.class) {
			throw new ProcessException("Inputgeometries are not Linestrings");
		}
	}
	

}
