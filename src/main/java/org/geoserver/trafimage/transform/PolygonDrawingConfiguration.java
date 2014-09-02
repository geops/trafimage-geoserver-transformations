package org.geoserver.trafimage.transform;

import java.util.ArrayList;

import org.geotools.process.ProcessException;
import org.opengis.feature.simple.SimpleFeature;

abstract class PolygonDrawingConfiguration {

	protected SimpleFeatureAggregator.AggregationStatistics statistics = null;
	protected String widthAttributeName = null;
	protected String offsetAttributeName = null;
	protected int maxPolygonWidth = 10;
	protected String aggCountAttributeName = null;
	
	public PolygonDrawingConfiguration() {
		
	}

	abstract ArrayList<String> getAdditionalAggregationAttributes();
	
	abstract public double getPolygonOffset(final SimpleFeature feature);
	abstract public double getPolygonWidth(final SimpleFeature feature);
	
	public void setOffsetAttributeName(final String name) {
		this.offsetAttributeName = name;
	}
	
	public void setStatistics(SimpleFeatureAggregator.AggregationStatistics statistics) {
		this.statistics = statistics;
	}
	
	public void setWidthAttributeName(final String name) {
		this.widthAttributeName = name;
	}
	
	public void setMaxPolygonWidth(final int width) {
		this.maxPolygonWidth = width;
	}
	
	public void setAggCountAttributeName(final String name) {
		this.aggCountAttributeName = name;
	}
	
	/**
	 * 
	 * @param feature
	 * @param attributeName
	 * @return
	 */
	protected int parseAttributeToInteger(final SimpleFeature feature, final String attributeName, final int defaultValue) {
		if (attributeName == null) {
			return defaultValue;
		}
		final Object attribute = feature.getAttribute(attributeName);
		int value = defaultValue;
		if (attribute != null) {
			try {
				value = Integer.parseInt(attribute.toString());
			} catch(NumberFormatException e) {
				throw new ProcessException("Illegal value for attribute "+attributeName+": <"+attribute.toString()+">");
			}
		}
		return value;
	}
}
