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

import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.forrestguice.suntimes.addon.LocaleHelper;
import com.forrestguice.suntimes.addon.SuntimesInfo;
import com.forrestguice.suntimes.alarm.AlarmHelper;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity
{
    public static final String TAG = "AlarmNFC";
    public static final String DIALOG_HELP = "helpDialog";
    public static final String DIALOG_ABOUT = "aboutDialog";

    private NfcAdapter nfcAdapter;
    private long alarmID = -1;
    private byte[] nfcTagID = null;
    private int wrongTagCount = 0;

    protected SuntimesInfo suntimesInfo = null;

    @Override
    protected void attachBaseContext(Context context)
    {
        suntimesInfo = SuntimesInfo.queryInfo(context);    // obtain Suntimes version info
        super.attachBaseContext( (suntimesInfo != null && suntimesInfo.appLocale != null) ?    // override the locale
                LocaleHelper.loadLocale(context, suntimesInfo.appLocale) : context );
    }

    public void onSaveInstanceState( Bundle outState )
    {
        super.onSaveInstanceState(outState);
        outState.putInt("wrongTagCount", wrongTagCount);
        outState.putLong("alarmID", alarmID);
        outState.putByteArray("nfcTagID", nfcTagID);
    }
    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedState)
    {
        super.onRestoreInstanceState(savedState);
        wrongTagCount = savedState.getInt("wrongTagCount", 0);
        alarmID = savedState.getLong("alarmID", alarmID);
        nfcTagID = savedState.getByteArray("nfcTagID");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setResult(RESULT_CANCELED);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        Intent intent = getIntent();
        if (intent != null)
        {
            Uri data = intent.getData();
            if (data != null) {
                alarmID = ContentUris.parseId(data);
            }
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcTagID = AddonSettings.loadPrefDismissTag(this);
        initViews(this);
    }

    protected void initViews(Context context)
    {
        FloatingActionButton helpButton = (FloatingActionButton) findViewById(R.id.helpButton);
        if (helpButton != null)
        {
            helpButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showHelp();
                }
            });
        }

        FloatingActionButton aboutButton = (FloatingActionButton) findViewById(R.id.aboutButton);
        if (aboutButton != null)
        {
            aboutButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showAbout();
                }
            });
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (nfcAdapter != null)
        {
            int requestCode = 0;
            Intent intent = new Intent(this, this.getClass());
            intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, requestCode, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, new IntentFilter[] {}, null);
        }
    }

    @Override
    protected void onPause()
    {
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
        super.onPause();
    }

    @Override
    public void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        byte[] tagID = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
        if (tagID != null)
        {
            Log.d(TAG, "onNewIntent: nfcTag: " + Arrays.toString(tagID));
            if (AddonSettings.loadPrefAnyTag(MainActivity.this) || nfcTagID == null || Arrays.equals(tagID, nfcTagID))
            {
                Log.i(TAG, "onNewIntent: nfcTag: tag scanned! " + Arrays.toString(nfcTagID));
                if (nfcTagID == null || wrongTagCount >= AddonSettings.loadPrefWrongTagLimit(MainActivity.this))
                {
                    Log.i(TAG, "onNewIntent: nfcTag: null tag, updating to " + Arrays.toString(nfcTagID));
                    AddonSettings.savePrefDismissTag(MainActivity.this, tagID);
                }
                onTagScanned();

            } else {
                Log.w(TAG, "onNewIntent: nfcTag: wrong tag! " + Arrays.toString(tagID) + ", expected " + Arrays.toString(nfcTagID));
                onWrongTag();
            }
        }
    }

    protected void onTagScanned()
    {
        setResult(RESULT_OK, new Intent().setData(AlarmHelper.getAlarmUri(alarmID)));
        finish();
    }

    protected void onWrongTag()
    {
        wrongTagCount++;
        Toast.makeText(MainActivity.this, getString(R.string.message_wrong_tag), Toast.LENGTH_SHORT).show();
    }

    protected void showHelp()
    {
        HelpDialog dialog = new HelpDialog();
        //if (suntimesInfo != null && suntimesInfo.appTheme != null) {
        //    dialog.setTheme(getThemeResID(suntimesInfo.appTheme));
       // }

        String[] help = getResources().getStringArray(R.array.help_topics);
        String helpContent = help[0];
        for (int i=1; i<help.length; i++) {
            helpContent = getString(R.string.format_help, helpContent, help[i]);
        }
        dialog.setContent(helpContent + "<br/>");
        dialog.show(getSupportFragmentManager(), DIALOG_HELP);
    }

    protected void showAbout()
    {
        AboutDialog dialog = MainActivity.createAboutDialog(suntimesInfo);
        dialog.show(getSupportFragmentManager(), DIALOG_ABOUT);
    }

    public static AboutDialog createAboutDialog(@Nullable SuntimesInfo suntimesInfo)
    {
        AboutDialog dialog = new AboutDialog();
        if (suntimesInfo != null) {
            dialog.setVersion(suntimesInfo);
            //if (suntimesInfo.appTheme != null) {
            //    dialog.setTheme(getThemeResID(suntimesInfo.appTheme));
            //}
        }
        return dialog;
    }

}