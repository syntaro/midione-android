# midione-android
Android BLE-Midi1.0 Example

I almost made it.
Bluetooth LE, Android 10 to 16, MiDI1.0.
Very stable and good response.

Thank you. Apache2.0 License. 
Feel free to use it. 
Have a nice day.with BLE Computing.




Android's Bluetooth LE is said to be unstable in many ways, but my code has become fairly stable.

Please use this as an example implementation for BLE-Midi 1.0.

I borrowed the logic from kshoji's OSS in github and worked hard to implement it in a way that was closer to the low-level hardware.

@kshoji さんのものに2年間のテストを重ね改善を加えたものです。
https://github.com/kshoji/BLE-MIDI-for-Android

1 MIDI1.0を理解してるプログラマー前提で、カプセル化はすこし遠慮しています。

2 バッファは16を超えて転送すると、古いMIDI機器はエラーになるので、16バイトで区切りました。

3 BLEの操作は専用のスレッドから行っています。そのため、レスポンスの待機がしやすくなっていて、安定しました。

4 javax sound midiパッケージのクローンをせず、独自のフレームワークにしました。MIDIOneです。
  1と被りますが、

5 MIDIOneはいろいろつくっていますが、比較的安定しているのは、BLEのみで、それだけ公開しています。

6 16バイトで区切っても、MTUを64未満もだめなようでした。これは憶測てす。でも、アンドロイド端末によりエラーが出てました。