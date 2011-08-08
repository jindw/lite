package org.xidea.lite.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jside.webserver.action.URLMatcher;
import org.w3c.dom.Document;
import org.xidea.jsi.JSIRuntime;
import org.xidea.jsi.impl.RuntimeSupport;
import org.xidea.lite.impl.ParseConfigImpl;
import org.xidea.lite.impl.ParseUtil;
import org.xml.sax.SAXException;

public class ResourceManagerImpl extends ParseConfigImpl implements
		ResourceManager {
	private static Log log = LogFactory.getLog(ResourceManagerImpl.class);
	private final File root;
	private ArrayList<MatcherFilter<byte[]>> streamFilters = new ArrayList<MatcherFilter<byte[]>>();

	private ArrayList<MatcherFilter<String>> stringFilters = new ArrayList<MatcherFilter<String>>();
	private ArrayList<MatcherFilter<Document>> documentFilters = new ArrayList<MatcherFilter<Document>>();
	private final Map<String, ResourceItem> cached = new HashMap<String, ResourceItem>();
	private ThreadLocal<ResourceItem> currentItem = new ThreadLocal<ResourceItem>();
	private JSIRuntime jsr = RuntimeSupport.create();
	private URI currentScript;
	private List<File> scripts = new ArrayList<File>();

	public ResourceManagerImpl(URI root, URI config) throws IOException {
		super(root, config);
		this.root = new File(root);
		this.currentScript = root;
		this.jsr.eval("var resourceManager=1;");
		Object initfn = this.jsr.eval("(function(rm){resourceManager = rm;})");
		this.jsr.invoke(this, initfn, this);
		this.jsr.eval(ResourceManagerImpl.class.getResource("env.s.js"));
		this.initialize();
	}
	protected void initialize() throws IOException {
		String defaultInit = "/WEB-INF/initialize.s.js";
		File initFile = new File(this.root,defaultInit);
		if(initFile.exists()){
			include(defaultInit);
		}else{
			defaultInit = "/initialize.s.js";
			initFile = new File(this.root,defaultInit);
			if(initFile.exists()){
				include(defaultInit);
			}else{
				include("classpath:///org/xidea/lite/tools/initialize.s.js");
			}
		}
	}
	public void include(String path) throws IOException{
		URI u;
		if(path.startsWith("classpath:")){
			u = URI.create(path);
		}else{
			if(path.charAt(0) == '/'){
				path = path.substring(1);
			}
			u = this.currentScript.resolve(path);
			if("file".equals(u.getScheme())){
				this.scripts .add(new File(u));
			}
		}
		URI oldScript = this.currentScript;
		try{
			this.currentScript = u;
			jsr.eval(ParseUtil.loadTextAndClose(ParseUtil.openStream(u),"utf-8"),u.toString());
		}finally{
			currentScript = oldScript;
		}
	}

	public List<File> getScriptFileList(){
		return scripts;
	}
	public void addBytesFilter(String pattern, ResourceFilter<byte[]> filter) {
		streamFilters.add(new MatcherFilter<byte[]>(pattern, filter));
	}

	public void addTextFilter(String pattern, ResourceFilter<String> filter) {
		stringFilters.add(new MatcherFilter<String>(pattern, filter));
	}

	public void addDocumentFilter(String pattern, ResourceFilter<Document> filter) {
		documentFilters.add(new MatcherFilter<Document>(pattern, filter));
	}

	public void addRelation(String relationPath) {
		ResourceItem current = currentItem.get();
		if (current == null) {
			log.error("addRelation 不能在插件外调用。否则无法找到被添加依赖的源路径。");
		} else {
			current.relations.add(relationPath);
		}
	}

	public String getEncoding(String path) {
//		Group group = this.find(path, false);
//		String encoding = this.getFeatureMap(path).get(ParseContext.FEATURE_ENCODING);
//		if(group != null){
//			return encoding;
//		}
		return resource(path).encoding;
	}

	public byte[] getRawBytes(String path) throws IOException {
		InputStream in = ParseUtil.openStream(this.getRoot().resolve(
				path.substring(1)));
		if(in == null){
			log.warn("Unknow file:"+path);
		}
		return loadAndClose(in);
	}

	public byte[] getFilteredBytes(String path) throws IOException {
		try {
			return getFilteredContent(path, byte[].class, null).data;// ,streamFilters,stringFilters,documentFilters);
		} catch (SAXException e) {
			throw new IOException(e);
		}
	}

	public String getFilteredText(String path) throws IOException {
		try {
			ResourceItem item = getFilteredContent(path, String.class, null);
			String text = item.text;// ,streamFilters,stringFilters,documentFilters);
			if (text == null) {
				text = loadText(item,item.data);
			}
			item.text = text;
			return text;
		} catch (SAXException e) {
			throw new IOException(e);
		}
	}

	private String loadText(ResourceItem item,byte[] data) throws IOException {
		String[] rtCharset = new String[1];
		String text = ParseUtil.loadTextAndClose(new ByteArrayInputStream(
				data),rtCharset,item.encoding);
		item.encoding = rtCharset[0];
		return text;
	}

	public Document getFilteredDocument(String path) throws IOException,
			SAXException {
		return getFilteredContent(path, Document.class, null).dom;// ,streamFilters,stringFilters,documentFilters);
	}
	public Document loadXML(URI uri) throws IOException, SAXException{
		if("lite".equals(uri.getScheme())){
			Document doc = getFilteredDocument(uri.getPath());
			Document doc2 = (Document) doc.cloneNode(true);
			doc2.setDocumentURI(doc.getDocumentURI());
			return doc2;
		}else{
			return super.loadXML(uri);
		}
	}

	public Object getFilteredContent(String path) throws IOException {
		try {
			ResourceItem item = getFilteredContent(path, String.class, null);
			String text = item.text;// ,streamFilters,stringFilters,documentFilters);
			if (text == null) {
				return item.data;
			} else {
				return text;
			}
		} catch (SAXException e) {
			throw new IOException(e);
		}
	}

	public String loadChainText(String path) throws IOException {
		ResourceItem item = currentItem.get();
		if (item != null) {
			try {
				ResourceItem item2 = getFilteredContent(path, String.class, item.currentFilter);
				Object data = item2.currentData;
				if(data instanceof byte[]){
					return loadText(item2,(byte[])data);
				}else{
					return (String)data;
				}
			} catch (SAXException e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	/**
	 * 注意: getFilteredContent(path,String.class):
	 * 如果text没有被getFilterdDocument初始化，也没有相关的文本filter,返回null
	 * 但是：getFilterdText，能有效返回数据
	 * 
	 * @param <T>
	 * @param path
	 * @param type
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 */
	private <T> ResourceItem getFilteredContent(String path, Class<T> type,
			Object endFilter
	// ,List<FilterPlugin<byte[]>> streamFilters,
	// List<FilterPlugin<String>> stringFilters,
	// List<FilterPlugin<Document>> documentFilters
	) throws IOException, SAXException {
		ResourceItem res = resource(path);
		ResourceItem old = currentItem.get();
		synchronized (res) {
			res.currentData = null;
			try {
				currentItem.set(res);
				byte[] data = null;
				if (res.data == null || endFilter !=null) {// byte[]
					data = getRawBytes(path);
					for (MatcherFilter<byte[]> filter : streamFilters) {
						if(endFilter == filter){
							res.currentData = data;
							return res;
						}
						if (filter.match(path)) {
							data = filter.doFilter(path, data);
						}
					}
					if(endFilter == null){
						res.data = data;
					}
				}
				if (type == byte[].class && endFilter == null) {
					return res;
				}else if(data == null){
					data = res.data;
				}

				if (type == String.class) {
					if (res.text == null || endFilter != null) {// string
						String text = null;
						for (MatcherFilter<String> filter : stringFilters) {
							if(endFilter == filter){
								break;
							}
							if (filter.match(path)) {
								if (text == null) {
									text = loadText(res,res.data);
								}
								text = filter.doFilter(path, text);
							}
						}
						if(endFilter == null){
							res.text = text;
						}else{
							if (text == null) {
								text = loadText(res,data);
							}
							res.currentData = text;
						}
					}
					return res;
				} else {
					if (res.dom == null) {
						String text = ParseUtil
								.loadXMLTextAndClose(new ByteArrayInputStream(
										res.data));
						// check xml instruction utf-8
						for (MatcherFilter<String> filter : stringFilters) {
							if (filter.match(path)) {
								text = filter.doFilter(path, text);
							}
						}
						if (res.text == null) {
							res.text = text;
						}
						// 没有默认的xml正规化
						Document doc = ParseUtil.loadXMLBySource(text,
								"lite:///" + path);
						for (ResourceFilter<Document> filter : documentFilters) {
							doc = filter.doFilter(path, doc);
						}
						res.dom = doc;
					}
					return res;
				}
			} finally {
				currentItem.set(old);
				res.lastModified = System.currentTimeMillis();
			}
		}
	}

	private ResourceItem resource(String path) {
		ResourceItem item = cached.get(path);
		if (item != null) {
			long lastModified = new File(root, path).lastModified();
			for (String path2 : item.relations) {
				File f = new File(root, path2);
				if (!f.exists()) {
					item = null;
					break;
				}
				lastModified = Math.max(lastModified, f.lastModified());
			}
			if (item != null && lastModified > item.lastModified) {
				item = null;
			}
		}
		if (item == null) {
			item = new ResourceItem();
			item.path = path;
			cached.put(path, item);
		}
		return item;
	}

	private byte[] loadAndClose(InputStream in) throws IOException {
		byte[] data = new byte[1024];
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int b;
		while ((b = in.read(data)) >= 0) {
			out.write(data, 0, b);
		}
		in.close();
		return out.toByteArray();
	}

	private class MatcherFilter<T> implements ResourceFilter<T> {
		ResourceFilter<T> base;
		private URLMatcher matcher;

		MatcherFilter(String pattern, ResourceFilter<T> base) {
			this.matcher = URLMatcher.createMatcher(pattern);
			this.base = base;
		}

		public boolean match(String url) {
			return matcher.match(url);
		}

		public T doFilter(String path, T in) {
			currentItem.get().currentFilter = this;
			return base.doFilter(path, in);
		}

		// public ResourceManager getFactory() {
		// return base.getFactory();
		// }

	}

	private static class ResourceItem {
		public Object currentFilter;
		ArrayList<String> relations = new ArrayList<String>();
		// List<FilterPlugin<byte[]>> streamFilters = new
		// ArrayList<FilterPlugin<byte[]>>();
		// List<FilterPlugin<String>> stringFilters = new
		// ArrayList<FilterPlugin<String>>();
		// List<FilterPlugin<Document>> documentFilters = new
		// ArrayList<FilterPlugin<Document>>();
		String path;
		String encoding;
		byte[] data;
		String text;
		Document dom;
		long lastModified;
		String hash;
		transient Object currentData;
	}

	public String getContentHash(String path) {
		try {
			ResourceItem item = getFilteredContent(path, String.class, null);
			if (item.hash == null) {
				hash(item.data);
				String text = item.text;
				if (text == null) {
					item.hash = hash(item.data);
				} else {
					item.hash = hash(text.getBytes());
				}
			}
			return item.hash;
		} catch (Exception e) {
			log.warn("计算hash值失败：" + path);
		}

		return null;
	}

	private static String hash(byte[] in) throws NoSuchAlgorithmException,
			IOException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.reset();
		md.update(in);
		long sum = 0;
		byte[] bs = md.digest();
		for (int i = 0; i < bs.length; i++) {
			int b = (0xFF & bs[i]) << (i % 8 * 8);
			sum += b;
		}
		String hash = Long.toString(Math.abs(sum), Character.MAX_RADIX);
		return hash;
	}

	public void saveText(String path, Object content)
			throws IOException {
		File dest = new File(this.root, path);
		FileOutputStream out = new FileOutputStream(dest);
		if (content instanceof byte[]) {
			out.write((byte[]) content);
		} else if (content instanceof InputStream) {
			out.write(loadAndClose((InputStream) content));
		} else {
			out.write(String.valueOf(content).getBytes(getEncoding(path)));
		}
		out.close();
	}
	public Object createFilterProxy(Object object){
		return jsr.wrapToJava(object, ResourceFilter.class);
	}
}
