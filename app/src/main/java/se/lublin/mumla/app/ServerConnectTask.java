/*
 * Copyright (C) 2014 Andrew Comminos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.lublin.mumla.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.AsyncTask;

import java.util.ArrayList;

import se.lublin.humla.HumlaService;
import se.lublin.humla.model.Server;
import se.lublin.mumla.R;
import se.lublin.mumla.Settings;
import se.lublin.mumla.db.MumlaDatabase;
import se.lublin.mumla.service.MumlaService;
import se.lublin.mumla.util.MumlaTrustStore;

/**
 * Constructs an intent for connection to a MumlaService and executes it.
 * Created by andrew on 20/08/14.
 */
public class ServerConnectTask extends AsyncTask<Server, Void, Intent> {
    private Context mContext;
    private MumlaDatabase mDatabase;
    private Settings mSettings;

    public ServerConnectTask(Context context, MumlaDatabase database) {
        mContext = context;
        mDatabase = database;
        mSettings = Settings.getInstance(context);
    }

    @Override
    protected Intent doInBackground(Server... params) {
        Server server = params[0];

        /* Convert input method defined in settings to an integer format used by Humla. */
        int inputMethod = mSettings.getHumlaInputMethod();

        int audioSource = mSettings.isHandsetMode() ?
                MediaRecorder.AudioSource.DEFAULT : MediaRecorder.AudioSource.MIC;
        int audioStream = mSettings.isHandsetMode() ?
                AudioManager.STREAM_VOICE_CALL : AudioManager.STREAM_MUSIC;

        String applicationVersion = "";
        try {
            applicationVersion = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        Intent connectIntent = new Intent(mContext, MumlaService.class);
        connectIntent.putExtra(HumlaService.EXTRAS_SERVER, server);
        connectIntent.putExtra(HumlaService.EXTRAS_CLIENT_NAME, mContext.getString(R.string.app_name)+" "+applicationVersion);
        connectIntent.putExtra(HumlaService.EXTRAS_TRANSMIT_MODE, inputMethod);
        connectIntent.putExtra(HumlaService.EXTRAS_DETECTION_THRESHOLD, mSettings.getDetectionThreshold());
        connectIntent.putExtra(HumlaService.EXTRAS_AMPLITUDE_BOOST, mSettings.getAmplitudeBoostMultiplier());
        connectIntent.putExtra(HumlaService.EXTRAS_AUTO_RECONNECT, mSettings.isAutoReconnectEnabled());
        connectIntent.putExtra(HumlaService.EXTRAS_AUTO_RECONNECT_DELAY, MumlaService.RECONNECT_DELAY);
        connectIntent.putExtra(HumlaService.EXTRAS_USE_OPUS, !mSettings.isOpusDisabled());
        connectIntent.putExtra(HumlaService.EXTRAS_INPUT_RATE, mSettings.getInputSampleRate());
        connectIntent.putExtra(HumlaService.EXTRAS_INPUT_QUALITY, mSettings.getInputQuality());
        connectIntent.putExtra(HumlaService.EXTRAS_FORCE_TCP, mSettings.isTcpForced());
        connectIntent.putExtra(HumlaService.EXTRAS_USE_TOR, mSettings.isTorEnabled());
        connectIntent.putStringArrayListExtra(HumlaService.EXTRAS_ACCESS_TOKENS, (ArrayList<String>) mDatabase.getAccessTokens(server.getId()));
        connectIntent.putExtra(HumlaService.EXTRAS_AUDIO_SOURCE, audioSource);
        connectIntent.putExtra(HumlaService.EXTRAS_AUDIO_STREAM, audioStream);
        connectIntent.putExtra(HumlaService.EXTRAS_FRAMES_PER_PACKET, mSettings.getFramesPerPacket());
        connectIntent.putExtra(HumlaService.EXTRAS_TRUST_STORE, MumlaTrustStore.getTrustStorePath(mContext));
        connectIntent.putExtra(HumlaService.EXTRAS_TRUST_STORE_PASSWORD, MumlaTrustStore.getTrustStorePassword());
        connectIntent.putExtra(HumlaService.EXTRAS_TRUST_STORE_FORMAT, MumlaTrustStore.getTrustStoreFormat());
        connectIntent.putExtra(HumlaService.EXTRAS_HALF_DUPLEX, mSettings.isHalfDuplex());
        connectIntent.putExtra(HumlaService.EXTRAS_ENABLE_PREPROCESSOR, mSettings.isPreprocessorEnabled());
        if (server.isSaved()) {
            ArrayList<Integer> muteHistory = (ArrayList<Integer>) mDatabase.getLocalMutedUsers(server.getId());
            ArrayList<Integer> ignoreHistory = (ArrayList<Integer>) mDatabase.getLocalIgnoredUsers(server.getId());
            connectIntent.putExtra(HumlaService.EXTRAS_LOCAL_MUTE_HISTORY, muteHistory);
            connectIntent.putExtra(HumlaService.EXTRAS_LOCAL_IGNORE_HISTORY, ignoreHistory);
        }

        if (mSettings.isUsingCertificate()) {
            long certificateId = mSettings.getDefaultCertificate();
            byte[] certificate = mDatabase.getCertificateData(certificateId);
            if (certificate != null)
                connectIntent.putExtra(HumlaService.EXTRAS_CERTIFICATE, certificate);
            // TODO(acomminos): handle the case where a certificate's data is unavailable.
        }

        connectIntent.setAction(HumlaService.ACTION_CONNECT);
        return connectIntent;
    }

    @Override
    protected void onPostExecute(Intent intent) {
        super.onPostExecute(intent);
        mContext.startService(intent);
    }
}
