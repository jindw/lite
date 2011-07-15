package org.xidea.lite.tools.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class CachedInputStream extends InputStream{
	@SuppressWarnings("unused")
	private static Log log = LogFactory.getLog(CachedInputStream.class);
	private byte[] data;
	private int offset;
	public CachedInputStream(InputStream in) throws IOException{
		if(in instanceof CachedInputStream){
			this.data = ((CachedInputStream)in).data;
		}else{
			byte[] data = new byte[1024];
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int b;
			while((b = in.read(data))>=0){
				out.write(data,0,b);
			}
			this.data = out.toByteArray();
		}
	}
	public int getLength(){
		return this.data.length;
	}
	public void writeTo(OutputStream out) throws IOException{
		out.write(data);
	}
	public byte[] getData(){
		return data;
	}
	@Override
	public int read() throws IOException {
		if(offset<data.length){
			return 0xFF & data[offset++];
		}else{
			return -1;
		}
	}

}
