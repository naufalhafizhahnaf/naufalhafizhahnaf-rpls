# I-HSSE Safety Induction Universitas Gunadarma J6

Aplikasi desktop **Java Swing** untuk pengelolaan HSSE (Health, Security, Safety,
Environment) kampus J6. Data disimpan di **MySQL/MariaDB XAMPP** melalui JDBC.

Versi ini menggantikan demo console lama: seluruh proses bisnis dijalankan dari
GUI dan data tidak lagi disimpan di memori.

---

## Fitur

1. **Login admin** dari tabel `admin` (password di-hash SHA-256).
2. **Dashboard** berisi ringkasan pengunjung, visitor pass, sertifikat, inspeksi,
   dan status insiden.
3. **Informasi HSSE** — CRUD untuk:
   - Area Merokok (Health)
   - Fasilitas Keamanan (Security)
   - Fasilitas Keselamatan (Safety)
   - Zona Risiko (Safety)
   - Fasilitas Lingkungan (Environment)
   - Daftar video tutorial dan denah gedung lantai 1–11.
4. **Pengunjung & Visitor Pass** — registrasi pengunjung dan penerbitan pass
   yang bisa dipratinjau/dicetak.
5. **Induksi & Sertifikat** — menyelesaikan safety induction, menyimpan materi
   wajib, dan mencetak sertifikat.
6. **Inspeksi Fasilitas** — pencatatan pemeriksaan APAR/hydrant, update kondisi
   master fasilitas secara otomatis, dan cetak laporan.
7. **Laporan Insiden** — form pelaporan insiden, proses status `BARU -> DIPROSES ->
   SELESAI`, catatan admin, dan cetak laporan.

---

## Kebutuhan

- **JDK 11 / 17 / 21** (project dikonfigurasi `javac.source=11`)
- **NetBeans 17** (project Java with Ant)
- **XAMPP** dengan MySQL/MariaDB berjalan di port **3306**
- Driver **mysql-connector-j-8.3.0.jar** (sudah disertakan di folder `lib/`)

---

## Cara Menjalankan

### 1. Jalankan MySQL XAMPP

Buka **XAMPP Control Panel**, klik **Start** pada MySQL.

### 2. Buat database

Dari folder project, jalankan:

```bat
setup_database.bat
```

atau import manual `database/ihsse_db.sql` melalui **phpMyAdmin**
(`http://localhost/phpmyadmin`).

Script tersebut membuat database `ihsse_db`, seluruh tabel, dan data awal.

### 3. Buka di NetBeans

1. Buka **NetBeans 17**.
2. Pilih **File > Open Project**.
3. Arahkan ke folder `C:\Users\Admin\I-HSSE-SafetyInduction`.
4. Klik **Open Project**.
5. Tekan **F6** atau klik kanan project > **Run**.

Main class sudah diatur ke `ihsse.gui.GuiMain` di
`nbproject/project.properties`.

### 4. Login

| Username | Password |
|----------|----------|
| `admin`  | `admin123` |

Password disimpan sebagai hash SHA-256 di tabel `admin`.

### Menjalankan dari command line

```bat
build.bat
```

Atau manual:

```bat
dir /s /b src\*.java > sources.txt
javac --release 11 -encoding UTF-8 -cp lib\mysql-connector-j-8.3.0.jar -d build\classes @sources.txt
java -cp "build\classes;lib\mysql-connector-j-8.3.0.jar" ihsse.gui.GuiMain
```

---

## Konfigurasi Database

Konfigurasi ada di `db.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/ihsse_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jakarta&characterEncoding=UTF-8
db.user=root
db.password=
```

Jika MySQL memakai password, isi `db.password`. Nilai ini juga bisa
ditimpa lewat parameter JVM:

```bat
java -Dihsse.db.user=root -Dihsse.db.password=rahasia ...
```

---

## Struktur Project

```text
I-HSSE-SafetyInduction/
├── database/
│   └── ihsse_db.sql              # schema + seed data
├── lib/
│   └── mysql-connector-j-8.3.0.jar
├── src/ihsse/
│   ├── dao/                      # akses database (JDBC)
│   │   ├── AdminDao.java
│   │   ├── DenahDao.java
│   │   ├── InformasiDao.java
│   │   ├── InsidenDao.java
│   │   ├── InspeksiDao.java
│   │   ├── NomorDao.java
│   │   ├── PengunjungDao.java
│   │   ├── SertifikatDao.java
│   │   ├── StatistikDao.java
│   │   ├── VideoDao.java
│   │   └── VisitorPassDao.java
│   ├── db/
│   │   ├── Database.java         # koneksi + transaksi
│   │   └── DataAccessException.java
│   ├── gui/                      # Swing GUI
│   │   ├── GuiMain.java          # entry point
│   │   ├── LoginFrame.java
│   │   ├── MainFrame.java
│   │   ├── DashboardPanel.java
│   │   ├── InformasiPanel.java
│   │   ├── InformasiDialog.java
│   │   ├── PengunjungPanel.java
│   │   ├── VisitorPassDialog.java
│   │   ├── InduksiPanel.java
│   │   ├── InspeksiPanel.java
│   │   ├── InsidenPanel.java
│   │   ├── DokumenPreviewDialog.java
│   │   ├── FormatUtil.java
│   │   ├── Refreshable.java
│   │   └── Theme.java
│   ├── main/
│   │   └── Main.java             # launcher kompatibilitas ke GuiMain
│   ├── model/                    # model OOP (abstract, inheritance, enum)
│   ├── service/                  # logika bisnis + transaksi
│   │   ├── InduksiService.java
│   │   ├── InformasiService.java
│   │   ├── InsidenService.java
│   │   ├── InspeksiService.java
│   │   ├── NomorService.java
│   │   ├── PengunjungService.java
│   │   └── StatistikService.java
│   ├── tools/
│   │   ├── SmokeTest.java        # uji DAO + alur bisnis
│   │   └── GuiSmokeTest.java     # uji inisialisasi seluruh panel
│   └── util/
│       ├── PasswordUtil.java
│       ├── Penomoran.java
│       └── Validasi.java
├── build.bat
├── setup_database.bat
├── db.properties
└── README.md
```

---

## Tabel Database

| Tabel | Keterangan |
|-------|------------|
| `admin` | data admin + hash password |
| `pengunjung` | data pengunjung |
| `informasi_hsse` | semua konten HSSE (single table inheritance) |
| `video_tutorial` | video tutorial |
| `denah_gedung`, `denah_fasilitas` | denah lantai 1–11 + fasilitas penting |
| `visitor_pass` | dokumen visitor pass |
| `sertifikat_induksi`, `sertifikat_materi` | sertifikat + materi induksi |
| `laporan_inspeksi` | riwayat inspeksi fasilitas |
| `laporan_insiden` | laporan insiden + status |
| `nomor_urut` | sequence nomor dokumen |

---

## Aturan Bisnis

- Satu pengunjung hanya boleh memiliki **satu sertifikat induksi**
  (unique constraint + validasi service).
- Status insiden hanya boleh bergerak `BARU -> DIPROSES -> SELESAI`.
- Inspeksi memperbarui kondisi dan masa kedaluwarsa fasilitas dalam satu
  transaksi dengan penyimpanan laporan.
- Nomor dokumen (`PGN`, `VP`, `SERT`, `INS`, `INC`) memakai sequence database
  agar tetap unik setelah aplikasi dibuka ulang.
- Fasilitas yang sudah dipakai pada laporan inspeksi tidak bisa dihapus.

---

## Uji Cepat

Setelah database dibuat:

```bat
java -cp "build\classes;lib\mysql-connector-j-8.3.0.jar" ihsse.tools.SmokeTest
java -cp "build\classes;lib\mysql-connector-j-8.3.0.jar" ihsse.tools.GuiSmokeTest
```

`SmokeTest` menguji login, CRUD informasi, registrasi, visitor pass, sertifikat,
inspeksi, dan alur status insiden. Data uji dibersihkan otomatis.

---

## Troubleshooting

| Gejala | Solusi |
|--------|--------|
| `Communications link failure` | MySQL XAMPP belum Start atau port bukan 3306. |
| `Unknown database 'ihsse_db'` | Jalankan `setup_database.bat`. |
| `Access denied for user 'root'` | Isi `db.password` di `db.properties`. |
| `No suitable driver` / `ClassNotFoundException` | Pastikan `lib/mysql-connector-j-8.3.0.jar` ada dan masuk classpath NetBeans. |
| Tampilan GUI tidak muncul | Jalankan dari NetBeans (F6) atau `build.bat`, bukan `java build`. |
| Login gagal padahal password benar | Cek tabel `admin`; seed default memakai `admin` / `admin123`. |

---

## Catatan

- `CODE_LENGKAP.md` berisi kompilasi source terbaru.
- `demo-output.txt` adalah output demo console lama dan tidak dipakai lagi.
- Aplikasi ini dibuat untuk tugas Praktikum Rekayasa Perangkat Lunak 2.
