package org.xidea.lite.tools;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.CompositeContext;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;

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
	public static int[][] getRgb(BufferedImage image) {
		final int width = image.getWidth();
		final int height = image.getHeight();

		final int[][] rgb = new int[width][height];

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				rgb[x][y] = image.getRGB(x, y);
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
		final int[][] bitmap = getRgb(mattedSource);

		// Quantize colors and shift palette by one for transparency color
		// We'll keep transparency color black for now.
		final int[] colors = ImageQuantize.quantizeImage(bitmap, maxColors);
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
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				final int value = (rgb[x][y] & 0xff000000) != 0x00000000 ? bitmap[x][y] + 1
						: 0;
				raster.setPixel(x, y, new int[] { value });
			}
		}

		return quantized;
	}

}