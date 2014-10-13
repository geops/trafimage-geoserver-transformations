package org.geoserver.trafimage.transform.script;

//import java.util.logging.Logger;

//import org.geotools.util.logging.Logging;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;


class Script {
	/**
	 * the execute context
	 * 
	 * according to 
	 * https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino/Scopes_and_Contexts
	 * the context is thread specific and there should be only one context per thread
	 * 
	 */
	private final Context ctx = Context.enter(); 
	
	private final Scriptable scope;
	
	//private static final Logger LOGGER = Logging.getLogger(Script.class);
	
	private final String scriptName;
	
	public Script(String scriptSource, String scriptName) throws ScriptException {
		this.scriptName = scriptName;
		
		scope = ctx.initStandardObjects();
		
		// add a minimal console object for logging
		Object consoleObj = Context.javaToJS(new ConsoleObject(), scope);
		ScriptableObject.putConstProperty(scope, "console", consoleObj);
		
		try {
			ctx.evaluateString(scope, scriptSource, scriptName, 1, null);
		} catch (RhinoException e) {
			throw makeScriptException(e);
		}
	}
	
	/**
	 * call a JS function and return its result as Object
	 * 
	 * @param functionName
	 * @param args
	 * @throws ScriptException
	 * @return
	 */
	private Object callFunction(String functionName, Object[] args) throws ScriptException {
		
		Object functionObj = scope.get(functionName, scope);
		if (!(functionObj instanceof Function)) {
			throw new ScriptException(functionName+ " is undefined or not a function");
		}
		Function func = (Function)functionObj;
		
		try {
			Object result = func.call(ctx, scope, scope, args);
			// LOGGER.info("Call to javascript function "+functionName+" returned "+result.toString());
			return result;
		} catch (RhinoException e) {
			throw makeScriptException(e);
		}
	}
	
	/**
	 * 
	 * @param functionName
	 * @param args
	 * @return
	 * @throws ScriptException
	 */
	protected Integer callFunctionInteger(String functionName, Object[] args) throws ScriptException {
		Object result = callFunction(functionName, args);
		if (result != null) {
			return (int) Math.rint(Double.parseDouble(result.toString()));
		}
		return null;
	}
	
	/**
	 * 
	 * @param functionName
	 * @param args
	 * @return
	 * @throws ScriptException
	 */
	protected String callFunctionString(String functionName, Object[] args) throws ScriptException {
		Object result = callFunction(functionName, args);
		if (result != null) {
			return result.toString();
		}
		return null;
	}
	
	/**
	 * check if a function exists
	 * 
	 * @param functionName
	 * @return
	 */
	protected boolean hasFunction(String functionName) {
		return scope.get(functionName, scope) instanceof Function;
	}
	
	/**
	 * create a ScriptException from a RhinoException
	 * 
	 * @param e
	 * @return
	 */
	private ScriptException makeScriptException(RhinoException e) {
		StringBuilder messageBuilder = new StringBuilder()
			.append("Javascript error in ")
			.append(this.scriptName)
			.append(" on line ")
			.append(e.lineNumber())
			.append(": ")
			.append(e.getMessage());
			//.append("\nStack:\n")
			//.append(e.getScriptStackTrace());
		return new ScriptException(messageBuilder.toString(), e);
	}
	
	/**
	 * register a variable in the global scope of the script
	 * 
	 * NOTE: the variable will be registered AFTER the initial evaluation
	 * of the script.
	 * 
	 * @param variableName
	 * @param variableValue
	 */
	public void registerVariable(String variableName, Object variableValue) {
		scope.put(variableName, scope, variableValue);
	}
	
	/**
	 * 
	 */
	public void terminate() {
		Context.exit();
	}
}
