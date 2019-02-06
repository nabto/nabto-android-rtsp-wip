package com.nabto.nabtovideo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.nabto.nabtovideo.util.VideoDevice;
import com.nabto.nabtovideo.R;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.example.widget.media.IjkVideoView;

import java.util.Timer;
import java.util.TimerTask;

public class VideoPlayerActivity extends Activity {

    private ProgressDialog progress;
    private Timer timer;
    IjkVideoView videoView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_player);
		
		Intent intent = getIntent();
		String url = intent.getStringExtra(VideoDevice.DEVICEURL);
		String connectionType = intent.getStringExtra("connection_type");
		
		Log.d(this.getClass().getSimpleName(), "Loading video from " + url + " via " + connectionType);

        timer = new Timer();

        progress = new ProgressDialog(this);
        progress.setMessage("Initiating (" + connectionType + ")...");
        progress.show();

		videoView = (IjkVideoView)findViewById(R.id.videoView);

        videoView.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
            public boolean onInfo(final IMediaPlayer mediaPlayer, int i, int i2) {

                switch (i) {
                    case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                        progress.setMessage("Buffering...");
                        if (!progress.isShowing()) {
                            progress.show();
                        }

                        timer.cancel();
                        timer = new Timer();

                        TimerTask timerTask = new TimerTask() {
                            @Override
                            public void run() {
                                if (videoView.getBufferPercentage() > 0) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            progress.setMessage("Buffering: " + String.valueOf(videoView.getBufferPercentage()) + "%");
                                        }
                                    });
                                }
                            }
                        };
                        timer.schedule(timerTask, 0, 100);
                        break;

                    case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                        timer.cancel();
                        progress.dismiss();
                        break;
                }

                return false;
            }
        });

        videoView.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer mediaPlayer) {
                Log.d(this.getClass().getSimpleName(), "MediaPlayer onPrepared");
                timer.cancel();
                progress.dismiss();
            }
        });

        videoView.setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(IMediaPlayer mp) {
                Log.e(this.getClass().getSimpleName(), "MediaPlayer onCompletion (eof) should not be called");

                showErrorDialog(R.string.video_completion_details);
            }
        });

        videoView.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(IMediaPlayer mp, int what, int extra) {
                Log.e(this.getClass().getSimpleName(), "MediaPlayer onError: " + what + ", extra: " + extra);

                showErrorDialog(R.string.video_error_details);
                return true;
            }
        });

        videoView.setVideoPath(url);
        videoView.requestFocus();
        videoView.start();
	}

    private void showErrorDialog(int message) {
        timer.cancel();
        progress.dismiss();

        final AlertDialog alert = new AlertDialog.Builder(VideoPlayerActivity.this)
                .setTitle(R.string.video_error)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                alert.dismiss();
                finish();
            }
        };
        handler.postDelayed(runnable, 2000);
    }

    @Override
    public void onPause() {
        super.onPause();

        videoView.stopPlayback();
        timer.cancel();
        progress.dismiss();
        finish();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getActionBar().setDisplayHomeAsUpEnabled(true);
		return true;
	}
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item.getItemId() == android.R.id.home) {
    		finish();
    	}
    	return true;
    }
}
