package com.acube.twiliosample;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.twilio.video.CameraCapturer;
import com.twilio.video.ConnectOptions;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalParticipant;
import com.twilio.video.LocalVideoTrack;
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
import com.twilio.video.VideoView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tvi.webrtc.Camera1Enumerator;
import tvi.webrtc.Camera2Enumerator;
import tvi.webrtc.VideoCapturer;
import tvi.webrtc.voiceengine.WebRtcAudioUtils;

public class MainActivity2 extends AppCompatActivity {

    LocalVideoTrack localVideoTrack;
    LocalAudioTrack localAudioTrack;
    VideoView videoView;
    ImageView cancel;
    String accessToken = "78157ea15e7c396c73c9ebdbda4fda34";

    /* The CameraCapturer is a default video capturer provided by Twilio which can
   capture video from the front or rear-facing device camera */
    private CameraCapturer cameraCapturer;

    /* A VideoView receives frames from a local or remote video track and renders them
       to an associated view. */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        boolean enable = true;
        localAudioTrack = LocalAudioTrack.create(getApplicationContext(), enable);
        if (localAudioTrack != null) {
            localAudioTrack.enable(true);
        } videoView = (VideoView) findViewById(R.id.video_view);
        cancel = (ImageView) findViewById(R.id.btnCancel);

// A video track requires an implementation of a VideoCapturer. Here's how to use the front camera with a Camera2Capturer.
        Camera2Enumerator camera2Enumerator = new Camera2Enumerator(getApplicationContext());
        String frontCameraId = null;
        for (String cameraId : camera2Enumerator.getDeviceNames()) {
            if (camera2Enumerator.isFrontFacing(cameraId)) {
                frontCameraId = cameraId;
                break;
            }
        }

        Camera1Enumerator enumerator = new Camera1Enumerator(false);
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = (VideoCapturer) enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    localVideoTrack = LocalVideoTrack.create(getApplicationContext(), enable, videoCapturer);
                    videoView.setMirror(true);
                    localVideoTrack.addSink(videoView);
                }
            }
        }

        ConnectOptions connectOptions = new ConnectOptions.Builder(accessToken)
                .roomName("RMX6053240")
                .audioTracks(Collections.singletonList(localAudioTrack))
                .videoTracks(Collections.singletonList(localVideoTrack))
                .enableAutomaticSubscription(false)
//                .dataTracks(localDataTracks)
                .build();

        Room room = Video.connect(getApplicationContext(), connectOptions, new Room.Listener() {
            @Override
            public void onConnected(@NonNull Room room) {
                Toast.makeText(getApplicationContext(), "room connected", Toast.LENGTH_LONG).show();

            }

            @Override
            public void onConnectFailure(@NonNull Room room, @NonNull TwilioException twilioException) {

            }

            @Override
            public void onReconnecting(@NonNull Room room, @NonNull TwilioException twilioException) {

            }

            @Override
            public void onReconnected(@NonNull Room room) {

            }

            @Override
            public void onDisconnected(@NonNull Room room, @Nullable TwilioException twilioException) {

            }

            @Override
            public void onParticipantConnected(@NonNull Room room, @NonNull RemoteParticipant remoteParticipant) {
                Log.v("TAG", "Participant connected: " + remoteParticipant.getIdentity());
                Toast.makeText(getApplicationContext(), remoteParticipant.getIdentity().toString(), Toast.LENGTH_LONG).show();
                remoteParticipant.setListener(new RemoteParticipant.Listener() {
                    @Override
                    public void onAudioTrackPublished(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication) {

                    }

                    @Override
                    public void onAudioTrackUnpublished(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication) {

                    }

                    @Override
                    public void onAudioTrackSubscribed(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication, @NonNull RemoteAudioTrack remoteAudioTrack) {

                    }

                    @Override
                    public void onAudioTrackSubscriptionFailed(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication, @NonNull TwilioException twilioException) {

                    }

                    @Override
                    public void onAudioTrackUnsubscribed(@NonNull RemoteParticipant remoteParticipant,
                                                         @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication,
                                                         @NonNull RemoteAudioTrack remoteAudioTrack) {

                    }

                    @Override
                    public void onVideoTrackPublished(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication) {

                    }

                    @Override
                    public void onVideoTrackUnpublished(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication) {

                    }

                    @Override
                    public void onVideoTrackSubscribed(@NonNull RemoteParticipant remoteParticipant,
                                                       @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication,
                                                       @NonNull RemoteVideoTrack remoteVideoTrack) {

                        videoView.setMirror(false);
                        remoteVideoTrack.addSink(videoView);
                    }

                    @Override
                    public void onVideoTrackSubscriptionFailed(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication, @NonNull TwilioException twilioException) {

                    }

                    @Override
                    public void onVideoTrackUnsubscribed(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication, @NonNull RemoteVideoTrack remoteVideoTrack) {

                    }

                    @Override
                    public void onDataTrackPublished(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteDataTrackPublication remoteDataTrackPublication) {

                    }

                    @Override
                    public void onDataTrackUnpublished(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteDataTrackPublication remoteDataTrackPublication) {

                    }

                    @Override
                    public void onDataTrackSubscribed(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteDataTrackPublication remoteDataTrackPublication, @NonNull RemoteDataTrack remoteDataTrack) {

                    }

                    @Override
                    public void onDataTrackSubscriptionFailed(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteDataTrackPublication remoteDataTrackPublication, @NonNull TwilioException twilioException) {

                    }

                    @Override
                    public void onDataTrackUnsubscribed(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteDataTrackPublication remoteDataTrackPublication, @NonNull RemoteDataTrack remoteDataTrack) {

                    }

                    @Override
                    public void onAudioTrackEnabled(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication) {

                    }

                    @Override
                    public void onAudioTrackDisabled(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication) {

                    }

                    @Override
                    public void onVideoTrackEnabled(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication) {

                    }

                    @Override
                    public void onVideoTrackDisabled(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication) {

                    }
                });


            }

            @Override
            public void onParticipantDisconnected(@NonNull Room room, @NonNull RemoteParticipant remoteParticipant) {
                Log.v("TAG", "Participant connected: " + remoteParticipant.getIdentity());
                Toast.makeText(getApplicationContext(), remoteParticipant.getIdentity().toString(), Toast.LENGTH_LONG).show();

            }

            @Override
            public void onRecordingStarted(@NonNull Room room) {

            }

            @Override
            public void onRecordingStopped(@NonNull Room room) {

            }
        });

        // After receiving the connected callback the LocalParticipant becomes available
        LocalParticipant localParticipant = room.getLocalParticipant();
//        Log.i("LocalParticipant ", localParticipant.getIdentity());

// Get the first participant from the room
        if (room.getRemoteParticipants().size() > 0) {
            RemoteParticipant participant = room.getRemoteParticipants().get(0);
        }
//        Log.i("HandleParticipants", participant.getIdentity() + " is in the room.");


        cancel.setOnClickListener(view -> {
            room.disconnect();
        });

        WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true);
        WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(false);
        WebRtcAudioUtils.setWebRtcBasedAutomaticGainControl(false);
    }
}