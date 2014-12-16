/* Copyright (c) 2014 geOps - www.geops.de. All rights reserved.
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.trafimage.transform;

import java.util.logging.Logger;

import org.geotools.process.ProcessException;
import org.geotools.util.logging.Logging;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
import com.vividsolutions.jts.operation.buffer.OffsetCurveBuilder;

public class CurveBuilder {

	private final OffsetCurveBuilder curveBuilder;
	private final GeometryFactory geomFactory = new GeometryFactory(new PrecisionModel());
	
	private static final Logger LOGGER = Logging.getLogger(CurveBuilder.class);
	
	public CurveBuilder() {
		BufferParameters bufferParameters = new BufferParameters();
		bufferParameters.setEndCapStyle(BufferParameters.CAP_FLAT);
		bufferParameters.setJoinStyle(BufferParameters.JOIN_ROUND);

		/*
		 * Sets the number of line segments used to approximate an angle fillet.
		 * 
		 *   If quadSegs >= 1, joins are round, and quadSegs indicates the number of segments to use to approximate a quarter-circle.
    	 *   If quadSegs = 0, joins are bevelled (flat)
         *   If quadSegs < 0, joins are mitred, and the value of qs indicates the mitre ration limit as
         *       mitreLimit = |quadSegs|
         *
         * For round joins, quadSegs determines the maximum error in the approximation to the true buffer curve. 
         * The default value of 8 gives less than 2% max error in the buffer distance. For a max error of < 1%, 
         * use QS = 12. For a max error of < 0.1%, use QS = 18. The error is always less than the buffer distance 
         * (in other words, the computed buffer curve is always inside the true curve). 
		 */
		bufferParameters.setQuadrantSegments(18);
		
		this.curveBuilder = new OffsetCurveBuilder(new PrecisionModel(), bufferParameters);
	}

	/**
	 * 
	 * @param coordinates
	 * @param lineOffset
	 * @return
	 */
	public Coordinate[] buildOffsettedCoordinates(final Coordinate[] coordinates, final double lineOffset) {
		/*
		 * The source for the curvebuilder is at 
		 * http://sourceforge.net/p/jts-topo-suite/code/HEAD/tree/trunk/jts/java/src/com/vividsolutions/jts/operation/buffer/OffsetCurveBuilder.java#l142
		 */
		if (lineOffset == 0.0) {
			// JTS OffsetCurveBuilder will return null in this case
			return coordinates;
		}
		final Coordinate[] result = this.curveBuilder.getOffsetCurve(coordinates, lineOffset);
		if (result == null || (result.length == 0 && coordinates.length != 0)) {
			LineString logLineGeom = this.geomFactory.createLineString(coordinates);
			
			StringBuilder logSB = new StringBuilder()
				.append("Could not build offsetted line ")
				.append("(lineOffset=").append(lineOffset).append(")")
				.append(" for LineString ")
				.append(logLineGeom.toText());
			LOGGER.severe(logSB.toString());
			
			throw new ProcessException("Could not build offsetted line");
		}
		return result;
	}
	
	/**
	 * 
	 * @param linestring
	 * @param lineOffset
	 * @return
	 */
	public LineString buildOffsettedLineString(final LineString linestring, final double lineOffset) {
		final Coordinate[] coordinates = this.buildOffsettedCoordinates(linestring.getCoordinates(), lineOffset);
		final LineString outLineGeom = this.geomFactory.createLineString(coordinates);
		return outLineGeom;
	}
}
