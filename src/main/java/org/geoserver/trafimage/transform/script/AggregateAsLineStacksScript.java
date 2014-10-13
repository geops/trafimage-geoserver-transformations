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
