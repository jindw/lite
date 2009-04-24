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
	public int findStart(ParseContext context,String text,int start);
}
