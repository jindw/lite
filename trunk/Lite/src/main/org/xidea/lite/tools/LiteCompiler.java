package org.xidea.lite.tools;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.lite.Template;

public class LiteCompiler {
	private static final Log log = LogFactory.getLog(LiteCompiler.class);
	private File webRoot;
	private String path;
	private String[] parsers;
	private String[] featrues;
	private File htmlcached;
	private File litecached;
	private String encoding = "utf-8";
	private boolean writeError = true;
	private TemplateCompilerEngine engine;

	public LiteCompiler(String[] args) {
		new CommandParser().setup(this, args);
	}

	public static void main(String[] args) {
		new LiteCompiler(args).execute();
	}

	public void execute() {
		try {
			initialize();
			if (path == null) {
				this.processDir(webRoot, "/");
			} else {
				this.processFile(path);
			}
		} catch (Exception e) {
			File file = new File(webRoot, "log.txt");
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

	protected void initialize() {
		HashMap<String, String> featrueMap = new HashMap<String, String>();
		if (featrues != null) {
			for (String f : featrues) {
				int p = f.indexOf("=");
				featrueMap.put(f.substring(0, p).trim(), f.substring(p + 1)
						.trim());
			}
		}
		engine = new TemplateCompilerEngine(webRoot, parsers, featrueMap);

		litecached = createIfNotExist(litecached, "WEB-INF/litecached/");
		htmlcached = createIfNotExist(htmlcached, null);
	}

	protected File createIfNotExist(File cached, String defaultPath) {
		if (cached == null && defaultPath != null) {
			cached = new File(webRoot, defaultPath);
		}
		if (cached != null) {
			if (!cached.exists()) {
				log.info("mkdirs:" + cached);
				cached.mkdirs();
			}
		}
		return cached;
	}

	public void processFile(final String path) {
		Template template = engine.getTemplate(path);
		try {
			{
				File cachedFile = new File(litecached, URLEncoder.encode(path,
						"utf-8"));
				Writer out = new OutputStreamWriter(new FileOutputStream(
						cachedFile), encoding);

				try {
					out.write(engine.getCacheCode(path));
				} catch (Exception e) {
					if(writeError){
					out.write(engine.toErrorCode(path,e));
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
					template.render(new HashMap<String, String>(), out);
				} catch (Exception e) {
					log.error("生成HTML 静态数据出错：" + path, e);
				} finally {
					out.close();
				}
			}
		} catch (IOException e) {
			log.error("处理模板异常（可能是模板文件生成异常）：" + path, e);
		}
	}

	public void processDir(final File dir, final String path) {
		dir.listFiles(new FileFilter() {
			public boolean accept(File file) {
				if (!file.equals(htmlcached) && !file.equals(litecached)) {
					if (file.isDirectory()) {
						processDir(file, path + file.getName() + '/');
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

	public void setWebRoot(File webRoot) {
		this.webRoot = webRoot;
	}

	public void setHtmlcached(File htmlcached) {
		this.htmlcached = htmlcached;
	}

	public void setLitecached(File litecached) {
		this.litecached = litecached;
	}

	public void setFeatrues(String[] featrues) {
		this.featrues = featrues;
	}

	public void setParsers(String[] additional) {
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
