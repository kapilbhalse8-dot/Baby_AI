package com.babyai.assistant;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import java.util.*;
public class CallAnnouncementReceiver extends BroadcastReceiver {
 public void onReceive(Context c,Intent i) {
  if(!android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(i.getAction())) return;
  String state=i.getStringExtra(android.telephony.TelephonyManager.EXTRA_STATE);
  SharedPreferences p=c.getSharedPreferences("baby_ai_local",Context.MODE_PRIVATE);
  if(android.telephony.TelephonyManager.EXTRA_STATE_IDLE.equals(state)){p.edit().putBoolean("call_announced",false).apply();return;}
  if(!android.telephony.TelephonyManager.EXTRA_STATE_RINGING.equals(state)||p.getBoolean("call_announced",false))return;
  p.edit().putBoolean("call_announced",true).apply();
  String number=i.getStringExtra(android.telephony.TelephonyManager.EXTRA_INCOMING_NUMBER);
  String name=null; Cursor cur=null;
  try { if(number!=null){Uri u=Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,Uri.encode(number));cur=c.getContentResolver().query(u,new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},null,null,null);if(cur!=null&&cur.moveToFirst())name=cur.getString(0);} } catch(Exception ignored){} finally{if(cur!=null)cur.close();}
  final String msg=(name!=null&&!name.trim().isEmpty())?"बॉस, "+name+" का फोन आ रहा है।":(number!=null&&!number.trim().isEmpty()?"बॉस, किसी अनजान नंबर से कॉल आ रही है।":"बॉस, किसी नंबर से कॉल आ रही है।");
  speakFemale(c.getApplicationContext(),msg);
 }
 private void speakFemale(Context c,String msg){
  final TextToSpeech[] a=new TextToSpeech[1];
  a[0]=new TextToSpeech(c,s->{if(s!=TextToSpeech.SUCCESS){if(a[0]!=null)a[0].shutdown();return;}
   TextToSpeech t=a[0];t.setLanguage(new Locale("hi","IN"));boolean ok=false;
   if(Build.VERSION.SDK_INT>=21)try{Set<String> hints=new HashSet<>(Arrays.asList("female","woman","fem","स्त्री","महिला","-hia-"));for(android.speech.tts.Voice v:t.getVoices()){String n=v.getName().toLowerCase(Locale.ROOT);boolean hi=n.contains("hi-in")||n.contains("hindi")||n.contains("hin"),f=false;for(String h:hints)if(n.contains(h))f=true;if(hi&&f&&!v.isNetworkConnectionRequired()){t.setVoice(v);ok=true;break;}}}catch(Exception ignored){}
   if(ok){t.setSpeechRate(.95f);t.speak(msg,TextToSpeech.QUEUE_FLUSH,null,"babyai_call");}else t.shutdown();
  });
 }
}