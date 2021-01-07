package si.uni_lj.fri.pbd.miniapp2;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import java.io.IOException;


public class MediaPlayerService extends Service {

    private final IBinder serviceBinder = new MediaPlayerServiceLocalBinder();

    private MediaPlayer mediaPlayer;
    private MediaPlayer radioPlayer;

    // shows the state
    private boolean isPlaying;
    private boolean isPaused;
    private boolean isStopped;

    // shows which song is playing / should play next
    private int song;

    // shows if we choose song group via temperature
    private int songGroup;

    private Handler handler;

    // AccelerationService object
    private AccelerationService accelerationService;

    // TemperatureService object
    private TemperatureService temperatureService;

    // GyroscopeService object
    private GyroscopeService gyroscopeService;

    // variable that indicates whether the AccelerationService is connected
    private boolean accelerationServiceBound;

    // variable that indicates whether the TemperatureService is connected
    private boolean temperatureServiceBound;

    // variable that indicates whether the GyroscopeService is connected
    private boolean gyroscopeServiceBound;

    // variable that indicates whether the broadcast for acceleration is registered
    private boolean isRegisteredAcc;

    // variable that indicates whether the broadcast for temperature is registered
    private boolean isRegisteredTemp;

    // variable that indicates whether the broadcast for gyroscope is registered
    private boolean isRegisteredGyro;

    // variable that shows radio state
    private boolean radioOn;

    // variable that shows which radio station is playing
    private int radioStation;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    public class MediaPlayerServiceLocalBinder extends Binder {
        MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        isPlaying = false;
        isPaused = false;
        isStopped = false;
        isRegisteredAcc = false;
        isRegisteredTemp = false;
        isRegisteredGyro = false;
        songGroup = -1;
        radioOn = false;
        radioStation = 1;

        // register the broadcast from MainActivity
        IntentFilter filter = new IntentFilter("SeekBarChange");
        registerReceiver(broadcastReceiverMain, filter);
    }

    public void updateUI() {
        Intent intent = new Intent("Song and time");
        if (isPlaying() || isPaused()) {
            intent.putExtra("song", getSongInfo());
            intent.putExtra("time", getTime());
        }
        else if ((isStopped() && !radioOn) || (!isStopped() && !radioOn)) {
            intent.putExtra("song", "no song");
            intent.putExtra("time", "no time");
        }
        else if (radioOn) {
            intent.putExtra("song", "radio");
            if (radioStation == 1) {
                intent.putExtra("time", "Nederland");
            }
            if (radioStation == 2) {
                intent.putExtra("time", "Nederland 2");
            }
            if (radioStation == 3) {
                intent.putExtra("time", "Norway");
            }
            if (radioStation == 4) {
                intent.putExtra("time", "Norway 2");
            }
            if (radioStation == 5) {
                intent.putExtra("time", "Spain");
            }
            if (radioStation == 6) {
                intent.putExtra("time", "Spain 2");
            }
        }
        sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // handler
        handler = new Handler();

        // bound the AccelerationService if is not
        if (!accelerationServiceBound) {
            Intent i = new Intent(MediaPlayerService.this, AccelerationService.class);
            bindService(i, accelerationServiceConnection, Context.BIND_AUTO_CREATE);
        }

        // bound the TemperatureService if is not
        if (!temperatureServiceBound) {
            Intent i = new Intent(MediaPlayerService.this, TemperatureService.class);
            bindService(i, temperatureServiceConnection, Context.BIND_AUTO_CREATE);
        }

        // bound the gyroScopeService if is not
        if (!gyroscopeServiceBound) {
            Intent i = new Intent(MediaPlayerService.this, GyroscopeService.class);
            bindService(i, gyroscopeServiceConnection, Context.BIND_AUTO_CREATE);
        }

        return Service.START_STICKY;
    }

    public void start() {
        if (radioOn) {
            radioPlayer.stop();
            radioOn = false;
        }
        // user clicks play for the first time
        if (mediaPlayer == null) {
            if (songGroup == -1 || songGroup == 1) {
                mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.song1);
                song = 1;
            }
            else if (songGroup == 2) {
                mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.song4);
                song = 4;
            }
            else if (songGroup == 3) {
                mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.song7);
                song = 7;
            }
            mediaPlayer.start();
        }
        else if (!isPlaying() && !isPaused() && !isStopped()) {
            mediaPlayer.start();
        }
        // user clicks play, to start the paused / stopped music
        else if (isPaused() || isStopped()) {
            mediaPlayer.start();
        }
        isPlaying = true;
        isPaused = false;
        isStopped = false;
        startTimer();

        // if the song end => start the next one
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stop();
                start();
            }
        });
    }

    public void pause() {
        if (!radioOn) {
            // pause the song if is playing
            if (mediaPlayer != null && isPlaying()) {
                mediaPlayer.pause();
                isPaused = true;
                isPlaying = false;
                isStopped = false;
                stopTimer();
            }
        }
    }

    public void next() {
        if (!radioOn) {
            // play next song if is already playing
            if (mediaPlayer != null && isPlaying()) {
                stop();
                start();
            }
            // prepair the next song if is paused
            else if (isPaused()) {
                stop();
                start();
                pause();
            }
        }
    }

    public void stop() {
        if (!radioOn) {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                isStopped = true;
                isPlaying = false;
                isPaused = false;
                // go to the next song (change the song on next play-click)
                if (song == 1) {
                    mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.song2);
                    song = 2;
                }
                else if (song == 2) {
                    mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.song3);
                    song = 3;
                }
                else if (song == 3) {
                    if (songGroup == 1) {
                        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.song1);
                        song = 1;
                    }
                    else if (songGroup == -1) {
                        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.song4);
                        song = 4;
                    }
                }
                else if (song == 4) {
                    mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.song5);
                    song = 5;
                }
                else if (song == 5) {
                    mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.song6);
                    song = 6;
                }
                else if (song == 6) {
                    if (songGroup == -1) {
                        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.song7);
                        song = 7;
                    }
                    else if (songGroup == 2) {
                        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.song4);
                        song = 4;
                    }
                }
                else if (song == 7) {
                    mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.song8);
                    song = 8;
                }
                else if (song == 8) {
                    mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.song9);
                    song = 9;
                }
                else if (song == 9) {
                    if (songGroup == -1) {
                        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.song1);
                        song = 1;
                    }
                    else if (songGroup == 3) {
                        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.song7);
                        song = 7;
                    }
                }
                stopTimer();
            }
        }
    }

    public void exit() {
        // stop the music if it't playing
        if (isPlaying()) {
            mediaPlayer.stop();
        }
        // stop the MediaPlayerService (we already know that is bound)
        Intent intent = new Intent(MediaPlayerService.this, MediaPlayerService.class);
        stopService(intent);
        // stop the AccelerationService if is bound
        if (accelerationServiceBound) {
            // if the gestures are on => unregister the listener
            accelerationService.gesturesOff();
            // stop the AccelerationService
            stopService(new Intent(this, AccelerationService.class));
        }
        // stop the TemperatureService
        if (temperatureServiceBound) {
            // if the temperature sensor is on => unregister the listener
            temperatureService.temperatureOff();
            // stop the TemperatureService
            stopService(new Intent(this, TemperatureService.class));
        }
        // unregister the broadcast from MainActivity
        unregisterReceiver(broadcastReceiverMain);
        // exit the app
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    // get the song order
    public int getSongNumber() {
        return song;
    }

    // get the song state
    public boolean isPlaying() {
        return isPlaying;
    }
    public boolean isPaused() {
        return isPaused;
    }
    public boolean isStopped() {
        return isStopped;
    }

    // get the String in form playing or paused time / time duration
    public String getTime() {
        String s1 = String.valueOf(mediaPlayer.getCurrentPosition());
        String s2 = String.valueOf(mediaPlayer.getDuration());
        return s1 + "/" + s2;
    }

    // get the song info that is playing/paused
    public String getSongInfo() {
        if (!radioOn) {
            if (getSongNumber() == 1) {
                return "Sebastian Ingrosso & Alesso - Calling";
            }
            else if (getSongNumber() == 2) {
                return "The Chainsmokers - Don't Let Me Down";
            }
            else if (getSongNumber() == 3) {
                return "Fifth Harmony - Work from Home";
            }
            else if (getSongNumber() == 4) {
                return "Santa Claus is coming to town";
            }
            else if (getSongNumber() == 5) {
                return "We wish you a Merry Christmas";
            }
            else if (getSongNumber() == 6) {
                return "Jingle Bells";
            }
            else if (getSongNumber() == 7) {
                return "Calvin Harris - Summer";
            }
            else if (getSongNumber() == 8) {
                return "Simple Plan - Summer Paradise";
            }
            else if (getSongNumber() == 9) {
                return "Moje leto - IN VIVO";
            }
        }
        return "";
    }

    public void startTimer() {
        runnable.run();
    }

    public void stopTimer() {
        handler.removeCallbacks(runnable);
        // refresh the songinfo and timer
        updateUI();
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            // refresh the timer every second
            updateUI();
            handler.postDelayed(this, 1000);
        }
    };

    public void gesturesOn() {
        if (accelerationServiceBound) {
            accelerationService.gesturesOn();
            // register a broadcast receiver
            IntentFilter filter = new IntentFilter("AccChange");
            registerReceiver(broadcastReceiverAcc, filter);
            isRegisteredAcc = true;
        }
    }

    public void gesturesOff() {
        if (accelerationServiceBound) {
            accelerationService.gesturesOff();
            if (isRegisteredAcc) {
                unregisterReceiver(broadcastReceiverAcc);
                isRegisteredAcc = false;
            }
        }
    }

    public void temperatureOn() {
        if (temperatureServiceBound) {
            temperatureService.temperatureOn();
            // register a broadcast receiver
            IntentFilter filter = new IntentFilter("TemperatureChange");
            registerReceiver(broadcastReceiverTemp, filter);
            isRegisteredTemp = true;
        }
    }

    public void temperatureOff() {
        if (temperatureServiceBound) {
            songGroup = -1;
            temperatureService.temperatureOff();
            if (isRegisteredTemp) {
                unregisterReceiver(broadcastReceiverTemp);
                isRegisteredTemp = false;
            }
        }
    }

    public void gyroscopeOn() {
        if (gyroscopeServiceBound) {
            gyroscopeService.gyroscopeOn();
            // register a broadcast receiver
            IntentFilter filter = new IntentFilter("GyroChange");
            registerReceiver(broadcastReceiverGyro, filter);
            isRegisteredGyro = true;
        }
    }

    public void gyroscopeOff() {
        if (gyroscopeServiceBound) {
            gyroscopeService.gyroscopeOff();
            if (isRegisteredGyro) {
                unregisterReceiver(broadcastReceiverGyro);
                isRegisteredGyro = false;
            }
        }
    }

    public void radioOn() {
        if (!radioOn) {
            // stop the song from the phone
            stop();
            radioOn = true;
            radioPlayer = new MediaPlayer();
            radioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                if (songGroup == -1 || songGroup == 1) {
                    radioPlayer.setDataSource("https://icecast.omroep.nl/radio1-bb-mp3"); // Nederland
                    radioStation = 1;
                }
                else if (songGroup == 2) {
                    radioPlayer.setDataSource("https://live-bauerno.sharp-stream.com/radionorge_no_mp3"); // Norway
                    radioStation = 3;
                }
                else if (songGroup == 3) {
                    radioPlayer.setDataSource("https://rne.rtveradio.cires21.com/rne_hc.mp3"); // Spain
                    radioStation = 5;
                }
                updateUI();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                radioPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            radioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    radioPlayer.start();
                }
            });
        }
    }

    public void radioOff() {
        if (radioOn) {
            radioOn = false;
            radioPlayer.stop();
            updateUI();
        }
    }

    public void nextRadio() {
        if (radioOn) {
            radioPlayer.stop();
            radioPlayer = new MediaPlayer();
            radioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            if (songGroup == 1) {
                if (radioStation == 1) {
                    radioStation = 2;
                    try {
                        radioPlayer.setDataSource("https://icecast.omroep.nl/radio2-bb-mp3");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else if (radioStation == 2) {
                    radioStation = 1;
                    try {
                        radioPlayer.setDataSource("https://icecast.omroep.nl/radio1-bb-mp3");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (songGroup == 2) {
                if (radioStation == 3) {
                    radioStation = 4;
                    try {
                        radioPlayer.setDataSource("https://live-bauerno.sharp-stream.com/radio1_no_mp3");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else if (radioStation == 4) {
                    radioStation = 3;
                    try {
                        radioPlayer.setDataSource("https://live-bauerno.sharp-stream.com/radionorge_no_mp3");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (songGroup == 3) {
                if (radioStation == 5) {
                    radioStation = 6;
                    try {
                        radioPlayer.setDataSource("https://radio3.rtveradio.cires21.com/radio3_hc.mp3");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else if (radioStation == 6) {
                    radioStation = 5;
                    try {
                        radioPlayer.setDataSource("https://rne.rtveradio.cires21.com/rne_hc.mp3");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (songGroup == -1) {
                if (radioStation == 1) {
                    radioStation = 2;
                    try {
                        radioPlayer.setDataSource("https://icecast.omroep.nl/radio2-bb-mp3");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else if (radioStation == 2) {
                    radioStation = 3;
                    try {
                        radioPlayer.setDataSource("https://live-bauerno.sharp-stream.com/radionorge_no_mp3");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else if (radioStation == 3) {
                    radioStation = 4;
                    try {
                        radioPlayer.setDataSource("https://live-bauerno.sharp-stream.com/radio1_no_mp3");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else if (radioStation == 4) {
                    radioStation = 5;
                    try {
                        radioPlayer.setDataSource("https://rne.rtveradio.cires21.com/rne_hc.mp3");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else if (radioStation == 5) {
                    radioStation = 6;
                    try {
                        radioPlayer.setDataSource("https://radio3.rtveradio.cires21.com/radio3_hc.mp3");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else if (radioStation == 6) {
                    radioStation = 1;
                    try {
                        radioPlayer.setDataSource("https://icecast.omroep.nl/radio1-bb-mp3");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            updateUI();
            try {
                radioPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            radioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    radioPlayer.start();
                }
            });
        }
    }

    //  define a AccelerationService connection and disconnection
    private ServiceConnection accelerationServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            AccelerationService.AccelerationServiceLocalBinder binder = (AccelerationService.AccelerationServiceLocalBinder) iBinder;
            accelerationService = binder.getService();
            accelerationServiceBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            accelerationServiceBound = false;
        }
    };

    // define a temperatureService connection and disconnection
    private ServiceConnection temperatureServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            TemperatureService.TemperatureServiceLocalBinder binder = (TemperatureService.TemperatureServiceLocalBinder) iBinder;
            temperatureService = binder.getService();
            temperatureServiceBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            temperatureServiceBound = false;
        }
    };

    // define a gyroscopeService connection and disconnection
    private ServiceConnection gyroscopeServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            GyroscopeService.GyroscopeServiceLocalBinder binder = (GyroscopeService.GyroscopeServiceLocalBinder) iBinder;
            gyroscopeService = binder.getService();
            gyroscopeServiceBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            gyroscopeServiceBound = false;
        }
    };

    // get the data from the AccelerationService
    private BroadcastReceiver broadcastReceiverAcc = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("AccChange")) {
                String value = intent.getExtras().getString("action");
                if (value.equals("pause")) {
                    if (radioOn) {
                        radioOff();
                    }
                    else {
                        pause();
                    }
                }
                else if (value.equals("play")) {
                    start();
                }
            }
        }
    };

    // get the data from GyroscopeService
    private BroadcastReceiver broadcastReceiverGyro = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("GyroChange") && !radioOn && (isPlaying() || isPaused())) {
                String value = intent.getExtras().getString("action");
                if (value.equals("1/3")) {
                    next();
                    mediaPlayer.seekTo(mediaPlayer.getDuration() / 3);
                    updateUI();
                }
                else if (value.equals("2/3")) {
                    next();
                    mediaPlayer.seekTo(2 * mediaPlayer.getDuration() / 3);
                    updateUI();
                }
            }
        }
    };

    // get the data from TemperatureService
    private BroadcastReceiver broadcastReceiverTemp = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("TemperatureChange")) {
                String change = intent.getExtras().getString("change");
                songGroup = Integer.parseInt(change);
                if (songGroup == 1) {
                    song = 1;
                }
                else if (songGroup == 2) {
                    song = 4;
                }
                else {
                    song = 7;
                }
                if (isPlaying() && !radioOn) {
                    stop();
                    start();
                }
                else if (isPaused && !radioOn) {
                    isPlaying = false;
                    isPaused = false;
                    isStopped = false;
                    mediaPlayer = null;
                    start();
                    pause();
                }
                else if (isStopped && !radioOn) {
                    isPlaying = false;
                    isPaused = false;
                    isStopped = false;
                    mediaPlayer = null;
                    start();
                    stop();
                }
                else if (radioOn) {
                    radioOff();
                    radioOn();
                }
            }
        }
    };

    private BroadcastReceiver broadcastReceiverMain = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("SeekBarChange") && !radioOn && (isPlaying() || isPaused())) {
                String value = intent.getExtras().getString("change");
                if (mediaPlayer == null) {
                    mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.song1);
                }
                mediaPlayer.seekTo(Integer.parseInt(value));
            }
        }
    };
}
