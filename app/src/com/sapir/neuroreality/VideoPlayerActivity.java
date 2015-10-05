/*
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sapir.neuroreality;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.sapir.neuroreality.graph.ListViewMultiChartActivity;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * Sample activity showing how to properly enable custom fullscreen behavior.
 * <p/>
 * This is the preferred way of handling fullscreen because the default fullscreen implementation
 * will cause re-buffering of the video.
 */
public class VideoPlayerActivity extends YouTubeBaseActivity implements
        CompoundButton.OnCheckedChangeListener,
        YouTubePlayer.OnFullscreenListener,
        YouTubePlayer.OnInitializedListener {

    private static final String TAG = "neuroReality";

    private static final int PORTRAIT_ORIENTATION = Build.VERSION.SDK_INT < 9
            ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            : ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;

    private LinearLayout baseLayout;
    private YouTubePlayerView playerView;
    private YouTubePlayer player;
    private Button saveGraphButton;
    private Button showGraphButton;
    private Button GraphButton;
    private Button clearGraphButton;
    private ScrollView scrollView;
    private View otherViews;
    private String videoId;
    private TextView stateText;
    private StringBuilder logString;
    private TextView eventLog;

    private boolean fullscreen;
    private MyPlaybackEventListener playbackEventListener;
    private MyPlayerStateChangeListener playerStateChangeListener;

    DataMediator dataMediator = DataMediator.getInstance();
    WebSocketClient mWebSocketClient;
    private boolean videoIsPlaying = false;

    private void initSocket() {

        URI uri;
        try {
            uri = new URI("ws://cloud.neurosteer.com:8080/v1/features/0006664E5C18/pull");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        log("Socket is open.");
                    }
                });
            }

            @Override
            public void onMessage(String s) {
                if (videoIsPlaying) {
                    Log.v(TAG, s);
                    dataMediator.updateData(getTimesText(), s);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            log("Collecting data...");
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            log("Cloud is alive...");
                        }
                    });
                }
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        log("Socket is close.");
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
                final String error = e.getMessage();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        log("Socket error: " + error);
                    }
                });
            }
        };

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        videoId = intent.getStringExtra("videoId");

        setContentView(R.layout.activity_video_player);
        baseLayout = (LinearLayout) findViewById(R.id.layout);
//        playerView = (YouTubePlayerView) findViewById(R.id.player);

        playerView = new YouTubePlayerView(this);
        ((LinearLayout)findViewById(R.id.playerView)).addView(playerView);

        showGraphButton = (Button) findViewById(R.id.showGraph_button);
        saveGraphButton = (Button) findViewById(R.id.saveGraph_button);
        clearGraphButton = (Button) findViewById(R.id.clearGraph_button);
        otherViews = findViewById(R.id.other_views);

        stateText = (TextView) findViewById(R.id.state_text);
        eventLog = (TextView) findViewById(R.id.event_log);

        logString = new StringBuilder();

        scrollView = (ScrollView) findViewById(R.id.scrollView);
        scrollView.fullScroll(View.FOCUS_DOWN);

        dataMediator.clear();

        playerStateChangeListener = new MyPlayerStateChangeListener();
        playbackEventListener = new MyPlaybackEventListener();

        playerView.initialize(DeveloperKey.DEVELOPER_KEY, this);
        doLayout();
    }

    @Override
    public void onResume() {
        super.onResume();
        initSocket();
        mWebSocketClient.connect();
    }

    @Override
    public void onPause () {
        if (player != null)
            player.pause();

        super.onPause();
        if (!mWebSocketClient.getConnection().isClosed()) {
            mWebSocketClient.close();
        }
    }

    @Override
    public void onDestroy() {
        if (player != null)
            player.release();

        super.onDestroy();
        if (!mWebSocketClient.getConnection().isClosed()) {
            mWebSocketClient.close();
        }
    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player,
                                        boolean wasRestored) {
        if (this.player != null)
            this.player.release();

        this.player = player;

        // Specify that we want to handle fullscreen behavior ourselves.
        player.addFullscreenControlFlag(YouTubePlayer.FULLSCREEN_FLAG_CUSTOM_LAYOUT);
        player.setOnFullscreenListener(this);
        player.setPlaybackEventListener(playbackEventListener);
        player.setPlayerStateChangeListener(playerStateChangeListener);

        Log.d(TAG, "wasRestored = " + wasRestored);

        if (!wasRestored) {
            player.cueVideo(videoId);
        }
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
        Log.d(TAG, "YouTube failed to initialize!");
    }

    public void showGraph_onClick(View v) {
        if (!dataMediator.isEmpty()) {
            Intent myIntent = new Intent(this, ListViewMultiChartActivity.class);
            startActivity(myIntent);
        } else {
            Toast.makeText(v.getContext(), "No data was collected. ", Toast.LENGTH_LONG).show();
            log("No data was collected. Did you remember to turn on the EEG box " +
                    "and run 'eeg-android' app in the background?");
        }
    }
    public void saveGraph_onClick(View v) {
        try {
            if (!dataMediator.isEmpty()) {

                // Prepare file name and data to write
                DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss");
                dateFormatter.setLenient(false);
                Date today = new Date();
                String time = dateFormatter.format(today);
                String fileName = "Events_" + time + ".txt";
                DateFormat dateFormatter2 = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
                String data = String.format("%% TimeNow: %s\n%s", dateFormatter2.format(today),
                        dataMediator.serialize(videoId));

                // Creating an internal dir
                File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File (sdCard.getAbsolutePath() + "/Neuroreality");
                dir.mkdirs();

//                String dir = getFilesDir().getAbsolutePath();
//                File mydir = this.getDir("Neuroreality/", this.MODE_PRIVATE);
                // Getting a file within the dir.
                File myFile = new File(dir, fileName);
//                myFile.createNewFile();
                FileOutputStream fOut = new FileOutputStream(myFile);
                OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                myOutWriter.append(data);
                myOutWriter.close();
                fOut.close();

                // Write data to screen
                log(data);
                Toast.makeText(v.getContext(), "File saved to SD card. FIle name:" + fileName, Toast.LENGTH_LONG).show();
                log("Data saved.");
            }
            else {
                Toast.makeText(v.getContext(), "No data to save. ", Toast.LENGTH_LONG).show();
                log("No data to save.");
            }

        }
        catch (Exception e)
        {
            Toast.makeText(v.getContext(), e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }
    public void clearGraph_onClick(View v) {

        dataMediator.clear();
        log("Data deleted");
        Toast.makeText(v.getContext(), "Data deleted. ", Toast.LENGTH_LONG).show();

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int controlFlags = player.getFullscreenControlFlags();
        if (isChecked) {
            setRequestedOrientation(PORTRAIT_ORIENTATION);
            controlFlags |= YouTubePlayer.FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE;
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            controlFlags &= ~YouTubePlayer.FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE;
        }
        player.setFullscreenControlFlags(controlFlags);
    }

    private void doLayout() {
        LinearLayout.LayoutParams playerParams =
                (LinearLayout.LayoutParams) playerView.getLayoutParams();
        if (fullscreen) {
            // When in fullscreen, the visibility of all other views than the player should be set to
            // GONE and the player should be laid out across the whole screen.
            playerParams.width = LayoutParams.MATCH_PARENT;
            playerParams.height = LayoutParams.MATCH_PARENT;

            otherViews.setVisibility(View.GONE);
        } else {
            // This layout is up to you - this is just a simple example (vertically stacked boxes in
            // portrait, horizontally stacked in landscape).
            otherViews.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams otherViewsParams = otherViews.getLayoutParams();
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                playerParams.width = otherViewsParams.width = 0;
                playerParams.height = WRAP_CONTENT;
                otherViewsParams.height = MATCH_PARENT;
                playerParams.weight = 1;
                baseLayout.setOrientation(LinearLayout.HORIZONTAL);
            } else {
                playerParams.width = otherViewsParams.width = MATCH_PARENT;
                playerParams.height = WRAP_CONTENT;
                playerParams.weight = 0;
                otherViewsParams.height = 0;
                baseLayout.setOrientation(LinearLayout.VERTICAL);
            }
        }
    }

    @Override
    public void onFullscreen(boolean isFullscreen) {
        fullscreen = isFullscreen;
        doLayout();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        doLayout();
    }

    private void updateText() {
        stateText.setText(String.format("Current state: %s %s %s",
                playerStateChangeListener.playerState, playbackEventListener.playbackState,
                playbackEventListener.bufferingState));
    }


    private void log(String message) {
        logString.append(message + "\n");
        eventLog.setText(logString);
        Log.d(TAG, message);
    }

    private String getTimesText() {

        if (player == null)
            return ("NULL");

        int currentTimeMillis = player.getCurrentTimeMillis();
        int durationMillis = player.getDurationMillis();
        return String.format("(%s/%s)", formatTime(currentTimeMillis), formatTime(durationMillis));
    }

    private String formatTime(int millis) {
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        int hours = minutes / 60;

        return (hours == 0 ? "" : hours + ":")
                + String.format("%02d:%02d", minutes % 60, seconds % 60);
    }

    private final class MyPlaybackEventListener implements YouTubePlayer.PlaybackEventListener {
        String playbackState = "NOT_PLAYING";
        String bufferingState = "";
        private int mInterval = 1000; // 5 seconds by default, can be changed later
        private Handler mHandler = new Handler();

        @Override
        public void onPlaying() {
            playbackState = "PLAYING";
            updateText();
            log("\tPLAYING " + getTimesText());
            videoIsPlaying = true;
        }

        @Override
        public void onBuffering(boolean isBuffering) {
            bufferingState = isBuffering ? "(BUFFERING)" : "";
            updateText();
            log("\t" + (isBuffering ? "BUFFERING " : "NOT BUFFERING ") + getTimesText());
            // Uncomment to ignore data when buffering
            // videoIsPlaying = !isBuffering;
        }

        @Override
        public void onStopped() {
            playbackState = "STOPPED";
            updateText();
            log("\tSTOPPED");
            videoIsPlaying = false;
        }

        @Override
        public void onPaused() {
            playbackState = "PAUSED";
            updateText();
            log("\tPAUSED " + getTimesText());
            videoIsPlaying = false;
        }

        @Override
        public void onSeekTo(int endPositionMillis) {
            log(String.format("\tSEEKTO: (%s/%s)",
                    formatTime(endPositionMillis),
                    formatTime(player.getDurationMillis())));
        }
    }

    private final class MyPlayerStateChangeListener implements YouTubePlayer.PlayerStateChangeListener {
        String playerState = "UNINITIALIZED";

        @Override
        public void onLoading() {
            playerState = "LOADING";

            updateText();
            log(playerState);
        }

        @Override
        public void onLoaded(String videoId) {
            playerState = String.format("LOADED %s", videoId);
            updateText();
            log(playerState);
        }

        @Override
        public void onAdStarted() {
            playerState = "AD_STARTED";
            updateText();
            log(playerState);
        }

        @Override
        public void onVideoStarted() {
            playerState = "VIDEO_STARTED";
            updateText();
            log(playerState);
        }

        @Override
        public void onVideoEnded() {
            playerState = "VIDEO_ENDED";
            updateText();
            log(playerState);
        }

        @Override
        public void onError(YouTubePlayer.ErrorReason reason) {
            playerState = "ERROR (" + reason + ")";
            if (reason == YouTubePlayer.ErrorReason.UNEXPECTED_SERVICE_DISCONNECTION) {
                // When this error occurs the player is released and can no longer be used.
                player = null;
            }
            updateText();
            log(playerState);
        }

    }
}

