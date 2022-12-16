/*
    Copyright (C) 2022 Forrest Guice
    This file is part of Suntimes.

    Suntimes is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Suntimes is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Suntimes.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.forrestguice.suntimes.alarmnfc;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Base64;

public class AddonSettings
{
    public static final String PREF_KEY_TAG_ANY = "anytag";
    public static final boolean PREF_DEF_TAG_ANY = false;

    public static final String PREF_KEY_TAG_DISMISS = "dismisstag";

    public static final String PREF_KEY_WRONG_TAG_LIMIT = "wrongtaglimit";
    public static final int PREF_DEF_WRONG_TAG_LIMIT = 5;


    public static int loadPrefWrongTagLimit(Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(PREF_KEY_WRONG_TAG_LIMIT, PREF_DEF_WRONG_TAG_LIMIT);
    }

    public static boolean loadPrefAnyTag(Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_KEY_TAG_ANY, PREF_DEF_TAG_ANY);
    }

    @Nullable
    public static byte[] loadPrefDismissTag(Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String value = prefs.getString(PREF_KEY_TAG_DISMISS, null);
        return value != null ? Base64.decode(value, Base64.NO_WRAP) : null;
    }

    public static void savePrefDismissTag(Context context, byte[] tagID)
    {
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
        prefs.putString(PREF_KEY_TAG_DISMISS, Base64.encodeToString(tagID, Base64.NO_WRAP));
        prefs.apply();
    }

}