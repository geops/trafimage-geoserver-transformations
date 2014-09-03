package org.geoserver.trafimage.transform;

import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Iterator;

import net.jpountz.xxhash.StreamingXXHash32;
import net.jpountz.xxhash.XXHashFactory;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBWriter;

class SimpleFeatureHasher {

	private boolean includeGeometry = true;
	private HashSet<String> includedAttributes = new HashSet<String>();
	private XXHashFactory factory = XXHashFactory.fastestInstance();
	private WKBWriter wkbWriter = new WKBWriter();
	
	public SimpleFeatureHasher() {
	}
	
	public void addIncludedAttribute(final String attributeName) {
		this.includedAttributes.add(attributeName);
	}
	
	
	public int getHash(final SimpleFeature feature) throws NoSuchAlgorithmException {
		
		StreamingXXHash32 hash32 = this.factory.newStreamingHash32(0x12af028e);
		if (this.includeGeometry) {
			final Geometry geom = (Geometry) feature.getDefaultGeometry();
			if (geom != null) {
				final byte[] geomBytes = wkbWriter.write(geom);
				hash32.update(geomBytes, 0, geomBytes.length);
			}
		}
		
		final Iterator<String> attributeIt = this.includedAttributes.iterator();
		while (attributeIt.hasNext()) {
			final String attributeName = attributeIt.next();
			final Object value = feature.getAttribute(attributeName);
			if (value != null) {
				final byte[] valueBytes = value.toString().getBytes();
				hash32.update(valueBytes, 0, valueBytes.length);
			}
		}
		return hash32.getValue();
	}
	
	public HashSet<String> getIncludedAttributes() {
		return this.includedAttributes;
	}
	
	public void setIncludedAttributes(final HashSet<String> attributeNames) {
		this.includedAttributes = attributeNames;
	}

	public void setIncludeGeometry(boolean includeGeometry) {
		this.includeGeometry = includeGeometry;
	}

}
