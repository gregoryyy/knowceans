package org.knowceans.sandbox;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * Memory-mapped files allow you to create and modify files that are too big to
 * bring into memory. With a memory-mapped file, you can pretend that the entire
 * file is in memory and that you can access it by simply treating it as a very
 * large array. This approach greatly simplifies the code you write in order to
 * modify the file.
 * <p>
 * http://www.linuxtopia.org/online_books/programming_books/thinking_in_java/
 * TIJ314_029.htm
 */
public class LargeMappedFiles {
	static int length = 0x8FFFFFF; // 128 Mb

	public static void main(String[] args) throws Exception {
		/*
		 * To do both writing and reading, we start with a RandomAccessFile, get
		 * a channel for that file, and then call map( ) to produce a
		 * MappedByteBuffer, which is a particular kind of direct buffer. Note
		 * that you must specify the starting point and the length of the region
		 * that you want to map in the file; this means that you have the option
		 * to map smaller regions of a large file.
		 * 
		 * MappedByteBuffer is inherited from ByteBuffer, so it has all of
		 * ByteBuffer’s methods. Only the very simple uses of put( ) and get( )
		 * are shown here, but you can also use things like asCharBuffer( ),
		 * etc.
		 * 
		 * The file created is 128 MB long, which is probably larger than the
		 * space your OS will allow. The file appears to be accessible all at
		 * once because only portions of it are brought into memory, and other
		 * parts are swapped out. This way a very large file (up to 2 GB) can
		 * easily be modified. Note that the file-mapping facilities of the
		 * underlying operating system are used to maximize performance.
		 */
		MappedByteBuffer out = new RandomAccessFile("test.dat", "rw")
				.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, length);
		for (int i = 0; i < length; i++)
			out.put((byte) 'x');
		System.out.println("Finished writing");
		for (int i = length / 2; i < length / 2 + 6; i++)
			System.out.print((char) out.get(i));
	}
}