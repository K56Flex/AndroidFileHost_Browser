package browser.afh.activities;

/*
 * Copyright (C) 2016 Harsh Shandilya (MSF-Jarvis)
 */
/*
 * This file is part of AFH Browser.
 *
 * AFH Browser is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AFH Browser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AFH Browser. If not, see <http://www.gnu.org/licenses/>.
 */

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.view.View;

import com.afollestad.appthemeengine.ATE;
import com.afollestad.appthemeengine.ATEActivity;
import com.afollestad.appthemeengine.Config;
import com.afollestad.appthemeengine.customizers.ATEActivityThemeCustomizer;
import com.afollestad.appthemeengine.prefs.ATEColorPreference;
import com.afollestad.appthemeengine.prefs.ATESwitchPreference;
import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.afollestad.materialdialogs.prefs.MaterialListPreference;

import browser.afh.MainActivity;
import browser.afh.R;

public class PreferencesActivity extends ATEActivity implements ColorChooserDialog.ColorCallback, ATEActivityThemeCustomizer {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment
    {
        String mAteKey;

        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            invalidateSettings();
        }

        public void invalidateSettings() {
            mAteKey = MainActivity.getATEKey();

            ATEColorPreference primaryColorPref = (ATEColorPreference) findPreference("primary_color");
            primaryColorPref.setColor(Config.primaryColor(getActivity(), mAteKey), Color.BLACK);
            primaryColorPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new ColorChooserDialog.Builder((PreferencesActivity) getActivity(), R.string.primary_color)
                            .preselect(Config.primaryColor(getActivity(), mAteKey))
                            .show();
                    return true;
                }
            });

            ATEColorPreference accentColorPref = (ATEColorPreference) findPreference("accent_color");
            accentColorPref.setColor(Config.accentColor(getActivity(), mAteKey), Color.BLACK);
            accentColorPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new ColorChooserDialog.Builder((PreferencesActivity) getActivity(), R.string.accent_color)
                            .preselect(Config.accentColor(getActivity(), mAteKey))
                            .show();
                    return true;
                }
            });

            /*final MaterialListPreference lightStatusMode = (MaterialListPreference) findPreference("light_status_bar_mode");
            final MaterialListPreference lightToolbarMode = (MaterialListPreference) findPreference("light_toolbar_mode");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                lightStatusMode.setEnabled(true);
                lightStatusMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        @Config.LightStatusBarMode
                        int constant = Integer.parseInt((String) newValue);
                        ATE.config(getActivity(), mAteKey)
                                .lightStatusBarMode(constant)
                                .apply(getActivity());
                        return true;
                    }
                });
            } else {
                lightStatusMode.setEnabled(false);
                lightStatusMode.setSummary(R.string.not_available_below_m);
            }

            lightToolbarMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    @Config.LightToolbarMode
                    int constant = Integer.parseInt((String) newValue);
                    ATE.config(getActivity(), mAteKey)
                            .lightToolbarMode(constant)
                            .apply(getActivity());
                    return true;
                }
            });*/


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ATE.config(getActivity(), mAteKey)
                        .coloredStatusBar(true)
                        .apply(getActivity());
                ATE.config(getActivity(), mAteKey)
                        .coloredNavigationBar(true)
                        .apply(getActivity());
                }
            }
        }

    @Override
    public void onColorSelection(@NonNull ColorChooserDialog dialog, @ColorInt int selectedColor) {
        final Config config = ATE.config(this, getATEKey());
        switch (dialog.getTitle()) {
            case R.string.primary_color:
                config.primaryColor(selectedColor);
                break;
            case R.string.accent_color:
                config.accentColor(selectedColor);
                config.navigationViewSelectedIcon(selectedColor);
                config.navigationViewSelectedText(selectedColor);
                break;
        }
        config.commit();
        recreate();
    }

    @StyleRes
    @Override
    public int getActivityTheme() {
        return R.style.AppTheme;
    }
}
