package org.xidea.lite.parser.impl;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class JoinedStream extends FilterInputStream{
	static byte[] EMPTY = {};
	private byte[] defaultEnds;
	private byte[] defaultStarts;
	private int start = 0;
	private int end = 0;
	
	public JoinedStream(byte[] defaultStarts, InputStream in, byte[] defaultEnds) {
		super(in);
		this.defaultStarts = defaultStarts == null?EMPTY:defaultStarts;
		this.defaultEnds = defaultEnds == null?EMPTY:defaultEnds;
	}

	@Override
	public int read() throws IOException {
		byte[] b = new byte[1];
		if(read(b,0,1)>=0){
			return b[0];
		}
		return -1;
		
	}

	@Override
	public int read(byte[] b, int off, int len)
			throws IOException {
		if(start<defaultStarts.length){
			int length = Math.min(len, defaultStarts.length-start);
			System.arraycopy(defaultStarts, start, b, off, length);
			start += length;
			return length;
		}
		int c = super.read(b, off, len);
		if(c>0){
			return c;
		}else{
			if(end<defaultEnds.length){
				int length = Math.min(len, defaultEnds.length-end);
				System.arraycopy(defaultEnds, end, b, off, length);
				end += length;
				return length;
			}
			return -1;
		}
	}
	
};
