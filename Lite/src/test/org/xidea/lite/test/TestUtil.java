package org.xidea.lite.test;

import java.net.URI;

import org.xidea.lite.impl.ParseContextImpl;
import org.xidea.lite.impl.ResourceContextImpl;

public class TestUtil {

	public static ParseContextImpl buildParseContext(URI uri){
		ParseContextImpl context =  new ParseContextImpl(uri == null?null:uri.getPath(),new ResourceContextImpl(uri),null);
		context.setCurrentURI(uri);
		return context;
	}

}
