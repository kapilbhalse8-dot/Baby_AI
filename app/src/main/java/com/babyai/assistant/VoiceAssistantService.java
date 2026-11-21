package com.babyai.assistant;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;
import android.speech.*;
import android.speech.tts.TextToSpeech;
import android.telephony.TelephonyManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import org.json.*;

public class VoiceAssistantService extends Service {
    private static final String CHANNEL="baby_ai_voice";
    private static final int NOTIFICATION_ID=7101;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean femaleVoice=false, listening=false, busy=false;
    private Handler handler=new Handler(Looper.getMainLooper());
    private android.content.SharedPreferences prefs;

    @Override public void onCreate() {
        super.onCreate();
        prefs=getSharedPreferences("baby_ai_local",MODE_PRIVATE);
        createChannel();
        Notification n=new Notification.Builder(this,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Baby AI चालू है")
            .setContentText("बॉस, मैं आपकी आवाज़ सुन रही हूँ")
            .setOngoing(true)
            .build();
        if(Build.VERSION.SDK_INT>=34) startForeground(NOTIFICATION_ID,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        else if(Build.VERSION.SDK_INT>=29) startForeground(NOTIFICATION_ID,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        else startForeground(NOTIFICATION_ID,n);
        tts=new TextToSpeech(this,status->{
            if(status==TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("hi","IN"));
                if(Build.VERSION.SDK_INT>=21) try {
                    Set<String> hints=new HashSet<>(Arrays.asList("female","woman","fem","स्त्री","महिला","-hia-"));
                    for(android.speech.tts.Voice v:tts.getVoices()) {
                        String name=v.getName().toLowerCase(Locale.ROOT);
                        boolean hi=name.contains("hi-in")||name.contains("hindi")||name.contains("hin");
                        boolean f=false; for(String h:hints) if(name.contains(h)) f=true;
                        if(hi&&f&&!v.isNetworkConnectionRequired()){tts.setVoice(v);femaleVoice=true;break;}
                    }
                } catch(Exception ignored) {}
                tts.setSpeechRate(.95f);
                startListening();
            }
        });
    }
    private void createChannel() {
        if(Build.VERSION.SDK_INT>=26) {
            NotificationChannel c=new NotificationChannel(CHANNEL,"Baby AI voice assistant",NotificationManager.IMPORTANCE_LOW);
            c.setDescription("Voice assistant microphone is active");
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
        }
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        String action=intent==null?null:intent.getAction();
        if("STOP_LISTENING".equals(action)){listening=false;if(recognizer!=null)recognizer.cancel();}
        else if("START_LISTENING".equals(action)){startListening();}
        return START_STICKY;
    }
    private void say(String text) {
        if(tts!=null&&femaleVoice) tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"babyai_service");
    }
    private void startListening() {
        if(listening||busy) return;
        if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        if(!SpeechRecognizer.isRecognitionAvailable(this)) {say("बॉस, इस फोन में आवाज़ पहचानने की सुविधा उपलब्ध नहीं है।");return;}
        listening=true;
        handler.postDelayed(()->{
            if(!listening)return;
            try {
                if(recognizer!=null)recognizer.destroy();
                recognizer=SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
                recognizer.setRecognitionListener(new RecognitionListener(){
                    public void onReadyForSpeech(Bundle b){}
                    public void onBeginningOfSpeech(){}
                    public void onRmsChanged(float v){}
                    public void onBufferReceived(byte[] b){}
                    public void onEndOfSpeech(){}
                    public void onError(int e){if(listening&&!busy){listening=false;handler.postDelayed(()->startListening(),900);}}
                    public void onResults(Bundle b){
                        ArrayList<String> a=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        if(a!=null&&!a.isEmpty())handleUtterance(a.get(0));
                        else if(listening)handler.postDelayed(()->startListening(),700);
                    }
                    public void onPartialResults(Bundle b){}
                    public void onEvent(int t,Bundle b){}
                });
                Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"hi-IN");
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                recognizer.startListening(i);
            } catch(Exception e){if(listening)handler.postDelayed(()->startListening(),1600);}
        },350);
    }
    private void handleUtterance(String q) {
        String s=q.toLowerCase(Locale.ROOT).trim();
        if(s.contains("सुनना बंद")||s.contains("आवाज़ बंद")||s.contains("चुप हो जाओ")) {
            listening=false;if(recognizer!=null)recognizer.cancel();say("ठीक है बॉस, मैंने सुनना रोक दिया है।");return;
        }
        if(s.contains("फिर से सुनो")||s.contains("सुनना शुरू")||s.contains("बेबी सुनो")) {
            listening=true;say("जी बॉस, सुन रही हूँ।");startListening();return;
        }
        if(s.contains("whatsapp")||s.contains("व्हाट्सएप")){openApp("com.whatsapp","WhatsApp");return;}
        if(s.contains("youtube")||s.contains("यूट्यूब")){openApp("com.google.android.youtube","YouTube");return;}
        if(s.contains("chrome")||s.contains("क्रोम")||s.contains("ब्राउज़र")){openApp("com.android.chrome","Chrome");return;}
        if(s.contains("समय बताओ")||s.contains("टाइम बताओ")){say("बॉस, अभी समय "+new java.text.SimpleDateFormat("hh:mm a",Locale.getDefault()).format(new Date())+" है।");handler.postDelayed(()->startListening(),2500);return;}
        if(s.contains("api key")||s.contains("एपीआई की")||s.contains("ऑनलाइन सेटअप")){say("बॉस, API Key डालने के लिए Baby AI ऐप खोलें।");handler.postDelayed(()->startListening(),3000);return;}
        askOnline(q);
    }
    private void openApp(String pkg,String label) {
        Intent i=getPackageManager().getLaunchIntentForPackage(pkg);
        if(i!=null){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);say("बॉस, "+label+" खोल रही हूँ।");}
        else say("बॉस, "+label+" आपके फोन में नहीं मिला।");
        handler.postDelayed(()->startListening(),2200);
    }
    private void askOnline(String question) {
        String key=prefs.getString("gemini_api_key","");
        if(key.isEmpty()){say("बॉस, ऑनलाइन AI चालू करने के लिए Baby AI ऐप खोलकर API Key सेव करें।");handler.postDelayed(()->startListening(),4500);return;}
        busy=true;listening=false;
        new Thread(()->{
            String answer=callGemini(key,question);
            handler.post(()->{
                busy=false;say(answer);
                handler.postDelayed(()->{listening=true;startListening();},Math.max(3000,Math.min(18000,answer.length()*75L)));
            });
        }).start();
    }
    private String callGemini(String key,String question) {
        HttpURLConnection c=null;
        try {
            String history=prefs.getString("history","");
            String prompt="तुम Baby AI हो, हिंदी में बात करने वाली महिला सहायक। उपयोगकर्ता को बॉस कहो। संक्षिप्त, उपयोगी और ईमानदार जवाब दो।\nपिछली बातचीत:\n"+history+"\nनया सवाल: "+question;
            JSONObject part=new JSONObject().put("text",prompt);
            JSONObject body=new JSONObject().put("contents",new JSONArray().put(new JSONObject().put("parts",new JSONArray().put(part))));
            c=(HttpURLConnection)new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent").openConnection();
            c.setRequestMethod("POST");c.setConnectTimeout(20000);c.setReadTimeout(30000);c.setDoOutput(true);
            c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("x-goog-api-key",key);
            c.getOutputStream().write(body.toString().getBytes("UTF-8"));
            int status=c.getResponseCode();
            java.io.InputStream in=(status>=200&&status<300)?c.getInputStream():c.getErrorStream();
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buf=new byte[4096];int n;
            while((n=in.read(buf))!=-1)out.write(buf,0,n);
            if(status<200||status>=300)return status==429?"बॉस, AI की उपयोग सीमा पूरी है। थोड़ी देर बाद पूछें।":"बॉस, ऑनलाइन AI से कनेक्शन नहीं हुआ। API Key और इंटरनेट जाँचें।";
            JSONObject j=new JSONObject(new String(out.toByteArray(),"UTF-8"));
            JSONArray cand=j.optJSONArray("candidates");
            if(cand!=null&&cand.length()>0){JSONArray parts=cand.getJSONObject(0).getJSONObject("content").optJSONArray("parts");StringBuilder a=new StringBuilder();if(parts!=null)for(int k=0;k<parts.length();k++)a.append(parts.getJSONObject(k).optString("text",""));if(a.length()>0)return a.toString();}
            return "बॉस, जवाब नहीं मिला। फिर से पूछें।";
        }catch(Exception e){return "बॉस, इंटरनेट या AI कनेक्शन में समस्या है।";}
        finally{if(c!=null)c.disconnect();}
    }
    @Override public void onDestroy(){listening=false;handler.removeCallbacksAndMessages(null);if(recognizer!=null)recognizer.destroy();if(tts!=null)tts.shutdown();super.onDestroy();}
    @Override public android.os.IBinder onBind(Intent intent){return null;}
}