package org.xidea.lite.parser;

import java.util.List;

public abstract interface Parser {
	/**
     * 给出文件内容或url，解析模版源文件〄1�7
     * 如果指定了base，当作url解析，无base，当作纯文本解析
     * @public
     * @return <Array> result
     */
    public abstract List<Object> parse(Object node,ParseContext context);
    public List<Object> parse(Object data);
}
