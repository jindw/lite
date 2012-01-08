package org.xidea.lite.tools.image.test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.xidea.lite.tools.ImageUtil;

public class ImageAlphaUtil {

	static File base = new File("D:/workspace/JSideHosts/CSSSprite/images/box/");

	public static void main(String[] args) throws Exception {
		// boxa.png
		for (File file : base.listFiles()) {
			//File file = new File(base, "boxlt.png");
			if(file.getName().endsWith(".png")){
			BufferedImage image = ImageIO.read(file);
			image = sc(image);

			int width = image.getWidth();
			int height = image.getHeight();
			
			final int[] imgRGB = image.getRGB(0, 0, width, height, null, 0, width);
//			System.out.println(Integer.toHexString(imgRGB[0]));
//			System.out.println(imgRGB[0]);
			image = ImageUtil.matte(image, Color.BLACK);
			for (int i = 0; i < imgRGB.length; i++) {
				int c = imgRGB[i];
				if (c == -1 || (c & 0xFFFFFF) == 0x000000) {
					image.setRGB(i % width, i / width, 0);
				}
			}
			System.out.println(file);
			ImageIO.write(image, "png", file);//new File(base,"out.png"));
			}
		}
		// image.setRGB(x, y, rgb);
	}

	private static BufferedImage sc(BufferedImage image) {

//		int width = (int)((image.getWidth()+1)/2);
//		int height = (int)((image.getHeight()+1)/2);
//		Image image2 = image.getScaledInstance(width, height, Image.SCALE_DEFAULT);
//		image= new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
//		image.getGraphics().drawImage(image2, 0,0,width,height,new JFrame());
		return image;
	}

}
