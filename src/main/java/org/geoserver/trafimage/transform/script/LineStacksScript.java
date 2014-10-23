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
