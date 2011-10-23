package org.xidea.lite.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xidea.jsi.JSIRuntime;
import org.xidea.jsi.impl.RuntimeSupport;
import org.xidea.lite.LiteTemplate;
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
	private ArrayList<String> linkedResources = new ArrayList<String>();

	private final Map<String, ResourceItem> cached = new HashMap<String, ResourceItem>();
	private ThreadLocal<ResourceItem> currentItem = new ThreadLocal<ResourceItem>();
	private JSIRuntime jsr = RuntimeSupport.create();
	// private URI currentScript;
	private List<File> scripts = new ArrayList<File>();

	public ResourceManagerImpl(URI root, URI config) throws IOException {
		super(root, config);
		this.root = new File(root);
		// this.currentScript = root;
		this.jsr
				.eval("$import('org.xidea.jsidoc.util:JSON');console=$import('org.xidea.jsi:$log');");
		this.jsr.eval("var resourceManager=1;");
		Object initfn = this.jsr.eval("(function(rm){resourceManager = rm;})");
		this.jsr.invoke(this, initfn, this);
		this.jsr.eval(ResourceManagerImpl.class
				.getResource("internal/module.js"));
		this.initialize();
	}

	protected void initialize() throws IOException {
		String defaultInit = "/WEB-INF/setup";
		File initFile = new File(this.root, defaultInit);
		if (!initFile.exists()) {
			defaultInit = "/setup";
			initFile = new File(this.root, defaultInit);
			if (!initFile.exists()) {
				defaultInit = ("org/xidea/lite/tools/internal/setup");
			}
		}

		this.jsr.eval("require('" + defaultInit + "')");
	}

	public Object eval(String code, String path) {
		return this.jsr.eval(code, path);
	}

	public String loadScript(String absPath) throws IOException {
		InputStream in;
		File file = new File(this.root, absPath);
		if (file.exists()) {
			in = new FileInputStream(file);
			this.scripts.add(file);
		} else {
			in = this.getClass().getClassLoader().getResourceAsStream(absPath);
			if (in == null) {
				return null;
			}
		}
		return ParseUtil.loadTextAndClose(in, "utf-8");
	}

	public List<File> getScriptFileList() {
		return scripts;
	}

	public void addBytesFilter(String pattern, ResourceFilter<byte[]> filter) {
		streamFilters.add(new MatcherFilter<byte[]>(pattern, filter));
	}

	public void addTextFilter(String pattern, ResourceFilter<String> filter) {
		stringFilters.add(new MatcherFilter<String>(pattern, filter));
	}

	public void addDocumentFilter(String pattern,
			ResourceFilter<Document> filter) {
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

	public String getSourceEncoding(String path) throws IOException {
		File file = getFile(path);
		String encoding = null;
		if (file.exists()) {
			this.getFilteredText(path);
			encoding = resource(path).encoding;
		}
		return encoding;
	}

	private File getFile(String path) {
		if (path.charAt(0) == '/') {
			return new File(root, path.substring(1));
		} else {
			throw new IllegalArgumentException(
					"resource path must be start with '/' you gave:"+path);
		}
	}

	public byte[] getRawBytes(String path) throws IOException {
		File file = getFile(path);
		if (file.exists()) {
			return loadAndClose(new FileInputStream(file));
		} else {
			log.warn("Unknow file:" + path);
			throw new FileNotFoundException(file + "  Not Found");
		}

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
				text = loadText(item, item.data);
			}
			item.text = text;
			return text;
		} catch (SAXException e) {
			throw new IOException(e);
		}
	}

	private String loadText(ResourceItem item, byte[] data) throws IOException {
		String[] rtCharset = new String[1];
		String path = item.path;
		boolean isXML = containsFilter(path, documentFilters);
		InputStream in = new ByteArrayInputStream(data);
		final String text = ParseUtil.loadTextAndClose(in, rtCharset, isXML,
				item.encoding);
		item.encoding = rtCharset[0];
		return text;
	}

	private <T> boolean containsFilter(String path,
			List<MatcherFilter<T>> documentFilters) {
		boolean isXML = false;
		for (MatcherFilter<T> filter : documentFilters) {
			if (filter.match(path)) {
				isXML = true;
				break;
			}
		}
		return isXML;
	}

	public Document getFilteredDocument(String path) throws IOException,
			SAXException {
		return getFilteredContent(path, Document.class, null).dom;// ,streamFilters,stringFilters,documentFilters);
	}

	public Document loadXML(URI uri) throws IOException, SAXException {
		if ("lite".equals(uri.getScheme())) {
			Document doc = getFilteredDocument(uri.getPath());
			Document doc2 = (Document) doc.cloneNode(true);
			doc2.setDocumentURI(doc.getDocumentURI());
			return doc2;
		} else {
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
				ResourceItem item2 = getFilteredContent(path, String.class,
						item.currentFilter);
				Object data = item2.currentData;
				if (data instanceof byte[]) {
					return loadText(item2, (byte[]) data);
				} else {
					return (String) data;
				}
			} catch (SAXException e) {
				throw new RuntimeException(e);
			}
		} else {
			log.error("loadChainText must called in filter plugin context!!");
		}
		return null;
	}

	/**
	 * 注意: getFilteredContent(path,String.class,breakFilter):
	 * 如果text没有被getFilterdDocument初始化，也没有相关的文本filter,返回null
	 * 但是：getFilterdText，能有效返回数据
	 * 
	 * @param <T>
	 * @param path
	 * @param type
	 * @param lastFilter
	 *            not include
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 */
	private <T> ResourceItem getFilteredContent(String path, Class<T> type,
			Object lastFilter
	// ,List<FilterPlugin<byte[]>> streamFilters,
	// List<FilterPlugin<String>> stringFilters,
	// List<FilterPlugin<Document>> documentFilters
	) throws IOException, SAXException {
		ResourceItem res = resource(path);
		ResourceItem old = currentItem.get();
		if (old == res) {// 不能两次进入同一个资源，否则会死锁
			// TODO: 逻辑还有问题
			return res;
		}
		synchronized (res) {
			File file = getFile(path);
			try {
				currentItem.set(res);
				{
					byte[] data = null;
					if (file.exists()) {
						data = getRawBytes(path);
					}
					res.currentData = data;
					byte[] oldData = res.data;
					data = doFilter(res, lastFilter, streamFilters, data,
							oldData);
					if (data == null) {
						return res;// filter result
					}
					res.data = data;
					if (type == byte[].class) {
						return res;
					}
				}
				String text = loadText(res, res.data);
				{
					String oldText = res.text;
					text = doFilter(res, lastFilter, stringFilters, text,
							oldText);
					if (text == null) {
						return res;// filter result
					}
					res.text = text;
					if (type == String.class) {
						return res;
					}
				}
				{
					if (res.dom == null) {// no lastFilter
						// 没有默认的xml正规化
						Document doc = ParseUtil.loadXMLBySource(text, "lite:"
								+ path);
						for (MatcherFilter<Document> filter : documentFilters) {
							try {
								doc = filter.doFilter(path, doc);
							} catch (RuntimeException e) {
								log.error("filter error:" + filter);
								throw e;
							}
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

	private <T> T doFilter(ResourceItem res, Object lastFilter,
			List<MatcherFilter<T>> streamFilters, T data, T oldData)
			throws FileNotFoundException {
		lastFilter = getFilter(lastFilter, streamFilters);
		String path = res.path;
		if (oldData == null || lastFilter != null) {
			for (MatcherFilter<T> filter : streamFilters) {
				if (filter == lastFilter) {
					break;
				}
				if (filter.match(path)) {
					try {
						data = filter.doFilter(path, data);
					} catch (RuntimeException e) {
						log.error("filter error:" + filter);
						throw e;
					}
				}
			}
			if (lastFilter != null) {
				// throw new
				return null;
			}
			if (data == null) {
				throw new FileNotFoundException("File not exists:" + path);
			}
			// data not null
			return data;
		} else {
			return oldData;
		}
	}

	private <T> MatcherFilter<T> getFilter(Object lastFilter,
			List<MatcherFilter<T>> filters) {
		if (lastFilter != null) {
			for (MatcherFilter<T> filter : filters) {
				if (lastFilter == filter) {
					return filter;
				}
			}
		}
		return null;
	}

	private ResourceItem resource(String path) {
		ResourceItem item = cached.get(path);
		if (item != null) {
			if (item.lastModified > 0) {
				long lastModified = getLastModified(path);
				if (lastModified < 0 || lastModified > item.lastModified) {
					item = null;
				}
			}
		}
		if (item == null) {
			item = new ResourceItem();
			item.path = path;
			Group group = this.find(path, false);
			if (group != null) {
				String encoding = this.getFeatureMap(path).get(
						LiteTemplate.FEATURE_ENCODING);
				item.encoding = encoding;
			}
			cached.put(path, item);
		}
		return item;
	}

	public long getLastModified(String path) {
		ResourceItem item = cached.get(path);
		if (item == null) {
			return -1;
		}
		File f = new File(root, path);
		if (!f.exists()) {
			return -1;
		}
		long lastModified = f.lastModified();
		for (String path2 : item.relations) {
			f = new File(root, path2);
			if (!f.exists()) {
				return -1;
			}
			lastModified = Math.max(lastModified, f.lastModified());
		}
		return lastModified;
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
		private PathMatcher matcher;

		MatcherFilter(String pattern, ResourceFilter<T> base) {
			this.matcher = PathMatcher.createMatcher(pattern);
			this.base = base;
		}

		public boolean match(String url) {
			return matcher.match(url);
		}
		public String toString(){
			return base.toString();
		}

		public T doFilter(String path, T in) {
			ResourceItem item = currentItem.get();
			item.currentFilter = this;// must before doFilter
			T result = base.doFilter(path, in);
			if (result == null) {
				result = in;
			}
			item.currentData = result;
			return result;

		}

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
		byte[] data;
		/**
		 * 初始化为配置编码， 读取内容后，编码可能根据具体内容更正。
		 */
		String encoding;
		String text;
		Document dom;
		long lastModified = -1;
		String hash;
		transient Object currentData;
	}

	public String getContentHash(String path) {
		try {
			ResourceItem item = getFilteredContent(path, String.class, null);
			if (item.hash == null) {
				// hash(item.data);
				String text = item.text;
				if (text == null) {
					item.hash = hash(item.data);
				} else {
					item.hash = hash(text.getBytes(item.encoding));
				}
			}
			// 需要hash的资源地址，一般都是需要打包的静态资源
			addLinkedResource(path);
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

	public void saveText(String path, Object content) throws IOException {
		File dest = new File(this.root, path);
		FileOutputStream out = new FileOutputStream(dest);
		if (content instanceof byte[]) {
			out.write((byte[]) content);
		} else if (content instanceof InputStream) {
			out.write(loadAndClose((InputStream) content));
		} else {
			out
					.write(String.valueOf(content).getBytes(
							getSourceEncoding(path)));
		}
		out.close();
	}

	public Object createFilterProxy(Object object) {
		return jsr.wrapToJava(object, ResourceFilter.class);
	}

	public void addLinkedResource(String path) {
		if (path.startsWith("/")) {
			if (!linkedResources.contains(path)) {
				linkedResources.add(path);
			}
		} else {
			log.warn("关联资源只能通过绝对地址添加!!您添加的地址是：" + path);
		}

	}

	public List<String> getLinkedResources() {
		return Collections.unmodifiableList(linkedResources);
	}

	public String[] dir(String path) throws IOException {
		File dir = new File(root,path);
		return dir.list();
	}
}
