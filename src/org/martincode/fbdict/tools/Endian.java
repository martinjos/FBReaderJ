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

public class Endian {
	public static long swapInt(long intAsLong) {
		return ((0xFF & intAsLong) << 24) |
				((0xFF00 & intAsLong) << 8) | 
				((0xFF0000 & intAsLong) >> 8) | 
				((0xFF000000 & intAsLong) >> 24); 
	}
	public static int swapShort(int shortAsInt) {
		return ((0xFF & shortAsInt) << 8) |
				((0xFF00 & shortAsInt) >> 8);
	}
}
