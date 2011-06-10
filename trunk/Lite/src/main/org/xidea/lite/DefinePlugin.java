package org.xidea.lite;

import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.Invocable;
import org.xidea.el.ValueStack;
/**
 * 自定义函数和扩展函数（Invocable接口类）
 * @author jindw
 */
public class DefinePlugin implements RuntimePlugin,Invocable {
	private static Log log = LogFactory.getLog(DefinePlugin.class);
	private String name;
	private String[] params;
	private Object[] children;
	private Object[] defaults;
	
	private String type;
	private Object instance;
	private Template template;

	public void initialize(Template template,Object[] children) {
		this.template = template;
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
	public void setDefaults(List<String> defaults) {
		this.defaults = defaults.toArray(new String[defaults.size()]);
	}
	public void setType(String type) {
		this.type = type;
	}

	public void execute(ValueStack context,Writer out) {
		if (type==null) {
			context.put(this.name, this);
		} else {
			context.put(name, instance);
		}
	}

	public Object invoke(Object thiz, Object... args) throws Exception {
		StringWriter out = new StringWriter();
		apply(thiz, out, args);
		return out.toString();

	}

	public void apply(Object thiz, Writer out, Object... args) {
		Context context = new Context(null);
		for (int i = 0; i < params.length; i++) {
			if(i<args.length){
				context.put(params[i], args[i]);
			}else{
				int begin = i - (params.length -defaults.length);
				if(i>=0 && i<defaults.length){
					context.put(params[i], defaults[begin]);
				//}else{
				//	//context.put(params[i], null);
				}
				
			}
		}
		template.renderList(context,children, out);
	}

}
