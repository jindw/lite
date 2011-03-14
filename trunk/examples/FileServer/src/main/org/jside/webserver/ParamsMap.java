package org.jside.webserver;

import java.net.URLDecoder;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
			.compile("([^=&]+)(?:=([^&]*))?");
	private Map<String, String[]> cacheMap = null;
	private Map<String, String> paramMap;
	private String raw;
	private String encoding;
	public ParamsMap(String raw,String encoding) {
		this.raw = raw;
		this.encoding = encoding;
		parse();
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



	private void parse() {
		cacheMap =  new LinkedHashMap<String, String[]>();
		if (raw != null) {
			Matcher matcher = QUERY_PATTERN.matcher(raw);
			while (matcher.find()) {
				String name = matcher.group(1);
				String value = matcher.group(2);
				try {
					if(value!=null){
						value = URLDecoder.decode(value, encoding);
						this.addRaw(name, value);
					}
				} catch (Exception e) {
					log.info("解析失败: " + raw + "\n" + name + "=" + value, e);
				}

			}
		}
	}

	private void addRaw(String name, String value) {
		String[] vs = cacheMap.get(name);
		if (vs == null) {
			vs = new String[]{value};
		}else{
			String[] vs2 = new String[vs.length+1];
			System.arraycopy(vs, 0, vs2, 0, vs.length);
			vs2[vs.length] = value;
			vs = vs2;
		}
		cacheMap.put(name, vs);
	}
	public void reset(String encoding) {
		this.encoding = encoding;
		parse();
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
	public String toString(){
		return raw;
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
