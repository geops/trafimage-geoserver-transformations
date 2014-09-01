package org.geoserver.trafimage.transform;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Iterator;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

class SimpleFeatureHasher {

	private boolean includeGeometry = true;
	private HashSet<String> includedAttributes = new HashSet<String>();
	
	public SimpleFeatureHasher() {
		// TODO Auto-generated constructor stub
	}
	
	public void addIncludedAttribute(final String attributeName) {
		this.includedAttributes.add(attributeName);
	}
	
	private String byteArrayToHexString(final byte[] b) {
		String result = "";
		for (int i=0; i < b.length; i++) {
			result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
		}
		return result;
	}
	
	public String getHash(final SimpleFeature feature) throws NoSuchAlgorithmException {
		final MessageDigest md = MessageDigest.getInstance("SHA-1");
		if (this.includeGeometry) {
			final Geometry geom = (Geometry) feature.getDefaultGeometry();
			if (geom != null) {
				md.update(geom.toText().getBytes());
			}
		}
		
		final Iterator<String> attributeIt = this.includedAttributes.iterator();
		while (attributeIt.hasNext()) {
			final String attributeName = attributeIt.next();
			final Object value = feature.getAttribute(attributeName);
			if (value != null) {
				md.update(value.toString().getBytes());
			}
		}
		
		return byteArrayToHexString(md.digest());
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
