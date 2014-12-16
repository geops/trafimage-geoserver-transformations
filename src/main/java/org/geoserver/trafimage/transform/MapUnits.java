/* Copyright (c) 2014 geOps - www.geops.de. All rights reserved.
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/**
 * Reading material:
 *   http://wiki.deegree.org/deegreeWiki/HowToUseScaleHintAndScaleDenominator#What_is_the_ScaleHint.3F
 *   
 * Geoserver also offers a wms_scale_denominator SLD parameter
 */

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
	public static double pixelDistanceToMapUnits(final ReferencedEnvelope bbox, final int imageWidth, final int imageHeight, final double pixelToConvert) {
		
		final double pixelPerMuX =  bbox.getWidth() / (double) imageWidth;
		final double pixelPerMuY =  bbox.getHeight() / (double) imageHeight;
		
		// use the average of the two ratios in case a skewed WMS image is requested
		return ((pixelPerMuX + pixelPerMuY) / 2.0) * pixelToConvert;
	}

}