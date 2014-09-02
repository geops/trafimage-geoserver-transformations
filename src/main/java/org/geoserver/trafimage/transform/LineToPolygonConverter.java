package org.geoserver.trafimage.transform;

import java.util.logging.Logger;

import org.geotools.process.ProcessException;
import org.geotools.util.logging.Logging;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
import com.vividsolutions.jts.operation.buffer.OffsetCurveBuilder;

/**
 * convert lines to polygons
 * 
 * 
 *      	o
 *      	|
 *      	|
 *      	o
 *      	|
 *      	|
 *      	o
 *       	 \
 *        	  \
 *         	   \
 *          	\
 *           	 o
 *           
 *        
 *           
 *      +---o---+
 *      |	|   |
 *      |	|   |
 *      |	o   |
 *      |	|   |
 *      |	|   |
 *      |	o   \
 *       \	 \   \
 *        \	  \   \
 *         \   \   \
 *          \	\  /+
 *           \  -o-
 *            +/
 *             
 * 
 * @author nico
 *
 */
class LineToPolygonConverter {

	private double offset = 0.0;
	private double width = 10.0; 
	
	private final OffsetCurveBuilder curveBuilder;
	
	private static final Logger LOGGER = Logging.getLogger(LineToPolygonConverter.class);
	
	public LineToPolygonConverter() {
		BufferParameters bufferParameters = new BufferParameters();
		//bufferParameters.setEndCapStyle(BufferParameters.CAP_SQUARE);
		//bufferParameters.setJoinStyle(BufferParameters.CAP_FLAT);
		
		bufferParameters.setEndCapStyle(BufferParameters.CAP_FLAT);
		
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
	 * @param line
	 * @param lineOffset
	 * @return
	 */
	private Coordinate[] buildOffsettedLine(final LineString line, final double lineOffset) {
		/*
		 * The source for the curvebuilder is at 
		 * http://sourceforge.net/p/jts-topo-suite/code/HEAD/tree/trunk/jts/java/src/com/vividsolutions/jts/operation/buffer/OffsetCurveBuilder.java#l142
		 */
		if (lineOffset == 0.0) {
			// JTS OffsetCurveBuilder will return null in this case
			return line.getCoordinates();
		}
		final Coordinate[] result = this.curveBuilder.getOffsetCurve(line.getCoordinates(), lineOffset);
		if (result == null) {
			LOGGER.severe("Could not build offsetted line for "+line.toText());
			throw new ProcessException("Could not build offsetted line");
		}
		return result;
	}
	
	/**
	 * 
	 * @param line
	 * @return
	 */
	public Polygon convert(final LineString line) {
		
		final double offset1 = this.offset;
		// draw the width in the other direction in case of a negative offset
		final double directionOfWidth = Math.abs(this.offset)/this.offset;
		final double offset2 = this.offset + (this.width * directionOfWidth);
		
		// create the two lines for the sides of the polygon
		final Coordinate[] cLine1 = this.buildOffsettedLine(line, offset1);
		final Coordinate[] cLine2 = this.buildOffsettedLine(line, offset2);

		// use the two lines to build an polygon and close the open ends
		final Coordinate[] cPolygon = new Coordinate[cLine1.length+cLine2.length+1];
		for(int i = 0; i<cLine1.length; i++) {
			cPolygon[i] = cLine1[i];
		}
		for(int i = (cLine2.length-1); i>=0; i--) {
			cPolygon[cLine1.length+(cLine2.length-1-i)] = cLine2[i];
		}
		// close the polygon
		cPolygon[cLine1.length+cLine2.length] = cLine1[0];
		
		GeometryFactory geomFactory = new GeometryFactory(line.getPrecisionModel(), line.getSRID());
		LinearRing linearRing = geomFactory.createLinearRing(cPolygon);
		Polygon polygon = geomFactory.createPolygon(linearRing);
		return polygon;
		
	}
	
	/**
	 * set the offset the polygon has to the line it originates from
	 * 
	 * use negative/positive values to place the polygon to the right\left of the line
	 * 
	 * @param offset Offset in map units
	 */
	public void setOffset(final double offset) {
		this.offset = offset;
	}
	
	
	/**
	 * set the width of the resulting polygon
	 * @param width width in map units
	 */
	public void setWidth(final double width) {
		this.width = width;
	}
}
