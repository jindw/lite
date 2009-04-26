package org.xidea.lite.plugin;

import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.w3c.dom.Node;
import org.xidea.lite.parser.InstructionParser;
import org.xidea.lite.parser.Parser;

public class RhinoContext {
	public static final Log log = LogFactory.getLog(RhinoContext.class);
	private PluginLoader initializer;
	private Context context;
	private ScriptableObject scope;
	private static String INITIALIZE_SCRIPT;
	static {
		try {
			INITIALIZE_SCRIPT = PluginLoader.loadText(new InputStreamReader(
					PluginLoader.class.getResourceAsStream("PluginLoader.js"),
					"utf-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

	}

	public RhinoContext(PluginLoader initializer) {
		this.initializer = initializer;
		context = Context.enter();
		scope = ScriptRuntime.getGlobal(context);
		context.evaluateString(scope, INITIALIZE_SCRIPT, "data:", 1, null);

	}

	public void setUp(String source) {
		scope.put("context", scope, this);
		scope.put("Node", scope,new NativeJavaClass(scope,Node.class));
		context.evaluateString(scope, source, "data:" + source, 1, null);
	}

	@SuppressWarnings("unchecked")
	public void doImport(String path) {
		try {

				Class clazz = Class.forName(path);
			scope.put(path.substring(path.lastIndexOf('.') + 1), scope,
					new NativeJavaClass(scope,clazz));
			
		} catch (Exception e) {
			log.error(e);
		}

	}

	public void addInstructionParser(Object iparser) {
		InstructionParser parser;
		if (iparser instanceof Scriptable) {
			Function findStart = (Function) RhinoContext.getProperty(
					(Scriptable) iparser, "findStart");
			Function parse = (Function) RhinoContext.getProperty(
					(Scriptable) iparser, "parse");
			parser = new RhinoInstructionParserProxy(context,
					(Scriptable) iparser, findStart, parse);
		} else {
			parser = (InstructionParser) iparser;
		}
		initializer.addInstructionParser(parser);
	}

	public void addNodeParser(Object parser) {
		this.addNodeParser(parser, Node.ELEMENT_NODE);
	}

	@SuppressWarnings("unchecked")
	public void addNodeParser(Object parser, int type) {
		Parser<Node> nodeParser;
		if (parser instanceof Scriptable) {
			Function parse = (Function) RhinoContext.getProperty(
					(Scriptable) parser, "parse");
			nodeParser = new RhinoParserProxy(context, (Scriptable) parser,
					parse, type);
		} else {
			nodeParser = (Parser<Node>) parser;
		}
		initializer.addNodeParser(nodeParser);
	}

	public static Object call(final Scriptable thisObj,
			final Function memberFn, final Object[] args) {
		if (memberFn == null) {
			return Undefined.instance;
		}
		final Scriptable scope = memberFn.getParentScope();
		return Context.call(ContextFactory.getGlobal(), memberFn, scope,
				thisObj, args);
	}

	public static Object getProperty(Scriptable base, String functionName) {
		Object x = ScriptableObject.getProperty(base, functionName);
		if (x == Scriptable.NOT_FOUND) {
			log.error("method not found");
			return null;
		}
		return x;
	}
}