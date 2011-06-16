package org.xidea.lite.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.parse.OptimizePlugin;
import org.xidea.lite.parse.OptimizeScope;
import org.xidea.lite.parse.OptimizeWalker;
import org.xidea.lite.parse.OptimizeContext;

@SuppressWarnings("unchecked")
public class OptimizeContextImpl implements OptimizeContext {
	private static final Log log = LogFactory.getLog(ParseContextImpl.class);
	private List templateList;
	private Map<String, List> defMap;

	private IdentityHashMap<List<List>, OptimizePlugin> pluginMap = new IdentityHashMap<List<List>, OptimizePlugin>();
//	private IdentityHashMap<ParsePlugin, List<List>> codeMap = new IdentityHashMap<ParsePlugin, List<List>>();
//	private IdentityHashMap<ParsePlugin, String> positionMap = new IdentityHashMap<ParsePlugin, String>();

	public OptimizeContextImpl(List templateList, Map defMap) {
		this.templateList = templateList;
		this.defMap = defMap;
	}

	public List<Object> optimize() {
		List<OptimizePlugin> pluginObjectList = initPlugin();
		parse(pluginObjectList);
		
		replaceWithChild();
		templateList.addAll(0, defMap.values());
		return templateList;
	}

	private List<OptimizePlugin> initPlugin() {
		final List<OptimizePlugin> pluginObjectList = new ArrayList<OptimizePlugin>();
		OptimizeUtil.walk(templateList, new OptimizeWalker(){
			public int visit(List<Object> parent, int index, String position) {
				List cmd = (List) parent.get(index);
				Map<String, Object> config = (Map<String, Object>) cmd.get(2);
				String className = (String) config.get("class");
				try {
					OptimizePlugin plugin = (OptimizePlugin) Class.forName(className).newInstance();
					List<Object> children = (List<Object>) cmd.get(1);
//					ReflectUtil.setValues(plugin, config);
					plugin.initialize(config, children, OptimizeContextImpl.this);
					pluginObjectList.add(plugin);
					pluginMap.put(cmd, plugin);
//					codeMap.put(plugin, cmd);
//					positionMap.put(plugin, position);
					
				} catch (Exception e) {
					log.warn("ParsePlugin initialize failed:" + config, e);
				}
				return index;
			}
			
		}, new StringBuilder());
		return pluginObjectList;
	}

	private void parse(List<OptimizePlugin> pluginObjectList) {
		for (OptimizePlugin plugin : pluginObjectList) {
			plugin.before();
		}
		// sort walk
		OptimizeUtil.walk(templateList, new OptimizeWalker() {
			public int visit(List<Object> parent, int index,String position) {
				List<?> cmd = (List<?>) parent.get(index);
				OptimizePlugin p = pluginMap.get(cmd);
				if (p != null) {
					p.optimize();
				}
				return index;
			}
		},null);
	}

	private void replaceWithChild() {
		OptimizeUtil.walk(templateList, new OptimizeWalker() {
			public int visit(List<Object> parent, int index,String position) {
				List<?> cmd = (List<?>) parent.get(index);
				OptimizePlugin p = pluginMap.get(cmd);
				if (p != null) {
					parent.remove(index);
					List children = (List) cmd.get(1);
					parent.addAll(index, children);
					index--;
					index += children.size();
				}
				return index;
			}
		},null);
	}

	private HashMap<String, Set<String>>defCallMap ; 
	public Map<String, Set<String>> getDefCallMap() {
		if(defCallMap == 	null){
			defCallMap = new HashMap<String, Set<String>>();
			for(Map.Entry<String, List> entry : defMap.entrySet()){
				List data = entry.getValue();
				Map config = (Map) data.get(2);
				List code = (List) data.get(1);
				defCallMap.put(entry.getKey(), getCall(OptimizeUtil.parseList(code, (List)config.get("params"))));
			}
		}
		return defCallMap;
	}
	private static Set<String> getCall(OptimizeScope si) {
		HashSet<String> cs = new HashSet<String>(si.getCalls());
		if (cs.contains("*")) {
			cs.addAll(si.getExternalRefs());
		}
		cs.remove("*");
		return cs;
	}
	public OptimizePlugin getPlugin(List<Object> code) {
		return pluginMap.get(code);
	}

	public void walk(OptimizeWalker revicer){
		OptimizeUtil.walk(templateList,revicer,new StringBuilder());
	}


	public OptimizeScope parseScope(List<Object> children, List<String> params) {
		return OptimizeUtil.parseList(children, params);
	}

	public void optimizeCallClosure(Map<String, Set<String>> callMap,
			Set<String> closure) {
		OptimizeUtil.optimizeCallClosure(callMap, closure);
	}

	public List<Object> getDefCode(String defName) {
		return defMap.get(defName);
	}



}
