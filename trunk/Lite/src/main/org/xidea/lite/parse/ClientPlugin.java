package org.xidea.lite.parse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.impl.ParseUtil;

public class ClientPlugin implements OptimizePlugin, OptimizeScope {

	private String name;
	private List<String> params;
	private List<String> defaults;

	private List<Object> children;
	private OptimizeContext context;
	private OptimizeScope scopeInfo;
	private Set<String> optimizedCall = null;
	private boolean first;

	@SuppressWarnings("unchecked")
	public void initialize(Map<String, Object> config, List<Object> children,
			OptimizeContext context) {
		this.children = children;
		this.name = (String) config.get("name");
		this.params =(List<String>) config.get("params");
		this.defaults = (List<String>) config.get("defaults");
		this.context = context;
		this.scopeInfo = context.parseScope(children, params);
	}
	public void optimize() {
		if(this.optimizedCall == null){
			optimizeAll(context);
		}
		ArrayList<Object> result = new ArrayList<Object>();
		for(String n : optimizedCall){
			result.add(context.getDefCode(n));
		}
		result.addAll(children);
		StringBuilder code = new StringBuilder("$import('org.xidea.lite.impl.js:JSTranslator');");
		code.append("(function(code,name,params,defaults){");
		code.append("	var t = new JSTranslator(name,params,defaults);");
		code.append("	t.liteImpl = ");
		code.append(this.first?"null":"'liteImpl'");
		code.append(";");
		code.append("	return t.translate(code);");
		code.append("})(");
		code.append(JSONEncoder.encode(result));
		code.append(',');
		code.append(JSONEncoder.encode(this.name));
		code.append(',');
		code.append(JSONEncoder.encode(this.params));
		code.append(',');
		code.append(JSONEncoder.encode(this.defaults));
		code.append(")");
		String script = (String) ParseUtil
				.getJSIRuntime()
				.eval(code.toString());
		this.children.clear();
		this.children.add(script);
	}
	//private static Pattern CAPTURE_REPLACER = Pattern.compile("[\\u0009].");

	private static void optimizeAll(final OptimizeContext context) {
		Map<String, Set<String>> callMap = new HashMap<String, Set<String>>(
				context.getDefCallMap());
		
		// List<String> call = scopeInfo.getCallList();
		final ArrayList<ClientPlugin> pluginList = new ArrayList<ClientPlugin>();
		final ArrayList<String> positionList = new ArrayList<String>();
		final Map<String, Set<String>> namedClientCallMap = new HashMap<String, Set<String>>();
		context.walk(new OptimizeWalker() {
			public int visit(List<Object> parent, int index, String post32) {
				@SuppressWarnings("unchecked")
				OptimizePlugin p = context.getPlugin((List<Object>) parent.get(index));
				if (p instanceof ClientPlugin) {
					ClientPlugin plugin = (ClientPlugin) p;
					positionList.add(post32);//CAPTURE_REPLACER.matcher(post32).replaceAll(""));
					pluginList.add(plugin);
					if (plugin.name != null && plugin.name.length() > 0) {
						namedClientCallMap.put(plugin.name, getCall(plugin));
					}
				}
				return index;
			}
		});
		callMap.putAll(namedClientCallMap);
		for (int i = 0, end = positionList.size(); i < end; i++) {
			ClientPlugin plugin = pluginList.get(i);
			String position = positionList.get(i);
			HashSet<String> optimizedCall = new HashSet<String>(getCall(plugin));
			context.optimizeCallClosure(callMap, optimizedCall);
			optimizedCall.removeAll(namedClientCallMap.keySet());
			boolean isFirst = true;
			for (int j = 0; j < i; j++) {
				if (position.startsWith(positionList.get(j))) {
					optimizedCall.removeAll(pluginList.get(j).optimizedCall);
					isFirst = false;
				}
			}
			plugin.first = isFirst;
			plugin.optimizedCall = optimizedCall;
		}
	}

	private static Set<String> getCall(OptimizeScope si) {
		HashSet<String> cs = new HashSet<String>(si.getCalls());
		//if (cs.contains("*")) {
		cs.addAll(si.getExternalRefs());
		//}
		cs.removeAll(si.getParams());
		cs.removeAll(si.getVars());
		cs.remove("*");
		return cs;
	}

	public List<String> getCalls() {
		return scopeInfo.getCalls();
	}

	public List<String> getExternalRefs() {
		return scopeInfo.getExternalRefs();
	}

	public List<String> getRefs() {
		return scopeInfo.getRefs();
	}

	public List<String> getVars() {
		return scopeInfo.getVars();
	}
	public List<String> getParams() {
		return scopeInfo.getParams();
	}
	public void before() {
	}
	public List<String> getDefs() {
		return new ArrayList<String>(context.getDefCallMap().keySet());
	}

}
