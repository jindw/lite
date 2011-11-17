package org.xidea.lite.tools;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.CompositeContext;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.util.HashMap;

public class ImageUtil {

	/**
	 * Draws <code>image<code> on the <code>canvas</code> placing the top left
	 * corner of <code>image</code> at <code>x</code> / <code>y</code> offset
	 * from the top left corner of <code>canvas</code>.
	 */
	static void drawImage(BufferedImage canvas, BufferedImage image, int x,
			int y) {
		final int[] imgRGB = image.getRGB(0, 0, image.getWidth(), image
				.getHeight(), null, 0, image.getWidth());
		canvas.setRGB(x, y, image.getWidth(), image.getHeight(), imgRGB, 0,
				image.getWidth());
	}

	static String getRepeat(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		int xc = width / 2;
		int yc = height / 2;
		int cc = image.getRGB(xc, yc);
		int c0 = image.getRGB(xc, 0);
		int zc = image.getRGB(0, yc);
		boolean repeatX = zc == cc;
		boolean repeatY = c0 == cc;
		if (repeatX || repeatY) {
			final int[] imgRGB = image.getRGB(0, 0, width, height, null, 0,
					width);
			if (repeatX) {
				int p = 0;
				outer: for (int i = 0; i < height; i++) {
					int p0 = imgRGB[p++];
					for (int j = 1; j < width; j++) {
						if (p0 != imgRGB[p++]) {
							repeatX = false;
							break outer;
						}
					}
				}
			}
			if (repeatY) {
				int p = width;
				outer: for (int i = width; i < height; i++) {
					for (int j = 0; j < width; j++) {
						if (imgRGB[j] != imgRGB[p++]) {
							repeatY = false;
							break outer;
						}
					}
				}
			}
		}
		return repeatX ? (repeatY ? "xy" : "x") : (repeatY ? "y" : "");
	}

	/**
	 * Performs matting of the <code>source</code> image using
	 * <code>matteColor</code>. Matting is rendering partial transparencies
	 * using solid color as if the original image was put on top of a bitmap
	 * filled with <code>matteColor</code>.
	 */
	public static BufferedImage matte(BufferedImage source, Color matteColor) {
		final int width = source.getWidth();
		final int height = source.getHeight();

		// A workaround for possibly different custom image types we can get:
		// draw a copy of the image
		final BufferedImage sourceConverted = new BufferedImage(width, height,
				BufferedImage.TYPE_4BYTE_ABGR);
		sourceConverted.getGraphics().drawImage(source, 0, 0, null);

		final BufferedImage matted = new BufferedImage(width, height,
				BufferedImage.TYPE_4BYTE_ABGR);

		final BufferedImage matte = new BufferedImage(width, height,
				BufferedImage.TYPE_4BYTE_ABGR);
		final int matteRgb = matteColor.getRGB();
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				matte.setRGB(x, y, matteRgb);
			}
		}

		CompositeContext context = AlphaComposite.DstOver.createContext(matte
				.getColorModel(), sourceConverted.getColorModel(), null);
		context.compose(matte.getRaster(), sourceConverted.getRaster(), matted
				.getRaster());

		return matted;
	}

	/**
	 * Returns a two dimensional array of the <code>image</code>'s RGB values,
	 * including transparency.
	 */
	private static int[][] getRgb(BufferedImage image) {
		final int width = image.getWidth();
		final int height = image.getHeight();

		final int[][] rgb = new int[height][width];

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				rgb[y][x] = image.getRGB(x, y);
			}
		}

		return rgb;
	}

	public static BufferedImage quantize(BufferedImage source, Color matteColor) {
		int maxColors = 255;
		final int width = source.getWidth();
		final int height = source.getHeight();

		// First put the matte color so that we have a sensible result
		// for images with full alpha transparencies
		final BufferedImage mattedSource = matte(source, matteColor);

		// Get two copies of RGB data (quantization will overwrite one)
		final int[][] indexMap = getRgb(mattedSource);

		// Quantize colors and shift palette by one for transparency color
		// We'll keep transparency color black for now.
		final int[] colors = ImageQuantize.quantizeColor(indexMap, maxColors);

		System.out.println(colors.length);
		final int[] colorsWithAlpha = new int[colors.length + 1];
		System.arraycopy(colors, 0, colorsWithAlpha, 1, colors.length);
		colorsWithAlpha[0] = matteColor.getRGB();
		final IndexColorModel colorModel = new IndexColorModel(8,
				colorsWithAlpha.length, colorsWithAlpha, 0, false, 0,
				DataBuffer.TYPE_BYTE);

		// Write the results to an indexed image, skipping the fully transparent
		// bits
		final BufferedImage quantized = new BufferedImage(width, height,
				BufferedImage.TYPE_BYTE_INDEXED, colorModel);
		final WritableRaster raster = quantized.getRaster();
		final int[][] rgb = getRgb(source);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if(x<width-4 && y<height-1){
					if(indexMap[y][x] != indexMap[y+1][x] && isRepeat(indexMap[y],x,x+4) && isRepeat(rgb[y+1],x,x+4)){
						int d1 = distance(rgb[y][x],rgb[y+1][x]);
						int d2 = distance(colors[indexMap[y][x]],colors[indexMap[y+1][x]]);
						int d10 = distance(colors[indexMap[y][x]],rgb[y][x]);
						int d02 = distance(colors[indexMap[y+1][x]],rgb[y][x]);
						if(x == 16){
							//System.out.println(y+":\t"+d1+"/\t"+d2+"/\t"+d10+"/\t"+d02);
						}
						if(d1>4  && d2 <18 && d2>d1){
							final int value1 = (rgb[y][x] & 0xff000000) != 0x00000000 ? indexMap[y][x] + 1
									: 0;
							final int value2 = (rgb[y+1][x] & 0xff000000) != 0x00000000 ? indexMap[y+1][x] + 1
									: 0;
							raster.setPixel(x, y, new int[] { value1});//x
							raster.setPixel(x+1, y, new int[] {value2});//x+1
							raster.setPixel(x+2, y, new int[] {Math.random()>0.3?value2:value1});//x+1
							raster.setPixel(x+3, y, new int[] {value2});//x+1

							indexMap[y+1][x+1] = indexMap[y][x];
							//indexMap[y+1][x+2] = indexMap[y][x];
							x+=3;
							continue;
						}
					}
				}
				final int value = (rgb[y][x] & 0xff000000) != 0x00000000 ? indexMap[y][x] + 1
						: 0;
				raster.setPixel(x, y, new int[] { value });
			}
		}
		return quantized;
	}
	static boolean isRepeat(int[] cs,int begin,int end){
		do {
			if(cs[begin] != cs[++begin]){
				return false;
			}
		}while(begin < end);
		return true;
	}

	static int distance(int c1, int c2) {
		return ImageQuantize.distance(c1,c2);
	}
}