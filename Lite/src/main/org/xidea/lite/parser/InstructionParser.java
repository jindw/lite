package org.xidea.lite.parser;


/**
 * 需要设计为线程安全
 * @author jindw
 */
public interface InstructionParser {
	/**
	 * @param text
	 * @param start 对jsel来说 就是 $出现的位置
	 * @param context
	 * @return 返回改指令结束位置的下一个位置，返回值一定要 大于 start 否则安失败算
	 */
	public int parse(String text,int start,ParseContext context);
	/**
	 * 查找EL或者模板指令的开始位置
	 * @param text
	 * @param start
	 * @param context
	 * @return 返回EL起始位置
	 */
	public int findStart(String text,int start,ParseContext context);
}
