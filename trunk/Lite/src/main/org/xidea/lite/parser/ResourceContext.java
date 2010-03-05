package org.xidea.lite.parser;

import java.io.InputStream;
import java.net.URI;

import org.xidea.lite.parser.impl.ParseContextImpl;

/**
 * @see ParseContextImpl
 * 要设计为继承安全，这个接口不能滥用
 * 而且，实现不能直接调用this。否则容易形成孤岛
 * @author jindw
 */
public interface ResourceContext{
	/**
	 * 如果file相于根目录（/path/...），以base作为根目录处理
	 * 否则以parentURI，或者base作为parent直接new URL处理。
	 * @param file
	 * @param parentURI
	 * @see org.xidea.lite.parser.impl.ParseContextImpl#createURI
	 * @see org.xidea.lite.parser.impl.ResourceContextImpl#createURI
	 * @return
	 */
	public URI createURI(String file, URI parentURI);
	public InputStream openStream(URI url);


}