// SPDX-License-Identifier: GPL-3.0-or-later
/*
    Copyright (C) 2025 Forrest Guice
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

package com.forrestguice.suntimes.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.IntentFilter;

public interface NfcInterface
{
    String EXTRA_ID();
    void enableForegroundDispatch(Activity activity, PendingIntent pendingIntent, IntentFilter[] intentFilters, String[][] techlists);
    void disableForegroundDispatch(Activity activity);
}
