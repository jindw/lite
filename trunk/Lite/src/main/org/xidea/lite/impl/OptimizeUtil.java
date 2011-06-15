package org.xidea.lite.impl;

import static org.xidea.lite.Template.CAPTURE_TYPE;
import static org.xidea.lite.Template.ELSE_TYPE;
import static org.xidea.lite.Template.EL_TYPE;
import static org.xidea.lite.Template.FOR_TYPE;
import static org.xidea.lite.Template.IF_TYPE;
import static org.xidea.lite.Template.PLUGIN_TYPE;
import static org.xidea.lite.Template.VAR_TYPE;
import static org.xidea.lite.Template.XA_TYPE;
import static org.xidea.lite.Template.XT_TYPE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.ExpressionToken;
import org.xidea.el.impl.TokenImpl;
import org.xidea.lite.DefinePlugin;
import org.xidea.lite.RuntimePlugin;
import org.xidea.lite.parse.OptimizePlugin;
import org.xidea.lite.parse.OptimizeScope;
import org.xidea.lite.parse.OptimizeWalker;

class OptimizeUtil {
	private static final Log log = LogFactory.getLog(OptimizeUtil.class);

	static void optimizeCallClosure(final Map<String, Set<String>> callMap,
			final Set<String> closure) {
		Collection<String> visits = closure;
		while (true) {
			HashSet<String> newClosure = new HashSet<String>();
			for (String fn : visits) {
				Collection<String> called = callMap.get(fn);
				for (String fn2 : called) {
					if (callMap.containsKey(fn2)
							&& !(closure.contains(fn2) || newClosure
									.contains(fn2))) {
						newClosure.add(fn2);
					}
				}
			}
			if (newClosure.isEmpty()) {
				return;
			} else {
				visits = newClosure;
				closure.addAll(newClosure);
			}
		}
	}

	/**
	 * 合并相邻文本
	 * 
	 * @param result
	 * @return
	 */
	static List<Object> optimizeList(List<Object> result) {
		ArrayList<Object> optimizeResult = new ArrayList<Object>(result.size());
		Object buf = null;
		for (Object item : result) {
			if (item instanceof String) {
				if (buf == null) {
					buf = item;
				} else {
					if (buf instanceof String) {
						buf = new StringBuilder((String) buf);
					}
					((StringBuilder) buf).append((String) item);
				}
			} else {
				if (buf != null) {
					optimizeResult.add(buf.toString());
					buf = null;
				}
				optimizeResult.add(item);
			}
		}
		if (buf != null) {
			optimizeResult.add(buf.toString());
		}
		return optimizeResult;
	}

	@SuppressWarnings( { "unchecked" })
	static List<Object> toListTree(List<Object> result,
			Map<String, List<Object>> defMap) {
		ArrayList<ArrayList<Object>> childStack = new ArrayList<ArrayList<Object>>();
		ArrayList<Object> currentList = new ArrayList<Object>();
		childStack.add(currentList);
		for (Object item : result) {
			if (item instanceof Object[]) {
				Object[] cmd = (Object[]) item;
				// System.out.println(Arrays.asList(cmd));
				if (cmd.length == 0) {
					int childTop = childStack.size() - 1;
					ArrayList<Object> children = childStack.remove(childTop--);
					currentList = childStack.get(childTop);

					int currentLast = currentList.size() - 1;
					ArrayList instruction = ((ArrayList) currentList
							.get(currentLast));
					instruction.set(1, children);
					Number type = (Number) instruction.get(0);
					if (type.intValue() == PLUGIN_TYPE) {
						Map<String, Object> config = (Map<String, Object>) instruction.get(2);
						String className = (String)config.get("class");
						if (DefinePlugin.class.getName().equals(className)) {
							// def 前移
							String name = (String)config.get("name");
							if(name == null){
								log.error("def name is required");
							}else {
								List<Object> old = defMap.get(name);
								if(old != null){
									if(!old.equals(currentLast)){
										log.error("def "+name+" is found before");
									}
								}
								defMap.put(name,instruction);
							}
							//remove def...
							currentList.remove(currentLast);
						} else {
							try {
								Class<?> clazz = Class.forName(className);
								if(OptimizePlugin.class.isAssignableFrom(clazz) || RuntimePlugin.class.isAssignableFrom(clazz) ){
									
								}else{
									log.warn( className
											+ " is not a ParsePlugin or RuntimePlugin");
									//remove unknow plugin
									currentList.remove(currentLast);
								}
								//DO by optimizer
//								if (ParsePlugin.class.isAssignableFrom(clazz)) {
//									parsePlugins.add(instruction);
//								}
							} catch (ClassNotFoundException e) {
								currentList.remove(currentLast);
								log.warn("ParsePlugin:" + className
										+ " not found");
							}

						}
					}
				} else {
					int type = (Integer) cmd[0];
					ArrayList<Object> cmd2 = new ArrayList<Object>(
							cmd.length + 1);
					cmd2.add(cmd[0]);
					currentList.add(cmd2);
					switch (type) {
					case CAPTURE_TYPE:
					case IF_TYPE:
					case ELSE_TYPE:
					case FOR_TYPE:
					case PLUGIN_TYPE:
						// case IF_STRING_IN_TYPE:
						cmd2.add(null);
						childStack.add(currentList = new ArrayList<Object>());
					}
					for (int i = 1; i < cmd.length; i++) {
						cmd2.add(cmd[i]);
					}

				}
			} else {
				currentList.add(item.toString());
			}
		}
		return currentList;
	}
	@SuppressWarnings("unchecked")
	static boolean walk(List source, OptimizeWalker revicer,
			StringBuilder position) {
		for (int i = 0; i < source.size(); i++) {
			Object item = source.get(i);
			if (item instanceof List<?>) {
				List<?> cmd = (List<?>) item;
				int type = (Integer) cmd.get(0);
				switch (type) {
				case PLUGIN_TYPE:
					Map<String, Object> config = (Map<String, Object>) cmd
							.get(2);
					String className = (String) config.get("class");
					try {
						Class<?> clazz = Class.forName(className);
						if (OptimizePlugin.class.isAssignableFrom(clazz)) {
							String p = position == null ? null : position
									.toString();
							int j = revicer.visit(source, i, p);
							if (j == -1) {
								return true;
							} else {
								i = j;
							}
						}
					} catch (Exception e) {
					}
				case CAPTURE_TYPE:
				case IF_TYPE:
				case ELSE_TYPE:
				case FOR_TYPE:
					if (position != null) {
						position.append((char) type);
						position.append((char) (i+32));
					}
					try{
						if (walk((List<?>) cmd.get(1), revicer, position)) {
							return true;
						} 
					}finally{
						if (position != null) {
							position.setLength(position.length() - 2);
						}
					}
					// case BREAK_TYPE:
					// case EL_TYPE:
					// case XA_TYPE:
					// case XT_TYPE:
					// case VAR_TYPE:

				}
			}
		}
		return false;
	}

	static BlockInfoImpl parseList(List<Object> list, List<String> params) {
		BlockInfoImpl bi = new BlockInfoImpl();
		bi.paramList.addAll(params);
		parseList(bi, list, true);
		return bi;
	}

	@SuppressWarnings("unchecked")
	private static void parseList(BlockInfoImpl bi, List<Object> list,
			boolean newScope) {
		for (Object i : list) {
			if (i instanceof List<?>) {
				List<?> item = (List<?>) i;
				int type = (Integer) item.get(1);
				switch (type) {
				case VAR_TYPE:
				case EL_TYPE:
				case XA_TYPE:
				case XT_TYPE:
					walkEL(bi, TokenImpl.toToken((List<Object>) item.get(1)));
					break;
				case IF_TYPE:
				case ELSE_TYPE:
				case FOR_TYPE:
					walkEL(bi, TokenImpl.toToken((List<Object>) item.get(2)));
					break;
				// case PLUGIN_TYPE:
				// case BREAK_TYPE:
				// case CAPTRUE_TYPE:
				// break;
				}
				switch (type) {
				case PLUGIN_TYPE:
					Map<String, Object> config = (Map<String, Object>) item
							.get(2);
					String className = (String) config.get("class");
					try {
						Class<?> clazz = Class.forName(className);
						if (OptimizeScope.class.isAssignableFrom(clazz)) {
							// List children = (List) item.get(1);
							break;
						}else if(DefinePlugin.class == clazz){
							log.warn("why defined is not removed?");
							break;
						}
					} catch (Exception e) {
					}
				case CAPTURE_TYPE:
				case IF_TYPE:
				case ELSE_TYPE:
				case FOR_TYPE:
					parseList(bi, (List<Object>) item.get(1), true);
				}
				switch (type) {
				case VAR_TYPE:
				case CAPTURE_TYPE:
					bi.addVar((String) item.get(2));
				}
			}
		}

	}

	private static void walkEL(BlockInfoImpl thiz, ExpressionToken el) {
		if (el == null) {
			return;
		}
		int op = el.getType();
		if (op <= 0) {
			if (op == ExpressionToken.VALUE_VAR) {
				String varName = (String) el.getParam();
				thiz.refList.add(varName);
				if(!thiz.varList.contains(varName) && thiz.paramList.contains(varName)){
					thiz.externalRefList.add(varName);
				}
			}
			return;
		} else {

			if (op == ExpressionToken.OP_INVOKE) {
				ExpressionToken arg1 = el.getLeft();
				int op1 = arg1.getType();
				if (op1 == ExpressionToken.VALUE_VAR) {
					String varName = (String) arg1.getParam();
					if (thiz.varList.contains(varName)
							//TODO:如果仅仅是参数被调用,问题不在自身,而在调用者!此问题忽略
							//|| thiz.argList.contains(varName) 
							) {// call 的是表达式, 不靠谱
						log.info("表达式函数调用");
						thiz.callList.add("*");
						walkEL(thiz, arg1);
					} else {
						thiz.callList.add(varName);
					}
				} else if (op1 == ExpressionToken.OP_GET) {// member
					// TODO:...
					ExpressionToken arg1Right = arg1.getRight();
					if (arg1Right.getType() == ExpressionToken.VALUE_CONSTANTS
							&& (arg1Right.getParam() instanceof String)) {
						// member
					} else {
						log.info("表达式函数调用");
						thiz.callList.add("*");
					}
					walkEL(thiz, arg1);
				} else {
					// TODO:...
					log.info("表达式函数调用");
					thiz.callList.add("*");
					walkEL(thiz, arg1);
				}
			} else {
				walkEL(thiz, el.getLeft());
			}
			walkEL(thiz, el.getRight());
		}
	}
}

class BlockInfoImpl implements OptimizeScope {

	ArrayList<String> paramList = new ArrayList<String>();
	ArrayList<String> varList = new ArrayList<String>();
	List<Set<String>> varStack = new ArrayList<Set<String>>();
	ArrayList<String> refList = new ArrayList<String>();
	ArrayList<String> externalRefList = new ArrayList<String>();
	/**
	 * 所有函数调用记录:[true,直接调用, false 可能调用(表达式出口函数)]
	 */
	ArrayList<String> callList = new ArrayList<String>();

	public void addVar(String name) {
		varList.add(name);
	}

	public List<String> getCallList() {
		return callList;
	}

	public List<String> getVarList() {
		return varList;
	}

	public List<String> getRefList() {
		return refList;
	}

	public List<String> getExternalRefList() {
		return externalRefList;
	}
	public List<String> getParamList() {
		return paramList;
	}
}
