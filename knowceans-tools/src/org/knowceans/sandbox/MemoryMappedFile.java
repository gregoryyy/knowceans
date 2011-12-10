package org.knowceans.sandbox;

import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MemoryMappedFile {
	private static final int SIZEOFINT = 4;
	// 128 Mo
	static int length = 0x8FFFFFF;

	public static void main(String[] args) throws Exception {
		testIntBuffer();
		// see http://mindprod.com/jgloss/bytebuffer.html

	}

	public static void testIntBuffer() throws Exception {
		// Mappen* provides force(), isLoaded() and load()
		MappedByteBuffer cache = new RandomAccessFile("cache.bin", "rw")
				.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, length);
		// write 128Mo of x chars
		for (int i = 0; i < length / 4; i++) {
			cache.putInt(i);
		}
		System.out.println("Finished writing");
		// read some
		int start = 200000;
		int end = 200400;
		for (int i = start; i < end; i++) {
			System.out.println(i + " " + cache.getInt(i * SIZEOFINT));
		}
	}
}
