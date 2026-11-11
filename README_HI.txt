BABY AI — Android starter project

इस पैकेज में अभी शामिल:
- Baby AI नाम की Android ऐप स्क्रीन
- हिंदी में लिखकर चैट और सीमित ऑफलाइन जवाब
- SpeechRecognizer से हिंदी बोलकर इनपुट (माइक अनुमति जरूरी)
- Android Text-to-Speech (हिंदी/महिला आवाज़ फोन में उपलब्ध engine/voice पर निर्भर)
- बातचीत की स्थानीय सेविंग (ऐप के निजी फोन स्टोरेज में)
- WhatsApp, YouTube, Chrome खोलने के बटन और कुछ वॉइस कमांड
- ऐप खुले रहने पर 20% बैटरी पर एक बार आवाज़ से चेतावनी

अभी शामिल नहीं / जरूरी सीमाएँ:
- यह सोर्स प्रोजेक्ट है, APK नहीं। इस रनटाइम में Android SDK/Gradle उपलब्ध न होने से APK build और device test नहीं किया गया।
- ChatGPT-जैसी ऑनलाइन AI अभी कनेक्ट नहीं है। उसके लिए ऑनलाइन मॉडल प्रदाता और सुरक्षित backend/API key आवश्यक हैं। API key ऐप में सीधे रखना सुरक्षित नहीं है।
- केवल फोन में सेव की गई चैट ही लोकल है; ऐप हटाने या app data clear करने पर यह मिट सकती है।
- 2 मिनट lock/unlock पर greeting और face recognition अभी लागू नहीं हैं। Android के background/camera privacy नियमों के कारण इन्हें अलग से implement/test करना होगा।
- महिला हिंदी TTS आवाज़ की गारंटी नहीं; Android speech engine में उपलब्ध आवाज़ पर निर्भर है।

Build:
1. Android Studio में यह पूरा folder खोलें।
2. Gradle sync करें।
3. फोन/एम्युलेटर चुनकर Run करें।
4. माइक्रोफोन अनुमति दें।

Target SDK 35 (Android 15), min SDK 26.
