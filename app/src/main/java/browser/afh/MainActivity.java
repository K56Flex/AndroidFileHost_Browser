package browser.afh;

/*
 * Copyright (C) 2016 Harsh Shandilya (MSF-Jarvis) and Ritayan Chakraborty (out386)
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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.SearchEvent;
import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.Display;
import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.github.javiersantos.bottomdialogs.BottomDialog;
import com.lapism.searchview.SearchView;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.util.Collections;

import browser.afh.data.FindDevices.AppbarScroll;
import browser.afh.data.FindDevices.FragmentInterface;
import browser.afh.fragments.MainFragment;
import browser.afh.tools.ConnectionDetector;
import browser.afh.tools.Constants;
import browser.afh.tools.Prefs;
import hugo.weaving.DebugLog;
import io.fabric.sdk.android.Fabric;

import static browser.afh.tools.Utils.isPackageInstalled;

public class MainActivity extends AppCompatActivity implements AppbarScroll, FragmentInterface {
    AppBarLayout appBarLayout;
    TextView headerTV;
    private Intent searchIntent;
    private Prefs prefs;
    private Drawer drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        prefs = new Prefs(getApplicationContext());
        String deviceID = getIntent().getStringExtra("device_id");
        //if (!BuildConfig.DEBUG){
          Fabric.with(this, new Crashlytics());
        //}
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        final SearchView searchView = (SearchView) findViewById(R.id.searchView);
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        appBarLayout = (AppBarLayout) findViewById(R.id.app_bar);
        headerTV = (TextView) findViewById(R.id.header_tv);
        if (prefs.get("device_id", null) != null && Build.VERSION.SDK_INT >= 25)
            addLauncherShortcut();

        updatesCheck();
        AccountHeader header = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.color.colorPrimary)
                .withProfileImagesVisible(false)
                .withSelectionListEnabledForSingleProfile(false)
                .addProfiles(
                        new ProfileDrawerItem().withName(getString(R.string.app_name)).withEmail
                                (BuildConfig.VERSION_NAME))
                .withCurrentProfileHiddenInList(true)
                .build();

        drawer = new DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(header)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.drawer_title_home)
                                .withIcon(R.drawable.ic_home)
                                .withIdentifier(0)
                                .withDescription(R.string.drawer_desc_home),
                        new PrimaryDrawerItem()
                                .withName(R.string.drawer_title_info)
                                .withIcon(R.drawable.ic_info)
                                .withIdentifier(1)
                                .withDescription(R.string.drawer_desc_info)
                                .withSelectable(false),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem()
                                .withName(R.string.drawer_title_settings)
                                .withIcon(R.drawable.ic_settings)
                                .withIdentifier(2)
                                .withDescription(R.string.drawer_desc_settings)
                )
                .withCloseOnClick(true)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem.getIdentifier() == 0) {
                            changeFragment(new MainFragment());
                        } else if (drawerItem.getIdentifier() == 1) {
                            startActivity(new Intent(getApplicationContext(), AboutActivity.class));
                        } else if (drawerItem.getIdentifier() == 2) {
                            changeFragment(new MyPreferenceFragment());
                        }
                        return false;
                    }
                })
                .withOnDrawerListener(new Drawer.OnDrawerListener() {
                    @Override
                    public void onDrawerOpened(View drawerView) {
                        if (searchView != null && searchView.isSearchOpen())
                            searchView.close(true);
                    }

                    @Override
                    public void onDrawerClosed(View drawerView) {

                    }

                    @Override
                    public void onDrawerSlide(View drawerView, float slideOffset) {

                    }
                })
                .build();

        final MaterialDialog.Builder useLabsVariantDialog = new MaterialDialog.Builder(getApplicationContext())
                .title(R.string.disclaimer_google_play_title)
                .content(R.string.disclaimer_google_play_desc)
                .negativeText(R.string.ok)
                .positiveText(R.string.download)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                        if (isPackageInstalled(Constants.XDA_LABS_PACKAGE_NAME, getPackageManager())){
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.XDA_LABS_APP_PAGE_LINK)));
                        } else {
                            Toast.makeText(getApplicationContext(), R.string.err_xda_labs_not_installed, Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.XDA_LABS_DOWNLOAD_PAGE)));
                        }

                    }
                });
        boolean its_unofficial = prefs.get(Constants.PREF_ASSERT_UNOFFICIAL_CLIENT, false);
        if (!its_unofficial){
            new MaterialDialog.Builder(this)
                    .title(R.string.unofficial_disclaimer_title)
                    .content(R.string.unofficial_disclaimer_text)
                    .neutralText(R.string.file_dialog_neutral_button_label)
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog.dismiss();
                            prefs.put(Constants.PREF_ASSERT_UNOFFICIAL_CLIENT, true);
                        }
                    })
                    .dismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            prefs.put(Constants.PREF_ASSERT_UNOFFICIAL_CLIENT, true);
                            if (BuildConfig.PLAY_COMPATIBLE) useLabsVariantDialog.show();
                        }
                    })
                    .show();
        }

        new CheckConnectivity(getApplicationContext()).execute();

        if (deviceID != null){
            Bundle bundle = new Bundle();
            bundle.putString("device_id", deviceID);
            Fragment mainFragment = new MainFragment();
            mainFragment.setArguments(bundle);
            changeFragment(mainFragment);
            return;
        }

        changeFragment(new MainFragment());

        searchView.setHint(getResources().getString(R.string.search_hint));
        searchView.setFocusable(false);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!BuildConfig.DEBUG){
                    Answers.getInstance().logSearch(new SearchEvent()
                            .putQuery(query));
                }
                searchView.close(true);
                searchIntent = new Intent(Constants.INTENT_SEARCH);
                searchIntent.putExtra(Constants.INTENT_SEARCH_QUERY, query);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(searchIntent);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchIntent = new Intent(Constants.INTENT_SEARCH);
                searchIntent.putExtra(Constants.INTENT_SEARCH_QUERY, newText);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(searchIntent);
                return true;
            }
        });
        searchView.setOnMenuClickListener(new SearchView.OnMenuClickListener() {
            @Override
            public void onMenuClick() {
                drawer.openDrawer();
            }
        });
    }


    public void changeFragment(Fragment fragment) {
        FragmentManager fragmentManager = getFragmentManager();
        if (fragment instanceof MainFragment) {
            expand();
            showSearch(true);
        }
        else {
            collapse();
            showSearch(false);
        }
        fragmentManager.beginTransaction()
                .replace(R.id.mainFrame, fragment)
                .commit();
    }

    @Override
    public void expand() {
        appBarLayout.setExpanded(true, true);
    }
    @Override
    public void collapse() {
        appBarLayout.setExpanded(false, true);
    }

    @Override
    public String getText() {
        return headerTV.getText().toString();
    }

    @Override
    public void setText(String message) {
        if (message != null)
            headerTV.setVisibility(View.VISIBLE);
        else {
            headerTV.setVisibility(View.GONE);
            return;
        }
        headerTV.setText(message);
    }

    @Override
    @DebugLog
    public void reattach() {
        FragmentManager fragmentManager = getFragmentManager();
        Fragment current = fragmentManager.findFragmentById(R.id.mainFrame);
        try {
            if (current instanceof MainFragment) {
                fragmentManager.beginTransaction()
                        .detach(current)
                        .attach(current)
                        .commit();
                changeFragment(current);
            }
        } catch(IllegalStateException e) {
            finish();
            // As the onClick listener won't work if this happens, anyway.
            // No point in keeping the blank activity up
        }
    }
    @Override
    public void onSuperBack() {
        super.onBackPressed();
    }

    @Override
    public void onBackPressed() {
        Intent backIntent = new Intent(Constants.INTENT_BACK);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(backIntent);
    }

    @Override
    @DebugLog
    public void showSearch(boolean show) {
        final SearchView search = (SearchView) findViewById(R.id.searchView);
        if(show) {
            search.setTranslationY(-search.getHeight());
            search.setAlpha(0);
            search.setVisibility(View.VISIBLE);
            search.animate()
                    .setDuration(Constants.ANIM_DURATION)
                    .translationY(0)
                    .alpha(1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            search.setVisibility(View.VISIBLE);

                        }
                    });

        }
        else {
            search.setTranslationY(0);
            search.setAlpha(1);
            search.setVisibility(View.VISIBLE);
            search.animate()
                    .setDuration(Constants.ANIM_DURATION)
                    .translationY(-search.getHeight())
                    .alpha(0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            search.setVisibility(View.GONE);

                        }
                    });
        }
    }

    @DebugLog
    public void updatesCheck(){
        new AppUpdater(this)
                .setUpdateFrom(UpdateFrom.GITHUB)
                .setGitHubUserAndRepo("out386","AndroidFileHost_Browser")
                .showEvery(5)
                .showAppUpdated(false)
                .setDisplay(Display.DIALOG)
                .start();
    }

    private class CheckConnectivity extends AsyncTask <Void, Void, Void> {
        boolean isConnected;
        Context context;

        CheckConnectivity(Context context) {
            this.context = context;
        }

        @Override
        protected Void doInBackground(Void... v) {
            isConnected = ConnectionDetector.isConnectingToInternet(context);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (!isConnected) {
                new BottomDialog.Builder(MainActivity.this)
                        .setTitle(R.string.bottom_dialog_warning_title)
                        .setContent(R.string.bottom_dialog_warning_desc)
                        .setPositiveText(R.string.bottom_dialog_positive_text)
                        .setNegativeTextColorResource(R.color.colorAccent)
                        .onPositive(new BottomDialog.ButtonCallback() {
                            @SuppressLint("CommitPrefEdits")
                            @Override
                            public void onClick(@NonNull BottomDialog bottomDialog) {
                                bottomDialog.dismiss();
                            }
                        })
                        .show();
            }
        }
    }

    public static class MyPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }
    }
    @TargetApi(25)
    private void addLauncherShortcut() {
        ShortcutManager sM = getSystemService(ShortcutManager.class);
        sM.removeAllDynamicShortcuts();

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra("device_id", prefs.get("device_id", null));

        ShortcutInfo shortcut = new ShortcutInfo.Builder(this, "shortcut1")
                .setIntent(intent)
                .setLongLabel(prefs.get("device_name","..."))
                .setShortLabel(prefs.get("device_name","..."))
                .setIcon(Icon.createWithResource(this, R.drawable.ic_device_placeholder))
                .build();

        sM.setDynamicShortcuts(Collections.singletonList(shortcut));
    }
}
