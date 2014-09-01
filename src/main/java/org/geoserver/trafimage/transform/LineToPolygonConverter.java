package org.geoserver.trafimage.transform;

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

	protected double offset = 0.0;
	protected double width = 10.0; 
	
	protected final OffsetCurveBuilder curveBuilder;
	
	public LineToPolygonConverter() {
		BufferParameters bufferParameters = new BufferParameters();
		bufferParameters.setEndCapStyle(BufferParameters.CAP_FLAT);
		
		this.curveBuilder = new OffsetCurveBuilder(new PrecisionModel(), bufferParameters);
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
	
	
	public Polygon convert(final LineString line) {
		
		final double offset1 = this.offset;
		final double offset2 = this.offset+this.width;
		
		// create the two lines for the sides of the polygon
		final Coordinate[] cLine1 = this.curveBuilder.getLineCurve(line.getCoordinates(), offset1);
		final Coordinate[] cLine2 = this.curveBuilder.getLineCurve(line.getCoordinates(), offset2);

		// use the two lines to build an polygon and close the open ends
		final Coordinate[] cPolygon = new Coordinate[(line.getNumPoints()*2)+1];
		for(int i = 0; i<cLine1.length; i++) {
			cPolygon[i] = cLine1[i];
		}
		for(int i = 0; i<cLine2.length; i++) {
			cPolygon[cLine1.length+i] = cLine2[i];
		}
		cPolygon[cLine1.length+cLine2.length] = cLine1[0]; // close the polygon
		
		GeometryFactory geomFactory = new GeometryFactory(line.getPrecisionModel(), line.getSRID());
		LinearRing linearRing = geomFactory.createLinearRing(cPolygon);
		return geomFactory.createPolygon(linearRing);
	}
}
