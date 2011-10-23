//package org.xidea.lite.tools.util;
//
//import java.awt.image.BufferedImage;
//import java.awt.image.ColorModel;
//import java.io.ByteArrayInputStream;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.util.ArrayList;
//
//import javax.imageio.ImageIO;
//
//import org.xidea.lite.tools.ImageResource;
//
//public class ImageResourceImpl implements ImageResource {
//	public static final String PNG = "png";
//	public static final String GIF = "gif";
//	public static final String JPG = "jpg";
//
//	public BufferedImage bufferedImage;
//	public String imageName;
//	public String imageType = PNG;
//	public int pixelSize;
//	public boolean transparent = false;
//	private boolean alpha;
//	private int width;
//	private int height;
//	private ArrayList<ImageResourceImpl> children;
//	
//	public ImageResourceImpl(String fileName,byte[] image) throws FileNotFoundException, IOException {
//		this.bufferedImage = ImageIO.read(new ByteArrayInputStream(image));
//		this.imageName = fileName.toLowerCase();
//		if (imageName.endsWith(".gif")) {
//			this.imageType = GIF;
//		} else if (imageName.endsWith(".png")) {
//			this.imageType = PNG;
//		} else if (imageName.endsWith(".jpg") || imageName.endsWith(".jpeg")) {
//			this.imageType = JPG;
//		}
//		ColorModel colorModel = bufferedImage.getColorModel();
//		this.pixelSize = colorModel.getPixelSize();
//		
//		this.alpha = colorModel.isAlphaPremultiplied();
//		this.width = bufferedImage.getWidth();
//		this.height = bufferedImage.getHeight();
//
//		this.transparent = this.alpha || colorModel.getTransparency() == ColorModel.BITMASK;
//	}
//
//
//	public ImageResourceImpl(int width, int height, boolean trans,
//			boolean alphaTrans) {
//		this.width = width;
//		this.height = height;
//		this.transparent = trans;
//		this.alpha = alphaTrans;
//	}
//
//
//	public int getHeight() {
//		return height;
//	}
//
//	public int getWidth() {
//		return width;
//	}
//
//
//	public boolean isAlphaTransparent() {
//		return alpha;
//	}
//
//
//	public boolean isTransparent() {
//		return transparent;
//	}
//
//
//	public void drawImage(ImageResource res, int x, int y,boolean repeatX,boolean repeatY) {
//		ImageResourceImpl resimpl = (ImageResourceImpl) res;
//		this.children.add(resimpl);
//	}
//
//	public byte[] compress() {
//		return null;
//	}
//
//}

