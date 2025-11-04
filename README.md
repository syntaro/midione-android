# midione-android
Android BLE-Midi1.0 Example

I almost made it.
Bluetooth LE, Android 10 to 16, MiDI1.0.
Very stable and good response.

Thank you. Apache2.0 License. 
Feel free to use it. 
Have a nice day.with BLE Computing.

@kshoji さんのものに改善を加えたものです。
https://github.com/kshoji/BLE-MIDI-for-Android

1 MIDI1.0を理解してるプログラマー前提で、カプセル化はすこし遠慮しています。
2 バッファは16以上転送すると、古いMIDI機器はエラーになるので、16バイトで区切りました。
3 BLEの操作は専用のスレッドから行っています。そのため、レスポンスの待機がしやすくなっていて、安定しました。
4 javax sound midiパッケージのクローンをせず、独自のフレームワークにしました。MIDIOneです。
5 MIDIOneはいろいろつくっていますが、比較的安定しているのは、BLEのみで、それだけ公開しています。
6 16バイトで区切っても、MTUを64未満もだめなようでした。