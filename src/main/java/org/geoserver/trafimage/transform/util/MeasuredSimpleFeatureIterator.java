package org.geoserver.trafimage.transform.util;

import java.util.NoSuchElementException;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

/**
 * measures the time spend on iterations
 * 
 * @author nico
 *
 */
public class MeasuredSimpleFeatureIterator extends MeasuredTime implements SimpleFeatureIterator {

	private final SimpleFeatureIterator internalIterator;

	public MeasuredSimpleFeatureIterator(final SimpleFeatureIterator internalIterator) {
		this.internalIterator = internalIterator;
	}

	public void close() {
		this.startMeasuring();
		try {
			internalIterator.close();
		} finally {
			this.stopMeasuring();
		}
	}
	
	public boolean hasNext() {
		this.startMeasuring();
		try {
			return internalIterator.hasNext();
		} finally {
			this.stopMeasuring();
		}
	}

	public SimpleFeature next() throws NoSuchElementException {
		this.startMeasuring();
		try {
			return internalIterator.next();
		} finally {
			this.stopMeasuring();
		}
	}

}
