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

import java.util.Map;

public class DictEntry {
	protected String idxWord;
	protected String sequence;
	protected Map<String, Object> values;
	public DictEntry(String idxWord, String sequence,
			Map<String, Object> values) {
		super();
		this.idxWord = idxWord;
		this.sequence = sequence;
		this.values = values;
	}
	public String getIdxWord() { return idxWord; }
	public String getSequence() { return sequence; }
	public Map<String, Object> getEntries() { return values; }
	public Object getValue(char ch) {
		return getValue(Character.toString(ch));
	}
	public Object getValue(String tag) {
		if (values.containsKey(tag)) {
			return values.get(tag);
		}
		return null;
	} 
}
