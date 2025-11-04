package com.forrestguice.suntimes.nfc;

import android.content.Context;

import org.junit.Test;

import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.assertArrayEquals;

public class AddonSettingsTest
{
    @Test
    public void test_pref_dismissTag()
    {
        byte[] tag0 = new byte[] { 0, 0, 0, 0 };
        AddonSettings.savePrefDismissTag(getContext(), tag0);
        assertArrayEquals(tag0, AddonSettings.loadPrefDismissTag(getContext()));

        byte[] tag1 = new byte[] { 1, 1, 1, 1 };
        AddonSettings.savePrefDismissTag(getContext(), tag1);
        assertArrayEquals(tag1, AddonSettings.loadPrefDismissTag(getContext()));
    }

    public static Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }
}