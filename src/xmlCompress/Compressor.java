package xmlCompress;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;

public class Compressor {

	public static String compress(String content) throws IOException {
		ByteArrayOutputStream rstBao = new ByteArrayOutputStream();
		GZIPOutputStream zos = new GZIPOutputStream(rstBao);
		zos.write(content.getBytes());
		IOUtils.closeQuietly(zos);
		byte[] bytes = rstBao.toByteArray();
		return Base64.getEncoder().encodeToString(bytes);

	}

	public static String decompress(String compressed) throws IOException {
		String result = null;

		byte[] bytes = Base64.getDecoder().decode(compressed);
		GZIPInputStream zi = null;
		try {
			zi = new GZIPInputStream(new ByteArrayInputStream(bytes));
			result = IOUtils.toString(zi);
		} finally {
			IOUtils.closeQuietly(zi);
		}
//		System.out.println("Decompressed  --->");
//		System.out.println(result);
//		result = StringEscapeUtils.unescapeXml(result);
//		System.out.println(result);
//		System.out.println("--------");
		return result;
	}
}
