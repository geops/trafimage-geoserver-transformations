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
		bufferParameters.setEndCapStyle(BufferParameters.CAP_SQUARE);
		bufferParameters.setJoinStyle(BufferParameters.CAP_FLAT);
		bufferParameters.setQuadrantSegments(0);
		
		this.curveBuilder = new OffsetCurveBuilder(new PrecisionModel(), bufferParameters);
	}

	/**
	 * 
	 * @param line
	 * @param lineOffset
	 * @return
	 */
	private Coordinate[] buildOffsettedLine(final LineString line, final double lineOffset) {
		// source for the curvebuilder is at 
		// http://sourceforge.net/p/jts-topo-suite/code/HEAD/tree/trunk/jts/java/src/com/vividsolutions/jts/operation/buffer/OffsetCurveBuilder.java#l142
		if (lineOffset == 0.0) {
			// JTS will return null in this case
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
		final double offset2 = this.offset+this.width;
		
		// create the two lines for the sides of the polygon
		final Coordinate[] cLine1 = this.buildOffsettedLine(line, offset1); // TODO
		final Coordinate[] cLine2 = this.buildOffsettedLine(line, offset2); // TODO

		// use the two lines to build an polygon and close the open ends
		final Coordinate[] cPolygon = new Coordinate[cLine1.length+cLine2.length+1];
		for(int i = 0; i<cLine1.length; i++) {
			cPolygon[i] = cLine1[i];
		}
		for(int i = (cLine2.length-1); i>=0; i--) {
			cPolygon[cLine1.length+(cLine2.length-1-i)] = cLine2[i];
		}
		cPolygon[cLine1.length+cLine2.length] = cLine1[0]; // close the polygon
		
		
		GeometryFactory geomFactory = new GeometryFactory(line.getPrecisionModel(), line.getSRID());
		LinearRing linearRing = geomFactory.createLinearRing(cPolygon);
		String lrText = linearRing.toText();
		Polygon polygon = geomFactory.createPolygon(linearRing);
		String plyText = polygon.toText();
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
