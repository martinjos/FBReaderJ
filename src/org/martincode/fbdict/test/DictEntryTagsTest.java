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

import org.martincode.fbdict.format.stardict.DictEntry;
import org.martincode.fbdict.format.stardict.DictStorage;
import org.martincode.fbdict.test.tools.Formatting;

public class DictEntryTagsTest {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		DictStorage stor = new DictStorage(args[0]);
		List<DictEntry> entries = stor.getEntries(args[1]);
		if (entries != null) {
			System.out.println("Got " + entries.size() + " entries");
			//System.out.println("offset: " + info.offset + ", size: " + info.size);
			for (int iE = 0; iE < entries.size(); iE++) {
				DictEntry entry = entries.get(iE);
				System.out.println(String.format("Entry %d\n Sequence is \"%s\" (length: %d; %s), and there are %d values", iE, entry.getSequence(), entry.getSequence().length(), Formatting.hex(entry.getSequence()), entry.getEntries().size()));
				for (int i = 0; i < entry.getSequence().length(); i++) {
					String type = Character.toString(entry.getSequence().charAt(i));
					if (Character.isLowerCase(type.charAt(0))) {
						System.out.println("  Type = '"+type+"'; length = "+((String)entry.getValue(type)).length()+"; value = \""+(String)entry.getValue(type)+"\"");
					} else {
						// you never know, it might still be valid UTF-8.
						System.out.println("  Type = '"+type+"'\nvalue = \""+new String((byte[])entry.getValue(type), "UTF-8")+"\"");
					}
				}
			}
		} else {
			System.out.println("Not found");
		}
	}

}
