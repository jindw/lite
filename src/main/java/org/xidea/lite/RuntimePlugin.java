package org.xidea.lite;


import java.io.IOException;
import java.util.Map;



/**
 * 插件属性可以被自动初始化。
 * 属性可以是String 类型，Boolean 类型，Number类型（还没确定如何转化），Java 集合类型（Map,Collection,List(不支持Set)）
 * @author jindw
 */
public interface RuntimePlugin {
	public void initialize(Template template,Object[] children);
	public void execute(Map<String, Object> context,Appendable out) throws IOException;
}
