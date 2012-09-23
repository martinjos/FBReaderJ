/*
 * Copyright (C) 2012 Martincode (https://github.com/martincode)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.martincode.fbdict.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class PlainChunkAccessFile implements ChunkAccessFile {
	
	RandomAccessFile raf;
	
	public PlainChunkAccessFile(String fileName) throws FileNotFoundException {
		this(new File(fileName));
	}
	
	public PlainChunkAccessFile(File file) throws FileNotFoundException {
		raf = new RandomAccessFile(file, "r");
	}

	// I think I should probably change size to int. Don't really want to be throwing
	// RuntimeExceptions around based on the contents of data files. Wouldn't really
	// be appropriate to throw IOException. The checking belongs at a higher level.
	@Override
	public byte[] getChunk(long offset, long size) throws IOException {
		
		//System.out.println("Using plain file to get chunk");
		
		if (size > Integer.MAX_VALUE || size < 0) {
			throw new IllegalArgumentException("size must be a positive integer <= " + Integer.MAX_VALUE);
		}
		int size_int = (int) size;
		byte[] result = new byte[size_int];
		raf.seek(offset);
		raf.readFully(result);
		return result;
	}

}
