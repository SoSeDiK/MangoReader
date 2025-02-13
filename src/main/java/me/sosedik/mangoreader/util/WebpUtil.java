package me.sosedik.mangoreader.util;

import com.luciad.imageio.webp.CompressionType;
import com.luciad.imageio.webp.WebPWriteParam;
import me.sosedik.mangoreader.misc.ImageType;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public class WebpUtil {

	/**
	 * Converts raw image rawData into raw webp image rawData
	 *
	 * @param imageData raw image rawData
	 * @param quality image quality, from 0 (lowest) to 1 (highest)
	 * @return raw webp image rawData
	 * @throws IOException if failed to read input or write output
	 */
	public static byte[] convertToWebP(byte[] imageData, float quality) throws IOException {
		try (InputStream inputStream = new ByteArrayInputStream(imageData);
		     ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream);
		     var byteArrayOutputStream = new ByteArrayOutputStream()) {

			Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
			if (!readers.hasNext())
				throw new IOException("No ImageReader found for the input format.");

			ImageReader reader = readers.next();
			reader.setInput(imageInputStream);

			BufferedImage image = reader.read(0);

			try (ImageOutputStream output = ImageIO.createImageOutputStream(byteArrayOutputStream)) {
				Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(ImageType.WEBP.getExtension());
				if (!writers.hasNext())
					throw new IOException("No ImageWriter found for WebP format.");

				ImageWriter writer = writers.next();
				writer.setOutput(output);

				WebPWriteParam writeParam = (WebPWriteParam) writer.getDefaultWriteParam();
				if (quality == 1F) {
					writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
					writeParam.setCompressionType(CompressionType.Lossless);
				} else {
					writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
					writeParam.setCompressionType(CompressionType.Lossy);
					writeParam.setCompressionQuality(quality);
				}

				writer.write(null, new IIOImage(image, null, null), writeParam);
				writer.dispose();

				return byteArrayOutputStream.toByteArray();
			}
		}
	}

}
