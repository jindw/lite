package org.xidea.lite;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.xidea.el.Invocable;
import org.xidea.lite.Template.Context;

public class MacroAdvice implements CompileAdvice {
	private String name;
	private String[] params;
	private Object[] defaults;

	public void setName(String name) {
		this.name = name;
	}

	public List<Object> getDefaults() {
		return Arrays.asList(defaults);
	}

	public void setDefaults(List<Object> defaults) {
		this.defaults = defaults.toArray(new Object[defaults.size()]);
	}
	
	public List<String> getParams() {
		return Arrays.asList(params);
	}


	public void setParams(List<String> params) {
		this.params = params.toArray(new String[params.size()]);
	}

	public List<Object> compile(final Map<String, Object> gloabls, final Object[] children) {
		gloabls.put(this.name, new Invocable() {
			public Object invoke(Object thizz, Object... args) throws Exception {
				StringWriter out = new StringWriter();
				int i = Math.min(args.length, params.length);
				Context context = (Context) thizz;
				context = context.newScope();
				while (i-- > 0) {
					context.put(params[i], args[i]);
				}
				context.renderList(children, out);
				return out.toString();

			}
		});
		return null;
	}

}
