package com.babyai.assistant;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.util.*;

public class CallAnnouncementReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent) {
        if(!android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) return;
        String state=intent.getStringExtra(android.telephony.TelephonyManager.EXTRA_STATE);
        android.content.SharedPreferences prefs=context.getSharedPreferences("baby_ai_local",Context.MODE_PRIVATE);
        if(android.telephony.TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            prefs.edit().putBoolean("call_announced",false).apply(); return;
        }
        if(!android.telephony.TelephonyManager.EXTRA_STATE_RINGING.equals(state)||prefs.getBoolean("call_announced",false)) return;
        prefs.edit().putBoolean("call_announced",true).apply();

        String number=intent.getStringExtra(android.telephony.TelephonyManager.EXTRA_INCOMING_NUMBER);
        String name=null; Cursor cursor=null;
        try {
            if(number!=null && context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS)==android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Uri lookup=Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,Uri.encode(number));
                cursor=context.getContentResolver().query(lookup,new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},null,null,null);
                if(cursor!=null&&cursor.moveToFirst()) name=cursor.getString(0);
            }
        } catch(Exception ignored) {} finally {if(cursor!=null)cursor.close();}

        String message=(name!=null&&!name.trim().isEmpty())
            ?"बॉस, "+name+" का फोन आ रहा है।"
            :(number!=null&&!number.trim().isEmpty()?"बॉस, किसी अनजान नंबर से कॉल आ रही है।":"बॉस, किसी नंबर से कॉल आ रही है।");
        announce(context.getApplicationContext(),message);
    }

    private void announce(Context context,String message) {
        final PendingResult pending=goAsync();
        final TextToSpeech[] engine=new TextToSpeech[1];
        final boolean[] finished={false};
        final Runnable finish=()->{
            if(!finished[0]) {
                finished[0]=true;
                if(engine[0]!=null) engine[0].shutdown();
                pending.finish();
            }
        };
        new Handler(Looper.getMainLooper()).postDelayed(finish,9000);
        engine[0]=new TextToSpeech(context,status->{
            if(status!=TextToSpeech.SUCCESS){finish.run();return;}
            TextToSpeech t=engine[0];
            t.setLanguage(new Locale("hi","IN"));
            boolean female=false;
            if(Build.VERSION.SDK_INT>=21) try {
                Set<String> hints=new HashSet<>(Arrays.asList("female","woman","fem","स्त्री","महिला","-hia-"));
                for(android.speech.tts.Voice voice:t.getVoices()) {
                    String n=voice.getName().toLowerCase(Locale.ROOT);
                    boolean hindi=n.contains("hi-in")||n.contains("hindi")||n.contains("hin");
                    boolean f=false;for(String h:hints)if(n.contains(h))f=true;
                    if(hindi&&f&&!voice.isNetworkConnectionRequired()){t.setVoice(voice);female=true;break;}
                }
            } catch(Exception ignored) {}
            if(!female){finish.run();return;}
            t.setSpeechRate(.95f);
            t.setOnUtteranceProgressListener(new UtteranceProgressListener(){
                @Override public void onStart(String id){}
                @Override public void onDone(String id){finish.run();}
                @Override public void onError(String id){finish.run();}
                @Override public void onStop(String id,boolean interrupted){finish.run();}
            });
            int result=t.speak(message,TextToSpeech.QUEUE_FLUSH,null,"babyai_call");
            if(result==TextToSpeech.ERROR)finish.run();
        });
    }
}
