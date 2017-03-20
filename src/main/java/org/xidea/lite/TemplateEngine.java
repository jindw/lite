package org.xidea.lite;

import java.io.IOException;
import java.io.Writer;

public interface TemplateEngine {

	public abstract Template getTemplate(String path) throws IOException;

	public abstract void clear(String path);
	
	public abstract void render(String path, Object context, Writer out) throws IOException;
}