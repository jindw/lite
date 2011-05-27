package org.xidea.lite.tools;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.impl.CommandParser;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.impl.HotTemplateEngine;
import org.xidea.lite.impl.ParseConfigImpl;
import org.xidea.lite.parse.ParseConfig;
import org.xidea.lite.parse.ParseContext;

public class LiteCompiler {
	private static final Log log = LogFactory.getLog(LiteCompiler.class);
	private File root;
	private File config;
	private String path;
	private File result;
	private File litecode;
	private HotTemplateEngine engine;
	private ParseConfig parseConfig;

	public LiteCompiler(String[] args) {
		log.info("Args:"+JSONEncoder.encode(args));
		CommandParser cp = new CommandParser(args);
		cp.setup(this);
	}

	public static void main(String[] args) {
		if(args == null || args .length == 0){
			args = new String[]{
					//"-root","D:\\workspace\\FileServer/src/main/org/jside/filemanager/","-litecode","D:\\workspace\\FileServer/build/dest/lite","-nodeParsers","org.xidea.lite.parser.impl.HTMLNodeParser"
					"-root","D:\\workspace\\FileServer/src/main/org/jside/filemanager/","-litecode","D:\\workspace\\FileServer/build/dest/lite"
			};
		}
		new LiteCompiler(args).execute();
	}

	public void execute() {
		try {
			initialize();
			if (path == null) {
				this.processDir(root, "/");
			} else {
				this.processFile(path);
			}
			log.info("执行成功");
		} catch (Exception e) {
			log.error("编译失败",e);
		}
	}

	protected void initialize() throws IOException {
		if(root == null){
			root = new File(".");
		}
		if(config == null){
			config = new File(root,"/WEB-INF/lite.xml");
		}
		this.parseConfig = new ParseConfigImpl(root.toURI(),config.exists()?config.toURI():null);
		engine = new HotTemplateEngine(parseConfig);

		litecode = createIfNotExist(litecode, "WEB-INF/litecode/");
		result = createIfNotExist(result, null);
	}

	protected File createIfNotExist(File cached, String defaultPath) {
		if (cached == null && defaultPath != null) {
			cached = new File(root, defaultPath);
		}
		if (cached != null) {
			if (!cached.exists()) {
				log.info("mkdirs:" + cached);
				cached.mkdirs();
			}
		}
		return cached;
	}

	public boolean processFile(final String path) {
		log.info("处理文件："+path);
		try {
			Map<String, String> fm = parseConfig.getFeatureMap(path);
			String encoding =  fm.get(ParseContext.FEATURE_ENCODING);
			{
				File cachedFile = new File(litecode, path);
				cachedFile.getParentFile().mkdirs();
				Writer out = new OutputStreamWriter(new FileOutputStream(
						cachedFile),encoding);
				try {
					out.write(engine.getLiteCode(path));
					log.info("Lite文件写入成功:"+cachedFile);
				} catch (Throwable e) {
					log.error("编译Lite中间代码出错：" + path, e);
				} finally {
					out.close();
				}
			}
			if (result != null) {
				File cachedFile = new File(result, path);
				cachedFile.getParentFile().mkdirs();
				Writer out = new OutputStreamWriter(new FileOutputStream(
						cachedFile), encoding);
				try {
					engine.render(path, new HashMap<String, String>(), out);
				} catch (Exception e) {
					log.error("生成HTML 静态数据出错：" + path, e);
				} finally {
					out.close();
				}
			}
			return true;
		} catch (Exception e) {
			//JOptionPane.showConfirmDialog(null, e);
			log.error("处理模板异常（可能是模板文件生成异常）：" + path, e);
			return false;
		}
	}

	public void processDir(final File dir, final String path) {
		log.info("处理目录："+dir+path);
		dir.listFiles(new FileFilter() {
			public boolean accept(File file) {
				if (!file.equals(result) && !file.equals(litecode)) {
					if (file.isDirectory()) {
						if(file.getName().startsWith(".")){
						   log.warn("跳过目录："+file);
						}else{
							processDir(file, path + file.getName() + '/');
						}
					} else if (isXhtmlFile(file)) {
						processFile(path + file.getName());
					}
				}
				return false;
			}
		});
	}

	protected boolean isXhtmlFile(File file) {
		return file.getName().endsWith(".xhtml");
	}

	public void setRoot(File webRoot) {
		this.root = webRoot;
	}

	public void setConfig(File config) {
		this.config = config;
	}

	public void setResult(File htmlresult) {
		this.result = htmlresult;
	}

	public void setLitecode(File litecode) {
		this.litecode = litecode;
	}

	public void setPath(String path) {
		this.path = path;
	}
}
