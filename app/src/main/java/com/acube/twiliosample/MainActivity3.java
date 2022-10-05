package com.acube.twiliosample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.twilio.audioswitch.AudioDevice;
import com.twilio.audioswitch.AudioSwitch;
import com.twilio.video.AudioCodec;
import com.twilio.video.ConnectOptions;
import com.twilio.video.EncodingParameters;
import com.twilio.video.G722Codec;
import com.twilio.video.IsacCodec;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalParticipant;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.OpusCodec;
import com.twilio.video.PcmaCodec;
import com.twilio.video.PcmuCodec;
import com.twilio.video.RemoteAudioTrack;
import com.twilio.video.RemoteAudioTrackPublication;
import com.twilio.video.RemoteDataTrack;
import com.twilio.video.RemoteDataTrackPublication;
import com.twilio.video.RemoteParticipant;
import com.twilio.video.RemoteVideoTrack;
import com.twilio.video.RemoteVideoTrackPublication;
import com.twilio.video.Room;
import com.twilio.video.TwilioException;
import com.twilio.video.Video;
import com.twilio.video.VideoCodec;
import com.twilio.video.VideoTrack;
import com.twilio.video.VideoView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kotlin.Unit;
import tvi.webrtc.VideoSink;

public class MainActivity3 extends AppCompatActivity {


    private static final int CAMERA_MIC_PERMISSION_REQUEST_CODE = 1;
    private static final int CAMERA_PERMISSION_INDEX = 0;
    private static final int MIC_PERMISSION_INDEX = 1;
    private static final String TAG = "VideoActivity";

    /*
     * Audio and video tracks can be created with names. This feature is useful for categorizing
     * tracks of participants. For example, if one participant publishes a video track with
     * ScreenCapturer and CameraCapturer with the names "screen" and "camera" respectively then
     * other participants can use RemoteVideoTrack#getName to determine which video track is
     * produced from the other participant's screen or camera.
     */
    private static final String LOCAL_AUDIO_TRACK_NAME = "mic";
    private static final String LOCAL_VIDEO_TRACK_NAME = "camera";

    /*
     * You must provide a Twilio Access Token to connect to the Video service
     */
    private static final String TWILIO_ACCESS_TOKEN = "";
    private static final String ACCESS_TOKEN_SERVER = "";

    /*
     * Access token used to connect. This field will be set either from the console generated token
     * or the request to the token server.
     */
    private String accessToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImN0eSI6InR3aWxpby1mcGE7dj0xIiwidHdyIjoiUk1YOTIyNDc0MSJ9.eyJqdGkiOiJTSzJkOTRlYjFkZmJhNjQyMjUxNzkzYTM3ODQ4Y2IzZmNjLTE2NjQ4NzkwNDgiLCJpc3MiOiJTSzJkOTRlYjFkZmJhNjQyMjUxNzkzYTM3ODQ4Y2IzZmNjIiwic3ViIjoiQUMyNGRhOWJiOTQ4ZmYwY2ZjNjY3MzBlNGFlMmJhNGI2ZSIsImV4cCI6MTY2NDg4MjY0OCwiZ3JhbnRzIjp7ImlkZW50aXR5IjoiaGFyb29uIiwidmlkZW8iOnt9fX0.RxcEazGwQkfQCiy1R1aoKtqsROKwKp99ZCVQFmCne-A";

    /*
     * A Room represents communication between a local participant and one or more participants.
     */
    private Room room;
    private LocalParticipant localParticipant;

    /*
     * AudioCodec and VideoCodec represent the preferred codec for encoding and decoding audio and
     * video.
     */
    private AudioCodec audioCodec;
    private VideoCodec videoCodec;

    /*
     * Encoding parameters represent the sender side bandwidth constraints.
     */
    private EncodingParameters encodingParameters;

    /*
     * A VideoView receives frames from a local or remote video track and renders them
     * to an associated view.
     */
    private VideoView primaryVideoView;
    private VideoView thumbnailVideoView;

    /*
     * Android shared preferences used for settings
     */

    /*
     * Android application UI elements
     */
    private CameraCapturerCompat cameraCapturerCompat;
    private LocalAudioTrack localAudioTrack;
    private LocalVideoTrack localVideoTrack;
    private FloatingActionButton switchCameraActionFab;
    private FloatingActionButton localVideoActionFab;
    private FloatingActionButton muteActionFab;
    private ProgressBar reconnectingProgressBar;
    private ImageView callEnd;
    private AlertDialog connectDialog;
    private String remoteParticipantIdentity;

    /*
     * Audio management
     */
    private AudioSwitch audioSwitch;
    private int savedVolumeControlStream;
    private MenuItem audioDeviceMenuItem;

    private VideoSink localVideoView;
    private boolean disconnectedFromOnDestroy;
    private boolean enableAutomaticSubscription;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);

        primaryVideoView = findViewById(R.id.primary_video_view);
        thumbnailVideoView = findViewById(R.id.thumbnail_video_view);
        reconnectingProgressBar = findViewById(R.id.reconnecting_progress_bar);

        switchCameraActionFab = findViewById(R.id.switch_camera_action_fab);
        localVideoActionFab = findViewById(R.id.local_video_action_fab);
        muteActionFab = findViewById(R.id.mute_action_fab);
        callEnd = findViewById(R.id.btnCallEnd);

        /*
         * Get shared preferences to read settings
         */

        /*
         * Setup audio management and set the volume control stream
         */
        audioSwitch = new AudioSwitch(getApplicationContext());
        savedVolumeControlStream = getVolumeControlStream();
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        /*
         * Check camera and microphone permissions. Needed in Android M. Also, request for bluetooth
         * permissions for enablement of bluetooth audio routing.
         */
        if (!checkPermissionForCameraAndMicrophone()) {
            requestPermissionForCameraMicrophoneAndBluetooth();
        } else {
            audioSwitch.start(
                    (audioDevices, audioDevice) -> {
                        return Unit.INSTANCE;
                    });
            createAudioAndVideoTracks();
            setAccessToken();
        }

        /*
         * Set the initial state of the UI
         */
        intializeUI();
        connectToRoom("");

        callEnd.setOnClickListener(view -> {
            disconnectClickListener();
        });
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_MIC_PERMISSION_REQUEST_CODE) {
            /*
             * The first two permissions are Camera & Microphone, bluetooth isn't required but
             * enabling it enables bluetooth audio routing functionality.
             */
            boolean cameraAndMicPermissionGranted =
                    (PackageManager.PERMISSION_GRANTED == grantResults[CAMERA_PERMISSION_INDEX])
                            & (PackageManager.PERMISSION_GRANTED
                            == grantResults[MIC_PERMISSION_INDEX]);

            /*
             * Due to bluetooth permissions being requested at the same time as camera and mic
             * permissions, AudioSwitch should be started after providing the user the option
             * to grant the necessary permissions for bluetooth.
             */
            audioSwitch.start(
                    (audioDevices, audioDevice) -> {
                        return Unit.INSTANCE;
                    });

            if (cameraAndMicPermissionGranted) {
                createAudioAndVideoTracks();
                setAccessToken();
            } else {
                Toast.makeText(this, "Denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onResume() {
        super.onResume();
        /*
         * Update preferred audio and video codec in case changed in settings
         */
       /* audioCodec =
                getAudioCodecPreference(
                        SettingsActivity.PREF_AUDIO_CODEC,
                        SettingsActivity.PREF_AUDIO_CODEC_DEFAULT);
        videoCodec =
                getVideoCodecPreference(
                        SettingsActivity.PREF_VIDEO_CODEC,
                        SettingsActivity.PREF_VIDEO_CODEC_DEFAULT);
        enableAutomaticSubscription =
                getAutomaticSubscriptionPreference(
                        SettingsActivity.PREF_ENABLE_AUTOMATIC_SUBSCRIPTION,
                        SettingsActivity.PREF_ENABLE_AUTOMATIC_SUBSCRIPTION_DEFAULT);*/
//         * Get latest encoding parameters
//        final EncodingParameters newEncodingParameters = getEncodingParameters();

        /*
         * If the local video track was released when the app was put in the background, recreate.
         */
        if (localVideoTrack == null && checkPermissionForCameraAndMicrophone()) {
            localVideoTrack =
                    LocalVideoTrack.create(
                            this, true, cameraCapturerCompat, LOCAL_VIDEO_TRACK_NAME);
            localVideoTrack.addSink(localVideoView);

            /*
             * If connected to a Room then share the local video track.
             */
            if (localParticipant != null) {
                localParticipant.publishTrack(localVideoTrack);

                /*
                 * Update encoding parameters if they have changed.
                 */
                /*if (!newEncodingParameters.equals(encodingParameters)) {
                    localParticipant.setEncodingParameters(newEncodingParameters);
                }*/
            }
        }

        /*
         * Update encoding parameters
         */
//        encodingParameters = newEncodingParameters;

        /*
         * Update reconnecting UI
         */
        if (room != null) {
            reconnectingProgressBar.setVisibility(
                    (room.getState() != Room.State.RECONNECTING) ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    protected void onPause() {
        /*
         * Release the local video track before going in the background. This ensures that the
         * camera can be used by other applications while this app is in the background.
         */
        if (localVideoTrack != null) {
            /*
             * If this local video track is being shared in a Room, unpublish from room before
             * releasing the video track. Participants will be notified that the track has been
             * unpublished.
             */
            if (localParticipant != null) {
                localParticipant.unpublishTrack(localVideoTrack);
            }

            localVideoTrack.release();
            localVideoTrack = null;
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        /*
         * Tear down audio management and restore previous volume stream
         */
        audioSwitch.stop();
        setVolumeControlStream(savedVolumeControlStream);

        /*
         * Always disconnect from the room before leaving the Activity to
         * ensure any memory allocated to the Room resource is freed.
         */
        if (room != null && room.getState() != Room.State.DISCONNECTED) {
            room.disconnect();
            disconnectedFromOnDestroy = true;
        }

        /*
         * Release the local audio and video tracks ensuring any memory allocated to audio
         * or video is freed.
         */
        if (localAudioTrack != null) {
            localAudioTrack.release();
            localAudioTrack = null;
        }
        if (localVideoTrack != null) {
            localVideoTrack.release();
            localVideoTrack = null;
        }

        super.onDestroy();
    }

    private boolean checkPermissions(String[] permissions) {
        boolean shouldCheck = true;
        for (String permission : permissions) {
            shouldCheck &=
                    (PackageManager.PERMISSION_GRANTED
                            == ContextCompat.checkSelfPermission(this, permission));
        }
        return shouldCheck;
    }

    private void requestPermissions(String[] permissions) {
        boolean displayRational = false;
        for (String permission : permissions) {
            displayRational |=
                    ActivityCompat.shouldShowRequestPermissionRationale(this, permission);
        }
        if (displayRational) {
            Toast.makeText(this, "denied", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(
                    this, permissions, CAMERA_MIC_PERMISSION_REQUEST_CODE);
        }
    }

    private boolean checkPermissionForCameraAndMicrophone() {
        return checkPermissions(
                new String[] {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO});
    }

    private void requestPermissionForCameraMicrophoneAndBluetooth() {
        String[] permissionsList;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            permissionsList =
                    new String[] {
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.BLUETOOTH_CONNECT
                    };
        } else {
            permissionsList =
                    new String[] {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        }
        requestPermissions(permissionsList);
    }

    private void createAudioAndVideoTracks() {
        // Share your microphone
        localAudioTrack = LocalAudioTrack.create(this, true, LOCAL_AUDIO_TRACK_NAME);

        // Share your camera
        cameraCapturerCompat =
                new CameraCapturerCompat(this, CameraCapturerCompat.Source.FRONT_CAMERA);
        localVideoTrack =
                LocalVideoTrack.create(this, true, cameraCapturerCompat, LOCAL_VIDEO_TRACK_NAME);
        primaryVideoView.setMirror(true);
        localVideoTrack.addSink(primaryVideoView);
        localVideoView = primaryVideoView;
    }

    private void setAccessToken() {
        /*if (!BuildConfig.USE_TOKEN_SERVER) {
            *//*
             * OPTION 1 - Generate an access token from the getting started portal
             * https://www.twilio.com/console/video/dev-tools/testing-tools and add
             * the variable TWILIO_ACCESS_TOKEN setting it equal to the access token
             * string in your local.properties file.
             *//*
            this.accessToken = TWILIO_ACCESS_TOKEN;
        } else {
            *//*
             * OPTION 2 - Retrieve an access token from your own web app.
             * Add the variable ACCESS_TOKEN_SERVER assigning it to the url of your
             * token server and the variable USE_TOKEN_SERVER=true to your
             * local.properties file.
             *//*
            retrieveAccessTokenfromServer();
        }*/
    }

    private void connectToRoom(String roomName) {
        audioSwitch.activate();
        ConnectOptions.Builder connectOptionsBuilder =
                new ConnectOptions.Builder(accessToken).roomName("RMX6602421");

        /*
         * Add local audio track to connect options to share with participants.
         */
        if (localAudioTrack != null) {
            connectOptionsBuilder.audioTracks(Collections.singletonList(localAudioTrack));
        }

        /*
         * Add local video track to connect options to share with participants.
         */
        if (localVideoTrack != null) {
            connectOptionsBuilder.videoTracks(Collections.singletonList(localVideoTrack));
        }

        /*
         * Set the preferred audio and video codec for media.
         */
//        connectOptionsBuilder.preferAudioCodecs(Collections.singletonList(audioCodec));
//        connectOptionsBuilder.preferVideoCodecs(Collections.singletonList(videoCodec));

        /*
         * Set the sender side encoding parameters.
         */
//        connectOptionsBuilder.encodingParameters(encodingParameters);

        /*
         * Toggles automatic track subscription. If set to false, the LocalParticipant will receive
         * notifications of track publish events, but will not automatically subscribe to them. If
         * set to true, the LocalParticipant will automatically subscribe to tracks as they are
         * published. If unset, the default is true. Note: This feature is only available for Group
         * Rooms. Toggling the flag in a P2P room does not modify subscription behavior.
         */
        connectOptionsBuilder.enableAutomaticSubscription(enableAutomaticSubscription);

        room = Video.connect(this, connectOptionsBuilder.build(), roomListener());
    }
    private void intializeUI() {
        switchCameraActionFab.show();
        switchCameraActionFab.setOnClickListener(switchCameraClickListener());
        localVideoActionFab.show();
        localVideoActionFab.setOnClickListener(localVideoClickListener());
        muteActionFab.show();
        muteActionFab.setOnClickListener(muteClickListener());
    }

    /*
     * Show the current available audio devices.
     */

    /*
     * Update the menu icon based on the currently selected audio device.
     */

    /*
     * Get the preferred audio codec from shared preferences
     */

    /*
     * Get the preferred video codec from shared preferences
     */


    /*
     * The actions performed during disconnect.
     */

    /*
     * Creates an connect UI dialog
     */

    /*
     * Called when remote participant joins the room
     */
    @SuppressLint("SetTextI18n")
    private void addRemoteParticipant(RemoteParticipant remoteParticipant) {
        /*
         * This app only displays video for one additional participant per Room
         */
        if (thumbnailVideoView.getVisibility() == View.VISIBLE) {
            Snackbar.make(
                    callEnd,
                    "Multiple participants are not currently support in this UI",
                    Snackbar.LENGTH_LONG)
                    .setAction("Action", null)
                    .show();
            return;
        }
        remoteParticipantIdentity = remoteParticipant.getIdentity();

        /*
         * Add remote participant renderer
         */
        if (remoteParticipant.getRemoteVideoTracks().size() > 0) {
            RemoteVideoTrackPublication remoteVideoTrackPublication =
                    remoteParticipant.getRemoteVideoTracks().get(0);

            /*
             * Only render video tracks that are subscribed to
             */
            if (remoteVideoTrackPublication.isTrackSubscribed()) {
                addRemoteParticipantVideo(remoteVideoTrackPublication.getRemoteVideoTrack());
            }
        }

        /*
         * Start listening for participant events
         */
        remoteParticipant.setListener(remoteParticipantListener());
    }

    /*
     * Set primary view as renderer for participant video track
     */
    private void addRemoteParticipantVideo(VideoTrack videoTrack) {
        moveLocalVideoToThumbnailView();
        primaryVideoView.setMirror(false);
        videoTrack.addSink(primaryVideoView);
    }

    private void moveLocalVideoToThumbnailView() {
        if (thumbnailVideoView.getVisibility() == View.GONE) {
            thumbnailVideoView.setVisibility(View.VISIBLE);
            localVideoTrack.removeSink(primaryVideoView);
            localVideoTrack.addSink(thumbnailVideoView);
            localVideoView = thumbnailVideoView;
            thumbnailVideoView.setMirror(
                    cameraCapturerCompat.getCameraSource()
                            == CameraCapturerCompat.Source.FRONT_CAMERA);
        }
    }

    /*
     * Called when remote participant leaves the room
     */
    @SuppressLint("SetTextI18n")
    private void removeRemoteParticipant(RemoteParticipant remoteParticipant) {
        if (!remoteParticipant.getIdentity().equals(remoteParticipantIdentity)) {
            return;
        }

        /*
         * Remove remote participant renderer
         */
        if (!remoteParticipant.getRemoteVideoTracks().isEmpty()) {
            RemoteVideoTrackPublication remoteVideoTrackPublication =
                    remoteParticipant.getRemoteVideoTracks().get(0);

            /*
             * Remove video only if subscribed to participant track
             */
            if (remoteVideoTrackPublication.isTrackSubscribed()) {
                removeParticipantVideo(remoteVideoTrackPublication.getRemoteVideoTrack());
            }
        }
        moveLocalVideoToPrimaryView();
    }

    private void removeParticipantVideo(VideoTrack videoTrack) {
        videoTrack.removeSink(primaryVideoView);
    }

    private void moveLocalVideoToPrimaryView() {
        if (thumbnailVideoView.getVisibility() == View.VISIBLE) {
            thumbnailVideoView.setVisibility(View.GONE);
            if (localVideoTrack != null) {
                localVideoTrack.removeSink(thumbnailVideoView);
                localVideoTrack.addSink(primaryVideoView);
            }
            localVideoView = primaryVideoView;
            primaryVideoView.setMirror(
                    cameraCapturerCompat.getCameraSource()
                            == CameraCapturerCompat.Source.FRONT_CAMERA);
        }
    }

    /*
     * Room events listener
     */
    @SuppressLint("SetTextI18n")
    private Room.Listener roomListener() {
        return new Room.Listener() {
            @Override
            public void onConnected(Room room) {
                localParticipant = room.getLocalParticipant();
                setTitle(room.getName());

                for (RemoteParticipant remoteParticipant : room.getRemoteParticipants()) {
                    addRemoteParticipant(remoteParticipant);
                    break;
                }
            }

            @Override
            public void onReconnecting(
                    @NonNull Room room, @NonNull TwilioException twilioException) {
                reconnectingProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onReconnected(@NonNull Room room) {
                reconnectingProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onConnectFailure(Room room, TwilioException e) {
                audioSwitch.deactivate();
                intializeUI();
            }

            @Override
            public void onDisconnected(Room room, TwilioException e) {
                localParticipant = null;
                reconnectingProgressBar.setVisibility(View.GONE);
                MainActivity3.this.room = null;
                // Only reinitialize the UI if disconnect was not called from onDestroy()
                if (!disconnectedFromOnDestroy) {
                    audioSwitch.deactivate();
                    intializeUI();
                    moveLocalVideoToPrimaryView();
                }
            }

            @Override
            public void onParticipantConnected(Room room, RemoteParticipant remoteParticipant) {
                addRemoteParticipant(remoteParticipant);
            }

            @Override
            public void onParticipantDisconnected(Room room, RemoteParticipant remoteParticipant) {
                removeRemoteParticipant(remoteParticipant);
            }

            @Override
            public void onRecordingStarted(Room room) {
                /*
                 * Indicates when media shared to a Room is being recorded. Note that
                 * recording is only available in our Group Rooms developer preview.
                 */
                Log.d(TAG, "onRecordingStarted");
            }

            @Override
            public void onRecordingStopped(Room room) {
                /*
                 * Indicates when media shared to a Room is no longer being recorded. Note that
                 * recording is only available in our Group Rooms developer preview.
                 */
                Log.d(TAG, "onRecordingStopped");
            }
        };
    }

    @SuppressLint("SetTextI18n")
    private RemoteParticipant.Listener remoteParticipantListener() {
        return new RemoteParticipant.Listener() {
            @Override
            public void onAudioTrackPublished(
                    RemoteParticipant remoteParticipant,
                    RemoteAudioTrackPublication remoteAudioTrackPublication) {
                Log.i(
                        TAG,
                        String.format(
                                "onAudioTrackPublished: "
                                        + "[RemoteParticipant: identity=%s], "
                                        + "[RemoteAudioTrackPublication: sid=%s, enabled=%b, "
                                        + "subscribed=%b, name=%s]",
                                remoteParticipant.getIdentity(),
                                remoteAudioTrackPublication.getTrackSid(),
                                remoteAudioTrackPublication.isTrackEnabled(),
                                remoteAudioTrackPublication.isTrackSubscribed(),
                                remoteAudioTrackPublication.getTrackName()));
            }

            @Override
            public void onAudioTrackUnpublished(
                    RemoteParticipant remoteParticipant,
                    RemoteAudioTrackPublication remoteAudioTrackPublication) {
                Log.i(
                        TAG,
                        String.format(
                                "onAudioTrackUnpublished: "
                                        + "[RemoteParticipant: identity=%s], "
                                        + "[RemoteAudioTrackPublication: sid=%s, enabled=%b, "
                                        + "subscribed=%b, name=%s]",
                                remoteParticipant.getIdentity(),
                                remoteAudioTrackPublication.getTrackSid(),
                                remoteAudioTrackPublication.isTrackEnabled(),
                                remoteAudioTrackPublication.isTrackSubscribed(),
                                remoteAudioTrackPublication.getTrackName()));
            }


            @Override
            public void onDataTrackPublished(
                    RemoteParticipant remoteParticipant,
                    RemoteDataTrackPublication remoteDataTrackPublication) {
                Log.i(
                        TAG,
                        String.format(
                                "onDataTrackPublished: "
                                        + "[RemoteParticipant: identity=%s], "
                                        + "[RemoteDataTrackPublication: sid=%s, enabled=%b, "
                                        + "subscribed=%b, name=%s]",
                                remoteParticipant.getIdentity(),
                                remoteDataTrackPublication.getTrackSid(),
                                remoteDataTrackPublication.isTrackEnabled(),
                                remoteDataTrackPublication.isTrackSubscribed(),
                                remoteDataTrackPublication.getTrackName()));
            }

            @Override
            public void onDataTrackUnpublished(
                    RemoteParticipant remoteParticipant,
                    RemoteDataTrackPublication remoteDataTrackPublication) {
                Log.i(
                        TAG,
                        String.format(
                                "onDataTrackUnpublished: "
                                        + "[RemoteParticipant: identity=%s], "
                                        + "[RemoteDataTrackPublication: sid=%s, enabled=%b, "
                                        + "subscribed=%b, name=%s]",
                                remoteParticipant.getIdentity(),
                                remoteDataTrackPublication.getTrackSid(),
                                remoteDataTrackPublication.isTrackEnabled(),
                                remoteDataTrackPublication.isTrackSubscribed(),
                                remoteDataTrackPublication.getTrackName()));
            }


            @Override
            public void onVideoTrackPublished(
                    RemoteParticipant remoteParticipant,
                    RemoteVideoTrackPublication remoteVideoTrackPublication) {
                Log.i(
                        TAG,
                        String.format(
                                "onVideoTrackPublished: "
                                        + "[RemoteParticipant: identity=%s], "
                                        + "[RemoteVideoTrackPublication: sid=%s, enabled=%b, "
                                        + "subscribed=%b, name=%s]",
                                remoteParticipant.getIdentity(),
                                remoteVideoTrackPublication.getTrackSid(),
                                remoteVideoTrackPublication.isTrackEnabled(),
                                remoteVideoTrackPublication.isTrackSubscribed(),
                                remoteVideoTrackPublication.getTrackName()));
            }

            @Override
            public void onVideoTrackUnpublished(
                    RemoteParticipant remoteParticipant,
                    RemoteVideoTrackPublication remoteVideoTrackPublication) {
                Log.i(
                        TAG,
                        String.format(
                                "onVideoTrackUnpublished: "
                                        + "[RemoteParticipant: identity=%s], "
                                        + "[RemoteVideoTrackPublication: sid=%s, enabled=%b, "
                                        + "subscribed=%b, name=%s]",
                                remoteParticipant.getIdentity(),
                                remoteVideoTrackPublication.getTrackSid(),
                                remoteVideoTrackPublication.isTrackEnabled(),
                                remoteVideoTrackPublication.isTrackSubscribed(),
                                remoteVideoTrackPublication.getTrackName()));
            }

            @Override
            public void onAudioTrackSubscribed(
                    RemoteParticipant remoteParticipant,
                    RemoteAudioTrackPublication remoteAudioTrackPublication,
                    RemoteAudioTrack remoteAudioTrack) {
                Log.i(
                        TAG,
                        String.format(
                                "onAudioTrackSubscribed: "
                                        + "[RemoteParticipant: identity=%s], "
                                        + "[RemoteAudioTrack: enabled=%b, playbackEnabled=%b, name=%s]",
                                remoteParticipant.getIdentity(),
                                remoteAudioTrack.isEnabled(),
                                remoteAudioTrack.isPlaybackEnabled(),
                                remoteAudioTrack.getName()));
            }

            @Override
            public void onAudioTrackUnsubscribed(
                    RemoteParticipant remoteParticipant,
                    RemoteAudioTrackPublication remoteAudioTrackPublication,
                    RemoteAudioTrack remoteAudioTrack) {
                Log.i(
                        TAG,
                        String.format(
                                "onAudioTrackUnsubscribed: "
                                        + "[RemoteParticipant: identity=%s], "
                                        + "[RemoteAudioTrack: enabled=%b, playbackEnabled=%b, name=%s]",
                                remoteParticipant.getIdentity(),
                                remoteAudioTrack.isEnabled(),
                                remoteAudioTrack.isPlaybackEnabled(),
                                remoteAudioTrack.getName()));
            }

            @Override
            public void onAudioTrackSubscriptionFailed(
                    RemoteParticipant remoteParticipant,
                    RemoteAudioTrackPublication remoteAudioTrackPublication,
                    TwilioException twilioException) {
                Log.i(
                        TAG,
                        String.format(
                                "onAudioTrackSubscriptionFailed: "
                                        + "[RemoteParticipant: identity=%s], "
                                        + "[RemoteAudioTrackPublication: sid=%b, name=%s]"
                                        + "[TwilioException: code=%d, message=%s]",
                                remoteParticipant.getIdentity(),
                                remoteAudioTrackPublication.getTrackSid(),
                                remoteAudioTrackPublication.getTrackName(),
                                twilioException.getCode(),
                                twilioException.getMessage()));
            }

            @Override
            public void onDataTrackSubscribed(
                    RemoteParticipant remoteParticipant,
                    RemoteDataTrackPublication remoteDataTrackPublication,
                    RemoteDataTrack remoteDataTrack) {
                Log.i(
                        TAG,
                        String.format(
                                "onDataTrackSubscribed: "
                                        + "[RemoteParticipant: identity=%s], "
                                        + "[RemoteDataTrack: enabled=%b, name=%s]",
                                remoteParticipant.getIdentity(),
                                remoteDataTrack.isEnabled(),
                                remoteDataTrack.getName()));
            }

            @Override
            public void onDataTrackUnsubscribed(
                    RemoteParticipant remoteParticipant,
                    RemoteDataTrackPublication remoteDataTrackPublication,
                    RemoteDataTrack remoteDataTrack) {
                Log.i(
                        TAG,
                        String.format(
                                "onDataTrackUnsubscribed: "
                                        + "[RemoteParticipant: identity=%s], "
                                        + "[RemoteDataTrack: enabled=%b, name=%s]",
                                remoteParticipant.getIdentity(),
                                remoteDataTrack.isEnabled(),
                                remoteDataTrack.getName()));
            }

            @Override
            public void onDataTrackSubscriptionFailed(
                    RemoteParticipant remoteParticipant,
                    RemoteDataTrackPublication remoteDataTrackPublication,
                    TwilioException twilioException) {
                Log.i(
                        TAG,
                        String.format(
                                "onDataTrackSubscriptionFailed: "
                                        + "[RemoteParticipant: identity=%s], "
                                        + "[RemoteDataTrackPublication: sid=%b, name=%s]"
                                        + "[TwilioException: code=%d, message=%s]",
                                remoteParticipant.getIdentity(),
                                remoteDataTrackPublication.getTrackSid(),
                                remoteDataTrackPublication.getTrackName(),
                                twilioException.getCode(),
                                twilioException.getMessage()));
            }

            @Override
            public void onVideoTrackSubscribed(
                    RemoteParticipant remoteParticipant,
                    RemoteVideoTrackPublication remoteVideoTrackPublication,
                    RemoteVideoTrack remoteVideoTrack) {
                Log.i(
                        TAG,
                        String.format(
                                "onVideoTrackSubscribed: "
                                        + "[RemoteParticipant: identity=%s], "
                                        + "[RemoteVideoTrack: enabled=%b, name=%s]",
                                remoteParticipant.getIdentity(),
                                remoteVideoTrack.isEnabled(),
                                remoteVideoTrack.getName()));
                addRemoteParticipantVideo(remoteVideoTrack);
            }

            @Override
            public void onVideoTrackUnsubscribed(
                    RemoteParticipant remoteParticipant,
                    RemoteVideoTrackPublication remoteVideoTrackPublication,
                    RemoteVideoTrack remoteVideoTrack) {
                Log.i(
                        TAG,
                        String.format(
                                "onVideoTrackUnsubscribed: "
                                        + "[RemoteParticipant: identity=%s], "
                                        + "[RemoteVideoTrack: enabled=%b, name=%s]",
                                remoteParticipant.getIdentity(),
                                remoteVideoTrack.isEnabled(),
                                remoteVideoTrack.getName()));
                removeParticipantVideo(remoteVideoTrack);
            }


            @Override
            public void onVideoTrackSubscriptionFailed(
                    RemoteParticipant remoteParticipant,
                    RemoteVideoTrackPublication remoteVideoTrackPublication,
                    TwilioException twilioException) {
                Log.i(
                        TAG,
                        String.format(
                                "onVideoTrackSubscriptionFailed: "
                                        + "[RemoteParticipant: identity=%s], "
                                        + "[RemoteVideoTrackPublication: sid=%b, name=%s]"
                                        + "[TwilioException: code=%d, message=%s]",
                                remoteParticipant.getIdentity(),
                                remoteVideoTrackPublication.getTrackSid(),
                                remoteVideoTrackPublication.getTrackName(),
                                twilioException.getCode(),
                                twilioException.getMessage()));
                Snackbar.make(
                        callEnd,
                        String.format(
                                "Failed to subscribe to %s video track",
                                remoteParticipant.getIdentity()),
                        Snackbar.LENGTH_LONG)
                        .show();
            }

            @Override
            public void onAudioTrackEnabled(
                    RemoteParticipant remoteParticipant,
                    RemoteAudioTrackPublication remoteAudioTrackPublication) {}

            @Override
            public void onAudioTrackDisabled(
                    RemoteParticipant remoteParticipant,
                    RemoteAudioTrackPublication remoteAudioTrackPublication) {}

            @Override
            public void onVideoTrackEnabled(
                    RemoteParticipant remoteParticipant,
                    RemoteVideoTrackPublication remoteVideoTrackPublication) {}

            @Override
            public void onVideoTrackDisabled(
                    RemoteParticipant remoteParticipant,
                    RemoteVideoTrackPublication remoteVideoTrackPublication) {}
        };
    }

    private DialogInterface.OnClickListener connectClickListener(final EditText roomEditText) {
        return (dialog, which) -> {
            connectToRoom(roomEditText.getText().toString());
        };
    }

    private View.OnClickListener disconnectClickListener() {
        return v -> {
            /*
             * Disconnect from room
             */
            if (room != null) {
                room.disconnect();
            }
            intializeUI();
        };
    }

    private DialogInterface.OnClickListener cancelConnectDialogClickListener() {
        return (dialog, which) -> {
            intializeUI();
            connectDialog.dismiss();
        };
    }

    private View.OnClickListener switchCameraClickListener() {
        return v -> {
            if (cameraCapturerCompat != null) {
                CameraCapturerCompat.Source cameraSource = cameraCapturerCompat.getCameraSource();
                cameraCapturerCompat.switchCamera();
                if (thumbnailVideoView.getVisibility() == View.VISIBLE) {
                    thumbnailVideoView.setMirror(
                            cameraSource == CameraCapturerCompat.Source.BACK_CAMERA);
                } else {
                    primaryVideoView.setMirror(
                            cameraSource == CameraCapturerCompat.Source.BACK_CAMERA);
                }
            }
        };
    }

    private View.OnClickListener localVideoClickListener() {
        return v -> {
            /*
             * Enable/disable the local video track
             */
            if (localVideoTrack != null) {
                boolean enable = !localVideoTrack.isEnabled();
                localVideoTrack.enable(enable);
                int icon;
                if (enable) {
                    icon = R.drawable.ic_videocam_white_24dp;
                    switchCameraActionFab.show();
                } else {
                    icon = R.drawable.ic_videocam_off_black_24dp;
                    switchCameraActionFab.hide();
                }
                localVideoActionFab.setImageDrawable(
                        ContextCompat.getDrawable(MainActivity3.this, icon));
            }
        };
    }

    private View.OnClickListener muteClickListener() {
        return v -> {
            /*
             * Enable/disable the local audio track. The results of this operation are
             * signaled to other Participants in the same Room. When an audio track is
             * disabled, the audio is muted.
             */
            if (localAudioTrack != null) {
                boolean enable = !localAudioTrack.isEnabled();
                localAudioTrack.enable(enable);
                int icon = enable ? R.drawable.ic_mic_white_24dp : R.drawable.ic_mic_off_black_24dp;
                muteActionFab.setImageDrawable(ContextCompat.getDrawable(MainActivity3.this, icon));
            }
        };
    }

   /* private void retrieveAccessTokenfromServer() {
        Ion.with(this)
                .load(
                        String.format(
                                "%s?identity=%s",
                                ACCESS_TOKEN_SERVER, UUID.randomUUID().toString()))
                .asString()
                .setCallback(
                        (e, token) -> {
                            if (e == null) {
                                VideoActivity.this.accessToken = token;
                            } else {
                                Toast.makeText(
                                        VideoActivity.this,
                                        R.string.error_retrieving_access_token,
                                        Toast.LENGTH_LONG)
                                        .show();
                            }
                        });
    }*/
}