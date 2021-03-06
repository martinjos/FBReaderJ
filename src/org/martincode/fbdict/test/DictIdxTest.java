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

import java.io.IOException;

import java.util.List;

import org.martincode.fbdict.format.stardict.DictStorage;
import org.martincode.fbdict.format.stardict.WordIdxInfo;


public class DictIdxTest {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		DictStorage stor = new DictStorage(args[0]);
		List<WordIdxInfo> infos = stor.findWord(args[1]);
		if (infos != null) {
			for (WordIdxInfo info : infos) {
				System.out.println("offset: " + info.offset + ", size: " + info.size);
			}
		} else {
			System.out.println("Not found");
		}
	}

}
