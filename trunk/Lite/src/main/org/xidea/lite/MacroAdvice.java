package org.xidea.lite;

import java.io.StringWriter;
import java.util.List;

import org.xidea.el.Invocable;
import org.xidea.lite.Template.Context;

public class MacroAdvice implements CompileAdvice {
	private String name;
	private String[] params;

	public void setName(String name) {
		this.name = name;
	}

	public void setParams(List<String> params) {
		this.params = params.toArray(new String[params.size()]);
	}

	public List<Object> compile(final Template template, final Object[] children) {
		template.gloabls.put(this.name, new Invocable() {
			public Object invoke(Object thizz, Object... args) throws Exception {
				StringWriter out = new StringWriter();
				int i = Math.min(args.length, params.length);
				Context context = (Context) thizz;
				context = context.newScope();
				while (i-- > 0) {
					context.put(params[i], args[i]);
				}
				template.renderList(context, children, out);
				return out.toString();

			}
		});
		return null;
	}

}
