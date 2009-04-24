package org.xidea.lite.parser.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xidea.el.ExpressionSyntaxException;
import org.xidea.lite.Template;
import org.xidea.lite.parser.ParseChain;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.Parser;

public abstract class AbstractTextParser implements Parser<String>  {

	protected Map<Object, InstructionParser> tagParser = new HashMap<Object, InstructionParser>();
	protected String elStart = "$";
	public void parse(ParseContext context, ParseChain chain, String text) {
		final boolean encode;
		final char qute;
		switch (context.getELType()) {
		case Template.XML_ATTRIBUTE_TYPE:
			encode = true;
			qute = '"';
			break;
		case Template.XML_TEXT_TYPE:
			encode = true;
			qute = 0;
			break;
		default:
			encode = false;
			qute = 0;
		}
		parse(context, text, encode, qute);
	}

	/**
	 * 解析指定文本
	 * 
	 * @public
	 * @abstract
	 * @return <Array> result
	 */
	protected void parse(ParseContext context, final String text,
			final boolean encode,final char qute) {
		int length = text.length();
		int start = 0;
		do {
			int p$ = appendToElStart(context, text, start, encode, qute);
			if (p$ >= start) {
				String fn = findFN(text, p$);
				// final int p1 = text.indexOf('{', p$);
				start = p$;
				// start == p$
				start = parseInstruction(context, text, fn,
						start, encode, qute);
			} else {
				break;
			}
		} while (start < length);
		if (start < length) {
			context.append(text.substring(start), encode, qute);
		}
	}

	protected int parseInstruction(ParseContext context,
			String text, String fn, final int start, boolean encode, char qute) {
		try {
			int next = tagParser.get(fn).parse(context, text, start);
			if (next > start) {
				return next;
			} else {
				throw new ExpressionSyntaxException(text.substring(start));
			}
		} catch (Exception e) {
		}
		context.append(text.substring(start, start + 1), encode, qute);
		return start + 1;
	}

	protected int appendToElStart(ParseContext context, final String text,
			final int start, boolean encode, char qute) {
		int p$ = text.indexOf(elStart, start);
		if (p$ > 0 && text.charAt(p$ - 1) == '\\') {
			int pre = p$ - 1;
			while (pre-- > 0 && text.charAt(pre) == '\\')
				;
			int countp1 = p$ - pre;
			context.append(text.substring(start, p$ - countp1 / 2), encode,
					qute);
			if ((countp1 & 1) == 0) {// escape
				context.append(elStart);
				return p$ + 1;
			}else{
				return p$;
			}
		}
		if (start < p$) {
			context.append(text.substring(start, p$), encode, qute);
		}
		return p$;
	}

	protected abstract String findFN(String text, int p$);


}