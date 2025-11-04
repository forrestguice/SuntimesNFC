package com.forrestguice.suntimes.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.nfc.NfcAdapter;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.forrestguice.suntimes.nfc.TestRobot.getContext;
import static com.forrestguice.suntimes.nfc.TestRobot.setAnimationsEnabled;
import static com.forrestguice.suntimes.nfc.espresso.ViewAssertionHelper.assertHidden;
import static com.forrestguice.suntimes.nfc.espresso.ViewAssertionHelper.assertShown;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MainActivityTest
{
    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class);

    @Before
    public void beforeTest() throws IOException {
        setAnimationsEnabled(false);
    }
    @After
    public void afterTest() throws IOException {
        setAnimationsEnabled(true);
    }


    @Test
    public void test_mainActivity_configMode() throws Throwable
    {
        Intent intent = new Intent(getContext(), MainActivity.class);
        activityRule.launchActivity(intent);
        assertFalse(activityRule.getActivity().isFinishing());
        assertNull(activityRule.getActivity().alarmID);

        MainActivity activity = activityRule.getActivity();
        injectNFCAdapter(activityRule, createMockNfcAdapter());

        MainActivityRobot robot = new MainActivityRobot()
                .assertConfigMode()
                .assertNfcReady();

        byte[] tag0 = new byte[] { 1, 1, 1, 1 };
        robot.assertNfcScanLocked(activityRule, false)
                .simulateNFC(activityRule, tag0)
                .assertNfcAssigned(tag0)
                .assertNfcScanLocked(activityRule, true);
        assertArrayEquals(tag0, AddonSettings.loadPrefDismissTag(activity));

        robot.sleep(3000)    // wait for scan to unlock
                .assertNfcScanLocked(activityRule, false)
                .assertNfcReady();
    }

    @Test
    public void test_mainActivity_configMode_noNFC() throws Throwable
    {
        Intent intent = new Intent(getContext(), MainActivity.class);
        activityRule.launchActivity(intent);
        assertFalse(activityRule.getActivity().isFinishing());
        assertNull(activityRule.getActivity().alarmID);

        MainActivity activity = activityRule.getActivity();
        injectNFCAdapter(activityRule, null);

        assertFalse(activity.nfcSupported());
        new MainActivityRobot()
                .assertConfigMode()
                .assertNfcNotSupported();
    }

    @Test
    public void test_mainActivity_alarmMode() throws Throwable
    {
        Long alarmID0 = 123L;
        byte[] tag0 = new byte[] { 1, 1, 1, 1 };
        byte[] tag1 = new byte[] { 0, 0, 0, 1 };

        Context context = getContext();
        Intent intent = new Intent(context, MainActivity.class);
        intent.setData(Uri.parse(alarmID0 + ""));
        activityRule.launchActivity(intent);

        MainActivity activity = activityRule.getActivity();
        AddonSettings.savePrefDismissTag(activity, tag0);
        injectNFCAdapter(activityRule, createMockNfcAdapter());

        assertFalse(activity.isFinishing());
        assertEquals(alarmID0, activity.alarmID);
        assertTrue(activity.nfcSupported());
        MainActivityRobot robot = new MainActivityRobot()
                .assertAlarmMode()
                .assertNfcReady();

        robot.assertNfcScanLocked(activityRule, false)
                .simulateNFC(activityRule, tag1)    // simulate scanning wrong tag
                .assertNfcScanLocked(activityRule, true)
                .assertNfcWrongTag(tag1);

        robot.sleep(3000)    // wait for scan to unlock
                .assertNfcScanLocked(activityRule, false)
                .assertNfcReady();

        robot.simulateNFC(activityRule, tag0)    // simulate scanning right tag
                .assertNfcScanLocked(activityRule, true)
                .assertNfcRightTag(tag0)
                .sleep(1500);

        // TODO: test resultCode
        /*long startTime = System.currentTimeMillis();
        boolean isFinished = true;
        while (!activity.isFinishing()) {    // wait for activity to finish
            if (System.currentTimeMillis() - startTime > 2000) {   // fail after 2 seconds
                isFinished = false;
                break;
            }
        }
        assertTrue("Activity should have finished", isFinished);
        Instrumentation.ActivityResult result = activityRule.getActivityResult();
        Intent resultData = result.getResultData();
        assertEquals(MainActivity.RESULT_OK, result.getResultCode());                                               // the expected result...
        assertEquals(MainActivity.RESULT_OK, resultData.getIntExtra(Intent.EXTRA_RETURN_RESULT, -9));   // should contain resultCode
        assertEquals(alarmID0, (Long) ContentUris.parseId(resultData.getData()));                                   // and alarm URI
        */
    }

    @Test
    public void test_mainActivity_alarmMode_noNFC() throws Throwable
    {
        Long alarmID0 = 123L;
        Intent intent = new Intent(getContext(), MainActivity.class);
        intent.setData(Uri.parse(alarmID0 + ""));
        activityRule.launchActivity(intent);

        MainActivity activity = activityRule.getActivity();
        assertFalse(activity.isFinishing());
        assertEquals(alarmID0, activity.alarmID);

        injectNFCAdapter(activityRule, null);
        assertFalse(activity.nfcSupported());
        new MainActivityRobot()
                .assertAlarmMode()
                .assertNfcNotSupported();
    }

    @Test
    public void test_mainActivity_help()
    {
        new MainActivityRobot()
                .showOverflowMenu(getContext())
                .assertOverflowMenuShown()
                .clickOverflowMenu_help()
                .assertHelpShown();
    }

    @Test
    public void test_mainActivity_about()
    {
        new MainActivityRobot()
                .showOverflowMenu(getContext())
                .assertOverflowMenuShown()
                .clickOverflowMenu_about()
                .assertAboutShown();
    }

    /**
     * MainActivityRobot
     */
    public static class MainActivityRobot extends TestRobot.ActivityRobot<MainActivityRobot>
    {
        public MainActivityRobot() {
            setRobot(this);
        }

        public MainActivityRobot clickOverflowMenu_help() {
            onView(withText(R.string.action_help)).perform(click());
            return this;
        }

        public MainActivityRobot simulateNFC(ActivityTestRule<MainActivity> activityRule, byte[] tagID) throws Throwable
        {
            final MainActivity activity = activityRule.getActivity();
            Intent intent = new Intent(activity, MainActivity.class);
            intent.putExtra(NfcAdapter.EXTRA_ID, tagID);
            activityRule.runOnUiThread(new Runnable()
            {
                @Override
                public void run() {
                    activity.onNewIntent(intent);
                }
            });
            return this;
        }

        public MainActivityRobot assertOverflowMenuShown()
        {
            onView(withText(R.string.action_help)).check(assertShown);
            onView(withText(R.string.action_about)).check(assertShown);
            return this;
        }

        public MainActivityRobot assertHelpShown() {
            onView(withId(R.id.help_content)).check(assertShown);
            return this;
        }

        public MainActivityRobot assertAboutShown()
        {
            onView(withText(R.string.app_name)).check(assertShown);
            onView(withText(R.string.app_desc)).check(assertShown);
            onView(withId(R.id.txt_about_version)).check(assertShown);
            return this;
        }

        public MainActivityRobot assertConfigMode()
        {
            onView(withId(R.id.bottombar)).check(assertShown);
            onView(withText(R.string.label_configuration_mode)).check(assertShown);
            onView(withId(R.id.dismissButton)).check(assertHidden);
            return this;
        }

        public MainActivityRobot assertAlarmMode() {
            onView(withId(R.id.bottombar)).check(assertHidden);
            onView(withText(R.string.label_configuration_mode)).check(assertHidden);
            return this;
        }

        public MainActivityRobot assertNfcReady() {
            onView(withText(R.string.action_scantag)).check(assertShown);
            //onView(withText(R.string.summary_scantag)).check(assertShown);
            return this;
        }

        public MainActivityRobot assertNfcScanLocked(ActivityTestRule<MainActivity> activityRule, boolean locked) {
            if (locked) {
                assertTrue(activityRule.getActivity().scan_locked);
            } else assertFalse(activityRule.getActivity().scan_locked);
            return this;
        }

        public MainActivityRobot assertNfcAssigned(byte[] tagID) {
            onView(withText(R.string.label_saved_tag)).check(assertShown);
            onView(withText(Arrays.toString(tagID))).check(assertShown);
            return this;
        }

        public MainActivityRobot assertNfcRightTag(byte[] tagID) {
            onView(withText(R.string.label_right_tag)).check(assertShown);
            return this;
        }

        public MainActivityRobot assertNfcWrongTag(byte[] tagID) {
            onView(withText(R.string.label_wrong_tag)).check(assertShown);
            return this;
        }

        public MainActivityRobot assertNfcNotSupported() {
            onView(withText(R.string.label_not_supported)).check(assertShown);
            onView(withText(R.string.summary_not_supported)).check(assertShown);
            return this;
        }
    }

    /**
     * createMockNfcAdapter
     */
    protected NfcInterface createMockNfcAdapter()
    {
        return new NfcInterface() {
            @Override
            public String EXTRA_ID() {
                return NfcAdapter.EXTRA_ID;
            }

            @Override
            public void enableForegroundDispatch(Activity activity, PendingIntent pendingIntent, IntentFilter[] intentFilters, String[][] techlists) {
                /* EMPTY */
            }

            @Override
            public void disableForegroundDispatch(Activity activity) {
                /* EMPTY */
            }
        };
    }

    protected void injectNFCAdapter(ActivityTestRule<MainActivity> activityRule, NfcInterface nfc) throws Throwable
    {
        MainActivity activity = activityRule.getActivity();
        activityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.nfcAdapter = nfc;
                activity.initViews(activity);
            }
        });
    }

}