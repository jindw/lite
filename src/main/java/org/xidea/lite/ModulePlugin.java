package org.xidea.lite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.json.JSONEncoder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 自定义函数和扩展函数（Invocable接口类）
 * 
 * @author jindw
 */
public class ModulePlugin implements RuntimePlugin {
	private final static String MODULE_TASK_KEY = "#MODULE_TASK";
	private static Log log = LogFactory.getLog(ModulePlugin.class);
	private Object[] children;
	private Template template;
	private String id;
	private ExecutorService executorService ;

	public void initialize(Template template, Object[] children) {
		this.template = template;
		this.children = children;
	}

	public void setId(String id){
		this.id = id;
	}
	public void execute(final Map<String, Object> context, Appendable out) throws IOException {

		Map<String,Future<CharSequence>> moduleResult = (Map<String,Future<CharSequence>>)context.get(MODULE_TASK_KEY);
		if(moduleResult == null){
			moduleResult = new HashMap<String, Future<CharSequence>>();
			context.put(MODULE_TASK_KEY,moduleResult);
		}
		FutureTask<CharSequence> result = new FutureTask<CharSequence>(new Callable<CharSequence>() {
			public CharSequence call(){
				StringBuilder buf = new StringBuilder();
				template.render(FutureWaitStack.wrap(context,true),children,buf);
				return buf;
			}
		});
		if(executorService != null){
			executorService.execute(result);
		}else{
			result.run();
		}
		moduleResult.put(id,result);
	}

	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}
	@SuppressWarnings({ "unchecked" })
	static class Appender implements RuntimePlugin {
		public void initialize(Template template, Object[] children) {
		}


		public void execute(Map<String, Object> context, Appendable out) throws IOException {
			//System.out.println("@@@@@@@@@@@@@@@@@");
			Map<String,Future<CharSequence>> moduleResult = (Map<String,Future<CharSequence>>)context.get(MODULE_TASK_KEY);
			Map.Entry<String, Future<CharSequence>>[] entries = moduleResult.entrySet().toArray(new Map.Entry[moduleResult.size()]);
			boolean waitModule = true;
			while(waitModule) {
				waitModule = false;
				try{
					Thread.sleep(10);
					for (Map.Entry<String, Future<CharSequence>> e : entries) {
						Future<CharSequence> value = e.getValue();
						if(value.isDone()){
							widgetArrived(out, e.getKey(),value.get());
						}else{
							waitModule = true;
						}
					}
				}catch (ExecutionException e){
					e.printStackTrace();
					log.error(e);
				}catch (InterruptedException e){
					//e.printStackTrace();
					log.error(e);
				}
			}

		}

		private void widgetArrived(Appendable out,String id, CharSequence value) throws IOException{
			//resp.write('<script>__widget_arrived("'+id+'",'+content+')</script>')
			out.append("<script>__widget_arrived(\"").append(id).append("\",").append(JSONEncoder.encode(value.toString())).append(")</script>");
		}
	}
}
