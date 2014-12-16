/* Copyright (c) 2014 geOps - www.geops.de. All rights reserved.
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.trafimage.transform;

import java.util.ArrayList;

import org.opengis.feature.simple.SimpleFeature;

public class LogarithmicPolygonDrawingAlgorithm extends
		PolygonDrawingAlgorithm {

	public LogarithmicPolygonDrawingAlgorithm() {
	}

	@Override
	public ArrayList<String> getAdditionalAggregationAttributes() {
		ArrayList<String> attrs = new ArrayList<String>();
		attrs.add(this.offsetAttributeName);
		return attrs;
	}

	@Override
	public double getPolygonOffset(final SimpleFeature feature) {
		if (this.getCenterOnLine()) {
			return this.getPolygonWidth(feature) / -2.0;
		}
		return (double) this.parseAttributeToInteger(feature, this.offsetAttributeName, 0);
	}

	@Override
	public double getPolygonWidth(final SimpleFeature feature) {
		final int aggCount = this.parseAttributeToInteger(feature, this.aggCountAttributeName, 1);

		final double scale = Math.log((double) Math.min(aggCount, this.maxPolygonWidthFeatureCount) + 1.0) 
				/ Math.log((double) this.maxPolygonWidthFeatureCount + 1.0);
		final double width = (double) this.maxPolygonWidth * scale;
		return width;
	}

}
