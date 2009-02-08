package org.xidea.lite.parser;

import java.util.List;

public abstract interface Parser {
	public static final Object END_INSTRUCTION = new Object[0];

	/**
     * 给出文件内容或url，解析模版源文件〄1�7
     * 如果指定了base，当作url解析，无base，当作纯文本解析
     * @public
     * @abstract
     * @return <Array> result
     */
    public abstract List<Object> parse(Object node,ParseContext context);

}
