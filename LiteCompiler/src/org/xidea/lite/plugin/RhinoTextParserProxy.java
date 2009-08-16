package org.xidea.lite.plugin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.xidea.lite.parser.TextParser;
import org.xidea.lite.parser.ParseContext;

public class RhinoTextParserProxy implements TextParser {

	private static final Log log = LogFactory
			.getLog(RhinoTextParserProxy.class);
	private Scriptable base;
	private Function findStart;
	private Function parse;
	private Object priority;

	public RhinoTextParserProxy(Context context, Scriptable base,
			Function findStart, Function parse, Object priority) {
		this.base = base;
		this.parse = parse;
		this.findStart = findStart;
		this.priority = priority;
	}

	@Override
	public int findStart(String text, int start, int otherStart) {
		try {
			Object[] args = new Object[] { text, start, otherStart };
			Number value = (Number) RhinoContext.call(base, findStart, args);
			return value.intValue();
		} catch (RuntimeException e) {
			if (log.isDebugEnabled()) {
				log.debug("error in:"
						+ RhinoContext.call(findStart, (Function) RhinoContext
								.getProperty(findStart, "toString"),
								new Object[0]), e);
			}
			return -1;
		}
	}

	@Override
	public int parse(String text, int start, ParseContext context) {
		try {
			Object[] args = new Object[] { text, start, context };
			Number value = (Number) RhinoContext.call(base, parse, args);
			return value.intValue();
		} catch (RuntimeException e) {
			if (log.isDebugEnabled()) {
				log.debug(
						"error in:"
								+ RhinoContext.call(parse,
										(Function) RhinoContext.getProperty(
												parse, "toString"),
										new Object[0]), e);
			}
			return -1;
		}
	}

	@Override
	public int getPriority() {
		if (priority != null) {
			if (priority instanceof Function) {
				try {
					Object[] args = new Object[0];
					Number value = (Number) RhinoContext.call(base,
							(Function) priority, args);
					return value.intValue();
				} catch (RuntimeException e) {
					if (log.isDebugEnabled()) {
						log.debug("error in:" + priority, e);
					}
				}
			}else{
				double value = ScriptRuntime.toNumber(priority);
				return (int)value;
			}
		}
		return 1;
	}

}
