package org.xidea.lite.parser;

public interface InstructionParser {
	/**
	 * @param context
	 * @param text
	 * @param p$
	 * @return 返回值一定要 大于 p$ 否则安失败算
	 */
	int parse(ParseContext context,String text,int p$);
}
