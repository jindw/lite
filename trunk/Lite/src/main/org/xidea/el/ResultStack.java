package org.xidea.el;

class ResultStack {
	private int pos = -1;
	private Object[] data = new Object[2];

	public Object top() {
		return data[pos];
	}

	public Object pop() {
		return data[pos--];
	}

	public Object push(Object value) {
		pos++;
		if (pos >= data.length) {
			Object[] data2 = new Object[data.length * 2];
			System.arraycopy(data, 0, data2, 0, data.length);
			data = data2;
		}
		return data[pos] = value;
	}

}
