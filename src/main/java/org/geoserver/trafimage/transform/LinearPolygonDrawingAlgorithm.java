package org.geoserver.trafimage.transform;

import java.util.ArrayList;

import org.opengis.feature.simple.SimpleFeature;

public class LinearPolygonDrawingAlgorithm extends PolygonDrawingAlgorithm {

	public LinearPolygonDrawingAlgorithm() {
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
		final double width = (double) this.maxPolygonWidth 
				* Math.min(1.0, (double) aggCount / (double) this.maxPolygonWidthFeatureCount);
		return width;
	}

}
