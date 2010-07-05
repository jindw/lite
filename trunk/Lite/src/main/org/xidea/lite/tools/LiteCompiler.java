package org.xidea.lite.tools;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.impl.CommandParser;
import org.xidea.el.impl.Convertor;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.Template;
import org.xidea.lite.parse.NodeParser;

public class LiteCompiler {
	private static final Log log = LogFactory.getLog(LiteCompiler.class);
	private File root;
	private String path;
	private NodeParser<Object>[] parsers;
	private Map<String,String> featrueMap = new HashMap<String, String>();
	private File htmlcached;
	private File litecached;
	private String encoding = "utf-8";
	private boolean writeError = true;
	private TemplateCompilerEngine engine;

	public LiteCompiler(String[] args) {
		System.out.println(JSONEncoder.encode(args));
		//args = new String[]{"-root","C:\\Users\\jindw\\workspace\\android-server/res","-featrueMap['http://www.xidea.org/ns/lite/autoform']","form","-litecached","C:\\Users\\jindw\\workspace\\android-server/src/org/jside/android/web/"};
		CommandParser cp = new CommandParser(args);
		cp.addConvertor(NodeParser.class,Convertor.Default.INSTANCE);
		cp.setup(this);
	}

	public static void main(String[] args) {
		if(args == null || args .length == 0){
			args = new String[]{"-root","D:\\workspace\\FileServer/src/main/org/jside/filemanager/","-litecached","D:\\workspace\\FileServer/build/dest/lite","-nodeParsers","org.xidea.lite.parser.impl.HTMLNodeParser"};
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
		} catch (Exception e) {
			log.error(e);
			File file = new File(root, "log.txt");
			try {
				if (!file.exists()) {
					file.createNewFile();
				}
				PrintWriter out = new PrintWriter(new FileOutputStream(file));
				e.printStackTrace(out);
				out.flush();
				out.close();
			} catch (IOException ex) {
				log.error(ex);
			}

		}
	}

	protected void initialize() throws IOException {
		if(root == null){
			root = new File(".");
		}
		engine = new TemplateCompilerEngine(root, parsers, featrueMap);

		litecached = createIfNotExist(litecached, "WEB-INF/litecached/");
		htmlcached = createIfNotExist(htmlcached, null);
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
			{
				File cachedFile = new File(litecached, path.replace('/', '^'));
				Writer out = new OutputStreamWriter(new FileOutputStream(
						cachedFile), encoding);
				try {
					out.write(engine.getLiteCode(path));
					log.info("Lite文件写入成功:"+cachedFile);
				} catch (Throwable e) {
					if(writeError){
						out.write(engine.buildLiteCode(path,e.getMessage()));
					}
					log.error("编译Lite中间代码出错：" + path, e);
				} finally {
					out.close();
				}
			}
			if (htmlcached != null) {
				File cachedFile = new File(htmlcached, path);
				cachedFile.getParentFile().mkdirs();
				Writer out = new OutputStreamWriter(new FileOutputStream(
						cachedFile), encoding);
				try {
					Template template = engine.getTemplate(path);
					template.render(new HashMap<String, String>(), out);
				} catch (Exception e) {
					if(writeError){
						out.write(engine.buildLiteCode(path,e));
					}
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
		log.info("处理目录："+dir+","+path);
		dir.listFiles(new FileFilter() {
			public boolean accept(File file) {
				if (!file.equals(htmlcached) && !file.equals(litecached)) {
					if (file.isDirectory()) {
						if(file.getName().startsWith(".")){
						   log.warn("跳过目录："+file);
						}else{
							processDir(file, path + file.getName() + '/');
						}
					} else if (isTemplateFile(file)) {
						processFile(path + file.getName());
					}
				}
				return false;
			}
		});
	}

	public boolean isTemplateFile(File file) {
		return file.getName().endsWith(".xhtml");
	}

	public void setRoot(File webRoot) {
		this.root = webRoot;
	}

	public void setHtmlcached(File htmlcached) {
		this.htmlcached = htmlcached;
	}

	public void setLitecached(File litecached) {
		this.litecached = litecached;
	}

	public Map<String, String> getFeatrueMap() {
		return this.featrueMap;
	}
	public void setFeatrueMap(Map<String, String>featrueMap) {
		this.featrueMap = featrueMap;
	}

	public void setNodeParsers(NodeParser<Object>[] additional) {
		this.parsers = additional;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public void setWriteError(boolean writeError) {
		this.writeError = writeError;
	}

	public void setPath(String path) {
		this.path = path;
	}
}
