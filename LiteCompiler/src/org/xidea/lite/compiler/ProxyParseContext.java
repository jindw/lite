package org.xidea.lite.compiler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xidea.lite.parser.ParseContextImpl;

public class ProxyParseContext extends ParseContextImpl {
	private static final URL BASE ;
	static{
		try {
			BASE = new URL("http://localhost/");
		} catch (MalformedURLException e) {
			throw new IllegalStateException();
		}
	}
	private Map<String, String> params;
	private String encoding = "utf-8";
	private ArrayList<String> missedResources = new ArrayList<String>();

	public ProxyParseContext( Map<String, String> params,
			String encoding) {
		super(BASE);
		this.params = params;
		if(encoding!=null){
			this.encoding = encoding;
		}
	}

	public List<String> getMissedResources() {
		return missedResources;
	}

	@Override
	public InputStream getInputStream(URL url) {
		String path = url.getPath().substring(this.BASE.getPath().length()-1);
		String result = params.get(path);
		try {
			if (result != null) {
				return new ByteArrayInputStream(result.getBytes(encoding));
			}
			InputStream in = super.getInputStream(url);
			if(in == null){
				return in;
			}else{
				this.missedResources .add(path);
				return new ByteArrayInputStream("<empty/>".getBytes());
			}
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

}
