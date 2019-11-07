/* Copyright (C) 2014  olie.xdev <olie.xdev@googlemail.com>
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>
*/

package com.hyperion.blescaleexample.core;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.text.format.DateFormat;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.hyperion.blescaleexample.core.bluetooth.BluetoothCommunication;
import com.hyperion.blescaleexample.core.bluetooth.BluetoothFactory;
import com.hyperion.blescaleexample.core.datatypes.ScaleMeasurement;
import com.hyperion.blescaleexample.core.datatypes.ScaleUser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import timber.log.Timber;

public class OpenScale {
    public static boolean DEBUG_MODE = false;

    public static final String DATABASE_NAME = "openScale.db";

    private static OpenScale instance;


    private ScaleUser selectedScaleUser;
    private List<ScaleMeasurement> scaleMeasurementList;

    private BluetoothCommunication btDeviceDriver;

    private Context context;

    private OpenScale(Context context) {
        this.context = context;
        btDeviceDriver = null;
    }

    public static void createInstance(Context context) {
        if (instance != null) {
            return;
        }

        instance = new OpenScale(context);
    }

    public static OpenScale getInstance() {
        if (instance == null) {
            throw new RuntimeException("No OpenScale instance created");
        }

        return instance;
    }

    public int addScaleData(final ScaleMeasurement scaleMeasurement) {
        // TODO o day
//        return addScaleData(scaleMeasurement, false);
        return -1;
    }

    public String getFilenameFromUriMayThrow(Uri uri) {
        Cursor cursor = context.getContentResolver().query(
                uri, null, null, null, null);
        try {
            cursor.moveToFirst();
            return cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public String getFilenameFromUri(Uri uri) {
        try {
            return getFilenameFromUriMayThrow(uri);
        }
        catch (Exception e) {
            String name = uri.getLastPathSegment();
            if (name != null) {
                return name;
            }
            name = uri.getPath();
            if (name != null) {
                return name;
            }
            return uri.toString();
        }
    }

    public void connectToBluetoothDeviceDebugMode(String hwAddress, Handler callbackBtHandler) {
        Timber.d("Trying to connect to bluetooth device [%s] in debug mode", hwAddress);

        disconnectFromBluetoothDevice();

        btDeviceDriver = BluetoothFactory.createDebugDriver(context);
        btDeviceDriver.registerCallbackHandler(callbackBtHandler);
        btDeviceDriver.connect(hwAddress);
    }

    public boolean connectToBluetoothDevice(String deviceName, String hwAddress, Handler callbackBtHandler) {
        Timber.d("Trying to connect to bluetooth device [%s] (%s)", hwAddress, deviceName);

        disconnectFromBluetoothDevice();

        btDeviceDriver = BluetoothFactory.createDeviceDriver(context, deviceName);
        if (btDeviceDriver == null) {
            return false;
        }

        btDeviceDriver.registerCallbackHandler(callbackBtHandler);
        btDeviceDriver.connect(hwAddress);

        return true;
    }

    public boolean disconnectFromBluetoothDevice() {
        if (btDeviceDriver == null) {
            return false;
        }

        Timber.d("Disconnecting from bluetooth device");
        btDeviceDriver.disconnect();
        btDeviceDriver = null;

        return true;
    }

    public ScaleUser getSelectedScaleUser() {
        if (selectedScaleUser != null) {
            return selectedScaleUser;
        }
        selectedScaleUser = new ScaleUser();
        return selectedScaleUser;
    }

    public int getSelectedScaleUserId() {
        if (selectedScaleUser != null) {
            return selectedScaleUser.getId();
        }
        selectedScaleUser = new ScaleUser();
        return selectedScaleUser.getId();
    }

    private void runUiToastMsg(final String text) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void syncInsertMeasurement(ScaleMeasurement scaleMeasurement) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.health.openscale.sync", "com.health.openscale.sync.core.service.SyncService"));
        intent.putExtra("mode", "insert");
        intent.putExtra("userId", scaleMeasurement.getUserId());
        intent.putExtra("weight", scaleMeasurement.getWeight());
        intent.putExtra("date", scaleMeasurement.getDateTime().getTime());
        ContextCompat.startForegroundService(context, intent);
    }

    private void syncUpdateMeasurement(ScaleMeasurement scaleMeasurement) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.health.openscale.sync", "com.health.openscale.sync.core.service.SyncService"));
        intent.putExtra("mode", "update");
        intent.putExtra("userId", scaleMeasurement.getUserId());
        intent.putExtra("weight", scaleMeasurement.getWeight());
        intent.putExtra("date", scaleMeasurement.getDateTime().getTime());
        ContextCompat.startForegroundService(context, intent);
    }

    private void syncDeleteMeasurement(Date date) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.health.openscale.sync", "com.health.openscale.sync.core.service.SyncService"));
        intent.putExtra("mode", "delete");
        intent.putExtra("date", date.getTime());
        ContextCompat.startForegroundService(context, intent);
    }

    private void syncClearMeasurements() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.health.openscale.sync", "com.health.openscale.sync.core.service.SyncService"));
        intent.putExtra("mode", "clear");
        ContextCompat.startForegroundService(context, intent);
    }

}
