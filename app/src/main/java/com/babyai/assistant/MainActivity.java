package com.babyai.assistant;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.speech.*;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout messages;
    EditText input;
    TextToSpeech tts;
    SpeechRecognizer recognizer;
    android.content.SharedPreferences prefs;
    static final int REQ_AUDIO=21;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs=getSharedPreferences("baby_ai_local",MODE_PRIVATE);
        tts=new TextToSpeech(this, status -> {
            if(status==TextToSpeech.SUCCESS) {
                int langResult = tts.setLanguage(new Locale("hi","IN"));
                // Prefer an installed Hindi voice whose name identifies it as female.
                // Android does not expose a universal gender flag, so use common voice-name hints.
                if (Build.VERSION.SDK_INT >= 21) {
                    try {
                        Set<String> femaleHints = new HashSet<>(Arrays.asList(
                            "female", "woman", "fem", "स्त्री", "महिला"
                        ));
                        for (android.speech.tts.Voice voice : tts.getVoices()) {
                            String name = voice.getName().toLowerCase(Locale.ROOT);
                            boolean hindiVoice = name.contains("hi-in") || name.contains("hindi") || name.contains("hin");
                            boolean femaleVoice = false;
                            for (String hint : femaleHints) if (name.contains(hint)) femaleVoice = true;
                            if (hindiVoice && femaleVoice && !voice.isNetworkConnectionRequired()) {
                                tts.setVoice(voice);
                                break;
                            }
                        }
                    } catch (Exception ignored) {}
                }
                tts.setSpeechRate(0.95f);
            }
        });
        buildUi();
        restore();
        if(messages.getChildCount()==0) add("Baby AI","नमस्ते बॉस! मैं Baby AI हूँ। आप लिखकर या बोलकर बात कर सकते हैं।");
        registerReceiver(batteryReceiver,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }
    void buildUi() {
        LinearLayout root=new LinearLayout(this); root.setOrientation(1);
        root.setPadding(18,18,18,18); root.setBackgroundColor(Color.rgb(250,242,255));
        TextView title=new TextView(this); title.setText("💗 Baby AI"); title.setTextSize(26); title.setTextColor(Color.rgb(120,40,150)); title.setGravity(17);
        root.addView(title,new LinearLayout.LayoutParams(-1,64));
        ScrollView scroll=new ScrollView(this);
        messages=new LinearLayout(this); messages.setOrientation(1); scroll.addView(messages);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        input=new EditText(this); input.setHint("बॉस, यहाँ लिखें..."); root.addView(input);
        LinearLayout row=new LinearLayout(this);
        Button send=new Button(this); send.setText("भेजें"); send.setOnClickListener(v->sendText());
        Button mic=new Button(this); mic.setText("🎙 बोलें"); mic.setOnClickListener(v->listen());
        row.addView(send,new LinearLayout.LayoutParams(0,56,1)); row.addView(mic,new LinearLayout.LayoutParams(0,56,1)); root.addView(row);
        LinearLayout apps=new LinearLayout(this);
        addAppButton(apps,"WhatsApp","com.whatsapp");
        addAppButton(apps,"YouTube","com.google.android.youtube");
        addAppButton(apps,"Chrome","com.android.chrome");
        root.addView(apps); setContentView(root);
    }
    void addAppButton(LinearLayout row,String label,String pkg) {
        Button b=new Button(this); b.setText(label); b.setOnClickListener(v->{
            Intent i=getPackageManager().getLaunchIntentForPackage(pkg);
            if(i!=null) startActivity(i); else speak(label+" आपके फोन में नहीं मिला।");
        });
        row.addView(b,new LinearLayout.LayoutParams(0,52,1));
    }
    void sendText() {
        String q=input.getText().toString().trim(); if(q.isEmpty()) return;
        input.setText(""); add("बॉस",q);
        String answer=offlineAnswer(q); add("Baby AI",answer); speak(answer);
    }
    String offlineAnswer(String q) {
        String s=q.toLowerCase(Locale.ROOT);
        if(s.contains("namaste")||s.contains("hello")||s.contains("नमस्ते")) return "नमस्ते बॉस! बताइए, मैं आपकी क्या मदद करूँ?";
        if(s.contains("tumhara naam")||s.contains("तुम्हारा नाम")||s.contains("your name")) return "मेरा नाम Baby AI है। मैं आपकी हिंदी AI सहायक हूँ।";
        if(s.contains("kaise ho")||s.contains("कैसी हो")||s.contains("कैसे हो")) return "मैं अच्छी हूँ बॉस! आप बताइए, मैं आपकी क्या मदद करूँ?";
        if(s.contains("samay")||s.contains("time")||s.contains("समय")) return "बॉस, अभी समय "+new java.text.SimpleDateFormat("hh:mm a",Locale.getDefault()).format(new Date())+" है।";
        if(s.contains("battery")||s.contains("बैटरी")) return "बॉस, बैटरी की जानकारी फोन के स्टेटस बार में देख सकते हैं।";
        if(s.contains("whatsapp")) { launch("com.whatsapp"); return "बॉस, WhatsApp खोलने की कोशिश कर रही हूँ।"; }
        if(s.contains("youtube")) { launch("com.google.android.youtube"); return "बॉस, YouTube खोलने की कोशिश कर रही हूँ।"; }
        if(s.contains("chrome")) { launch("com.android.chrome"); return "बॉस, Chrome खोलने की कोशिश कर रही हूँ।"; }
        return "बॉस, अभी मेरा ऑफलाइन जवाब सीमित है। पूरी बातचीत के लिए सुरक्षित ऑनलाइन AI सेवा जोड़नी होगी।";
    }
    void launch(String pkg) { Intent i=getPackageManager().getLaunchIntentForPackage(pkg); if(i!=null) startActivity(i); }
    void listen() {
        if(checkSelfPermission("android.permission.RECORD_AUDIO")!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.RECORD_AUDIO"},REQ_AUDIO); return;
        }
        if(!SpeechRecognizer.isRecognitionAvailable(this)) { speak("बॉस, इस फोन में speech recognition उपलब्ध नहीं है।"); return; }
        if(recognizer!=null) recognizer.destroy();
        recognizer=SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            public void onReadyForSpeech(Bundle b) {}
            public void onBeginningOfSpeech() {}
            public void onRmsChanged(float f) {}
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() {}
            public void onError(int e) { speak("बॉस, आवाज़ समझ नहीं आई। फिर कोशिश करें।"); }
            public void onResults(Bundle b) {
                ArrayList<String> a=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if(a!=null&&!a.isEmpty()) { input.setText(a.get(0)); sendText(); }
            }
            public void onPartialResults(Bundle b) {}
            public void onEvent(int t,Bundle b) {}
        });
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizer.startListening(i);
    }
    void speak(String s) { if(tts!=null) tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"babyai"); }
    void add(String who,String msg) {
        TextView t=new TextView(this); t.setText(who+": "+msg); t.setTextSize(16);
        t.setTextColor(Color.rgb(45,35,50)); t.setPadding(12,12,12,12);
        messages.addView(t); save();
    }
    void save() {
        StringBuilder b=new StringBuilder();
        for(int i=0;i<messages.getChildCount();i++) {
            if(i>0)b.append("\n"); b.append(((TextView)messages.getChildAt(i)).getText());
        }
        prefs.edit().putString("history",b.toString()).apply();
    }
    void restore() {
        String h=prefs.getString("history","");
        if(!h.isEmpty()) for(String line:h.split("\n")) {
            TextView t=new TextView(this); t.setText(line); t.setTextSize(16);
            t.setTextColor(Color.rgb(45,35,50)); t.setPadding(12,12,12,12); messages.addView(t);
        }
    }
    BroadcastReceiver batteryReceiver=new BroadcastReceiver() {
        public void onReceive(Context c,Intent i) {
            int level=i.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
            if(level==20 && prefs.getBoolean("battery20shown",false)==false) {
                prefs.edit().putBoolean("battery20shown",true).apply();
                speak("बॉस, आपके फोन की बैटरी 20 प्रतिशत रह गई है।");
            }
            if(level>20) prefs.edit().putBoolean("battery20shown",false).apply();
        }
    };
    @Override public void onDestroy() {
        try { unregisterReceiver(batteryReceiver); } catch(Exception ignored){}
        if(recognizer!=null) recognizer.destroy();
        if(tts!=null) tts.shutdown();
        super.onDestroy();
    }
}