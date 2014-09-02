package org.geoserver.trafimage.transform;

import java.util.ArrayList;

import org.opengis.feature.simple.SimpleFeature;

public class LogarithmicPolygonDrawingConfiguration extends
		PolygonDrawingConfiguration {

	public LogarithmicPolygonDrawingConfiguration() {
		// TODO Auto-generated constructor stub
	}

	@Override
	ArrayList<String> getAdditionalAggregationAttributes() {
		ArrayList<String> attrs = new ArrayList<String>();
		attrs.add(this.offsetAttributeName);
		return attrs;
	}

	@Override
	public double getPolygonOffset(final SimpleFeature feature) {
		return (double) this.parseAttributeToInteger(feature, this.offsetAttributeName, 0);
	}

	@Override
	public double getPolygonWidth(final SimpleFeature feature) {
		final int aggCount = this.parseAttributeToInteger(feature, this.aggCountAttributeName, 1);

		final double scale = Math.log((double)aggCount + 1.0) / Math.log((double) this.statistics.numMaxEntriesInAggregate + 1.0);
		final double width = (double) this.maxPolygonWidth * scale;
		return width;
	}

}
