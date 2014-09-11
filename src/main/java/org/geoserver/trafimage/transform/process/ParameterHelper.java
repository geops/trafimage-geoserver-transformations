package org.geoserver.trafimage.transform.process;

import java.util.ArrayList;

class ParameterHelper {

	/**
	 * Split a string at a separator and return the unique trimmed entries 
	 * from the string.
	 *  
	 * @param input
	 * @param seperator
	 * @return
	 */
	static public ArrayList<String> splitAt(final String input, final String seperator) {
		final String[] parts = input.split(seperator);
		final ArrayList<String> result = new ArrayList<String>();
		for (final String part: parts) {
			final String partTrimmed = part.trim();
			if (!partTrimmed.equals("")) {
				result.add(partTrimmed);
			}
		}
		return result;
	}

}
