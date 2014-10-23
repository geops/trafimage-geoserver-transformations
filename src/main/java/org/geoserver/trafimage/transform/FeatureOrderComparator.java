package org.geoserver.trafimage.transform;

import java.util.Comparator;

import org.opengis.feature.simple.SimpleFeature;


public class FeatureOrderComparator implements Comparator<SimpleFeature> {

	private String orderAttributeName;
	
	public int compare(SimpleFeature f0, SimpleFeature f1) {
		int orderF0 = this.getOrderValue(f0);
		int orderF1 = this.getOrderValue(f1);
		return orderF0 - orderF1;
	}
	
	private int getOrderValue(final SimpleFeature feature) {
		if (this.orderAttributeName == null || this.orderAttributeName.equals("")) {
			return 0;
		}
		Object value = feature.getAttribute(this.orderAttributeName);
		if (value == null) {
			return 0;
		}
		int orderValue = Integer.parseInt(value.toString());
		return orderValue;
	}
	
	public void setOrderAttributeName(final String orderAttributeName) {
		this.orderAttributeName = orderAttributeName;
	}
}