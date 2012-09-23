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
import java.util.NavigableSet;

import org.martincode.fbdict.format.stardict.DictStorage;
import org.martincode.fbdict.format.stardict.SimplifiedNavigableSet;


public class DictWordsTest {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		DictStorage stor = new DictStorage(args[0]);
		SimplifiedNavigableSet<String> words = stor.getWordSet();
		// TODO: implement SimplifiedNavigableSet
		/*
		for (String word : words) {
			System.out.println(word);
		}
		*/
	}

}
