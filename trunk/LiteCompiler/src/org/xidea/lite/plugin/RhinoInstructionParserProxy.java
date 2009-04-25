package org.xidea.lite.plugin;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.xidea.lite.parser.InstructionParser;
import org.xidea.lite.parser.ParseContext;

public class RhinoInstructionParserProxy implements InstructionParser {

	private Scriptable base;
	private Function findStart;
	private Function parse;


	public RhinoInstructionParserProxy(Context context, Scriptable base,Function findStart,Function parse) {
		this.base = base;
		this.parse = parse;
		this.findStart = findStart;
	}

	@Override
	public int findStart(ParseContext context, String text, int start) {
		Object[] args = new Object[]{context,text,start};
		Number value = (Number)RhinoContext.call(base, findStart, args);
		return value.intValue();
	}


	@Override
	public int parse(ParseContext context, String text, int start) {
		Object[] args = new Object[]{context,text,start};
		Number value = (Number)RhinoContext.call(base, parse, args);
		return value.intValue();
	}

}
