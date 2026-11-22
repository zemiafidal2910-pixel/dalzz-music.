
package com.dalzz.music;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.*;
import android.media.MediaPlayer;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends Activity {

    String API = "https://dalzz-music.dalzz6831.workers.dev/api/music";
    LinearLayout list;
    TextView status;
    MediaPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(8,13,11));
        getWindow().setNavigationBarColor(Color.rgb(8,13,11));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(1);
        page.setPadding(20,30,20,15);
        page.setBackgroundColor(Color.rgb(8,13,11));

        TextView title = new TextView(this);
        title.setText("DALZZ MUSIC");
        title.setTextSize(27);
        title.setTextColor(Color.rgb(32,232,121));
        title.setTypeface(null,Typeface.BOLD);
        page.addView(title);

        status = new TextView(this);
        status.setText("Memuat musik...");
        status.setTextColor(Color.LTGRAY);
        status.setPadding(0,20,0,15);
        page.addView(status);

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(1);
        scroll.addView(list);
        page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        setContentView(page);

        loadMusic();
    }

    private void loadMusic() {
        new Thread(() -> {
            try {
                URL url = new URL(API);
                HttpURLConnection c = (HttpURLConnection)url.openConnection();
                c.setRequestMethod("GET");
                c.setConnectTimeout(15000);
                c.setReadTimeout(20000);

                BufferedReader r = new BufferedReader(
                    new InputStreamReader(c.getInputStream())
                );

                StringBuilder result = new StringBuilder();
                String line;

                while ((line = r.readLine()) != null) {
                    result.append(line);
                }

                r.close();
                c.disconnect();

                JSONObject data = new JSONObject(result.toString());
                JSONArray tracks = data.optJSONArray("tracks");

                runOnUiThread(() -> showTracks(tracks));

            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Gagal memuat musik. Periksa koneksi."));
            }
        }).start();
    }

    private void showTracks(JSONArray tracks) {
        list.removeAllViews();

        if (tracks == null || tracks.length() == 0) {
            status.setText("Belum ada lagu.");
            return;
        }

        status.setText("Popular Music • " + tracks.length() + " lagu");

        for (int i=0; i<tracks.length(); i++) {
            JSONObject song = tracks.optJSONObject(i);

            String name = song.optString("title","Unknown");
            String artist = song.optString("artist","Unknown Artist");
            String audio = song.optString("audio","");

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(1);
            card.setPadding(16,15,16,15);
            card.setBackgroundColor(Color.rgb(20,31,26));

            LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1,-2);
            params.setMargins(0,0,0,12);
            list.addView(card,params);

            TextView songTitle = new TextView(this);
            songTitle.setText(name);
            songTitle.setTextColor(Color.WHITE);
            songTitle.setTextSize(16);
            songTitle.setTypeface(null,Typeface.BOLD);
            card.addView(songTitle);

            TextView songArtist = new TextView(this);
            songArtist.setText(artist);
            songArtist.setTextColor(Color.LTGRAY);
            songArtist.setTextSize(13);
            card.addView(songArtist);

            TextView play = new TextView(this);
            play.setText("▶  Putar");
            play.setTextColor(Color.rgb(32,232,121));
            play.setPadding(0,12,0,0);
            card.addView(play);

            card.setOnClickListener(v -> playMusic(audio,name));
        }
    }

    private void playMusic(String audio,String name) {
        if (audio.isEmpty()) {
            Toast.makeText(this,"Audio tidak tersedia",Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (player != null) {
                player.release();
            }

            status.setText("Memuat: " + name);
            player = new MediaPlayer();
            player.setDataSource(audio);

            player.setOnPreparedListener(mp -> {
                mp.start();
                status.setText("Sedang diputar: " + name);
            });

            player.setOnErrorListener((mp,what,extra) -> {
                status.setText("Audio gagal diputar");
                return true;
            });

            player.prepareAsync();

        } catch (Exception e) {
            status.setText("Gagal memutar audio");
        }
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}
