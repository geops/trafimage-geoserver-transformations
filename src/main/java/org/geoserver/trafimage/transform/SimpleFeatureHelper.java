package org.geoserver.trafimage.transform;

import org.opengis.feature.simple.SimpleFeature;

public class SimpleFeatureHelper {

	/**
	 * get the value of an attribute as boolean
	 * 
	 * @param feature
	 * @param attributeName
	 * @param defaultValue
	 * @return
	 */
	static public boolean getBooleanValue(SimpleFeature feature, String attributeName, boolean defaultValue) {
		if (attributeName != null && !attributeName.equals("")) {
			final Object attributeValue = feature.getAttribute(attributeName);
			if (attributeValue != null) {
				final String attributeStringValue = attributeValue.toString();
				if (attributeStringValue != null && Boolean.parseBoolean(attributeStringValue) 
						|| attributeStringValue.equals("1") 
						|| attributeStringValue.equals("t")
						|| attributeStringValue.equals("T")) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
		return defaultValue;
	}
}
