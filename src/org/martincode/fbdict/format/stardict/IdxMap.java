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

import java.io.File;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.text.Collator;
import java.util.AbstractMap;
import java.util.NavigableMap;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Comparator;

import org.martincode.fbdict.tools.DictBinaryInput;

public class IdxMap implements SimplifiedNavigableMap<String, List<WordIdxInfo>> {

	public static final int BLOCK_SIZE = 32;
	public static final String stdHeader = "StarDict's oft file\nversion=2.4.8\nurl=";

	protected File idxFile;
	protected File oftFile;
	protected RandomAccessFile idxRaf;
	protected RandomAccessFile oftRaf;

	protected Collator comp;

	protected int headerLen;
	protected int numBlocks;
	protected int numWords;

	protected void normalInit() throws IOException {
		idxRaf = new RandomAccessFile(idxFile, "r");
		oftRaf = new RandomAccessFile(oftFile, "r");
		for (int i = 0; i < 3; i++) {
			oftRaf.readLine();
		}
		oftRaf.read(); // read NUL
		numWords = oftRaf.readInt();
		numBlocks = (numWords + BLOCK_SIZE - 1) / BLOCK_SIZE;
		headerLen = (int) oftRaf.getFilePointer();
		System.err.println(String.format("# blocks: %d, words: %d", numBlocks, numWords));
	}

	// N.B. should only throw exception if failed to create oft
	public IdxMap(File idxFile) throws IOException {
		System.err.println("Trying to create IdxMap...");
		this.idxFile = idxFile;
		//comp = new StarDictComparator();
		comp = Collator.getInstance(Locale.ROOT);
		comp.setStrength(Collator.SECONDARY);
		this.oftFile = new File(idxFile.getPath() + ".oft");
		this.comp = comp;
		if (!oftFile.exists()) {
			System.err.println("Creating oft file...");
			DataOutputStream oftOut = null;
			try {
				oftOut = new DataOutputStream(new FileOutputStream(oftFile));
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.err.println("1");
			InputStream idxIn = new FileInputStream(idxFile);
			System.err.println("2");
			DictBinaryInput idxInput = new DictBinaryInput(idxIn);
			System.err.println("3");
			String header = stdHeader + idxFile.getName() + "\n";
			System.err.println("Writing header...");
			try {
				oftOut.write(header.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {}
			oftOut.writeByte(0); // for some reason it's supposed to have a NUL at the end (I think)
			int pos = 0;
			numWords = 0;
			List<Integer> ents = new ArrayList<Integer>();
			int savePos;
			System.err.println("Collecting words from idx file...");
			while (true) {
				//System.err.print(".");
				savePos = pos;
				try {
					int ch;
					while ((ch = idxIn.read()) > 0) {
						pos++;
					}
					if (ch == -1) {
						break;
					}
					pos++; // already read NUL!
					for (int i = 0; i < 8; i++) {
						ch = idxIn.read();
						if (ch == -1) {
							break;
						}
						pos++;
					}
				} catch (IOException e) {
					break;
				}
				if (numWords % BLOCK_SIZE == 0) {
					//System.err.println("Adding " + savePos);
					//System.err.print("#");
					ents.add(savePos);
				}
				numWords++;
			}
			oftOut.writeInt(numWords); // need to write # words, not blocks!
			System.err.println("\nFinished collecting words");
			numBlocks = ents.size();
			System.err.println("Writing positions to oft file...");
			for (int i = 0; i < ents.size(); i++) {
				oftOut.writeInt(ents.get(i));
			}
			oftOut.close();
			oftRaf = new RandomAccessFile(oftFile, "r");
		}
		System.err.println("Performing initialisation from oft file...");
		normalInit();
		System.err.println("Finished initialising IdxMap.");
	}

	protected int getBlockPos(int blockNum) throws IOException {
		//System.err.println(String.format("Trying to seek to %d in oftRaf, for block %d", headerLen + 4 * blockNum, blockNum));
		oftRaf.seek(headerLen + 4 * blockNum);
		return oftRaf.readInt();
	}

	protected WordIdxInfo getWord(int wordPos) throws IOException {
		idxRaf.seek(wordPos);
		DictBinaryInput input = new DictBinaryInput(idxRaf);
		WordIdxInfo info = new WordIdxInfo();
		info.word = input.readUTF8Z();
		info.offset = input.readUInt();
		info.size = input.readUInt();
		return info;
	}

	protected WordIdxInfo getAt(String key, int block) throws IOException {
		int blockPos = getBlockPos(block);
		System.err.println(String.format("Searching block (%d)", blockPos));
		idxRaf.seek(blockPos);
		DictBinaryInput input = new DictBinaryInput(idxRaf);
		WordIdxInfo info = new WordIdxInfo();
		for (int i = 0; i < BLOCK_SIZE; i++) {
			try {
				info.word = input.readUTF8Z();
				info.offset = input.readUInt();
				info.size = input.readUInt();
			} catch (IOException e) {
				break;
			}
			System.err.print(String.format("Testing %s against %s...", key, info.word));
			if (comp.compare(key, info.word) == 0) {
				System.err.println("yes match!");
				return info;
			} else {
				System.err.println("no match...");
			}
		}
		return null;
	}

	protected WordIdxInfo getWithin(String key, int start, int end) throws IOException {
		//System.err.println(String.format("getWithin(\"%s\", %d, %d)", key, start, end));
		if (start > end - 1) {
			// zero blocks - painted self into corner
			return null;
		} else if (start == end - 1) {
			// only one block
			return getAt(key, start);
		}
		int middle = (start + end) / 2;
		int blockPos = getBlockPos(middle); // need to be careful here
		WordIdxInfo info = getWord(blockPos);
		int side = comp.compare(key, info.word);
		System.err.println(String.format("Tested %s against %s, result %d", key, info.word, side));
		if (side < 0) {
			return getWithin(key, start, middle);
		} else if (side > 0) {
			// try start of next block
			boolean inThisBlock = false;
			if (middle + 1 < end) {
				blockPos = getBlockPos(middle + 1);
				info = getWord(blockPos);
				int nextSide = comp.compare(key, info.word);
				System.err.println(String.format("-- tested %s against %s, result %d", key, info.word, nextSide));
				if (nextSide < 0) {
					inThisBlock = true;
					System.err.println("-- in this block!");
				} else if (nextSide == 0) {
					return info; // might just have struck lucky!
				}
			} else {
				System.err.println("Must be in this block!");
				inThisBlock = true;
			}
			if (inThisBlock) {
				return getAt(key, middle);
			} else {
				return getWithin(key, middle + 1, end);
			}
		} else {
			return info;
		}
	}

	public List<WordIdxInfo> get(String key) {
		WordIdxInfo info = null;
		System.out.println("About to try getting information on " + key);
		try {
			info = getWithin(key, 0, numBlocks);
		} catch (IOException e) {
			info = null;
		}
		if (info != null) {
			List<WordIdxInfo> result = new ArrayList<WordIdxInfo>();
			result.add(info);
			return result;
		} else {
			return null;
		}
	}

	public SimplifiedNavigableSet<String> navigableKeySet() {
		return null;
	}
}
