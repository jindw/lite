package org.xidea.lite.parse;


/**
 * 需要设计为线程安全
 * @author jindw
 */
public interface TextParser {
	/**
	 * 执行解析操作
	 * @param text
	 * @param start 对jsel来说 就是 $出现的位置
	 * @param context
	 * @return 返回改指令结束位置的下一个位置，返回值一定要 大于 start 否则安失败算
	 */
	public int parseText(String text,int start,ParseContext context);
	
	/**
	 * 补充优先级位数,一般为匹配字长 
	 * 比如 :${aa}  priority 1
	 *     :$end  priority 3
	 *     :$if{ test }  priority 2
	 * @return 附加优先级
	 */
	public int getPriority();
	/**
	 * 查找EL或者模板指令的开始位置
	 * @param text
	 * @param start 开始查询的位置
	 * @param otherStart 其他的指令解析器找到的指令开始位置（以后必须出现在更前面，否则无效）
	 * @return 返回EL起始位置
	 */
	public int findStart(String text,int start,int otherStart);
}
