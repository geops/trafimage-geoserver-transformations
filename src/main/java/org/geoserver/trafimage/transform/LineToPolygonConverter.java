package org.geoserver.trafimage.transform;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.trafimage.transform.util.MeasuredTime;
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
class LineToPolygonConverter extends MeasuredTime {

	private double offset = 0.0;
	private double width = 10.0; 
	protected boolean centerOnLine = true;
	private boolean enableArtifactRemoval = false; 
	
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
		this.startMeasuring();
		try {
			double[] offsets = this.getOffsets();
			
			if (LOGGER.getLevel() == Level.FINE) {
				StringBuilder logSB = new StringBuilder()
					.append("Drawing polygon with")
					.append(" width=").append(this.width)
					.append(", centerOnLine=").append(Boolean.toString(this.centerOnLine))
					.append(" and enableArtifactRemoval=").append(Boolean.toString(this.enableArtifactRemoval))
					.append(" using offsets[0]=").append(offsets[0])
					.append(" and offsets[1]=").append(offsets[1]);
				LOGGER.fine(logSB.toString());
			}
			
			// create the two lines for the sides of the polygon
			// NOTE: the first line may also be used as a base to generate the second line from. This
			//       approach will lead to larger artifacts as small errors in rounded corners or line endings
			//       in the first line will multiply in the second line.
			final Coordinate[] cLine0 = this.buildOffsettedLine(line.getCoordinates(), offsets[0]);
			final Coordinate[] cLine1 = this.buildOffsettedLine(line.getCoordinates(), offsets[1]);
	
			// use the two lines to build an polygon and close the open ends
			final Coordinate[] cPolygon = new Coordinate[cLine0.length+cLine1.length+1];
			for(int i = 0; i<cLine0.length; i++) {
				cPolygon[i] = cLine0[i];
			}
			for(int i = (cLine1.length-1); i>=0; i--) {
				cPolygon[cLine0.length+(cLine1.length-1-i)] = cLine1[i];
			}
			// close the polygon
			cPolygon[cLine0.length+cLine1.length] = cLine0[0];
			
			final GeometryFactory geomFactory = new GeometryFactory(line.getPrecisionModel(), line.getSRID());
			
			final LinearRing linearRing;
			if (this.enableArtifactRemoval) {
				linearRing = geomFactory.createLinearRing(cutJoinLoops(cPolygon));
			} else {
				linearRing = geomFactory.createLinearRing(cPolygon);
			}
			final Polygon polygon = geomFactory.createPolygon(linearRing);
			return polygon;
		
		} finally {
			this.stopMeasuring();
		}
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
	 * line offsets for the left and right side of the polygon
	 * 
	 * @return double[2]
	 */
	private double[] getOffsets() {
		final double[] offsets = {0, 0};
		if (this.centerOnLine) {
			// draw the polygon centered on the line. just ignoring the offset here.
			offsets[0] = this.width / -2.0;
			offsets[1] = this.width / 2.0;
		} else {
			// draw the polygon besides the line, offsetted by this.offset, with the width
			// facing away from the line
			
			// draw the width in the other direction in case of a negative offset
			final double directionOfWidth = Math.abs(this.offset)/this.offset;
			offsets[0] = this.offset;
			offsets[1] = this.offset + (this.width * directionOfWidth);
		}	
		return offsets;
	}
	
	/**
	 * 
	 * @param centerOnLine
	 */
	public void setCenterOnLine(boolean centerOnLine) {
		this.centerOnLine = centerOnLine;
	}
	
	/**
	 * Attempt to remove rendering artifacts in polygons.
	 * 
	 * This is a very expensive operation and will only run in a acceptable time when there are just a few features.
	 * 
	 * @param enableArtifactRemoval
	 */
	public void setEnableArtifactRemoval(final boolean enableArtifactRemoval) {
		this.enableArtifactRemoval = enableArtifactRemoval;
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
