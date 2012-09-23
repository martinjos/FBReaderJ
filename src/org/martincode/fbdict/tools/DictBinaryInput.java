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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class DictBinaryInput {
	protected Readable in;
	protected RingBuffer buf;
	public DictBinaryInput(InputStream in) {
		this(new ReadableStream(in), 512);
	}
	public DictBinaryInput(RandomAccessFile in) {
		this(new ReadableRAF(in), 512);
	}
	public DictBinaryInput(Readable in, int capacity) {
		this.in = in;
		buf = new RingBuffer(capacity);
	}
	public long readUInt() throws IOException {
		if (buf.getLen() < 4) {
			buf.fill(in);
		}
		if (buf.getLen() < 4) {
			throw new EOFException("Failed to read unsigned 32-bit integer");
		}
		return buf.getUInt();
	}
	public String readUTF8Z() throws IOException {
		List<byte[]> segments = new ArrayList<byte[]>(); // N.B. should probably write a wrapper class to do this
		if (buf.getLen() == 0) {
			buf.fill(in);
		}
		boolean gotNul = false;
		int totalLen = 0;
		while (buf.getLen() > 0 && !(gotNul = buf.skipNulTerminator())) {
			byte[] segment = buf.getStrZSegment();
			segments.add(segment);
			totalLen += segment.length;
			buf.fill(in);
		}
		if (!gotNul) {
			throw new EOFException("Failed to read NUL-terminated UTF-8 string");
		}
		
		// convert buffers to single buffer
		byte[] allSegments = new byte[totalLen];
		int pos = 0;
		for (int i = 0; i < segments.size(); i++) {
			System.arraycopy(segments.get(i), 0, allSegments, pos, segments.get(i).length);
			pos += segments.get(i).length;
		}
		
		return new String(allSegments, "UTF-8");
	}
}
