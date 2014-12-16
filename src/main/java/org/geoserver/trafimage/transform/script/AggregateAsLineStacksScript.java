/* Copyright (c) 2014 geOps - www.geops.de. All rights reserved.
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.trafimage.transform.script;

public class AggregateAsLineStacksScript extends Script {

	public AggregateAsLineStacksScript(String scriptSource) throws ScriptException {
		super(scriptSource, "AggregateAsLineStacksScript");
	}

	public int getFeatureWidth(double featureLength, int aggCount) throws ScriptException {
		Object[] args = {featureLength, aggCount};
		return callFunctionInteger("getFeatureWith", args);
	}
}
