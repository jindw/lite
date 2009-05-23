package org.xidea.lite.parser;

import java.util.List;
import java.util.Map;

public interface ResultTransformer {

	/**
	 * 创建一个解析客户端模板的解析上下文对象
	 * 该上下文对象不能有任何特征，避免客户端无法支持
	 * @param fn 函数名称
	 * @return
	 */
	public String transform(List<Object> list,String id,Map<String, String> featrues);

	public java.util.Set<String> getSupportFeatrues();
}
