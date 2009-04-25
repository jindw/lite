package org.xidea.lite.parser;


/**
 * 需要设计为线程安全
 * @author jindw
 */
public interface InstructionParser {
	/**
	 * @param context
	 * @param text
	 * @param start 对jsel来说 就是 $出现的位置
	 * @return 返回值一定要 大于 start 否则安失败算
	 */
	public int parse(ParseContext context,String text,int start);
	/**
	 * 查找EL或者模板指令的开始位置
	 * @param context
	 * @param text
	 * @param start
	 * @return 返回EL起始位置
	 */
	public int findStart(ParseContext context,String text,int start);
}
