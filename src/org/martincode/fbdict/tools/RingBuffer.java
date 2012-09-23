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

import java.io.IOException;
import java.io.InputStream;

public class RingBuffer {
	protected byte[] buffer;
	int pos, len;
	public RingBuffer(int capacity) {
		buffer = new byte[capacity];
	}
	
	public byte[] getBuffer() { return buffer; }
	public int getPos() { return pos; }
	public int getLen() { return len; }
	public int getCapacity() { return buffer.length; }
	
	public int append(byte[] data) {
		return append(data, 0, data.length);
	}
	public int append(byte[] data, int doff, int dlen) {
		int actualdlen = Math.min(dlen,  space());
		int leftdlen = actualdlen;
		int addpos = (pos + len) % buffer.length;
		if (addpos + leftdlen > buffer.length) {
			System.arraycopy(data, doff, buffer, addpos, buffer.length - addpos);
			doff += buffer.length - addpos;
			leftdlen -= buffer.length - addpos;
			addpos = 0;
		}
		System.arraycopy(data, doff, buffer, addpos, leftdlen);
		len += actualdlen; // update len in one place only
		return actualdlen;
	}
	public int space() {
		return buffer.length - len;
	}
	public int fill(Readable input) throws IOException {
		byte[] temp = new byte[space()];
		int readlen = input.read(temp);
		if (readlen > 0) {
			append(temp, 0, readlen);
		}
		return readlen;
	}
	public int skip(int num) {
		int actualskip = Math.min(num, len);
		pos = (pos + actualskip) % buffer.length;
		len -= actualskip;
		return actualskip;
	}
	public void reset() {
		pos = 0;
		len = 0;
	}
	public byte[] read(int num) {
		int actualread = Math.min(num,  len);
		int leftread = actualread;
		byte[] result = new byte[actualread];
		int doff = 0;
		if (pos + leftread > buffer.length) {
			System.arraycopy(buffer, pos, result, doff, buffer.length - pos);
			doff += buffer.length - pos;
			leftread -= buffer.length - pos;
			pos = 0;
		}
		System.arraycopy(buffer, pos, result, doff, leftread);
		pos = (pos + leftread) % buffer.length; // already might have changed pos
		len -= actualread; // only update len in one place
		return result;
	}
	
	// The following functions build on top of the main functions above:
	
	public long getUInt() {
		long result = 0;
		if (len >= 4) {
			byte[] bytes = read(4);
			result = (((long)bytes[0] & 0xFF) << 24) |
					 (((long)bytes[1] & 0xFF) << 16) |
					 (((long)bytes[2] & 0xFF) << 8) |
					  ((long)bytes[3] & 0xFF);
		}
		return result;
	}
	
	public byte[] getStrZSegment() {
		int strlen = len;
		for (int i = 0; i < len; i++) {
			if (buffer[(pos + i) % buffer.length] == 0) {
				strlen = i;
				break;
			}
		}
		
//		String result = "";
//		try {
//			result = new String(read(strlen), "UTF-8");
//		} catch (UnsupportedEncodingException e) {}
//		return result;
		
		return read(strlen);
	}
	
	public boolean skipNulTerminator() {
		if (len > 0 && buffer[pos] == 0) {
			skip(1);
			return true;
		} else {
			return false;
		}
	}
}
