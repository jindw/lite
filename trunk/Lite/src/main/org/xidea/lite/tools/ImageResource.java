package org.xidea.lite.tools;

public interface ImageResource {
	int getWidth();
	int getHeight();
	boolean isAlpha();
	void drawImage(ImageResource res, int x, int y); 
}
