/*
 * Copyright 2013-2015 The GDG Frisbee Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gdg.frisbee.android.app;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.api.GoogleApiClient;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

import net.danlew.android.joda.JodaTimeAndroid;

import org.gdg.frisbee.android.BuildConfig;
import org.gdg.frisbee.android.Const;
import org.gdg.frisbee.android.R;
import org.gdg.frisbee.android.cache.ModelCache;
import org.gdg.frisbee.android.utils.CrashlyticsTree;
import org.gdg.frisbee.android.utils.GingerbreadLastLocationFinder;
import org.gdg.frisbee.android.utils.Utils;

import java.io.File;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

/**
 * Created with IntelliJ IDEA.
 * User: maui
 * Date: 20.04.13
 * Time: 12:09
 */
public class App extends Application implements LocationListener {

    private static App mInstance = null;

    public static App getInstance() {
        return mInstance;
    }

    private ModelCache mModelCache;
    private Picasso mPicasso;
    private SharedPreferences mPreferences;
    private GoogleAnalytics mGaInstance;
    private Tracker mTracker;
    private GingerbreadLastLocationFinder mLocationFinder;
    private Location mLastLocation;
    private OrganizerChecker mOrganizerChecker;

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());

            StrictMode.ThreadPolicy.Builder b = new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .penaltyFlashScreen();

            StrictMode.setThreadPolicy(b.build());
        } else {
            Fabric.with(this, new Crashlytics());
            Timber.plant(new CrashlyticsTree());
        }

        mInstance = this;

        mPreferences = getSharedPreferences("gdg", MODE_PRIVATE);

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);

            if (mPreferences.getInt(Const.SETTINGS_VERSION_CODE, 0) < pInfo.versionCode)
                migrate(mPreferences.getInt(Const.SETTINGS_VERSION_CODE, 0), pInfo.versionCode);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        // Initialize ModelCache and Volley
        getModelCache();
        GdgVolley.init(this);

        mPreferences.edit().putInt(Const.SETTINGS_APP_STARTS, mPreferences.getInt(Const.SETTINGS_APP_STARTS, 0) + 1).apply();

        // Initialize Picasso
        mPicasso = new Picasso.Builder(this)
                .downloader(new OkHttpDownloader(this))
                .memoryCache(new LruCache(this))
                .build();
        mPicasso.setIndicatorsEnabled(BuildConfig.DEBUG);

        mOrganizerChecker = new OrganizerChecker(mPreferences);

        GoogleAnalytics.getInstance(this).setAppOptOut(mPreferences.getBoolean("analytics", false));

        // Init LastLocationFinder
        mLocationFinder = new GingerbreadLastLocationFinder(this);
        mLocationFinder.setChangedLocationListener(this);
        updateLastLocation();

        JodaTimeAndroid.init(this);
    }

    public void migrate(int oldVersion, int newVersion) {

        if (oldVersion < 11100 || Const.ALPHA) {
            mPreferences.edit().remove(Const.SETTINGS_GCM_REG_ID).apply();

            mPreferences.edit().clear().apply();
            mPreferences.edit().putBoolean(Const.SETTINGS_FIRST_START, true);
            String rootDir = null;
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                // SD-card available
                String rootDirExt = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/Android/data/" + getPackageName() + "/cache";
                deleteDirectory(new File(rootDirExt));
            }

            File internalCacheDir = getCacheDir();
            rootDir = internalCacheDir.getAbsolutePath();
            deleteDirectory(new File(rootDir));

            if (Const.ALPHA) {
                Toast.makeText(getApplicationContext(), "Alpha version always resets Preferences on update.", Toast.LENGTH_LONG).show();
            }
        }
        mPreferences.edit().putInt(Const.SETTINGS_VERSION_CODE, newVersion).apply();
    }

    public void updateLastLocation() {
        if (Utils.isEmulator())
            return;

        Location loc = mLocationFinder.getLastBestLocation(5000, 60 * 60 * 1000);

        if (loc != null) {
            mLastLocation = loc;
        }
    }

    public Location getLastLocation() {
        return mLastLocation;
    }

    public Picasso getPicasso() {
        return mPicasso;
    }

    public Tracker getTracker() {
        if (mTracker == null) {
            // Initialize GA
            mGaInstance = GoogleAnalytics.getInstance(getApplicationContext());
            mTracker = mGaInstance.newTracker(getString(R.string.ga_trackingId));

            mTracker.setAppName(getString(R.string.app_name));
            mTracker.setAnonymizeIp(true);
        }

        return mTracker;
    }

    public ModelCache getModelCache() {
        if (mModelCache == null) {

            File rootDir = null;
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                // SD-card available
                rootDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/Android/data/" + getPackageName() + "/model_cache/");
            } else {
                File internalCacheDir = getCacheDir();
                rootDir = new File(internalCacheDir.getAbsolutePath() + "/model_cache/");
            }

            rootDir.mkdirs();

            mModelCache = new ModelCache.Builder(getApplicationContext())
                    .setMemoryCacheEnabled(true)
                    .setDiskCacheEnabled(true)
                    .setDiskCacheLocation(rootDir)
                    .build();
        }
        return mModelCache;
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            for (File child : dir.listFiles()) {
                deleteDirectory(child);
            }
        }

        dir.delete();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }

    public boolean isOrganizer() {
        return mOrganizerChecker.isOrganizer();
    }

    public void checkOrganizer(GoogleApiClient apiClient, OrganizerChecker.OrganizerResponseHandler responseHandler) {
        mOrganizerChecker.checkOrganizer(apiClient, responseHandler);
    }
}
