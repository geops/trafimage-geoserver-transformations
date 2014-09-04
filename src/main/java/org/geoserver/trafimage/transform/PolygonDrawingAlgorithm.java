package org.geoserver.trafimage.transform;

import java.util.ArrayList;

import org.geotools.process.ProcessException;
import org.opengis.feature.simple.SimpleFeature;

abstract class PolygonDrawingAlgorithm {

	/**
	 * @deprecated
	 */
	protected String widthAttributeName = null;
	
	protected String offsetAttributeName = null;
	protected int maxPolygonWidth = 10;
	protected String aggCountAttributeName = null;
	private boolean centerOnLine = true;
	protected int maxPolygonWidthFeatureCount = 10;
	
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
	 * set the number of features which have to be at least in an aggregation for
	 * the polygon to be drawn with the full width of maxPolygonWidth
	 * 
	 * @param maxPolygonWidthFeatureCount
	 */
	public void setMaxPolygonWidthFeatureCount(int maxPolygonWidthFeatureCount) {
		this.maxPolygonWidthFeatureCount = maxPolygonWidthFeatureCount;
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
