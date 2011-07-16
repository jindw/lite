package org.xidea.lite.tools.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xidea.lite.impl.ParseUtil;
import org.xidea.lite.parse.ParseConfig;
import org.xidea.lite.parse.ParseContext;
import org.xidea.lite.tools.FilterPlugin;
import org.xidea.lite.tools.ResourceManager;
import org.xml.sax.SAXException;

public class ResourceFactoryImpl implements ResourceManager {
	private static Log log = LogFactory.getLog(ResourceFactoryImpl.class);
	private ParseConfig config;
	private File root;
	private ArrayList<FilterPlugin<byte[]>> streamFilters = new ArrayList<FilterPlugin<byte[]>>();
	private ArrayList<FilterPlugin<String>> stringFilters = new ArrayList<FilterPlugin<String>>();
	private ArrayList<FilterPlugin<Document>> documentFilters = new ArrayList<FilterPlugin<Document>>();
	private final Map<String, ResourceItem> cached = new HashMap<String, ResourceItem>();
	private ThreadLocal<ResourceItem> currentItem = new ThreadLocal<ResourceItem>();

	public void addStringFilter(FilterPlugin<String> filter) {
		stringFilters.add(filter);
	}

	public void addDocumentFilter(FilterPlugin<Document> filter) {
		documentFilters.add(filter);
	}

	public void addByteFilter(FilterPlugin<byte[]> filter) {
		streamFilters.add(filter);
	}

	public File getRoot() {
		return new File(config.getRoot());
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
		return config.getFeatureMap(path).get(ParseContext.FEATURE_ENCODING);
	}

	public byte[] getRawBytes(String path) throws IOException {
		InputStream in = ParseUtil.openStream(config.getRoot().resolve(
				path.substring(1)));
		return loadAndClose(in);
	}

	public byte[] getFilteredBytes(String path) throws IOException {
		try {
			return getData(path, byte[].class);// ,streamFilters,stringFilters,documentFilters);
		} catch (SAXException e) {
			throw new IOException(e);
		}
	}

	public String getFilteredText(String path) throws IOException {
		try {
			return getData(path, String.class);// ,streamFilters,stringFilters,documentFilters);
		} catch (SAXException e) {
			throw new IOException(e);
		}
	}

	public Document getFilteredDocument(String path) throws IOException,
			SAXException {
		return getData(path, Document.class);// ,streamFilters,stringFilters,documentFilters);
	}

	
	
	
	
	
	@SuppressWarnings("unchecked")
	private <T> T getData(String path, Class<T> type
	// ,List<FilterPlugin<byte[]>> streamFilters,
	// List<FilterPlugin<String>> stringFilters,
	// List<FilterPlugin<Document>> documentFilters
	) throws IOException, SAXException {
		ResourceItem res = resource(path);
		ResourceItem old = currentItem.get();
		try {
			currentItem.set(res);
			if (res.data == null) {// byte[]
				byte[] data = getRawBytes(path);
				for (FilterPlugin<byte[]> filter : streamFilters) {
					data = filter.doFilter(path, data);
				}
				res.data = data;
			}
			if (type == byte[].class) {
				return (T) res.data;
			}

			if (type == String.class) {
				if (res.text == null) {// string
					String text = ParseUtil.loadTextAndClose(
							new ByteArrayInputStream(res.data),
							getEncoding(path));
					for (FilterPlugin<String> filter : stringFilters) {
						text = filter.doFilter(path, text);
					}
					res.text = text;
				}
				return (T) res.data;
			} else {
				if (res.dom == null) {
					String text = ParseUtil
							.loadXMLTextAndClose(new ByteArrayInputStream(
									res.data));
					// check xml instruction utf-8
					for (FilterPlugin<String> filter : stringFilters) {
						text = filter.doFilter(path, text);
					}
					res.text = text;
					// 没有默认的xml正规化
					Document doc = ParseUtil.loadXMLBySource(text, "lite:///"
							+ path);
					for (FilterPlugin<Document> filter : documentFilters) {
						doc = filter.doFilter(path, doc);
					}
					res.dom = doc;
				}
				return (T) res.dom;
			}
		} finally {
			currentItem.set(old);
			res.lastModified = System.currentTimeMillis();
		}
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
			if (lastModified > item.lastModified) {
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
	private static class ResourceItem {
		ArrayList<String> relations = new ArrayList<String>();
		// List<FilterPlugin<byte[]>> streamFilters = new
		// ArrayList<FilterPlugin<byte[]>>();
		// List<FilterPlugin<String>> stringFilters = new
		// ArrayList<FilterPlugin<String>>();
		// List<FilterPlugin<Document>> documentFilters = new
		// ArrayList<FilterPlugin<Document>>();
		@SuppressWarnings("unused")
		String path;
		byte[] data;
		String text;
		Document dom;
		long lastModified;
	}

}
