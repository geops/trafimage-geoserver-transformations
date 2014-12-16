/* Copyright (c) 2014 geOps - www.geops.de. All rights reserved.
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.trafimage.transform.script;

public class LineStacksScript extends Script {

	public LineStacksScript(String scriptSource) throws ScriptException {
		super(scriptSource, "LineStacksScript");
	}

	public int getFeatureWidth(double featureLength) throws ScriptException {
		Object[] args = {featureLength};
		return callFunctionInteger("getFeatureWith", args);
	}
}
