package org.xidea.lite;

import java.io.StringWriter;
import java.util.List;

import org.xidea.el.Invocable;
import org.xidea.lite.Template.Context;

public class MicroAdvice implements CompileAdvice {
	public final static String INSTANCE_MAP = "instanceMap";
	public final static String OBJECT_MAP = "objectMap";
	private String name;
	private String[] arguments;

	public void setName(String name) {
		this.name = name;
	}

	public void setArguments(List<String> arguments) {
		this.arguments = arguments.toArray(new String[arguments.size()]);
	}

	public void compile(final Template template, List<Object> result) {
		final Object[] children = template.compile(result);
		template.gloabls.put(this.name, new Invocable() {
			public Object invoke(Object thizz, Object... args) throws Exception {
				StringWriter out = new StringWriter();
				int i = Math.min(args.length, arguments.length);
				Context context = (Context) thizz;
				try {
					context.enter();
					while (i-- > 0) {
						context.put(arguments[i], args[i]);
					}
					template.renderList(context, children, out);
					return out.toString();
				} finally {
					context.exit();
				}
			}
		});
	}

}
