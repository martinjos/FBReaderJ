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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class DZChunkAccessFile implements ChunkAccessFile {
	
	public static int GZ_HDR_LEN = 10;
	public static int GZ_FTR_LEN = 8;
	public static short[] GZ_MAGIC = { 0x1f, 0x8b };
	public static byte[] GZ_RND_S = { 'R', 'A' };
	
	public static enum GZF {
		HCRC(0x02),
		EXTRA(0x04),
		NAME(0x08),
		COMMENT(0x10);
		
		protected final int val_;
		private GZF(int val_) { this.val_ = val_; }
		public boolean isSet(int flags) { return (flags & val_) != 0; }
	}
	
	RandomAccessFile raf;
	int headerLength;
	long[] chunkOffsets;
	int[] chunkLengths;
	int chunkDcLength;
	
	// for faking a gzip header to make GZIPInputStream work
	int method, os;
	
	public DZChunkAccessFile(String fileName) throws IOException {
		this(new File(fileName));
	}
	
	public DZChunkAccessFile(File file) throws IOException {
		raf = new RandomAccessFile(file, "r");
		
		int id1 = raf.readUnsignedByte();
		int id2 = raf.readUnsignedByte();
		
		if (id1 != GZ_MAGIC[0] || id2 != GZ_MAGIC[1]) {
			throw new IOException(".dz file does not have GZip magic numbers");
		}
		
		method = raf.readUnsignedByte();
		int flags = raf.readUnsignedByte();
		
		raf.skipBytes(5);
		//long mtime = Endian.swapInt(0xFFffFFffL & raf.readInt());
		//int extraFlags = raf.readUnsignedByte();
		
		os = raf.readUnsignedByte();
		
		if (!GZF.EXTRA.isSet(flags)) {
			throw new IOException(".dz file is actually just a .gz file (does not support random access)");
		}

		// moreLength will count everything in the header not covered by GZ_HDR_LEN or extraLength
		int moreLength = 2; // 2 bytes for extraLength field

		// length from just after this field, to the end of the chunk lengths
		int extraLength = Endian.swapShort(raf.readUnsignedShort());
		
		int si1 = raf.readUnsignedByte();
		int si2 = raf.readUnsignedByte();
		
		if (si1 != GZ_RND_S[0] || si2 != GZ_RND_S[1]) {
			throw new IOException(".dz file is actually just a .gz file (does not support random access)");
		}
		
		raf.skipBytes(4);
		//int subLength = Endian.swapShort(raf.readUnsignedShort());
		//int version = Endian.swapShort(raf.readUnsignedShort());
		
		chunkDcLength = Endian.swapShort(raf.readUnsignedShort());
		int chunkCount = Endian.swapShort(raf.readUnsignedShort());
		
		if (chunkCount == 0) {
			throw new IOException(".dz file has zero chunks");
		}
		
		if (chunkDcLength == 0) {
			throw new IOException(".dz file has chunks of size zero");
		}
		
		chunkLengths = new int[chunkCount];
		for (int i = 0; i < chunkCount; i++) {
			chunkLengths[i] = Endian.swapShort(raf.readUnsignedShort());
		}
		
		chunkOffsets = new long[chunkCount];
		for (int i = 1; i < chunkCount; i++) {
			chunkOffsets[i] = chunkOffsets[i-1] + chunkLengths[i-1];
		}
		
		if (GZF.NAME.isSet(flags)) {
			while (raf.readByte() != 0) {
				moreLength++;
			}
			moreLength++;
		}

		if (GZF.COMMENT.isSet(flags)) {
			while (raf.readByte() != 0) {
				moreLength++;
			}
			moreLength++;
		}

		if (GZF.HCRC.isSet(flags)) {
			//raf.skipBytes(2); // for DEBUG
			moreLength += 2;
		}
		
		headerLength = GZ_HDR_LEN + extraLength + moreLength;
		
		// DEBUG stuff
		//System.out.println("Full header length is " + headerLength);
		//System.out.println("File position is " + raf.getFilePointer());
		//System.out.println("Compressed chunks take up " + (chunkOffsets[chunkCount-1] + chunkLengths[chunkCount-1]));
	}

	// I think this is a bit uglier than I would like it to be. (But it works.)
	@Override
	public byte[] getChunk(long offset, long size) throws IOException {
		
		//System.out.println("Using DZ to get chunk");
		
		if (size > Integer.MAX_VALUE || size < 0) {
			throw new IllegalArgumentException("size must be a positive integer <= " + Integer.MAX_VALUE);
		}
		int size_int = (int) size;
		byte[] result = new byte[size_int];
		
		if (size_int == 0) {
			// I make the assumption that size >= 1
			return result;
		}
		
		// get the bytes
		int startBlock = (int) (offset / chunkDcLength); // gets floor
		if (startBlock >= chunkOffsets.length) {
			throw new IOException("Offset out of range");
		}
		int endBlock = (int) ((offset + size - 1) / chunkDcLength); // gets floor
		if (endBlock >= chunkOffsets.length) {
			throw new IOException("Size out of range");
		}
		raf.seek(headerLength + chunkOffsets[startBlock]);
		int blocksLen = 0;
		for (int i = startBlock; i <= endBlock; i++) {
			blocksLen += chunkLengths[i];
		}
		
		// fake a GZIP header so that I can use GZIPInputStream
		//byte[] gzBytes = new byte[GZ_HDR_LEN + blocksLen + GZ_FTR_LEN];
		//gzBytes[0] = (byte)GZ_MAGIC[0];
		//gzBytes[1] = (byte)GZ_MAGIC[1];
		//gzBytes[2] = (byte)method;
		// flags, mtime and extraFlags can all be zero
		//gzBytes[9] = (byte)os;
		//int compLength = (endBlock - startBlock + 1) * chunkDcLength;
		// should probably find some method that will do this for me. perhaps that would be overkill in this particular case.
		//gzBytes[gzBytes.length - 4] = (byte) (compLength & 0xFF);
		//gzBytes[gzBytes.length - 3] = (byte) ((compLength >> 8) & 0xFF);
		//gzBytes[gzBytes.length - 2] = (byte) ((compLength >> 16) & 0xFF);
		//gzBytes[gzBytes.length - 1] = (byte) ((compLength >> 24) & 0xFF);

		byte[] comBytes = new byte[blocksLen + 1]; // extra byte for Inflater with nowrap option
		raf.readFully(comBytes, 0, comBytes.length - 1);

		//raf.readFully(gzBytes, GZ_HDR_LEN, gzBytes.length - GZ_HDR_LEN - GZ_FTR_LEN);
		//GZIPInputStream zipStream = new GZIPInputStream(new ByteArrayInputStream(gzBytes));

		// N.B. think this should be okay even for last chunk,
		// as the size to get is specified by the value from the idx file
		InflaterInputStream zipStream = new InflaterInputStream(new ByteArrayInputStream(comBytes),
											new Inflater(true), chunkDcLength * (endBlock - startBlock + 1));

		zipStream.skip(offset % chunkDcLength);
		zipStream.read(result); // should be readFully
		
		return result;
	}

}
