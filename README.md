# SpeedTrail

Aplikasi Android untuk merekam perjalanan kendaraan bermotor — mirip Strava, tapi
fokus pada **kecepatan**: setiap perjalanan diputar ulang di peta dengan **jalur
berwarna sesuai kecepatan** dan analisis **di mana terjadi perlambatan**.

## Fitur
- Perekaman GPS real-time (kecepatan, posisi, jarak, durasi) lewat foreground service — tetap jalan saat layar mati.
- Riwayat semua perjalanan tersimpan lokal (Room/SQLite).
- Detail perjalanan: peta OpenStreetMap dengan polyline berwarna per-segmen.
- Grafik kecepatan yang warnanya selaras dengan peta.
- Deteksi otomatis **zona perlambatan** (kecepatan di bawah ambang relatif terhadap rata-rata).
- Dua mode pewarnaan: **Relatif** (kontekstual per-trip) & **Absolut** (ambang km/jam tetap).
- **Timeline slider** di tampilan detail — geser untuk memutar ulang perjalanan titik per titik: marker lingkaran muncul di peta dan panel status menampilkan kecepatan (berwarna), waktu berlalu, serta posisi titik.

## Teknologi
- Kotlin + Jetpack Compose (Material 3)
- osmdroid (OpenStreetMap) — tanpa API key
- FusedLocationProvider (Google Play Services Location)
- Room, Coroutines/Flow, Navigation Compose
- Arsitektur MVVM

## Struktur
```
app/src/main/java/com/zaid/speedtrail/
├─ data/          # Room: model, DAO, database, repository
├─ service/       # LocationTrackingService + TrackingState (live)
├─ util/          # SpeedColorMapper, SlowdownDetector, Formatters
├─ ui/
│  ├─ live/       # Layar perekaman + speedometer
│  ├─ history/    # Daftar perjalanan
│  ├─ detail/     # Peta berwarna + grafik + zona perlambatan
│  └─ components/ # RouteMapView (osmdroid), SpeedChart (Canvas)
└─ MainActivity.kt
```

## Cara menjalankan
1. Buka folder ini di **Android Studio** (Ladybug atau lebih baru).
2. Biarkan Gradle sync (Android Studio akan membuat Gradle wrapper otomatis bila belum ada).
3. Hubungkan HP Android (Developer Mode + USB Debugging) — disarankan device asli karena emulator GPS terbatas.
4. Run ▶. Saat pertama kali, izinkan **Lokasi** (pilih "Saat menggunakan aplikasi" lalu "Izinkan sepanjang waktu" bila ingin tracking background) dan **Notifikasi**.

> Catatan: kecepatan diambil dari `Location.getSpeed()` (Doppler GPS), umumnya lebih akurat daripada menghitung dari jarak/waktu.

## Rencana lanjutan (belum dikerjakan)
- Ekspor GPX/GeoJSON.
- Pengaturan ambang warna & perlambatan oleh pengguna.
- Penanda kecepatan maksimum & screenshot share.
- Penghalusan (smoothing) kecepatan & filter titik berakurasi buruk.
