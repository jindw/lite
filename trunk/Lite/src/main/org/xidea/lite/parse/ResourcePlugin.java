package org.xidea.lite.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ResourcePlugin implements OptimizePlugin{

	private OptimizeContext context;
	private List<Object> children;
	private String id;

	public void initialize(Map<String, Object> config, List<Object> children,
			OptimizeContext context) {
		this.context = context;
		this.id =(String) config.get("id");
		this.children = children;
		
	}
	public void before(){
		final ArrayList<List<?>> parentList = new ArrayList<List<?>>();
		final ArrayList<Integer> indexList = new ArrayList<Integer>();
		context.walk(new OptimizeWalker() {
			public int visit(List<Object> parent, int index,String position) {
				List<?> cmd = (List<?>) parent.get(index);
				@SuppressWarnings("unchecked")
				Map<String,Object> config = (Map<String, Object>) cmd.get(2);
				if (id.equals(config.get("targetId"))) {
					parentList.add(parent);
					indexList.add(index);
				}
				return index;
			}
		});
		int i = indexList.size();
		while(i-->0){
			Object cmd = parentList.get(i).remove((int)indexList.get(i));
			children.add(cmd);
		}
	}
	public void optimize() {
	}


}
