package org.xidea.lite.tools;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.HashMap;

import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.Template;
import org.xidea.lite.TemplateEngine;

public class Build {
	private File webRoot;
	private File htmlcached;
	private File litecached;
	private TemplateEngine engine;
	private String encoding = "utf-8";

	public Build(String[] args) {
		new CommandParser(args).setup(this);
	}

	public static void main(String[] args) {
		new Build(args).execute();
	}

	private void execute() {
		engine = new TemplateEngine(webRoot);
		litecached = createIfNotExist(litecached, "WEB-INF/litecached/");
		htmlcached = createIfNotExist(htmlcached, null);
		this.processDir(webRoot, "/");
	}

	private File createIfNotExist(File cached, String defaultPath) {
		if (cached == null && defaultPath != null) {
			cached = new File(webRoot, defaultPath);
		}
		if (cached != null) {
			if (!cached.exists()) {
				cached.mkdir();
			}
		}
		return cached;
	}

	public void processFile(final String path)
			throws UnsupportedEncodingException, FileNotFoundException,
			IOException {
		Template template = engine.getTemplate(path);
		{

			File cachedFile = new File(litecached, URLEncoder.encode(path,
					"utf-8"));
			Writer out = new OutputStreamWriter(
					new FileOutputStream(cachedFile), encoding);
			try {
				Field field = Template.class.getDeclaredField("items");
				field.setAccessible(true);
				out.write(JSONEncoder.encode(field.get(template)));
			} catch (Exception e) {
				e.printStackTrace();
			}finally{
				out.close();
			}
			
		}
		if (htmlcached != null) {
			File cachedFile = new File(htmlcached, path);
			cachedFile.getParentFile().mkdirs();
			Writer out = new OutputStreamWriter(
					new FileOutputStream(cachedFile), encoding);
			try {
				engine.render(path, new HashMap<String, String>(), out);
			}finally{
				out.close();
			}
		}
	}

	public void processDir(final File dir, final String path) {
		dir.listFiles(new FileFilter() {
			public boolean accept(File file) {
				if (!file.equals(htmlcached) && !file.equals(litecached)) {
					if (file.isDirectory()) {
						processDir(file, path + file.getName() + '/');
					} else if (file.getName().endsWith(".xhtml")) {
						try {
							processFile(path+file.getName());
						} catch (IOException e) {
							e.printStackTrace();
						}

					}
				}
				return false;
			}

		});

	}

	public File getWebRoot() {
		return webRoot;
	}

	public void setWebRoot(File webRoot) {
		this.webRoot = webRoot;
	}

	public File getHtmlcached() {
		return htmlcached;
	}

	public void setHtmlcached(File htmlcached) {
		this.htmlcached = htmlcached;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
}
