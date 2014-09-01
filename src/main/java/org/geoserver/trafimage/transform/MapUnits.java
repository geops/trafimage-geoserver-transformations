package org.geoserver.trafimage.transform;

import org.geotools.geometry.jts.ReferencedEnvelope;

public class MapUnits {

	/**
	 * 
	 * @param bbox	Envelope of the requested image
	 * @param imageWidth Width of the ReferencedEnvelope
	 * @param imageHeight  Height of the requested image
	 * @param pixelToConvert The number of pixel to convert to map units
	 * @return
	 */
	static double pixelToMapUnits(final ReferencedEnvelope bbox, final int imageWidth, final int imageHeight, final int pixelToConvert) {
		
		final double pixelPerMuX = imageWidth / (bbox.getMaxX() - bbox.getMinX());
		final double pixelPerMuY = imageHeight / (bbox.getMaxY() - bbox.getMinY());
		
		// use the average of the two ratios
		return ((pixelPerMuX + pixelPerMuY) / 2) * pixelToConvert;
	}

}