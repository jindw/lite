package org.xidea.lite.parse;

public interface XMLNormalize {
	public String normalize(String text,String id);
	public String addDefaultNS(String prefix,String namespace);
	public String addDefaultEntity(String entry,String replaceEntry);
	public void setDefaultRoot(String elementTag);

}