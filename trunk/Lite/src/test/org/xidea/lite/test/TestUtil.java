package org.xidea.lite.test;

import java.net.URI;

import org.xidea.lite.parser.impl.ParseContextImpl;
import org.xidea.lite.parser.impl.ResourceContextImpl;

public class TestUtil {

	public static ParseContextImpl buildParseContext(URI uri){
		return new ParseContextImpl(new ResourceContextImpl(uri),null,null,null,null);
	}

}
