package org.xidea.lite.tools;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.WeakHashMap;

import javax.imageio.ImageIO;



public class ImageSprite {
	private static final java.util.WeakHashMap<File,CachedImageResource> IMAGE_CACHE_MAP = new WeakHashMap<File, CachedImageResource>();
	private File root;
	public ImageSprite(URI root){
		this.root = new File(root);
	}
	public ImageResource getImage(String path) throws IOException{
		File f = new File(root,path);
		if(f.exists()){
			CachedImageResource c = IMAGE_CACHE_MAP.get(f);
			if(c == null || c.lastModified <= f.lastModified()){
				System.out.println(f);
				c = new CachedImageResource(f);
			}
			IMAGE_CACHE_MAP.put(f, c);
			return c;
		}
		return null;
	}
	public ImageResource createImage(int width,int height){
		return new CachedImageResource(width, height);
	}

	public byte[] compress(ImageResource resource) throws IOException{
		CachedImageResource image = (CachedImageResource)resource;
		BufferedImage canvas = new BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB); 
		ArrayList<ChildImageResource> children = image.children;
		for(ChildImageResource child : children){
			CachedImageResource item = child.resource;
			ImageUtil.drawImage(canvas, item.image, child.x, child.y);
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		canvas = ImageUtil.quantize(canvas, Color.WHITE);
		ImageIO.write(canvas, "png", out);
		return out.toByteArray();
	}

}
class ChildImageResource{
	CachedImageResource resource;
	int x;
	int y;
	public ChildImageResource(ImageResource res, int x,int y){
		this.resource = (CachedImageResource) res;
		this.x = x;
		this.y = y;
	}

	
}
class CachedImageResource implements ImageResource{
	long lastModified;
	int height;
	int width;
	boolean alpha;
	BufferedImage image;
	ArrayList<ChildImageResource> children = new ArrayList<ChildImageResource>();

	public CachedImageResource(int width,int height){
		this.width = width;
		this.height = height;
	}
	public CachedImageResource(File f) throws IOException {
		this.image = ImageIO.read(f);
		this.lastModified = f.lastModified();
		this.width = image.getWidth();
		this.height = image.getHeight();
		this.alpha = image.getColorModel().isAlphaPremultiplied();
	}

	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}

	public boolean isAlpha() {
		return alpha;
	}


	public void drawImage(ImageResource res, int x, int y) {
		this.children.add(new ChildImageResource(res,x, y));
	}
	
}