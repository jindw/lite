package org.xidea.el.impl;

import org.xidea.el.ResultStack;

class ResultStackImpl implements ResultStack {
	private int pos = -1;
	private Object[] data = new Object[2];


	/* (non-Javadoc)
	 * @see org.xidea.el.impl.ResultStack#pop()
	 */
	public Object pop() {
		return data[pos--];
	}

	/* (non-Javadoc)
	 * @see org.xidea.el.impl.ResultStack#get()
	 */
	public Object get() {
		return data[pos];
	}
	/* (non-Javadoc)
	 * @see org.xidea.el.impl.ResultStack#set(java.lang.Object)
	 */
	public void set(Object object) {
		data[pos] = object;
	}

	/* (non-Javadoc)
	 * @see org.xidea.el.impl.ResultStack#push(java.lang.Object)
	 */
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
