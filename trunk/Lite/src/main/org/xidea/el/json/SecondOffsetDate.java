package org.xidea.el.json;

import java.util.Date;

public class SecondOffsetDate extends Date {
	private static final long serialVersionUID = 1L;
	public SecondOffsetDate(int time){
		super(time * 1000);
	}
	public SecondOffsetDate(long time){
		super(time);
	}

}
