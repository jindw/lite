package org.xidea.lite.parse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.impl.ParseUtil;

public class ClientPlugin implements  OptimizeScope {

	private String name;
	private List<String> params;
	private List<String> defaults;

	private List<Object> children;
	private OptimizeContext context;
	private OptimizeScope scopeInfo;
	private Set<String> optimizedCall = null;

	@SuppressWarnings("unchecked")
	public void initialize(Map<String, Object> config, List<Object> children,
			OptimizeContext context) {
		this.children = children;
		this.name = (String) config.get("name");
		this.params = (List<String>) config.get("params");
		this.defaults = (List<String>) config.get("defaults");
		this.context = context;
		this.scopeInfo = context.parseScope(children, params);
	}
	public void optimize() {
		if(this.optimizedCall == null){
			optimizeAll(context);
		}
		StringBuilder code = new StringBuilder("$import('org.xidea.lite.impl.js:JSTranslator');");
		code.append("new JSTranslator(");
		code.append(JSONEncoder.encode(this.name));
		code.append(',');
		code.append(JSONEncoder.encode(this.params));
		code.append(',');
		code.append(JSONEncoder.encode(this.defaults));
		code.append(").translate(");
		code.append(JSONEncoder.encode(this.children));
		code.append(")");
		String result = (String) ParseUtil
				.getJSIRuntime()
				.eval(code.toString());
		this.children.clear();
		this.children.add(result);
	}

	private static void optimizeAll(final OptimizeContext context) {
		Map<String, Set<String>> callMap = new HashMap<String, Set<String>>(
				context.getDefCallMap());
		// List<String> call = scopeInfo.getCallList();
		final ArrayList<ClientPlugin> pluginList = new ArrayList<ClientPlugin>();
		final ArrayList<String> positionList = new ArrayList<String>();
		final Map<String, Set<String>> namedClientCallMap = new HashMap<String, Set<String>>();
		context.walk(new OptimizeWalker() {
			public int visit(List<Object> parent, int index, String position) {
				@SuppressWarnings("unchecked")
				OptimizePlugin p = context.getPlugin((List<Object>) parent.get(index));
				if (p instanceof ClientPlugin) {
					ClientPlugin plugin = (ClientPlugin) p;

					positionList.add(position);
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
			for (int j = 0; j < i; j++) {
				if (position.startsWith(positionList.get(i))) {
					optimizedCall.removeAll(pluginList.get(j).optimizedCall);
				}
			}
			plugin.optimizedCall = optimizedCall;
		}
	}

	private static Set<String> getCall(OptimizeScope si) {
		HashSet<String> cs = new HashSet<String>(si.getCallList());
		if (cs.contains("*")) {
			cs.addAll(si.getExternalRefList());
		}
		cs.remove("*");
		return cs;
	}

	public List<String> getCallList() {
		return scopeInfo.getCallList();
	}

	public List<String> getExternalRefList() {
		return scopeInfo.getExternalRefList();
	}

	public List<String> getRefList() {
		return scopeInfo.getRefList();
	}

	public List<String> getVarList() {
		return scopeInfo.getVarList();
	}

}
