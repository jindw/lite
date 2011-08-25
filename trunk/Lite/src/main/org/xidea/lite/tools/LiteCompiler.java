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
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.impl.HotTemplateEngine;
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
		log.info("Args:" + JSONEncoder.encode(args));
		CommandParser cp = new CommandParser(args);
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
				this.processDir(root, "/");
				for (String path : this.resourceManager.getLinkedResources()) {
					this.processFile(path);
				}
				if (linked) {
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
			log.info("执行成功");
		} catch (Exception e) {
			log.error("编译失败", e);
		}
	}

	protected void initialize() throws IOException {
		if (root == null) {
			root = new File("./").getAbsoluteFile();
		}

		if (output == null) {
			log.error("必须指定 -output 参数！！");
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
			engine = new HotTemplateEngine(resourceManager);
		}
	}

	

	public void processDir(final File dir, final String path) {
		log.info("处理目录：" + dir.getAbsolutePath());
		final PathMatcher includes = PathMatcher.createMatcher(this.includes);
		final PathMatcher excludes = PathMatcher.createMatcher(this.excludes);

		dir.listFiles(new FileFilter() {
			public boolean accept(File file) {
				if (!file.equals(output)) {
					String path2 = path + file.getName();
					if (file.isDirectory()) {
						if (file.getName().startsWith(".")) {
							log.warn("跳过目录：" + file);
						} else {
							if (excludes == null || !excludes.must(path)) {
								processDir(file, path2 + '/');
							}
						}
					} else if ((includes == null || includes.match(path2))
							&& (excludes == null || !excludes.match(path2))) {
						try {
							processFile(path2);
						} catch (IOException e) {
							log.error("文件处理异常:" + path2, e);
						}
					}
				}
				return false;
			}
		});
	}

	public boolean processFile(final String path) throws IOException {
		log.info("处理文件：" + path);
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
				String path2 = LiteCompilerHelper.translatePath(path);
				String result = engine.getLiteCode(path);

				String encoding = resourceManager.getFeatureMap(path).get(
						ParseContext.FEATURE_ENCODING);
				// 中间代码永远是UTF-8；但是静态文本中大大字符还是要确保安全。
				this.resultMap.put(path2, result.getBytes("utf-8"));
				if (this.translator != null) {
					if ("php".equals(translator)) {
						LiteCompilerHelper.buildPHP(path, result, encoding,
								this.resultMap);
					}
				}

				return true;
			} catch (Exception e) {
				// JOptionPane.showConfirmDialog(null, e);
				log.error("处理模板异常：" + path, e);
				return false;
			}
		} else {
			Object data = resourceManager.getFilteredContent(path);
			if (data instanceof byte[]) {
				this.resultMap.put(path, (byte[]) data);
			} else {
				String text = resourceManager.getFilteredText(path);
				String encoding = resourceManager.getFeatureMap(path).get(
						ParseContext.FEATURE_ENCODING);
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
