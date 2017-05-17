package org.xidea.lite;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;






import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.json.JSONDecoder;
import org.xidea.lite.LiteTemplate;
import org.xidea.lite.LiteEngine;
import org.xidea.lite.Template;

public class HotLiteEngine extends LiteEngine {
	private static final Log log = LogFactory.getLog(HotLiteEngine.class);
	private HashMap<String, Object> lock = new HashMap<String, Object>();
	private HashMap<String, Info> infoMap = new HashMap<String, Info>();
	private LiteCompiler compiler;
	private File cached;
	private File root;

	public HotLiteEngine(File root, File cached) {
		super(cached==null?null:cached.toURI());
		this.root = root;
		this.cached = cached;
		try {
			this.compiler = new LiteCompiler(root);
        } catch (FileNotFoundException e) {
            log.error(e.getMessage());
            throw new LinkageError(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
		}
	}

	public File getRoot(){
		return root;
	}
	public Template getTemplate(String path) throws IOException {
		Template template = (Template) templateMap.get(path);
		if (template == null || isModified(path)) {
			Object lock2 = null;
			synchronized (lock) {
				lock2 = lock.get(path);
				if (lock2 == null) {
					lock.put(path, lock2 = new Object());
				}
			}
			synchronized (lock2) {
				template = (Template) templateMap.get(path);
				if (template == null || isModified(path)) {
					template = createTemplate(path);
					templateMap.put(path, template);
				}
			}
			lock.remove(path);
			return template;
		} else {
			return template;
		}
	}

	protected boolean isModified(String path) {
		Info templateEntry = (Info) infoMap.get(path);
		return templateEntry == null || templateEntry.isModified();
	}

	private File saveCache(String path, String litecode) {
		//System.out.println("save cached:"+(cached != null && cached.exists())+cached);
		if (cached != null && cached.exists() && cached.canWrite()) {
			try {
				File file = getLiteFile(path);
				file.getParentFile().mkdirs();
				//System.out.println(file);
				OutputStreamWriter out = new OutputStreamWriter(
						new FileOutputStream(file), "UTF-8");
				out.write(litecode);
				out.close();
				return file;
			} catch (Exception e) {
				if (log.isDebugEnabled()) {
					log.debug("complie result(" + path + ") save failed", e);
				}
			}
		}
		return null;
	}

	private File getLiteFile(String path) {
		//return new File(cached, path);
		//path.slice(1).replace(/[^\w\_]/g,'_');
		return new File(cached, path.substring(1).replaceAll("[^\\w\\_]", "_"));
	}

	@Override
	protected Template createTemplate(String path) throws IOException {
		try {
			String litecode = null;
			Info entry = infoMap.get(path);
			if (entry == null ||entry.isModified()) {
				if (cached != null && cached.exists() ) {
					File liteFile = getLiteFile(path);
					litecode = loadText(new FileInputStream(liteFile));
					if (litecode != null) {
						entry = new Info(root, liteFile,litecode);
						if (entry.isModified()) {
							litecode = null;
						} else {
							//log.info("载入成功！ 文件关联：" + path + ":\t"+ Arrays.asList(entry.files));
							infoMap.put(path, entry);
						}
					}
				}
				if (litecode == null) {
                    long time = System.currentTimeMillis();
					litecode = compiler.compile(path);
                    long passed = System.currentTimeMillis() - time;
					if (litecode != null) {
						File liteFile = saveCache(path, litecode);
						entry = new Info(root, liteFile,litecode);
						log.info("编译成功！耗时："+passed/1000.0+"妙； 文件关联：" + path + ":\t"
								+ Arrays.asList(entry.files));
						infoMap.put(path, entry);
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Info entry = infoMap.get(path);
		return new LiteTemplate(executorService,entry.items, entry.config);
	}

	static class Info {
		File[] files;
		long lastModified;
		List<Object> items;
		Map<String, String> config;
		private File liteFile;

		Info(File root, File liteFile,String litecode) {
			//long t1 = System.currentTimeMillis();
			List<Object> data = JSONDecoder.decode(litecode);
			@SuppressWarnings("unchecked")
			List<String> resources = (List<String>) data.get(0);
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>) data.get(1);
			@SuppressWarnings("unchecked")
			Map<String, String> config = (Map<String, String>) data.get(2);
			this.items = list;
			this.config = config;

			int len = resources.size();
			this.files = new File[len];
			this.liteFile = liteFile;
			for (int i = 0; i < len; i++) {
				files[i] = new File(root, resources.get(i).substring(1));
			}
			this.lastModified = getModifiedToken(this.files);
			//System.out.println(System.currentTimeMillis()-t1);
		}



		boolean isModified() {
			long token = getModifiedToken(files);
			if (liteFile!=null) {
				long lm = token;
				if(lm<0){
					for (File file : files) {
						lm = Math.max(lm,file.lastModified());
					}
				}
				if(lm > liteFile.lastModified()){
					return true;
				}
			}
			return this.lastModified != token;
		}

		long getModifiedToken(File[] files) {
			long i = 0;
			long j = 0;
			for (File file : files) {
				long k = file.lastModified();
				//System.out.println(k+file.getName());
				if (k == 0) {
					j++;
				}
				j *= 2;
				i = Math.max(k, i);
			}
			//System.out.println(i+"/"+j);
			i+=j;
			return j>0?-i:i;
		}
	}

}
