package org.xidea.lite.tools;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.Template;
import org.xidea.lite.TemplateEngine;
import org.xidea.lite.parser.ParseContext;

public class LiteCompiler {
	private static final Log log = LogFactory.getLog(LiteCompiler.class);
	private File root;
	private File htmlcached;
	private File litecached;
	private String encoding = "utf-8";
	private TemplateCompilerEngine engine;

	public LiteCompiler(String[] args) {
		new CommandParser(args).setup(this);
	}

	public static void main(String[] args) {
		new LiteCompiler(args).execute();
	}

	public void execute() {
		engine = new TemplateCompilerEngine(root);
		litecached = createIfNotExist(litecached, "WEB-INF/litecached/");
		htmlcached = createIfNotExist(htmlcached, null);
		this.processDir(root, "/");
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
					log.error("编译Lite中间代码出错："+path, e);
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
					log.error("生成HTML 静态数据出错："+path, e);
				} finally {
					out.close();
				}
			}
		} catch (IOException e) {
			log.error("处理模板异常（可能是模板文件生成异常）："+path, e);
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


	public void setRoot(File webRoot) {
		this.root = webRoot;
	}

	public void setHtmlcached(File htmlcached) {
		this.htmlcached = htmlcached;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
}
class TemplateCompilerEngine extends TemplateEngine{

	private Map<String, List<Object>> itemsMap = new HashMap<String, List<Object>>();
	private Map<String, File[]> filesMap = new HashMap<String,File[]>();

	public TemplateCompilerEngine(File root) {
		super(root);
	}

	public String getCacheCode(String path){
		String root = super.webRoot.getAbsolutePath();
		if(root.endsWith("/") || root.endsWith("\\")){
			root = root.substring(0,root.length()-1);
		}
		ArrayList<String> filesList = new ArrayList<String>();
		for(File file : filesMap.get(path)){
			String item = file.getAbsolutePath();
			if(item.startsWith(root)){
				filesList.add(item);
			}
		}
		return JSONEncoder.encode(new Object[]{filesList,itemsMap.get(path)});
	}

	
	protected Template createTemplate(String path,ParseContext context) throws IOException {
		Template template = super.createTemplate(path, context);
		this.itemsMap.put(path,context.toList());
		this.filesMap.put(path,this.getAssociatedFiles(context));
		return template;
	}
	
}
