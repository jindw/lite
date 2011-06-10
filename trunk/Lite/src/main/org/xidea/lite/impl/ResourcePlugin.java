package org.xidea.lite.impl;

import java.util.List;

import org.xidea.lite.parse.ParsePlugin;

public class ResourcePlugin implements ParsePlugin{

	private List<Object> pluginList;
	private List<Object> templateList;
	public void initialize(List<Object> pluginList,List<Object> templateList) {
		this.pluginList = pluginList;
		this.templateList = templateList;
	}

	public void parse() {
		
	}
	public List<Object> getPluginList(){
		return pluginList;
	}

}
