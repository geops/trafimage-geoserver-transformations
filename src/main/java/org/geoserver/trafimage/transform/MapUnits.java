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
	static double pixelDistanceToMapUnits(final ReferencedEnvelope bbox, final int imageWidth, final int imageHeight, final double pixelToConvert) {
		
		final double pixelPerMuX =  bbox.getWidth() / (double) imageWidth;
		final double pixelPerMuY =  bbox.getHeight() / (double) imageHeight;
		
		// use the average of the two ratios in case a skewed WMS image is requested
		return ((pixelPerMuX + pixelPerMuY) / 2) * pixelToConvert;
	}

}