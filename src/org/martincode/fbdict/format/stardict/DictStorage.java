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

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.text.Collator;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Properties;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

//import android.util.Log;

import org.martincode.fbdict.tools.ChunkAccessFile;
import org.martincode.fbdict.tools.DZChunkAccessFile;
import org.martincode.fbdict.tools.DictBinaryInput;
import org.martincode.fbdict.tools.PlainChunkAccessFile;


public class DictStorage {

	protected String ifoName;
	protected String baseName;
	protected Properties ifoProps = new Properties();
	protected Collator wordIdxCollator;
	protected SimplifiedNavigableMap<String,List<WordIdxInfo>> wordIdx;
	protected ChunkAccessFile dictAccess;

	/**
	 * @param ifoName
	 * @throws IOException
	 */
	public DictStorage(String ifoName) throws IOException {
		this.ifoName = ifoName;

		System.err.println("In start of DictStorage.init()");

		// figure out basename (may have to improve error handling here)
		Pattern p = Pattern.compile("(?i)\\.ifo$");
		Matcher m = p.matcher(ifoName);
		if (!m.find()) {
			//throw new IOException(ifoName + " is not a .ifo file");
			m = null;
			File dir = new File(ifoName);
			if (dir.isDirectory()) {
				File[] list = dir.listFiles();
				for (File subfile: list) {
					m = p.matcher(subfile.getPath());
					if (m.find()) {
						ifoName = subfile.getPath();
						break;
					} else {
						m = null;
					}
				}
			}
		}
		if (m != null) {
			baseName = m.replaceFirst("");
		} else {
			//System.err.println("Warning: \"" + ifoName + "\" is not a .ifo file");
			//baseName = ifoName;
			throw new IOException(ifoName + " is not a .ifo file");
		}

		System.err.println("Just figured out baseName of ifo file");

		// get .ifo file contents
		BufferedReader rdr = new BufferedReader(new InputStreamReader(new FileInputStream(ifoName), "UTF-8"));
		String firstLine = rdr.readLine();
		// N.B.: seems to suffer from Byte-Order-Mark problems
		//System.out.println("The first line is: \"" + firstLine + "\" (length: "+firstLine.length()+"; first char: '"+firstLine.charAt(0)+"')");
		if (!firstLine.equals("StarDict's dict ifo file")) {
			System.err.println("Warning: \"" + ifoName + "\" does not start with the ifo file identifier");
		}
		ifoProps.clear();
		ifoProps.load(rdr);
		//ifoProps.list(System.out);
		rdr.close();

		System.err.println("Just got ifo file");

		// extract relevant properties
		if (ifoProps.containsKey("idxoffsetbits")) {
			String offsetBits = ((String)ifoProps.get("idxoffsetbits")).trim();
			if (!offsetBits.equals("32")) {
				throw new IOException("Unsupported value for idxoffsetbits: " + offsetBits);
			}
		}

		// set up map for .idx file contents
		// N.B. should really use a collator appropriate to the dictionary language,
		//      but for some reason the StarDict format doesn't seem to contain language information (!)
		wordIdxCollator = Collator.getInstance();
		wordIdxCollator.setStrength(Collator.SECONDARY); // letters + diacritics, but not case

		// load index (.idx file)
		File idxFile = new File(baseName + ".idx");
		File gzIdxFile = new File(baseName + ".idx.gz");

		if (idxFile.exists()) {
			System.err.println("Going to try using an IdxMap...");
			try {
				wordIdx = new IdxMap(idxFile);
			} catch (IOException e) {}
		}

		System.err.println("About to load idx file");

    	if (wordIdx == null) {

			InputStream idxIn = null;

			TreeMap<String,List<WordIdxInfo>> realIdx = new TreeMap<String,List<WordIdxInfo>>(wordIdxCollator);
			wordIdx = new SimplifiedMap<String,List<WordIdxInfo>>(realIdx);
			if (idxFile.exists()) {
				idxIn = new FileInputStream(idxFile);
			} else if (gzIdxFile.exists()) {
				idxIn = new GZIPInputStream(new FileInputStream(gzIdxFile));
			} else {
				throw new IOException(".idx file for \"" + ifoName + "\" not found");
			}

			DictBinaryInput idxInput = new DictBinaryInput(idxIn);

			System.err.println("Ready to read idx file");

			while (true) {
				String word = null;
				try {
					word = idxInput.readUTF8Z();
				} catch (EOFException e) {
					break; // EOF before NUL terminator
				}
				WordIdxInfo info = new WordIdxInfo();
				info.offset = idxInput.readUInt();
				info.size = idxInput.readUInt();
				//System.out.println("Word \"" + word + "\", offset " + info.offset + ", size " + info.size);
				if (!realIdx.containsKey(word)) {
					realIdx.put(word, new ArrayList<WordIdxInfo>());
				}
				realIdx.get(word).add(info);
			}
		}

		System.err.println("Setting up dict file access");

		// set up .dict file access
		File dictFile = new File(baseName + ".dict");
		File dzDictFile = new File(baseName + ".dict.dz");
		if (dictFile.exists()) {
			dictAccess = new PlainChunkAccessFile(dictFile);
		} else {
			dictAccess = new DZChunkAccessFile(dzDictFile);
		}
	}

	// only for internal use, really, and testing
	public List<WordIdxInfo> findWord(String word) {
		return wordIdx.get(word);
	}

	// this is what most external users should use
	public SimplifiedNavigableSet<String> getWordSet() {
		return wordIdx.navigableKeySet();
	}

	protected byte[] getEntryBytes(WordIdxInfo info) throws IOException {
		byte[] bytes = dictAccess.getChunk(info.offset, info.size);

		// DEBUG
		for (int i = 0; i < bytes.length; i++) {
			if (bytes[i] == 0) {
				System.out.println("First NUL character at position " + i + " (length = "+bytes.length+")");
				break;
			}
		}

		return bytes;
	}

	// only for testing purposes
	public String getEntryString(WordIdxInfo info) throws IOException {
		return new String(getEntryBytes(info), "UTF-8");
	}

	public DictEntry getEntry(String word) throws IOException {
		List<WordIdxInfo> info = findWord(word);
		if (info == null) {
			return null;
		}
		return getEntry(word, info.get(0));
	}

	public List<DictEntry> getEntries(String word) throws IOException {
		List<WordIdxInfo> infos = findWord(word);
		List<DictEntry> entries = new ArrayList<DictEntry>();
		if (infos == null) {
			return null;
		}
		for (WordIdxInfo info : infos) {
			System.out.println(info.offset + " " + info.size); // DEBUG
			DictEntry entry = getEntry(word, info);
			if (entry != null) {
				entries.add(entry);
			}
		}
		return entries;
	}

	public DictEntry getEntry(String word, WordIdxInfo info) throws IOException {
		if (info == null) {
			return null;
		}
		Map<String, Object> values = new HashMap<String, Object>();
		String sameTypeSequence = null;
		String sequence = "";
		if (ifoProps.containsKey("sametypesequence")) {
			sameTypeSequence = (String) ifoProps.get("sametypesequence");
			sequence = sameTypeSequence; 
			// guarantee they will all have an entry
			for (int i = 0; i < sameTypeSequence.length(); i++) {
				values.put(Character.toString(sameTypeSequence.charAt(i)), null);
			}
		}
		byte[] bytes = getEntryBytes(info);
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		int stsPos = 0;
		boolean skipNul;
		while (buf.hasRemaining()) {
			char nextType;
			skipNul = false;
			if (sameTypeSequence != null) {
				nextType = sameTypeSequence.charAt(stsPos);
				stsPos++;
			} else {
				nextType = String.format("%c", buf.get()).charAt(0);
			}
			byte[] nextBytes;
			int length;
			if (sameTypeSequence != null && stsPos == sameTypeSequence.length()) {
				// no need for length field
				length = buf.remaining();
			} else {
				if (Character.isUpperCase(nextType)) { 
					if (buf.remaining() >= 4) {
						// if it's big enough to matter that it's unsigned, it's too big.
						length = buf.getInt();
						if (length > buf.remaining()) {
							length = buf.remaining();
						}
					} else {
						System.out.println("Clearing buffer"); // DEBUG
						buf.clear();
						length = 0;
					}
				} else {
					int i;
					for (i = buf.position(); i < buf.limit() && bytes[i] != 0; i++) {
						// do nothing
					}
					length = i - buf.position();
					if (i < buf.limit()) {
						skipNul = true;
					}
				}
			}
			System.out.println("Got " + nextType + " of size " + length + " with " + buf.remaining() + " remaining"); // DEBUG
			nextBytes = new byte[length];
			buf.get(nextBytes);
			if (skipNul) {
				buf.get(); // skip NUL-terminator
			}
			if (sameTypeSequence == null) {
				sequence += nextType;
			}
			if (Character.isUpperCase(nextType)) {
				values.put(Character.toString(nextType), nextBytes);
			} else {
				// I'm assuming NUL-terminated strings (specified to use lower-case letters) will always be UTF-8
				values.put(Character.toString(nextType), new String(nextBytes, "UTF-8"));
			}
		}
		return new DictEntry(word, sequence, values);
	}
}
