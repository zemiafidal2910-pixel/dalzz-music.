
package com.dalzz.music;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import android.media.MediaPlayer;
import org.json.*;
import java.net.*;
import java.io.*;

public class MainActivity extends Activity {

    String API = "https://dalzz-music.dalzz6831.workers.dev";
    LinearLayout list;
    EditText search;
    TextView status, miniTitle, miniArtist, playButton;
    MediaPlayer player;
    String currentTitle = "";
    boolean playing = false;

    int bg = Color.rgb(7,13,10);
    int green = Color.rgb(32,232,121);
    int glass = Color.rgb(20,35,27);

    int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density);
    }

    GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (stroke > 0) d.setStroke(dp(1), stroke);
        return d;
    }

    TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(1);
        page.setPadding(dp(20),dp(25),dp(20),dp(10));
        page.setBackgroundColor(bg);

        TextView logo = text("DALZZ MUSIC",27,green,true);
        page.addView(logo);

        TextView subtitle = text("Your music, your world.",13,Color.LTGRAY,false);
        page.addView(subtitle);

        TextView heading = text("Discover Music",23,Color.WHITE,true);
        heading.setPadding(0,dp(25),0,dp(15));
        page.addView(heading);

        LinearLayout searchBox = new LinearLayout(this);
        searchBox.setGravity(Gravity.CENTER_VERTICAL);
        searchBox.setPadding(dp(13),0,dp(8),0);
        searchBox.setBackground(shape(glass,17,0x5535D98B));

        search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Cari judul atau artis...");
        search.setHintTextColor(Color.GRAY);
        search.setTextColor(Color.WHITE);
        search.setTextSize(14);
        search.setBackgroundColor(Color.TRANSPARENT);
        searchBox.addView(search,new LinearLayout.LayoutParams(0,dp(50),1));

        Button searchBtn = new Button(this);
        searchBtn.setText("Cari");
        searchBtn.setTextColor(Color.BLACK);
        searchBtn.setTextSize(12);
        searchBtn.setAllCaps(false);
        searchBtn.setBackground(shape(green,13,0));
        searchBox.addView(searchBtn);

        page.addView(searchBox);

        status = text("Memuat musik...",12,Color.LTGRAY,false);
        status.setPadding(0,dp(18),0,dp(12));
        page.addView(status);

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(1);
        scroll.addView(list);
        page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout mini = new LinearLayout(this);
        mini.setGravity(Gravity.CENTER_VERTICAL);
        mini.setPadding(dp(15),dp(10),dp(15),dp(10));
        mini.setBackground(shape(glass,18,0x5535D98B));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(1);

        miniTitle = text("DALZZ MUSIC",14,Color.WHITE,true);
        miniArtist = text("Pilih lagu untuk diputar",11,Color.LTGRAY,false);

        info.addView(miniTitle);
        info.addView(miniArtist);

        mini.addView(info,new LinearLayout.LayoutParams(0,-2,1));

        playButton = text("▶",23,green,true);
        mini.addView(playButton);

        page.addView(mini,new LinearLayout.LayoutParams(-1,dp(65)));

        setContentView(page);

        searchBtn.setOnClickListener(v -> searchMusic());
        search.setOnEditorActionListener((v,a,e) -> {
            searchMusic();
            return true;
        });

        playButton.setOnClickListener(v -> {
            if (player != null) {
                if (playing) {
                    player.pause();
                    playing = false;
                    playButton.setText("▶");
                } else {
                    player.start();
                    playing = true;
                    playButton.setText("Ⅱ");
                }
            }
        });

        loadMusic(API + "/api/music", "Popular Music");
    }

    void searchMusic() {
        String q = search.getText().toString().trim();

        if (q.isEmpty()) {
            Toast.makeText(this,"Masukkan judul lagu atau artis",Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String url = API + "/api/music/search?q=" +
                URLEncoder.encode(q,"UTF-8");

            ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(search.getWindowToken(),0);

            loadMusic(url,"Hasil pencarian: " + q);

        } catch (Exception e) {
            status.setText("Pencarian gagal.");
        }
    }

    void loadMusic(String url, String label) {
        status.setText("Mencari musik...");

        new Thread(() -> {
            try {
                HttpURLConnection c =
                    (HttpURLConnection)new URL(url).openConnection();

                c.setRequestMethod("GET");
                c.setConnectTimeout(15000);
                c.setReadTimeout(20000);

                BufferedReader r = new BufferedReader(
                    new InputStreamReader(c.getInputStream())
                );

                StringBuilder result = new StringBuilder();
                String line;

                while ((line = r.readLine()) != null)
                    result.append(line);

                r.close();
                c.disconnect();

                JSONObject data = new JSONObject(result.toString());
                JSONArray tracks = data.optJSONArray("tracks");

                if (tracks == null)
                    tracks = data.optJSONArray("data");

                JSONArray finalTracks = tracks;

                runOnUiThread(() -> showTracks(finalTracks,label));

            } catch (Exception e) {
                runOnUiThread(() ->
                    status.setText("Gagal memuat musik. Coba lagi.")
                );
            }
        }).start();
    }

    void showTracks(JSONArray tracks, String label) {
        list.removeAllViews();

        if (tracks == null || tracks.length() == 0) {
            status.setText("Lagu tidak ditemukan.");
            return;
        }

        status.setText(label + " · " + tracks.length() + " lagu");

        for (int i=0; i<tracks.length(); i++) {
            JSONObject song = tracks.optJSONObject(i);

            if (song == null) continue;

            String name = song.optString("title","Unknown");
            String artist = song.optString("artist","Unknown Artist");
            String audio = song.optString("audio","");

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(1);
            card.setPadding(dp(17),dp(16),dp(17),dp(16));
            card.setBackground(shape(glass,19,0x4435D98B));

            LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(-1,-2);
            cp.setMargins(0,0,0,dp(12));

            list.addView(card,cp);

            TextView title = text(name,16,Color.WHITE,true);
            card.addView(title);

            TextView by = text(artist,12,Color.LTGRAY,false);
            by.setPadding(0,dp(5),0,dp(10));
            card.addView(by);

            TextView play = text("▶  PLAY",12,green,true);
            card.addView(play);

            card.setOnClickListener(v -> playMusic(audio,name,artist));
        }
    }

    void playMusic(String audio,String name,String artist) {
        if (audio.isEmpty()) {
            Toast.makeText(this,"Audio tidak tersedia",Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (player != null) player.release();

            player = new MediaPlayer();
            currentTitle = name;
            playing = false;

            miniTitle.setText(name);
            miniArtist.setText(artist);
            playButton.setText("…");
            status.setText("Memuat: " + name);

            player.setDataSource(audio);

            player.setOnPreparedListener(mp -> {
                mp.start();
                playing = true;
                playButton.setText("Ⅱ");
                status.setText("Sedang diputar: " + name);
            });

            player.setOnErrorListener((mp,w,e) -> {
                status.setText("Audio gagal diputar.");
                playButton.setText("▶");
                playing = false;
                return true;
            });

            player.prepareAsync();

        } catch (Exception e) {
            status.setText("Gagal memutar audio.");
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
