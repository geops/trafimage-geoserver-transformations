package org.geoserver.trafimage.transform.script;

import java.util.logging.Logger;

import org.geotools.util.logging.Logging;

public class ConsoleObject {
	private static final Logger LOGGER = Logging.getLogger(ConsoleObject.class);
	
	public void log(String message) {
		LOGGER.info(message);
	}
	
	public void error(String message) {
		LOGGER.severe(message);
	}
}
