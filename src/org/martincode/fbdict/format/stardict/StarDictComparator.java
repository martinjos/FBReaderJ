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

package org.martincode.fbdict.format.stardict;

import java.util.Comparator;
import java.io.UnsupportedEncodingException;

public class StarDictComparator implements Comparator<Object> {
	public int compare(Object o1, Object o2) {
		String s1 = o1.toString();
		String s2 = o2.toString();
		byte[] b1 = null;
		byte[] b2 = null;
		try {
			b1 = s1.getBytes("UTF-8");
			b2 = s2.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {}
		int len = Math.min(s1.length(), s2.length());
		for (int i = 0; i < len; i++) {
			if (b1[i] != b2[i]) {
				int upp1 = b1[i] & ~0x20;
				int upp2 = b2[i] & ~0x20;
				if (upp1 >= 'A' && upp1 <= 'Z' && upp2 >= 'A' && upp2 <= 'Z') {
					if (upp1 < upp2) {
						return -1;
					} else if (upp1 > upp2) {
						return 1;
					}
					// else fall back on normal behaviour
				}
				if (b1[i] < b2[i]) {
					return -1;
				} else {
					return 1;
				}
			}
		}
		if (s1.length() < s2.length()) {
			return -1;
		} else if (s1.length() > s2.length()) {
			return 1;
		} else {
			return 0;
		}
	}
	public boolean equals(Object obj) {
		return obj.getClass() == this.getClass();
	}
}
