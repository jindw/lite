package org.xidea.lite.parse;

import java.util.List;

public interface OptimizeWalker {
	/**
	 * 遍历Lite代码
	 * @param parent 当前序列
	 * @param index 当前位置
	 * @param post32 位置信息
	 * 					位置信息每步两个字符,
	 * 					第一字符是当前节点类别
	 * 					第二个字符是当前的位置信息+32
	 * @return
	 */
	public int visit(List<Object> parent, int index,String post32) ;
}
