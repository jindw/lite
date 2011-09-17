package org.xidea.el;

import java.util.Map;






/**
 * 做2值之间的计算。
 * 三元运算符，需要转化为二元表示
 * 值类型运算符，创建或者从vs中获取值
 * @author jindw
 */
public interface OperationStrategy {

	/**
	 * @param op 操作符对象
	 * @param vs 运算变量表
	 * @return 运算结果
	 * @see org.xidea.el.impl.OperationStrategyImpl#evaluate
	 */
	public Object evaluate(ExpressionToken token,Map<String,Object> vs) ;
	public Object getVar(Map<String, Object> vs,Object key);
}
