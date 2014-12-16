/* Copyright (c) 2014 geOps - www.geops.de. All rights reserved.
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.trafimage.transform;

import java.util.Comparator;
import java.util.logging.Logger;

import org.geoserver.trafimage.transform.process.LineStacksProcess;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;


/**
 * compares features by an integer attribute
 * 
 * @author nico
 *
 */
public class FeatureOrderComparator implements Comparator<SimpleFeature> {

	private static final Logger LOGGER = Logging.getLogger(FeatureOrderComparator.class);
	
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
		if (value instanceof Integer) {
			return (Integer)value;
		}
		throw new IllegalArgumentException("Can not compare values of attributes of the type "+value.getClass().getCanonicalName());
	}
	
	public void setOrderAttributeName(final String orderAttributeName) {
		this.orderAttributeName = orderAttributeName;
	}
}