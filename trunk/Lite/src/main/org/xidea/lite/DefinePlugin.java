package org.xidea.lite;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.Invocable;
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
	public void setDefaults(List<Object> defaults) {
		this.defaults = defaults.toArray(new Object[defaults.size()]);
	}
	public void setType(String type) {
		this.type = type;
	}

	public void execute(Map<String, Object> context,Appendable out) {
		if (type==null) {
			template.addVar(name, this);
		} else {
			template.addVar(name, instance);
		}
	}

	public Object invoke(Object thiz, Object... args) throws Exception {
		StringBuilder out = new StringBuilder();
		apply(thiz, out, args);
		return out.toString();

	}

	public void apply(Object thiz, Appendable out, Object... args) {
		HashMap<String, Object> context = new HashMap<String, Object>();
		for (int i = 0; i < params.length; i++) {
			if(i<args.length){
				context.put(params[i], args[i]);
			}else{
				int begin = i - (params.length -defaults.length);
				if(begin>=0 && begin<defaults.length){
					context.put(params[i], defaults[begin]);
				}
				
			}
		}
		template.render(context,children, out);
	}

}
