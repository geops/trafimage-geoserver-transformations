package org.geoserver.trafimage.transform;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.process.ProcessException;
import org.geotools.util.logging.Logging;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
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
	protected boolean centerOnLine = true;
	
	private final OffsetCurveBuilder curveBuilder;
	
	private static final Logger LOGGER = Logging.getLogger(LineToPolygonConverter.class);
	
	public LineToPolygonConverter() {
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
	 * @param line
	 * @param lineOffset
	 * @return
	 */
	private Coordinate[] buildOffsettedLine(final Coordinate[] coordinates, final double lineOffset) {
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
			GeometryFactory geomFactory = new GeometryFactory(new PrecisionModel());
			LineString logLineGeom = geomFactory.createLineString(coordinates);
			
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
	 * @param line
	 * @return
	 */
	public Polygon convert(final LineString line) {

		double offsetLine1;
		double offsetLine2;
		if (this.centerOnLine) {
			// draw the polygon centered on the line. just ignoring the offset here.
			offsetLine1 = this.width / -2.0;
			offsetLine2 = this.width / 2.0;
		} else {
			// draw the polygon besides the line, offsetted by this.offset, with the width
			// facing away from the line
			
			// draw the width in the other direction in case of a negative offset
			final double directionOfWidth = Math.abs(this.offset)/this.offset;
			offsetLine1 = this.offset;
			offsetLine2 = this.offset + (this.width * directionOfWidth);
		}
		
		if (LOGGER.getLevel() == Level.FINE) {
			StringBuilder logSB = new StringBuilder()
				.append("Drawing polygon with")
				.append(" width=").append(this.width)
				.append(" and centerOnLine=").append(Boolean.toString(this.centerOnLine))
				.append(" using offsetLine1=").append(offsetLine1)
				.append(" and offsetLine2=").append(offsetLine2);
			LOGGER.fine(logSB.toString());
		}
		
		// create the two lines for the sides of the polygon
		// NOTE: the first line may also be used as a base to generate the second line from. This
		//       approach will lead to larger artifacts as small errors in rounded corners or line endings
		//       in the first line will multiply in the second line.
		final Coordinate[] cLine1 = this.buildOffsettedLine(line.getCoordinates(), offsetLine1);
		final Coordinate[] cLine2 = this.buildOffsettedLine(line.getCoordinates(), offsetLine2);

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
		LinearRing linearRing = geomFactory.createLinearRing(cutJoinLoops(cPolygon));
		Polygon polygon = geomFactory.createPolygon(linearRing);
		return polygon;
	}
	
	/**
	 * cut off loops JTS generates when creating the offsetted curve
	 * 
	 * this is a quite expensive method and will also cut loops which originate in the 
	 * line itself.
	 * 
	 * @param coordinates
	 * @return
	 */
	private Coordinate[] cutJoinLoops(final Coordinate[] coordinates) {
		if (coordinates.length < 3) {
			return coordinates;
		}
		GeometryFactory geomFactory = new GeometryFactory(new PrecisionModel());
		LineString[] segments = new LineString[coordinates.length-1];
		for (int i=0; i<(coordinates.length-2); i++) {
			final Coordinate[] segmentCoords = new Coordinate[2];
			segmentCoords[0] = coordinates[i];
			segmentCoords[1] = coordinates[i+1];
			segments[i] = geomFactory.createLineString(segmentCoords);
		}
		
		ArrayList<Coordinate> noloopCoords = new ArrayList<Coordinate>();
		noloopCoords.add(segments[0].getCoordinateN(0));
		for (int i=0; i<(segments.length-1); i++) {
			boolean foundIntersection = false;
			
			for (int j=i+1; j<(segments.length-1); j++) {
				final Geometry segmentsIntersection = (Geometry) segments[i].intersection(segments[j]);
				if (segmentsIntersection != null) {
					final Coordinate[] intersectionCoords = segmentsIntersection.getCoordinates();
					if (intersectionCoords.length != 0) {
						if (intersectionCoords[0] != segments[i].getCoordinateN(1)) {
							foundIntersection = true;
							i = j;
							noloopCoords.add(intersectionCoords[0]);
							break;
						}
					}
				}
			}
			if (!foundIntersection) {
				noloopCoords.add(segments[i].getCoordinateN(1));
			}
		}
		noloopCoords.add(coordinates[coordinates.length-1]); // always add the last coordinate of a line
		
		Coordinate[] noloopCoordsA = new Coordinate[noloopCoords.size()];
		return noloopCoords.toArray(noloopCoordsA);
	}
	
	/**
	 * 
	 * @param centerOnLine
	 */
	public void setCenterOnLine(boolean centerOnLine) {
		this.centerOnLine = centerOnLine;
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
