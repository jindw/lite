package org.xidea.lite.tools;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.impl.CommandParser;
import org.xidea.el.impl.ReflectUtil;
import org.xidea.el.json.JSONEncoder;
import org.xidea.jsi.JSIRuntime;
import org.xidea.lite.LiteTemplate;
import org.xidea.lite.impl.HotTemplateEngine;
import org.xidea.lite.impl.ParseUtil;
import org.xidea.lite.parse.ParseContext;

public class LiteCompiler {
	private static final Log log = LogFactory.getLog(LiteCompiler.class);
	private File root;
	private File config;
	private String path;
	private Map<String, byte[]> resultMap;
	private File output;
	private String[] includes;
	private String[] excludes;
	private boolean linked;
	private HotTemplateEngine engine;
	private String translator;
	private ResourceManagerImpl resourceManager;

	public LiteCompiler(String[] args) {
		log.info("Lite Compiler Args:" + JSONEncoder.encode(args));
		CommandParser cp = new CommandParser(args){
			protected void onMissedProperty(final Object context, String name) {
				if (log.isInfoEnabled()) {
					String msg = "illegal param(无效参数)：-" + name+"; avaliable params may be():"+ ReflectUtil.map(context).keySet();
					log.error(msg);
				}
			}
		};
		cp.setup(this);
	}

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[] {
					// "-root","D:\\workspace\\FileServer/src/main/org/jside/filemanager/","-litecode","D:\\workspace\\FileServer/build/dest/lite","-nodeParsers","org.xidea.lite.parser.impl.HTMLNodeParser"
					"-root", "D:\\workspace\\Lite2/web/", "-output",
					"D:\\workspace\\Lite2/build/dest/web",
					// "-path","/doc/guide/index.xhtml",
					"-includes", "/doc/guide/*.xhtml", "-excludes",
					"/doc/guide/layout.xhtml", "-translators", "php" };
		}
		new LiteCompiler(args).execute();
	}

	public void execute() {
		try {
			initialize();
			this.resultMap = new LinkedHashMap<String, byte[]>();
			if (path == null) {
				final PathMatcher includes = PathMatcher.createMatcher(this.includes);
				final PathMatcher excludes = PathMatcher.createMatcher(this.excludes);
				this.processDir(root, "/",includes,excludes);
				if (linked && !output.equals(root)) {
					List<String> lrs = this.resourceManager
							.getLinkedResources();
					for (String rp : lrs) {
						this.processFile(rp);
					}
				}
				LiteCompilerHelper.writeOutput(this.resultMap, this.output);
			} else {
				this.processFile(path);
			}
			log.info("Compile success(执行成功):");
		} catch (Exception e) {
			log.error("Compile failed(编译失败):", e);
		}
	}

	protected void initialize() throws IOException {
		if (root == null) {
			root = new File("./").getAbsoluteFile();
		}

		if (output == null) {
			log.error("-output is required(必须指定 -output 参数)!!");
			throw new IllegalArgumentException(" -output is required!!");
		} else {
			if (output.equals(root)) {
				String[] includes = this.includes;
				LiteCompilerHelper.checkOutput(includes);
			}
		}
		if (config == null) {
			config = new File(root, "/WEB-INF/lite.xml");
		}
		if (this.resourceManager == null) {
			this.resourceManager = new ResourceManagerImpl(root.toURI(), config
					.toURI());
		}
		if (engine == null) {
			engine = new HotTemplateEngine(resourceManager,null);
		}
	}

	

	public void processDir(final File dir, final String path,final PathMatcher includes,final PathMatcher excludes) {
		log.info("process dir(处理目录)：" + dir.getAbsolutePath());
		dir.listFiles(new FileFilter() {
			public boolean accept(File file) {
				if (!file.equals(output)) {
					String name = file.getName();
					String path2 = path + name;
					if (file.isDirectory()) {
						path2 += '/';
						if (name.startsWith(".")) {
							log.debug("skip dir(跳过目录)：" + file);
						} else {
							if ((includes == null || includes.maybe(path2))
								&&(excludes == null || !excludes.must(path2))) {
								processDir(file, path2,includes,excludes);
							}else{
								log.debug("skip dir(跳过目录)：" + file);
							}
						}
					} else if ((includes == null && path2.endsWith(".xhtml")
								|| includes.match(path2))
							&& (excludes == null || !excludes.match(path2))) {
						try {
							processFile(path2);
						} catch (IOException e) {
							log.error("file process exception(文件处理异常):" + path2, e);
						}
					}
				}
				return false;
			}
		});
	}
	public static String buildPHP(String path, String litecode) {
		JSIRuntime runtime = ParseUtil.getJSIRuntime();
		Object translator = runtime
				.eval("new ($import('org.xidea.lite.impl.php:PHPTranslator',{}))('"
						+ path + "'," + litecode + ")");
		return (String) runtime.invoke(translator, "translate");
	}
	public boolean processFile(final String path) throws IOException {
		log.info("process file(处理文件)：" + path);
		if (this.resultMap.containsKey(path)) {
			return false;
		}
		if (resourceManager.isTemplate(path)) {
			try {
				String layout = resourceManager.getFeatureMap(path).get(
						ParseContext.FEATURE_LAYOUT);
				if (path.equals(layout)) {
					return false;
				}
				String litecodepath = "/WEB-INF/litecode/" + path.replace('/', '^');
				String result = engine.getLitecode(path);

				String encoding = resourceManager.getFeatureMap(path).get(
						LiteTemplate.FEATURE_ENCODING);
				// 中间代码永远是UTF-8；但是静态文本中大大字符还是要确保安全。
				this.resultMap.put(litecodepath, result.getBytes("utf-8"));
				if (this.translator != null) {
					if ("php".equals(translator)) {
//						LiteCompilerHelper.buildPHP(path, result);
						String code = buildPHP(path, result);
						resultMap.put(litecodepath + ".php",
								code.getBytes(encoding));
					}
				}

				return true;
			} catch (Exception e) {
				// JOptionPane.showConfirmDialog(null, e);
				log.error("process template exception(处理模板异常)：" + path, e);
				return false;
			}
		} else {
			Object data = resourceManager.getFilteredContent(path);
			if (data instanceof byte[]) {
				this.resultMap.put(path, (byte[]) data);
			} else {
				String text = resourceManager.getFilteredText(path);
				String encoding = resourceManager.getFeatureMap(path).get(
						LiteTemplate.FEATURE_ENCODING);
				this.resultMap.put(path, text.getBytes(encoding));
			}
			return true;
		}
	}

	public void setRoot(File webRoot) {
		this.root = webRoot;
	}

	public void setConfig(File config) {
		this.config = config;
	}

	public void setOutput(File litecode) {
		this.output = litecode;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setIncludes(String[] includes) {
		this.includes = includes;
	}

	public void setExcludes(String[] excludes) {
		this.excludes = excludes;
	}

	public void setLinked(boolean linked) {
		this.linked = linked;
	}

	public void setTranslator(String translator) {
		this.translator = translator;
	}

}
