package org.xidea.lite;

import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.Invocable;
/**
 * 自定义函数和扩展函数（Invocable接口类）
 * @author jindw
 */
public class DefinePlugin implements Plugin,Invocable {
	private static Log log = LogFactory.getLog(DefinePlugin.class);
	private String name;
	private String[] params;
	private Object[] children;
	
	private String type;
	private Object instance;

	public void initialize(Object[] children) {
		this.children = children;
		if(type != null){
			try {
				this.instance = Class.forName(type).newInstance();
			} catch (Exception e) {
				log.warn("无法装载扩展：" + type,e);
			}
		}
		
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setParams(List<String> params) {
		this.params = params.toArray(new String[params.size()]);
	}
	public void setType(String type) {
		this.type = type;
	}

	public void execute(Context context,Writer out) {
		if (instance==null) {
			context.put(this.name, this);
		} else {
			context.put(name, instance);
		}
	}

	public Object invoke(Object thizz, Object... args) throws Exception {
		StringWriter out = new StringWriter();
		apply(thizz, out, args);
		return out.toString();

	}

	public void apply(Object thizz, Writer out, Object... args) {
		int i = Math.min(args.length, params.length);
		Context context = (Context) thizz;
		context = context.createScope();
		while (i-- > 0) {
			context.put(params[i], args[i]);
		}
		context.renderList(children, out);
	}

}
