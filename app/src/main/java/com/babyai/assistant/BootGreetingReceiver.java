package com.babyai.assistant;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import java.util.*;
public class BootGreetingReceiver extends BroadcastReceiver {
 public void onReceive(Context context,Intent intent) {
  if(!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
  final PendingResult pending=goAsync();
  final TextToSpeech[] t={null};
  t[0]=new TextToSpeech(context.getApplicationContext(),status->{
   if(status==TextToSpeech.SUCCESS) {
    t[0].setLanguage(new Locale("hi","IN")); boolean female=false;
    if(Build.VERSION.SDK_INT>=21) try {
     Set<String> hints=new HashSet<>(Arrays.asList("female","woman","fem","स्त्री","महिला","-hia-"));
     for(android.speech.tts.Voice v:t[0].getVoices()) { String n=v.getName().toLowerCase(Locale.ROOT); boolean hi=n.contains("hi-in")||n.contains("hindi")||n.contains("hin"), f=false; for(String h:hints)if(n.contains(h))f=true; if(hi&&f&&!v.isNetworkConnectionRequired()){t[0].setVoice(v);female=true;break;} }
    } catch(Exception ignored) {}
    if(female) t[0].speak("आप आ गए बॉस! आपका स्वागत है।",TextToSpeech.QUEUE_FLUSH,null,"babyai-boot");
   }
   new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(()->{if(t[0]!=null)t[0].shutdown();pending.finish();},5000);
  });
 }
}