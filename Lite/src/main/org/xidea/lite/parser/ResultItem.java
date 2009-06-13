package org.xidea.lite.parser;

import java.util.List;

/**
 * 一种编译期间的中间状态，其数据将在结果转换的时候再次编译成JSON中间代码
 * @author jindw
 */
public interface ResultItem {
	public List<Object> compile();
}
