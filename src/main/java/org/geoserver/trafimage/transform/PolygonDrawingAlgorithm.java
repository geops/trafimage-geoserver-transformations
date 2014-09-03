package org.geoserver.trafimage.transform;

import java.util.ArrayList;

import org.geotools.process.ProcessException;
import org.opengis.feature.simple.SimpleFeature;

abstract class PolygonDrawingAlgorithm {

	protected SimpleFeatureAggregator.AggregationStatistics statistics = null;
	
	/**
	 * @deprecated
	 */
	protected String widthAttributeName = null;
	
	protected String offsetAttributeName = null;
	protected int maxPolygonWidth = 10;
	protected String aggCountAttributeName = null;
	private boolean centerOnLine = true;
	
	public PolygonDrawingAlgorithm() {
		
	}

	abstract ArrayList<String> getAdditionalAggregationAttributes();
	
	/**
	 * returns true when polygon should be centered on the line regardless of the offset specified
	 * 
	 * @return
	 */
	public boolean getCenterOnLine() {
		return this.centerOnLine;
	}
	
	abstract public double getPolygonOffset(final SimpleFeature feature);
	
	abstract public double getPolygonWidth(final SimpleFeature feature);
	
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
	
	/**
	 * The name of the integer attribute to read the number of
	 * features in aggregation from
	 * 
	 * @param name
	 */
	public void setAggCountAttributeName(final String name) {
		this.aggCountAttributeName = name;
	}
	
	/**
	 * set the max width of a polygon in pixels
	 * 
	 * @param width
	 */
	public void setMaxPolygonWidth(final int width) {
		this.maxPolygonWidth = width;
	}
	
	/**
	 * the attribute to read the offset of a geometry from.
	 * 
	 * set to null or "" to center polygon on the lines
	 * 
	 * @param name
	 */
	public void setOffsetAttributeName(final String name) {
		this.offsetAttributeName = name;
		if (name == null) {
			this.centerOnLine = true;
		} else {
			if (name.equals("")) {
				this.centerOnLine = true;
			} else {
				this.centerOnLine = false;
			}
		}
	}
	
	/**
	 * the statistics gathered by the aggregator
	 * 
	 * @param statistics
	 */
	public void setStatistics(SimpleFeatureAggregator.AggregationStatistics statistics) {
		this.statistics = statistics;
	}
	
	/**
	 * The name of the integer attribute to read the width of a polygon from
	 * 
	 * @unused
	 * @deprecated
	 * @param name
	 */
	public void setWidthAttributeName(final String name) {
		this.widthAttributeName = name;
	}
}
