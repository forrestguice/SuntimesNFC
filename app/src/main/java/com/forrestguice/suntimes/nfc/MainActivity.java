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

package com.forrestguice.suntimes.nfc;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Vibrator;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.forrestguice.suntimes.addon.AddonHelper;
import com.forrestguice.suntimes.addon.AppThemeInfo;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.widget.Toolbar;
import androidx.vectordrawable.graphics.drawable.ArgbEvaluator;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.forrestguice.suntimes.addon.LocaleHelper;
import com.forrestguice.suntimes.addon.SuntimesInfo;
import com.forrestguice.suntimes.addon.ui.Messages;
import com.forrestguice.suntimes.alarm.AlarmHelper;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity
{
    public static final String TAG = "AlarmNFC";

    public static final String DIALOG_HELP = "helpDialog";
    public static final String DIALOG_ABOUT = "aboutDialog";

    public static final int REQUEST_NFC_DISPATCH = 100;

    protected SuntimesInfo suntimesInfo = null;

    private Vibrator vibrate;
    private NfcAdapter nfcAdapter;

    private Long alarmID = null;
    private byte[] nfcTagID = null;
    private int wrongTagCount = 0;
    private boolean scan_locked = false;

    private ImageView icon;
    private TextView text_title, text_summary;

    @Override
    protected void attachBaseContext(Context context)
    {
        AppThemeInfo.setFactory(new AppThemes());
        suntimesInfo = SuntimesInfo.queryInfo(context);    // obtain Suntimes version info
        super.attachBaseContext( (suntimesInfo != null && suntimesInfo.appLocale != null) ?    // override the locale
                LocaleHelper.loadLocale(context, suntimesInfo.appLocale) : context );
    }

    public void onSaveInstanceState( Bundle outState )
    {
        super.onSaveInstanceState(outState);
        if (alarmID != null) {
            outState.putLong("alarmID", alarmID);
        }
        outState.putInt("wrongTagCount", wrongTagCount);
        outState.putByteArray("nfcTagID", nfcTagID);
    }
    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedState)
    {
        super.onRestoreInstanceState(savedState);
        long id = savedState.getLong("alarmID", -1);
        alarmID = (id != -1 ? id : null);
        wrongTagCount = savedState.getInt("wrongTagCount", 0);
        nfcTagID = savedState.getByteArray("nfcTagID");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        Messages.forceActionBarIcons(menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (suntimesInfo.appTheme != null) {    // override the theme
            AppThemeInfo.setTheme(this, suntimesInfo);
        }

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

        vibrate = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcTagID = AddonSettings.loadPrefDismissTag(this);

        initViews(this);
    }

    protected void initViews(Context context)
    {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
            if (alarmID == null) {
                actionBar.setHomeAsUpIndicator(R.drawable.ic_launcher_action);
            }
        }

        View bottomBar = findViewById(R.id.bottombar);
        if (bottomBar != null) {
            bottomBar.setVisibility(alarmID == null ? View.VISIBLE : View.GONE);
        }

        icon = (ImageView) findViewById(R.id.icon);
        if (icon != null)
        {
            icon.setColorFilter(null);
            animateColors();
        }

        text_title = (TextView) findViewById(R.id.text_title);
        if (text_title != null) {
            text_title.setText(context.getString(nfcSupported() ? R.string.action_scantag : R.string.label_not_supported));
        }

        text_summary = (TextView) findViewById(R.id.text_summary);
        if (text_summary != null) {
            text_summary.setText(context.getString(nfcSupported() ? R.string.summary_scantag : R.string.summary_not_supported));
        }

        FloatingActionButton dismissButton = (FloatingActionButton) findViewById(R.id.dismissButton);
        if (dismissButton != null)
        {
            dismissButton.setOnClickListener(onDismissClicked);
            if (nfcSupported() || alarmID == null)
            {
                dismissButton.hide();
                dismissButton.setEnabled(false);
            } else {
                dismissButton.show();
                dismissButton.setEnabled(true);
            }
        }

        FloatingActionButton snoozeButton = (FloatingActionButton) findViewById(R.id.snoozeButton);
        if (snoozeButton != null) {
            snoozeButton.setOnClickListener(onSnoozeClicked);
            snoozeButton.hide();  // TODO
        }

        FloatingActionButton helpButton = (FloatingActionButton) findViewById(R.id.helpButton);
        if (helpButton != null) {
            helpButton.setOnClickListener(onHelpClicked);
        }

        FloatingActionButton aboutButton = (FloatingActionButton) findViewById(R.id.aboutButton);
        if (aboutButton != null) {
            aboutButton.setOnClickListener(onAboutClicked);
        }
    }

    private final View.OnClickListener onHelpClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            showHelp();
        }
    };
    private final View.OnClickListener onAboutClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            showAbout();
        }
    };
    private final View.OnClickListener onSnoozeClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            snoozeAlarm();
        }
    };
    private final View.OnClickListener onDismissClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            dismissAlarm();
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();
        if (itemId == R.id.action_help)
        {
            showHelp();
            return true;

        } else if (itemId == R.id.action_about) {
            showAbout();
            return true;

        } else if (itemId == android.R.id.home) {
            if (alarmID != null) {
                cancelDismiss();
            } else {
                AddonHelper.startSuntimesAlarmsActivity(MainActivity.this);
            }
            return true;

        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    protected void cancelDismiss()
    {
        setResult(RESULT_CANCELED, new Intent().setData(AlarmHelper.getAlarmUri(alarmID)));
        finish();
    }

    protected void snoozeAlarm()
    {
        setResult(RESULT_CANCELED, new Intent().setData(AlarmHelper.getAlarmUri(alarmID)).setAction(AlarmHelper.ACTION_SNOOZE));
        finish();
    }

    protected void dismissAlarm()
    {
        setResult(RESULT_OK, alarmID != null ? new Intent().setData(AlarmHelper.getAlarmUri(alarmID)).putExtra(Intent.EXTRA_RETURN_RESULT, RESULT_OK) : null);
        Log.d("DEBUG", "dismissAlarm: setResult: RESULT_OK: " + alarmID);
        finish();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        checkVersion();
        registerNfcDispatch();
    }

    @Override
    protected void onPause()
    {
        disableNfcDispatch();
        super.onPause();
    }

    protected boolean nfcSupported() {
        return (nfcAdapter != null);
    }

    protected void registerNfcDispatch()
    {
        if (nfcAdapter != null)
        {
            Intent intent = new Intent(this, this.getClass());
            intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            int pendingIntentFlags = PendingIntent.FLAG_CANCEL_CURRENT;
            if (Build.VERSION.SDK_INT >= 31) {
                pendingIntentFlags |= PendingIntent.FLAG_MUTABLE;    // NFC functionality requires MUTABLE (grants ability to alter intent semantics)
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(this, REQUEST_NFC_DISPATCH, intent, pendingIntentFlags);
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, new IntentFilter[] {}, null);
        }
    }

    protected void disableNfcDispatch()
    {
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    protected void onTagScanned(byte[] tagID)
    {
        scan_locked = true;
        stopAnimateColors();
        icon.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.tag_true));

        if (alarmID != null) {
            text_title.setText(getString(R.string.label_right_tag));
            text_summary.setVisibility(View.INVISIBLE);

        } else {
            AddonSettings.savePrefDismissTag(MainActivity.this, tagID);
            text_title.setText(getString(R.string.label_saved_tag));
            text_summary.setText(Arrays.toString(tagID));
        }

        int animationDelay = getResources().getInteger(alarmID != null ? R.integer.right_tag_delay_ms : R.integer.save_tag_delay_ms);
        icon.postDelayed(new Runnable() {
            @Override
            public void run()
            {
                if (alarmID != null) {
                    dismissAlarm();

                } else {
                    scan_locked = false;
                    text_title.setText(getString(R.string.action_scantag));
                    text_summary.setText(getString(R.string.summary_scantag));
                    icon.setColorFilter(null);
                    animateColors();
                }
            }
        }, animationDelay);
    }

    protected void onWrongTag(byte[] tagID)
    {
        scan_locked = true;
        wrongTagCount++;

        stopAnimateColors();
        icon.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.tag_false));

        text_title.setText(getString(R.string.label_wrong_tag));

        int wrongTagLimit = AddonSettings.loadPrefWrongTagLimit(MainActivity.this);
        int numRetries = (wrongTagLimit - wrongTagCount) + 1;
        String retriesDisplay = getResources().getQuantityString(R.plurals.scanNumMoreTimes, numRetries, numRetries);
        text_summary.setText(getString(R.string.summary_wrong_tag, retriesDisplay));

        icon.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                scan_locked = false;
                text_title.setText(getString(R.string.action_scantag));
                text_summary.setText("");
                icon.setColorFilter(null);
                animateColors();
            }
        }, getResources().getInteger(R.integer.wrong_tag_delay_ms));
    }

    @Override
    public void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        byte[] tagID = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
        if (tagID != null && !scan_locked)
        {
            Log.d(TAG, "onNewIntent: nfcTag: " + Arrays.toString(tagID));
            if (vibrate != null) {
                vibrate.vibrate(getResources().getInteger(R.integer.scan_tag_vibrate_ms));
            }

            if (AddonSettings.loadPrefAnyTag(MainActivity.this) || alarmID == null || nfcTagID == null || Arrays.equals(tagID, nfcTagID))
            {
                Log.i(TAG, "onNewIntent: nfcTag: tag scanned! " + Arrays.toString(nfcTagID) + " .. wrongTagCount: " + wrongTagCount);
                if (nfcTagID == null)
                {
                    Log.i(TAG, "onNewIntent: nfcTag: null tag, initializing to " + Arrays.toString(nfcTagID));
                    AddonSettings.savePrefDismissTag(MainActivity.this, tagID);
                }
                onTagScanned(tagID);

            } else {
                Log.w(TAG, "onNewIntent: nfcTag: wrong tag! " + Arrays.toString(tagID) + ", expected " + Arrays.toString(nfcTagID));
                if (wrongTagCount >= AddonSettings.loadPrefWrongTagLimit(MainActivity.this))
                {
                    Log.w(TAG, "onNewIntent: nfcTag: repeated wrong tag, updating to " + Arrays.toString(nfcTagID));
                    AddonSettings.savePrefDismissTag(MainActivity.this, tagID);
                    onTagScanned(tagID);
                } else {
                    onWrongTag(tagID);
                }
            }
        }
    }

    protected void checkVersion()
    {
        if (!SuntimesInfo.checkVersion(this, suntimesInfo))
        {
            View view = getWindow().getDecorView().findViewById(android.R.id.content);
            if (!suntimesInfo.hasPermission && suntimesInfo.isInstalled) {
                Messages.showPermissionDeniedMessage(this, view);
            } else {
                Messages.showMissingDependencyMessage(this, view);
            }
        }
    }

    protected void showHelp()
    {
        HelpDialog dialog = new HelpDialog();
        if (suntimesInfo != null && suntimesInfo.appTheme != null) {
            dialog.setTheme(AppThemeInfo.themePrefToStyleId(MainActivity.this, AppThemeInfo.themeNameFromInfo(suntimesInfo)));
        }

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
        AboutDialog dialog = MainActivity.createAboutDialog(MainActivity.this, suntimesInfo);
        dialog.show(getSupportFragmentManager(), DIALOG_ABOUT);
    }

    public static AboutDialog createAboutDialog(Context context, @Nullable SuntimesInfo suntimesInfo)
    {
        AboutDialog dialog = new AboutDialog();
        if (suntimesInfo != null) {
            dialog.setVersion(suntimesInfo);
            if (suntimesInfo.appTheme != null) {
                dialog.setTheme(AppThemeInfo.themePrefToStyleId(context, AppThemeInfo.themeNameFromInfo(suntimesInfo)));
            }
        }
        return dialog;
    }

    private Object animationObj;
    private void animateColors()
    {
        if (nfcSupported())
        {
            int startColor = ContextCompat.getColor(MainActivity.this, R.color.tag_scan_start);
            int endColor = ContextCompat.getColor(MainActivity.this, R.color.tag_scan_end);
            int duration = getResources().getInteger(R.integer.anim_scan_duration);
            animateColors(startColor, endColor, duration, new AccelerateDecelerateInterpolator(MainActivity.this, null));

        } else {
            int disabledColor = ContextCompat.getColor(MainActivity.this, R.color.tag_disabled);
            if (icon != null) {
                icon.setColorFilter(disabledColor);
            }
        }
    }
    private void animateColors(int startColor, int endColor, long duration, @Nullable TimeInterpolator interpolator)
    {
        if (icon != null)
        {
            @SuppressLint("RestrictedApi")
            ValueAnimator animation = ValueAnimator.ofObject(new ArgbEvaluator(), startColor, endColor);
            animationObj = animation;
            animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
            {
                @Override
                public void onAnimationUpdate(ValueAnimator animator)
                {
                    int animatedValue = (int) animator.getAnimatedValue();
                    icon.setColorFilter(animatedValue);
                }
            });

            animation.setRepeatCount(ValueAnimator.INFINITE);
            animation.setRepeatMode(ValueAnimator.REVERSE);

            if (interpolator != null) {
                animation.setInterpolator(interpolator);
            }
            animation.setDuration(duration);
            animation.start();
        }
    }
    private void stopAnimateColors()
    {
        ValueAnimator animation = (ValueAnimator)animationObj;
        if (animation != null) {
            animation.removeAllUpdateListeners();
        }
    }

}