package org.xidea.lite.parse;

import java.util.List;

public interface ParsePlugin{

	public void initialize(List<Object> childCode,List<Object> templateCode) ;

	public void parse();

	public List<Object> getPluginList();
}
