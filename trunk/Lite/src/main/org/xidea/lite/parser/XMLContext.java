package org.xidea.lite.parser;

public interface XMLContext {

	/**
	 * 如果compress为真，或者 reserveSpace为真 则该属性失效
	 * @param format
	 */
	public boolean isFormat();

	public void setFormat(boolean format);

	/**
	 * 如果 reserveSpace为真 则该属性失效
	 * @return
	 */
	public boolean isCompress();

	public void setCompress(boolean compress);

	/**
	 * 该属性为真时，compress 和 format都将失效
	 * @return
	 */
	public boolean isReserveSpace();

	public void setReserveSpace(boolean keepSpace);

	/**
	 * 在XMLParser中判断平设置，Core标签将缩进做了回逄1�7处理
	 * 
	 * @return
	 */
	//public int getDepth();

	public void beginIndent();

	/**
	 * @see ParseContextImpl#endIndent()
	 */
	public void endIndent();

}