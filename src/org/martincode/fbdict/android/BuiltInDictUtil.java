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

package org.martincode.fbdict.android;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.IOException;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.martincode.fbdict.format.stardict.DictStorage;
import org.martincode.fbdict.format.stardict.DictEntry;

public class BuiltInDictUtil {

	protected static DictStorage stor;

	public static void init() {
		try {
			stor = new DictStorage("/sdcard/dict");
		} catch (IOException e) {
			Log.e("martincode", "");
		}
	}

	public static PopupWindow show(Activity activity, String text, boolean singleWord, int selectionTop, int selectionBottom) {

		String displayText = "Failed to retrieve dictionary entry.";

		if (stor == null) {
			//return;
			displayText = "Dictionary couldn't initialise";
		} else {

			List<DictEntry> entries = null;
			try {
				entries = stor.getEntries(text);
				if (entries == null) {
					displayText = "Not found.";
				} else {
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < entries.size(); i++) {
						if (i > 0) {
							sb.append('\n');
						}
						DictEntry entry = entries.get(i);
						for (int j = 0; j < entry.getSequence().length(); j++) {
							char ch = entry.getSequence().charAt(j);
							if (Character.isLowerCase(ch)) {
								// lowercase entries are likely to be human-readable
								sb.append(entry.getValue(ch));
							}
						}
					}
					displayText = sb.toString();
				}
			} catch (IOException e) {}
		}

		Context ctx = activity.getBaseContext();
		TextView textView = new TextView(ctx);
		PopupWindow popup = new PopupWindow(ctx);
		popup.setContentView(textView);

		textView.setText(displayText);
		
		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		popup.setWidth(metrics.widthPixels);
		//popup.setHeight(metrics.heightPixels - selectionBottom - 20);
		popup.setHeight(metrics.heightPixels / 2);

		// N.B. probably want to favour being at the top, if possible
		int selMid = (selectionTop + selectionBottom) / 2;
		int g = Gravity.LEFT;
		if (selMid > metrics.heightPixels / 2) {
			g |= Gravity.TOP;
		} else {
			g |= Gravity.BOTTOM;
		}
		
		popup.showAtLocation(activity.getCurrentFocus(), g, 0, 0);

		// it looks like UncaughtExceptionHandler does something similar to this
		//} catch (RuntimeException e) {
			//StringWriter strWr = new StringWriter();
			//PrintWriter toStr = new PrintWriter(strWr);
			//e.printStackTrace(toStr);
			//Log.e("martincode", "martincode: " + strWr.toString());
			//throw e;
		//}

		return popup;
	}
}
