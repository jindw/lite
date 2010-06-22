package org.jside.webserver;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class ParamsMap extends AbstractMap<String, String[]> implements
		Map<String, String[]> {
	private static final Log log = LogFactory.getLog(ParamsMap.class);
	private static final Pattern QUERY_PATTERN = Pattern
			.compile("([^=&]+)(?:=([^&]+))?");
	private Map<String, List<String>> rawMap = new LinkedHashMap<String, List<String>>();
	private Map<String, String[]> cacheMap = new LinkedHashMap<String, String[]>();
	private Map<String, String> paramMap;
	public ParamsMap() {
	}
	Map<String, String> toParam(){
		if(paramMap == null){
			paramMap = new ParamMap();
		}
		return paramMap;
	}

	@Override
	public String[] get(Object key) {
		return cacheMap.get(key);
	}

	private String decode(String v, String encoding) {
		try {
			return URLDecoder.decode(v, encoding);
		} catch (UnsupportedEncodingException e) {
			return v;
		}
	}

	void reset(String encoding) {
		for (Map.Entry<String, List<String>> entry : rawMap.entrySet()) {
			List<String> vs = entry.getValue();
			String[] values = new String[vs.size()];
			for (int i = 0; i < values.length; i++) {
				values[i] = decode(values[i], encoding);
			}
			cacheMap.put(decode(entry.getKey(), encoding), values);
		}

	}

	void parse(String query) {
		if (query != null) {
			Matcher matcher = QUERY_PATTERN.matcher(query);
			while (matcher.find()) {
				String name = matcher.group(1);
				String value = matcher.group(2);
				try {
					this.addRaw(name, value);
				} catch (Exception e) {
					log.info("解析失败: " + query + "\n" + name + "=" + value, e);
				}

			}
		}
	}

	private void addRaw(String name, String value) {
		List<String> vs = rawMap.get(name);
		if (vs == null) {
			vs = new ArrayList<String>();
			rawMap.put(name, vs);
		}
		vs.add(value);
	}

	@Override
	public Set<Entry<String, String[]>> entrySet() {
		for (String key : keySet()) {
			get(key);
		}
		return cacheMap.entrySet();
	}

	@Override
	public Set<String> keySet() {
		return cacheMap.keySet();
	}

	@Override
	public boolean containsKey(Object key) {
		return cacheMap.containsKey(key);
	}

	class ParamMap extends AbstractMap<String, String> implements
			Map<String, String> {
		@Override
		public Set<Entry<String, String>> entrySet() {
			HashMap<String, String> base = new HashMap<String, String>();
			for (String key : keySet()) {
				base.put(key, get(key));
			}
			return base.entrySet();
		}

		@Override
		public Set<String> keySet() {
			return cacheMap.keySet();
		}

		@Override
		public boolean containsKey(Object key) {
			return cacheMap.containsKey(key);
		}

		@Override
		public String get(Object key) {
			String[] values = cacheMap.get(key);
			if (values != null && values.length > 0) {
				return values[0];
			}
			return null;
		}

	}

}
