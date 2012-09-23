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

package org.martincode.fbdict.test;

import java.io.UnsupportedEncodingException;

import org.martincode.fbdict.tools.RingBuffer;


public class RingBufferTest {

	/**
	 * @param args
	 * @throws UnsupportedEncodingException 
	 */
	public static void main(String[] args) throws UnsupportedEncodingException {
		RingBuffer buf = new RingBuffer(20);
		int num;
		String str;
		
		System.out.println("Start");
		printBuffer(buf);
		num = buf.append("Hello World!".getBytes("ASCII"));
		System.out.println("Added " + num);
		printBuffer(buf);
		num = buf.append("Hello World!".getBytes("ASCII"));
		System.out.println("Added " + num);
		printBuffer(buf);
		buf.reset();
		System.out.println("Reset buffer");
		printBuffer(buf);
		
		num = buf.append("Foo bar baz quux".getBytes("ASCII"));
		System.out.println("Added " + num);
		printBuffer(buf);
		
		num = buf.skip(16); // 8
		System.out.println("Skipped " + num);
		printBuffer(buf);
		
		num = buf.append("FOO BAR BAZ QUUX".getBytes("ASCII"));
		System.out.println("Added " + num);
		printBuffer(buf);
		
		str = new String(buf.read(8), "ASCII");
		System.out.println("Read \"" + str + "\"");
		printBuffer(buf);
		
		str = new String(buf.read(4), "ASCII");
		System.out.println("Read \"" + str + "\"");
		printBuffer(buf);
		
		str = new String(buf.read(10), "ASCII");
		System.out.println("Read \"" + str + "\" (tried to read 10 bytes, but only got " + str.length() + ")");
		printBuffer(buf);
		
		//num = buf.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes("ASCII"));
		//System.out.println("Added " + num + " - new length = " + buf.getLen());
		//printBuffer(buf);
	}
	
	protected static final String backslashEscapes = "abtnvfr";
	
	protected static void printBuffer(RingBuffer buf) {
		byte[] buffer = buf.getBuffer();
		for (int i = 0; i < buffer.length; i++) {
			if (buffer[i] >= 0x20 && buffer[i] < 0x7F) {
				try {
					System.out.print(" " + new String(buffer, i, 1, "ASCII"));
				} catch (UnsupportedEncodingException e) {}
			} else if (buffer[i] >= 7 && buffer[i] <= 13) {
				System.out.print("\\" + backslashEscapes.charAt(buffer[i] - 7));
			} else {
				System.out.print(String.format("%02x", buffer[i]));
			}
			System.out.print(" ");
		}
		System.out.println();
		
		int pos = buf.getPos();
		int len = buf.getLen();
		int cap = buf.getCapacity();
		
		for (int i = 0; i < cap; i++) {
			if ((i >= pos && i < pos + len) ||
					    (pos + len > cap && i < (pos + len) % cap)) {
				if (i == pos) {
					System.out.print("-^-");					
				} else {
					System.out.print("---");
				}
			} else if (i == pos) {
				System.out.print(" ^ ");
			} else {
				System.out.print("   ");
			}
		}
		System.out.println("\n"); // two lines
	}

}
