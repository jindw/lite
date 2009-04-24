package org.xidea.lite.parser.impl;

import java.util.HashMap;
import java.util.Map;

import org.xidea.el.ExpressionSyntaxException;
import org.xidea.lite.Template;
import org.xidea.lite.parser.InstructionParser;
import org.xidea.lite.parser.ParseChain;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.Parser;

public abstract class AbstractTextParser implements Parser<String> {
	protected InstructionParser[] instructionParser = { new ELParser("", true) };

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
			final boolean encode, final char qute) {
		int length = text.length();
		int start = 0;
		do {
			InstructionParser nip = null;
			int p$ = length + 1;
			for (InstructionParser ip : instructionParser) {
				int p$2 = ip.findStart(context, text, start);
				if (p$2 >= start && p$2 < p$) {
					p$ = p$2;
					nip = ip;
				}
			}
			if (nip != null) {
				int escapeCount = escapeCount(text,p$);
				context.append(text.substring(start, p$ - (escapeCount+1) / 2), encode,
							qute);
				if((escapeCount & 1) == 1){//escapsed
					start = nextPos(context, text, p$);
				}else{
					start = p$;
					int mark = context.mark();
					try{
					start = nip.parse(context, text, start);
					}catch (Exception e) {
					}
					if(start<=p$){
						context.reset(mark);
						start = nextPos(context, text, p$);
					}
					
				}
			} else {
				break;
			}
		} while (start < length);
		if (start < length) {
			context.append(text.substring(start), encode, qute);
		}
	}

	private int nextPos(ParseContext context, final String text, int p$) {
		int start;
		context.append(text.substring(p$,p$+1));
		start = p$+1;
		return start;
	}

	protected int escapeCount(final String text, int p$) {
		if (p$ > 0 && text.charAt(p$ - 1) == '\\') {
			int pre = p$ - 1;
			while (pre-- > 0 && text.charAt(pre) == '\\')
				;
			return p$ - pre-1;
		}
		return 0;
	}
//
//	protected boolean appendAndReturnEscaped(ParseContext context,
//			final String text, final int start, int p$, boolean encode,
//			char qute) {
//		if (p$ > 0 && text.charAt(p$ - 1) == '\\') {
//			int pre = p$ - 1;
//			while (pre-- > 0 && text.charAt(pre) == '\\')
//				;
//			int countp1 = p$ - pre;
//			if ((countp1 & 1) == 0) {// escape
//				return true;
//			} else {
//				return false;
//			}
//		}
//		if (start < p$) {
//			context.append(text.substring(start, p$), encode, qute);
//		}
//		return false;
//	}
}