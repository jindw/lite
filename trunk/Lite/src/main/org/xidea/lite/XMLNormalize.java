package org.xidea.lite;

public interface XMLNormalize {
	public String normalize(String text);
	public String addDefaultNS(String namespace,String prefix);
	public String addDefaultEntry(String entry,String value);
	public void setDefaultRoot(String start,String end);

}