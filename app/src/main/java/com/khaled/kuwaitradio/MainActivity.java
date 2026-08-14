package com.khaled.kuwaitradio;

import android.animation.*;
import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.view.*;
import android.view.animation.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {

    // ── colors ────────────────────────────────────────────────────
    static final int BG      = 0xFF0A0A0F;
    static final int CARD    = 0xFF13131A;
    static final int CARD2   = 0xFF1A1A24;
    static final int ACCENT  = 0xFF6C63FF;
    static final int ACCENT2 = 0xFFAB9FF2;
    static final int TEXT    = 0xFFFFFFFF;
    static final int SUB     = 0xFF6B7280;
    static final int LIVE    = 0xFF34D399;
    static final int RED     = 0xFFEF4444;
    static final int AMBER   = 0xFFF59E0B;
    static final int DIVIDER = 0xFF1F1F2E;

    static final int[][] GRADS = {
        {0xFF6C63FF, 0xFF48CAE4},
        {0xFF667EEA, 0xFF764BA2},
        {0xFF11998E, 0xFF38EF7D},
        {0xFFEB3349, 0xFFF45C43},
        {0xFFFC5C7D, 0xFF6A3093},
        {0xFFf7971e, 0xFFffd200},
        {0xFF1D2671, 0xFFC33764},
        {0xFF134E5E, 0xFF71B280},
        {0xFF373B44, 0xFF4286F4},
        {0xFF005C97, 0xFF363795},
        {0xFF1A1A2E, 0xFF16213E},
        {0xFF0F2027, 0xFF2C5364},
    };

    static class Station {
        String name, ar, flag, info, url, country;
        int grad;
        boolean fav;
        Station(String n,String a,String f,String i,String u,String c,int g){
            name=n;ar=a;flag=f;info=i;url=u;country=c;grad=g;
        }
    }

    List<Station> all   = new ArrayList<>();
    List<Station> shown = new ArrayList<>();
    SharedPreferences prefs;
    MediaPlayer player;
    int cur = -1;
    boolean playing = false;
    int retries = 0;
    Handler handler = new Handler(Looper.getMainLooper());
    Runnable retryRun, sleepRun;
    int activeTab = 0;
    String searchQ = "";

    AudioManager audioManager;
    AudioFocusRequest focusRequest;
    boolean hasFocus = false;
    boolean resumeOnFocusGain = false;
    AudioManager.OnAudioFocusChangeListener focusListener;

    ValueAnimator[] waveAnims = new ValueAnimator[5];
    View[] waveBars = new View[5];
    TextView nowName, nowAr, nowFlag, nowInfo, statusTv, playIc;
    LinearLayout stationList;
    FrameLayout miniBar;
    TextView miniName, miniPl;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs = getSharedPreferences("radio", MODE_PRIVATE);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        focusListener = change -> {
            switch (change) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    resumeOnFocusGain = false;
                    pausePlayback();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    resumeOnFocusGain = playing;
                    pausePlayback();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (player != null) player.setVolume(0.2f, 0.2f);
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (player != null) player.setVolume(1f, 1f);
                    if (resumeOnFocusGain && player != null && !playing) {
                        try { player.start(); playing = true;
                            playIc.setText("⏸"); if (miniPl != null) miniPl.setText("⏸");
                            setStatus("🔴 LIVE", LIVE); startWave();
                        } catch (Exception ignored) {}
                    }
                    resumeOnFocusGain = false;
                    break;
            }
        };
        initStations();
        setContentView(buildRoot());
        filter();
    }

    void initStations() {
        // ── Kuwait 🇰🇼 ───────────────────────────────────────────
        add("Marina FM",          "مارينا إف إم",       "🇰🇼","88.8 MHz","https://stream.zeno.fm/0r0xa792kwzuv","KW",0);
        add("Kuwait Radio 1",     "راديو الكويت 1",     "🇰🇼","96.5 MHz","https://kwtkrdota.cdn.mangomolo.com/k1rdo/k1rdo.stream_aac/chunklist.m3u8","KW",1);
        add("Kuwait Radio 2",     "راديو الكويت 2",     "🇰🇼","92.5 MHz","https://kwtkrdota.cdn.mangomolo.com/k2rdo/k2rdo.stream_aac/chunklist.m3u8","KW",0);
        add("Quran Kuwait",       "إذاعة القرآن",       "🇰🇼","Quran","http://stream.radiojar.com/0tpy1h0kxtzuv","KW",2);
        add("OFW Radio",          "راديو OFW",          "🇰🇼","Community","https://s74.radiolize.com:8020/radio.mp3","KW",9);
        // ── Saudi Arabia 🇸🇦 ─────────────────────────────────────
        add("MBC FM",             "إم بي سي إف إم",    "🇸🇦","103.0 MHz","https://mbc-fm.radiojar.com/mbc-fm.mp3","SA",3);
        add("Panorama FM",        "بانوراما إف إم",     "🇸🇦","100.0 MHz","https://panoramafm.radiojar.com/panoramafm.mp3","SA",4);
        add("Rotana FM",          "روتانا إف إم",       "🇸🇦","98.0 MHz","https://rotanafm.radiojar.com/rotanafm.mp3","SA",5);
        add("Rotana Khalijiah",   "روتانا خليجية",      "🇸🇦","Khaleeji","https://rotanakhalijiah.radiojar.com/rotanakhalijiah.mp3","SA",2);
        add("Rotana Clip",        "روتانا كليب",        "🇸🇦","Music","https://rotanaclip.radiojar.com/rotanaclip.mp3","SA",7);
        add("Al Arabiya FM",      "العربية إف إم",      "🇸🇦","News","https://alarabiya.radiojar.com/alarabiya.mp3","SA",3);
        add("Jeddah FM",          "جدة إف إم",          "🇸🇦","Jeddah","https://jeddahfm.radiojar.com/jeddahfm.mp3","SA",6);
        add("Saudi Radio",        "الإذاعة السعودية",   "🇸🇦","AM/FM","https://saudi-radio.radiojar.com/saudi-radio.mp3","SA",8);
        add("Quran KSA",          "إذاعة القرآن السعودية","🇸🇦","Quran","https://quraan.radiojar.com/quraan.mp3","SA",2);
        add("Quran Al-Sudais",    "القرآن - السديس",    "🇸🇦","Quran","https://radio.mp3islam.com/listen/sudais/radio.mp3","SA",7);
        add("Quran Mishary",      "القرآن - العفاسي",   "🇸🇦","Quran","https://radio.mp3islam.com/listen/mishary/radio.mp3","SA",4);
        // ── UAE 🇦🇪 ──────────────────────────────────────────────
        add("Dubai FM",           "دبي إف إم",          "🇦🇪","92.0 MHz","https://dubaifm.radiojar.com/dubaifm.mp3","AE",1);
        add("Virgin Radio UAE",   "فيرجن راديو",        "🇦🇪","104.4 MHz","https://virginfm.radiojar.com/virginfm.mp3","AE",0);
        add("UAE FM",             "الإمارات إف إم",     "🇦🇪","93.9 MHz","https://uaefm.radiojar.com/uaefm.mp3","AE",9);
        // ── Egypt 🇪🇬 ─────────────────────────────────────────────
        add("Nogoum FM",          "نجوم إف إم",         "🇪🇬","100.6 MHz","https://nogoumfm.radiojar.com/nogoumfm.mp3","EG",5);
        add("Nile FM",            "نايل إف إم",         "🇪🇬","104.2 MHz","https://nilefm.radiojar.com/nilefm.mp3","EG",3);
        add("Nagham FM",          "نغم إف إم",          "🇪🇬","Music","https://naghamfm.radiojar.com/naghamfm.mp3","EG",4);
        add("MBC Masr",           "إم بي سي مصر",       "🇪🇬","Music","https://mbcmasr.radiojar.com/mbcmasr.mp3","EG",5);
        add("9090 FM",            "تسعين إف إم",        "🇪🇬","90.9 MHz","https://9090streaming.mobtada.com/9090FMEGYPT","EG",6);
        add("Quran Cairo",        "القرآن الكريم",      "🇪🇬","Quran","http://n12.radiojar.com/8s5u5tpdtwzuv","EG",7);
        // ── Lebanon 🇱🇧 ──────────────────────────────────────────
        add("Voice of Lebanon",   "صوت لبنان",          "🇱🇧","93.3 MHz","https://l3.itworkscdn.net/itwaudio/9054/stream","LB",6);
        add("Beirut Nights",      "ليالي بيروت",        "🇱🇧","Music","http://byblosnights.com:8000/liveaac","LB",10);
        // ── Jordan 🇯🇴 ───────────────────────────────────────────
        add("Mazaj FM",           "مزاج إف إم",         "🇯🇴","105.1 MHz","https://mazajfm.ice.infomaniak.ch/mazajfm-192.mp3","JO",8);
        // ── Qatar 🇶🇦 ────────────────────────────────────────────
        add("Al Jazeera Voice",   "الجزيرة صوت",        "🇶🇦","News","https://live-hls-audio-web-aja.getaj.net/VOICE-AJA/01.m3u8","QA",9);
        // ── Bahrain 🇧🇭 ──────────────────────────────────────────
        add("Monte Carlo",        "مونت كارلو",         "🇧🇭","Arabic","https://montecarlodoualiya128k.ice.infomaniak.ch/mc-doualiya.mp3","BH",1);
        // ── Oman 🇴🇲 ─────────────────────────────────────────────
        add("Hala FM Oman",       "هلا إف إم عُمان",    "🇴🇲","Music","https://listen-halafm.sharp-stream.com/halafmlow.mp3","OM",11);
        add("Quran Oman",         "القرآن - عُمان",     "🇴🇲","Quran","https://partwota.cdn.mgmlcdn.com/quranrdoorg/quranrdo.stream_aac/chunklist.m3u8","OM",7);
        // ── Morocco 🇲🇦 ──────────────────────────────────────────
        add("Hit Radio",          "هيت راديو",          "🇲🇦","Morocco","https://hitradio.radiojar.com/hitradio.mp3","MA",3);
        add("Medi1 Radio",        "ميدي 1",             "🇲🇦","Morocco","https://medi1radio.radiojar.com/medi1radio.mp3","MA",8);
        add("Aswat Morocco",      "أصوات المغرب",       "🇲🇦","Morocco","http://broadcast.ice.infomaniak.ch/aswat-high.mp3","MA",6);
        // ── Tunisia 🇹🇳 ──────────────────────────────────────────
        add("Mosaique FM",        "موزاييك إف إم",      "🇹🇳","Tunisia","http://radio.mosaiquefm.net:8000/mosalive","TN",4);
        add("Jawhara FM",         "جوهرة إف إم",        "🇹🇳","Tunisia","http://streaming2.toutech.net:8000/jawharafm","TN",2);
        // ── News 📰 ──────────────────────────────────────────────
        add("Sky Arabia",         "سكاي نيوز عربية",   "📰","News","http://stream.skynewsarabia.com/hls/sna_720.m3u8","NEWS",9);
        add("France 24 Arabic",   "فرانس 24 عربي",      "📰","News","http://static.france24.com/live/F24_AR_LO_HLS/live_web.m3u8","NEWS",1);
        for (Station s : all) s.fav = prefs.getBoolean("f"+s.url.hashCode(), false);
    }
    void add(String n,String a,String f,String i,String u,String c,int g){all.add(new Station(n,a,f,i,u,c,g));}

    // ── root ──────────────────────────────────────────────────────
    View buildRoot() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);

        ScrollView sv = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(0, dp(16), 0, dp(72));

        page.addView(buildNowPlaying());
        page.addView(space(16));
        page.addView(buildSearchAndTabs());
        page.addView(space(8));
        stationList = new LinearLayout(this);
        stationList.setOrientation(LinearLayout.VERTICAL);
        stationList.setPadding(dp(16), 0, dp(16), 0);
        page.addView(stationList);

        sv.addView(page);
        root.addView(sv, new FrameLayout.LayoutParams(MATCH, MATCH));

        miniBar = buildMini();
        FrameLayout.LayoutParams mlp = new FrameLayout.LayoutParams(MATCH, dp(64));
        mlp.gravity = Gravity.BOTTOM;
        root.addView(miniBar, mlp);

        sv.getViewTreeObserver().addOnScrollChangedListener(() ->
            miniBar.setVisibility(sv.getScrollY() > dp(260) && cur >= 0 ? View.VISIBLE : View.GONE));
        return root;
    }

    // ── now playing ───────────────────────────────────────────────
    View buildNowPlaying() {
        // gradient bg card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(20), dp(24), dp(20), dp(24));
        card.setLayoutParams(new LinearLayout.LayoutParams(MATCH, WRAP));

        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xFF13131A, 0xFF0A0A0F});
        card.setBackground(bg);

        // flag + name row
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setLayoutParams(new LinearLayout.LayoutParams(MATCH, WRAP));

        nowFlag = tv("📻", 26, 0, 0);
        nowFlag.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        nowFlag.setGravity(Gravity.CENTER);

        LinearLayout nameCol = new LinearLayout(this);
        nameCol.setOrientation(LinearLayout.VERTICAL);
        nameCol.setPadding(dp(10), 0, 0, 0);
        nameCol.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP, 1f));

        nowAr = tv("اختر محطة", 17, Typeface.BOLD, TEXT);
        nowName = tv("Select a station", 12, 0, SUB);
        nameCol.addView(nowAr); nameCol.addView(nowName);

        statusTv = tv("  ⏸  ", 10, Typeface.BOLD, SUB);
        statusTv.setBackground(rr(CARD2, dp(20)));
        statusTv.setPadding(dp(10), dp(4), dp(10), dp(4));

        topRow.addView(nowFlag); topRow.addView(nameCol); topRow.addView(statusTv);
        card.addView(topRow);
        card.addView(space(20));

        // wave bars
        LinearLayout waveRow = new LinearLayout(this);
        waveRow.setOrientation(LinearLayout.HORIZONTAL);
        waveRow.setGravity(Gravity.CENTER | Gravity.BOTTOM);
        waveRow.setLayoutParams(new LinearLayout.LayoutParams(MATCH, dp(40)));
        int[] wdel = {0, 120, 60, 200, 90};
        for (int i = 0; i < 5; i++) {
            View bar = new View(this);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(4), dp(6));
            bp.setMargins(dp(4), 0, dp(4), 0);
            bar.setLayoutParams(bp);
            GradientDrawable bd = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{ACCENT, ACCENT2});
            bd.setCornerRadius(dp(3));
            bar.setBackground(bd);
            waveBars[i] = bar;
            waveRow.addView(bar);
        }
        card.addView(waveRow);
        card.addView(space(20));

        // info line
        nowInfo = tv("—", 11, 0, SUB);
        nowInfo.setGravity(Gravity.CENTER);
        card.addView(nowInfo);
        card.addView(space(20));

        // controls
        LinearLayout ctrl = new LinearLayout(this);
        ctrl.setOrientation(LinearLayout.HORIZONTAL);
        ctrl.setGravity(Gravity.CENTER);

        View prevB = mkBtn("⏮", dp(50), false);
        prevB.setOnClickListener(v -> move(-1));

        FrameLayout playB = new FrameLayout(this);
        int ps = dp(70);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(ps, ps);
        pp.setMargins(dp(20), 0, dp(20), 0);
        playB.setLayoutParams(pp);
        GradientDrawable pgd = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{ACCENT, ACCENT2});
        pgd.setCornerRadius(ps / 2f);
        playB.setBackground(pgd);
        playIc = tv("▶", 24, Typeface.BOLD, Color.WHITE);
        playIc.setGravity(Gravity.CENTER);
        playB.addView(playIc, new FrameLayout.LayoutParams(MATCH, MATCH));
        playB.setOnClickListener(v -> togglePlay());
        playB.setOnLongClickListener(v -> { showTimerMenu(); return true; });

        View nextB = mkBtn("⏭", dp(50), false);
        nextB.setOnClickListener(v -> move(1));

        ctrl.addView(prevB); ctrl.addView(playB); ctrl.addView(nextB);
        card.addView(ctrl);
        return card;
    }

    View mkBtn(String ic, int sz, boolean big) {
        FrameLayout b = new FrameLayout(this);
        b.setLayoutParams(new LinearLayout.LayoutParams(sz, sz));
        b.setBackground(rr(CARD2, sz/2));
        TextView t = tv(ic, big?22:16, 0, ACCENT2);
        t.setGravity(Gravity.CENTER);
        b.addView(t, new FrameLayout.LayoutParams(MATCH, MATCH));
        return b;
    }

    // ── search + tabs ─────────────────────────────────────────────
    View buildSearchAndTabs() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), 0, dp(16), 0);

        // search
        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        searchRow.setPadding(dp(14), 0, dp(14), 0);
        searchRow.setBackground(rr(CARD, dp(14)));

        TextView sic = tv("🔍", 14, 0, SUB);
        sic.setPadding(0, 0, dp(8), 0);

        EditText et = new EditText(this);
        et.setHint("بحث · Search");
        et.setHintTextColor(SUB);
        et.setTextColor(TEXT);
        et.setBackground(null);
        et.setTextSize(14);
        et.setSingleLine(true);
        et.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        et.setLayoutParams(new LinearLayout.LayoutParams(0, dp(46), 1f));
        et.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s){searchQ=s.toString().trim().toLowerCase();filter();}
            public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            public void onTextChanged(CharSequence s,int a,int b,int c){}
        });
        searchRow.addView(sic); searchRow.addView(et);
        col.addView(searchRow);
        col.addView(space(10));

        // tabs
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);

        String[] labels = {"🌍 الكل","🇰🇼 الكويت","🇸🇦 السعودية","🇦🇪 الإمارات","🌐 عربي","📰 أخبار","❤️"};
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView t = new TextView(this);
            t.setText(labels[i]);
            t.setTextSize(12); t.setTypeface(null, Typeface.BOLD);
            t.setGravity(Gravity.CENTER);
            t.setPadding(dp(16), dp(8), dp(16), dp(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP, WRAP);
            lp.setMargins(0, 0, dp(8), 0);
            t.setLayoutParams(lp);
            t.setBackground(rr(i==0?ACCENT:CARD2, dp(20)));
            t.setTextColor(i==0?Color.WHITE:SUB);
            t.setOnClickListener(v -> {
                activeTab = idx;
                for (int j = 0; j < tabs.getChildCount(); j++) {
                    View c = tabs.getChildAt(j);
                    c.setBackground(rr(j==idx?ACCENT:CARD2, dp(20)));
                    if (c instanceof TextView) ((TextView)c).setTextColor(j==idx?Color.WHITE:SUB);
                }
                filter();
            });
            tabs.addView(t);
        }
        hsv.addView(tabs);
        col.addView(hsv);
        return col;
    }

    void filter() {
        shown.clear();
        for (Station s : all) {
            boolean tab = activeTab==0
                || (activeTab==1 && "KW".equals(s.country))
                || (activeTab==2 && "SA".equals(s.country))
                || (activeTab==3 && "AE".equals(s.country))
                || (activeTab==4 && Arrays.asList("EG","LB","JO","TN","MA").contains(s.country))
                || (activeTab==5 && "NEWS".equals(s.country))
                || (activeTab==6 && s.fav);
            boolean sq = searchQ.isEmpty()
                || s.name.toLowerCase().contains(searchQ)
                || s.ar.contains(searchQ);
            if (tab && sq) shown.add(s);
        }
        rebuildList();
    }

    void rebuildList() {
        stationList.removeAllViews();
        if (shown.isEmpty()) {
            TextView e = tv("لا توجد محطات", 14, 0, SUB);
            e.setGravity(Gravity.CENTER); e.setPadding(0,dp(32),0,dp(32));
            stationList.addView(e); return;
        }
        for (int i = 0; i < shown.size(); i++) stationList.addView(buildCard(i));
    }

    View buildCard(int si) {
        Station s = shown.get(si);
        int ri = all.indexOf(s);
        boolean active = ri == cur;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(13), dp(14), dp(13));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.bottomMargin = dp(6);
        row.setLayoutParams(lp);

        GradientDrawable rowBg = new GradientDrawable();
        rowBg.setColor(active ? 0xFF1E1B3A : CARD);
        rowBg.setCornerRadius(dp(14));
        if (active) rowBg.setStroke(dp(1), ACCENT);
        row.setBackground(rowBg);

        // gradient circle
        int[] gc = GRADS[s.grad % GRADS.length];
        FrameLayout circle = new FrameLayout(this);
        int csz = dp(46);
        circle.setLayoutParams(new LinearLayout.LayoutParams(csz, csz));
        GradientDrawable cgd = new GradientDrawable(GradientDrawable.Orientation.TL_BR, gc);
        cgd.setCornerRadius(csz/2f);
        circle.setBackground(cgd);
        TextView fl = tv(s.flag, 18, 0, 0);
        fl.setGravity(Gravity.CENTER);
        circle.addView(fl, new FrameLayout.LayoutParams(MATCH, MATCH));

        // text col
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(12), 0, 0, 0);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP, 1f));

        TextView ar = tv(s.ar, 15, Typeface.BOLD, active ? ACCENT2 : TEXT);
        TextView en = tv(s.name + "  " + s.info, 11, 0, SUB);
        col.addView(ar); col.addView(en);

        // live dot
        if (active && playing) {
            TextView ld = tv("● LIVE", 10, Typeface.BOLD, LIVE);
            col.addView(ld);
        }

        // fav btn
        TextView fav = new TextView(this);
        fav.setText(s.fav?"❤️":"🤍");
        fav.setTextSize(18); fav.setPadding(dp(8),0,0,0);
        fav.setOnClickListener(v -> {
            s.fav=!s.fav;
            prefs.edit().putBoolean("f"+s.url.hashCode(),s.fav).apply();
            fav.setText(s.fav?"❤️":"🤍");
        });

        row.addView(circle); row.addView(col); row.addView(fav);
        row.setOnClickListener(v -> selectStation(ri));
        return row;
    }

    // ── mini bar ──────────────────────────────────────────────────
    FrameLayout buildMini() {
        FrameLayout mp = new FrameLayout(this);
        mp.setVisibility(View.GONE);
        GradientDrawable mgd = new GradientDrawable();
        mgd.setColor(0xF0111118); mgd.setStroke(dp(1), DIVIDER);
        mp.setBackground(mgd);
        mp.setPadding(dp(20), 0, dp(20), 0);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new FrameLayout.LayoutParams(MATCH, MATCH));

        miniName = tv("—", 14, Typeface.BOLD, TEXT);
        miniName.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP, 1f));
        miniName.setMaxLines(1); miniName.setEllipsize(TextUtils.TruncateAt.END);

        miniPl = new TextView(this);
        miniPl.setText("▶"); miniPl.setTextSize(22); miniPl.setTextColor(ACCENT2);
        miniPl.setOnClickListener(v -> togglePlay());

        row.addView(miniName); row.addView(miniPl);
        mp.addView(row);
        return mp;
    }

    // ── timer ─────────────────────────────────────────────────────
    void showTimerMenu() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("⏰ مؤقت النوم · Sleep Timer")
            .setItems(new String[]{"15 دقيقة","30 دقيقة","60 دقيقة","90 دقيقة","إلغاء المؤقت"},
                (d,w) -> { int[] m={15,30,60,90,0}; if(w==4)cancelSleep(); else setSleep(m[w]); })
            .show();
    }
    void setSleep(int m){cancelSleep();sleepRun=()->{stopPlayer();Toast.makeText(this,"📻 تم الإيقاف",Toast.LENGTH_SHORT).show();};handler.postDelayed(sleepRun,m*60000L);Toast.makeText(this,"⏰ "+m+" دقيقة",Toast.LENGTH_SHORT).show();}
    void cancelSleep(){if(sleepRun!=null){handler.removeCallbacks(sleepRun);sleepRun=null;}}

    // ── playback ──────────────────────────────────────────────────
    void selectStation(int idx) {
        cur=idx; retries=0;
        Station s=all.get(idx);
        nowAr.setText(s.ar); nowName.setText(s.name);
        nowFlag.setText(s.flag); nowInfo.setText(s.flag+" "+s.info+" · "+s.name);
        setStatus("⏳",AMBER);
        if(miniName!=null)miniName.setText(s.ar);
        playStation(s.url); rebuildList();
    }

    void playStation(String url) {
        stopPlayer();
        MediaPlayer mp0 = new MediaPlayer();
        player = mp0;
        mp0.setAudioAttributes(new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA).build());
        mp0.setWakeMode(getApplicationContext(), android.os.PowerManager.PARTIAL_WAKE_LOCK);
        try {
            mp0.setDataSource(this, Uri.parse(url));
            mp0.setOnPreparedListener(mp -> {
                if (mp != player) { mp.release(); return; } // stale callback from a station we already switched away from
                if (!requestFocus()) { setStatus("❌", RED); return; }
                mp.start(); playing=true; retries=0;
                playIc.post(()->{ playIc.setText("⏸"); if(miniPl!=null)miniPl.setText("⏸"); });
                setStatus("🔴 LIVE", LIVE);
                startWave(); rebuildList();
            });
            mp0.setOnErrorListener((mp,w,e)->{
                if (mp != player) return true; // stale callback, already replaced
                playing=false; stopWave();
                if(retries<3){ retries++;
                    setStatus("🔄 "+retries+"/3",AMBER);
                    retryRun=()->{ if(cur>=0)playStation(all.get(cur).url); };
                    handler.postDelayed(retryRun,3000);
                } else setStatus("❌",RED);
                return true;
            });
            mp0.prepareAsync();
        } catch(Exception e){ setStatus("❌",RED); }
    }

    void togglePlay() {
        if(player==null){if(cur>=0)selectStation(cur);return;}
        if(playing){
            pausePlayback();
        } else {
            if (!requestFocus()) return;
            player.start();playing=true;playIc.setText("⏸");if(miniPl!=null)miniPl.setText("⏸");setStatus("🔴 LIVE",LIVE);startWave();
        }
    }

    void pausePlayback(){
        if (player == null || !playing) return;
        try { player.pause(); } catch (Exception ignored) {}
        playing=false;
        playIc.setText("▶"); if(miniPl!=null) miniPl.setText("▶");
        setStatus("⏸",SUB); stopWave();
    }

    void move(int d){if(all.isEmpty())return;selectStation(cur<0?0:(cur+d+all.size())%all.size());}

    void stopPlayer(){
        stopWave();
        abandonFocus();
        if(retryRun!=null){handler.removeCallbacks(retryRun);retryRun=null;}
        if(player!=null){try{player.stop();}catch(Exception x){}try{player.release();}catch(Exception x){}player=null;}
        playing=false;
    }

    boolean requestFocus(){
        if (hasFocus) return true;
        int result;
        if (Build.VERSION.SDK_INT >= 26) {
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA).build())
                .setOnAudioFocusChangeListener(focusListener)
                .build();
            result = audioManager.requestAudioFocus(focusRequest);
        } else {
            result = audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        return hasFocus;
    }

    void abandonFocus(){
        if (!hasFocus) return;
        if (Build.VERSION.SDK_INT >= 26 && focusRequest != null) audioManager.abandonAudioFocusRequest(focusRequest);
        else audioManager.abandonAudioFocus(focusListener);
        hasFocus = false;
    }

    void setStatus(String t,int c){statusTv.post(()->{statusTv.setText(" "+t+" ");statusTv.setTextColor(c);});}

    // ── wave ──────────────────────────────────────────────────────
    void startWave() {
        int[] maxH={dp(16),dp(28),dp(20),dp(32),dp(18)};
        int[] del={0,100,200,50,150};
        for(int i=0;i<5;i++){
            if(waveAnims[i]!=null)waveAnims[i].cancel();
            final View bar=waveBars[i]; final int mh=maxH[i];
            ValueAnimator a=ValueAnimator.ofInt(dp(5),mh);
            a.setDuration(450+del[i]); a.setRepeatCount(ValueAnimator.INFINITE);
            a.setRepeatMode(ValueAnimator.REVERSE);
            a.setInterpolator(new AccelerateDecelerateInterpolator());
            a.addUpdateListener(an->{ViewGroup.LayoutParams lp=bar.getLayoutParams();lp.height=(int)an.getAnimatedValue();bar.setLayoutParams(lp);});
            a.setStartDelay(del[i]); a.start(); waveAnims[i]=a;
        }
    }
    void stopWave(){
        for(int i=0;i<5;i++){
            if(waveAnims[i]!=null){waveAnims[i].cancel();waveAnims[i]=null;}
            if(waveBars[i]!=null){ViewGroup.LayoutParams lp=waveBars[i].getLayoutParams();lp.height=dp(5);waveBars[i].setLayoutParams(lp);}
        }
    }

    // ── helpers ───────────────────────────────────────────────────
    static final int MATCH=ViewGroup.LayoutParams.MATCH_PARENT;
    static final int WRAP=ViewGroup.LayoutParams.WRAP_CONTENT;
    int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    GradientDrawable rr(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(r);return d;}
    TextView tv(String t,int sp,int st,int c){TextView v=new TextView(this);v.setText(t);v.setTextSize(sp);if(st!=0)v.setTypeface(null,st);if(c!=0)v.setTextColor(c);return v;}
    View space(int dp){View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(MATCH,dp(dp)));return v;}
    @Override protected void onDestroy(){super.onDestroy();stopPlayer();cancelSleep();}
}
