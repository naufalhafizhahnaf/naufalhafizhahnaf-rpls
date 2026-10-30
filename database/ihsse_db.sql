-- ============================================================
-- I-HSSE Safety Induction - Database MySQL/MariaDB (XAMPP)
-- Database : ihsse_db
-- User     : root (tanpa password, default XAMPP)
-- ============================================================
-- Jalankan dari phpMyAdmin (tab Import) atau:
--   C:\xampp\mysql\bin\mysql.exe -u root < database\ihsse_db.sql
--
-- PERHATIAN: script ini membuat ulang tabel dan mengisi data awal.
-- Semua data lama di database ihsse_db akan dihapus.
-- ============================================================

CREATE DATABASE IF NOT EXISTS ihsse_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ihsse_db;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS sertifikat_materi;
DROP TABLE IF EXISTS sertifikat_induksi;
DROP TABLE IF EXISTS visitor_pass;
DROP TABLE IF EXISTS laporan_inspeksi;
DROP TABLE IF EXISTS laporan_insiden;
DROP TABLE IF EXISTS denah_fasilitas;
DROP TABLE IF EXISTS denah_gedung;
DROP TABLE IF EXISTS video_tutorial;
DROP TABLE IF EXISTS informasi_hsse;
DROP TABLE IF EXISTS pengunjung;
DROP TABLE IF EXISTS admin;
DROP TABLE IF EXISTS nomor_urut;
SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------
-- 1. Admin
-- ------------------------------------------------------------
CREATE TABLE admin (
    id              VARCHAR(20)  NOT NULL,
    nama            VARCHAR(100) NOT NULL,
    nomor_identitas VARCHAR(30)  NOT NULL,
    no_telepon      VARCHAR(20)  NOT NULL,
    username        VARCHAR(50)  NOT NULL,
    jabatan         VARCHAR(100) NOT NULL,
    password_hash   CHAR(64)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_username (username)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 2. Pengunjung
-- ------------------------------------------------------------
CREATE TABLE pengunjung (
    id               VARCHAR(20)  NOT NULL,
    nama             VARCHAR(100) NOT NULL,
    nomor_identitas  VARCHAR(30)  NOT NULL,
    no_telepon       VARCHAR(20)  NOT NULL,
    instansi         VARCHAR(100) NOT NULL,
    tujuan_kunjungan VARCHAR(255) NOT NULL,
    tanggal_daftar   DATE         NOT NULL,
    sudah_induksi    TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 3. Informasi HSSE (single table inheritance)
--    tipe: AreaMerokok, FasilitasKeamanan, FasilitasKeselamatan,
--          ZonaRisiko, FasilitasLingkungan
-- ------------------------------------------------------------
CREATE TABLE informasi_hsse (
    kode              VARCHAR(20)  NOT NULL,
    kategori          VARCHAR(20)  NOT NULL,
    tipe              VARCHAR(30)  NOT NULL,
    nama              VARCHAR(150) NOT NULL,
    lokasi            VARCHAR(150) NOT NULL,
    deskripsi         TEXT         NOT NULL,
    jenis             VARCHAR(60)  NULL,
    jumlah            INT          NULL,
    kondisi           VARCHAR(20)  NULL,
    masa_kedaluwarsa  DATE         NULL,
    luas_area         DOUBLE       NULL,
    aturan            TEXT         NULL,
    sanksi            TEXT         NULL,
    tingkat_risiko    VARCHAR(20)  NULL,
    prosedur_evakuasi TEXT         NULL,
    titik_kumpul      VARCHAR(150) NULL,
    jam_operasional   VARCHAR(60)  NULL,
    penanggung_jawab  VARCHAR(100) NULL,
    PRIMARY KEY (kode)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 4. Video tutorial
-- ------------------------------------------------------------
CREATE TABLE video_tutorial (
    id           VARCHAR(10)  NOT NULL,
    judul        VARCHAR(150) NOT NULL,
    topik        VARCHAR(50)  NOT NULL,
    durasi_detik INT          NOT NULL,
    url          VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 5. Denah gedung + fasilitas penting per lantai
-- ------------------------------------------------------------
CREATE TABLE denah_gedung (
    lantai     INT          NOT NULL,
    nama_file  VARCHAR(100) NOT NULL,
    deskripsi  VARCHAR(255) NOT NULL,
    PRIMARY KEY (lantai)
) ENGINE=InnoDB;

CREATE TABLE denah_fasilitas (
    id             INT AUTO_INCREMENT NOT NULL,
    lantai         INT          NOT NULL,
    urutan         INT          NOT NULL,
    nama_fasilitas VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_denah_urutan (lantai, urutan),
    CONSTRAINT fk_denah_fasilitas_denah
        FOREIGN KEY (lantai) REFERENCES denah_gedung (lantai)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 6. Visitor pass
-- ------------------------------------------------------------
CREATE TABLE visitor_pass (
    nomor_dokumen     VARCHAR(30)  NOT NULL,
    tanggal_terbit    DATE         NOT NULL,
    diterbitkan_oleh  VARCHAR(100) NOT NULL,
    pengunjung_id     VARCHAR(20)  NOT NULL,
    tanggal_kunjungan DATE         NOT NULL,
    jam_masuk         TIME         NOT NULL,
    masa_berlaku_hari INT          NOT NULL,
    area_tujuan       VARCHAR(150) NOT NULL,
    PRIMARY KEY (nomor_dokumen),
    CONSTRAINT fk_pass_pengunjung
        FOREIGN KEY (pengunjung_id) REFERENCES pengunjung (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 7. Sertifikat induksi + materi
-- ------------------------------------------------------------
CREATE TABLE sertifikat_induksi (
    nomor_dokumen    VARCHAR(30)  NOT NULL,
    tanggal_terbit   DATE         NOT NULL,
    diterbitkan_oleh VARCHAR(100) NOT NULL,
    pengunjung_id    VARCHAR(20)  NOT NULL,
    tanggal_selesai  DATE         NOT NULL,
    nama_fasilitator VARCHAR(100) NOT NULL,
    PRIMARY KEY (nomor_dokumen),
    UNIQUE KEY uk_sertifikat_pengunjung (pengunjung_id),
    CONSTRAINT fk_sertifikat_pengunjung
        FOREIGN KEY (pengunjung_id) REFERENCES pengunjung (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE sertifikat_materi (
    id               INT AUTO_INCREMENT NOT NULL,
    nomor_sertifikat VARCHAR(30)  NOT NULL,
    urutan           INT          NOT NULL,
    materi           VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_materi_urutan (nomor_sertifikat, urutan),
    CONSTRAINT fk_materi_sertifikat
        FOREIGN KEY (nomor_sertifikat) REFERENCES sertifikat_induksi (nomor_dokumen)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 8. Laporan inspeksi fasilitas
-- ------------------------------------------------------------
CREATE TABLE laporan_inspeksi (
    nomor_dokumen    VARCHAR(30)  NOT NULL,
    tanggal_terbit   DATE         NOT NULL,
    diterbitkan_oleh VARCHAR(100) NOT NULL,
    fasilitas_kode   VARCHAR(20)  NOT NULL,
    tanggal_cek      DATE         NOT NULL,
    kondisi          VARCHAR(20)  NOT NULL,
    masa_kedaluwarsa DATE         NULL,
    nama_petugas     VARCHAR(100) NOT NULL,
    tindakan         TEXT         NOT NULL,
    catatan          TEXT         NULL,
    PRIMARY KEY (nomor_dokumen),
    CONSTRAINT fk_inspeksi_fasilitas
        FOREIGN KEY (fasilitas_kode) REFERENCES informasi_hsse (kode)
        ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 9. Laporan insiden
-- ------------------------------------------------------------
CREATE TABLE laporan_insiden (
    nomor_dokumen    VARCHAR(30)  NOT NULL,
    tanggal_terbit   DATE         NOT NULL,
    diterbitkan_oleh VARCHAR(100) NOT NULL,
    nama_pelapor     VARCHAR(100) NOT NULL,
    kontak_pelapor   VARCHAR(30)  NOT NULL,
    lokasi           VARCHAR(150) NOT NULL,
    deskripsi        TEXT         NOT NULL,
    waktu_kejadian   DATETIME     NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    catatan_admin    TEXT         NULL,
    ditangani_oleh   VARCHAR(20)  NULL,
    PRIMARY KEY (nomor_dokumen),
    CONSTRAINT fk_insiden_admin
        FOREIGN KEY (ditangani_oleh) REFERENCES admin (id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 10. Sequence nomor dokumen
-- ------------------------------------------------------------
CREATE TABLE nomor_urut (
    nama  VARCHAR(30) NOT NULL,
    nilai INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (nama)
) ENGINE=InnoDB;

INSERT INTO nomor_urut (nama, nilai) VALUES
    ('pengunjung', 0),
    ('visitor_pass', 0),
    ('sertifikat', 0),
    ('inspeksi', 0),
    ('insiden', 0);

-- ============================================================
-- SEED DATA
-- ============================================================

-- Admin default: admin / admin123
INSERT INTO admin (id, nama, nomor_identitas, no_telepon, username, jabatan, password_hash) VALUES
    ('ADM-01', 'Rina Kartika', '197812102005012001', '081210101010', 'admin',
     'Koordinator K3 Kampus J6', SHA2('admin123', 256));

-- Informasi HSSE
INSERT INTO informasi_hsse
    (kode, kategori, tipe, nama, lokasi, deskripsi, jenis, jumlah, kondisi,
     masa_kedaluwarsa, luas_area, aturan, sanksi, tingkat_risiko,
     prosedur_evakuasi, titik_kumpul, jam_operasional, penanggung_jawab)
VALUES
    ('H-01', 'Health', 'AreaMerokok',
     'Kawasan Bebas Asap Rokok Lantai 1', 'Lantai 1 - Lobby Utama',
     'Seluruh area dalam gedung J6 dilarang merokok.',
     NULL, NULL, NULL, NULL, 150.0,
     'Dilarang merokok, termasuk rokok elektrik, di dalam gedung.',
     'Teguran lisan dan pencatatan oleh petugas keamanan.',
     NULL, NULL, NULL, NULL, NULL),

    ('S-01', 'Security', 'FasilitasKeamanan',
     'Parkiran Motor', 'Area parkir belakang gedung',
     'Parkir motor dengan pengawasan petugas dan CCTV.',
     'Parkiran Motor', 250, 'BAIK', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

    ('S-02', 'Security', 'FasilitasKeamanan',
     'CCTV Area Publik', 'Lobby, koridor, lift, dan parkiran',
     'Pemantauan area publik selama jam operasional kampus.',
     'CCTV', 32, 'BAIK', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

    ('S-03', 'Security', 'FasilitasKeamanan',
     'Smart Lock Door', 'Pintu laboratorium dan ruang server',
     'Akses pintu menggunakan kartu identitas terdaftar.',
     'Smart Lock Door', 8, 'BAIK', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

    ('S-04', 'Security', 'FasilitasKeamanan',
     'Pos Satpam', 'Lobby utama dan gerbang belakang',
     'Pos keamanan dengan petugas jaga 24 jam.',
     'Pos Satpam', 2, 'BAIK', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

    ('SAF-01', 'Safety', 'FasilitasKeselamatan',
     'APAR Lantai 1', 'Lantai 1 - dekat lift',
     'APAR powder ABC untuk kebakaran awal.',
     'APAR', 4, 'BAIK', DATE_ADD(CURDATE(), INTERVAL 6 MONTH),
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

    ('SAF-02', 'Safety', 'FasilitasKeselamatan',
     'Hydrant Lantai 1', 'Lantai 1 - dekat tangga darurat',
     'Hydrant indoor beserta selang dan nozzle.',
     'Hydrant', 2, 'BAIK', DATE_ADD(CURDATE(), INTERVAL 1 YEAR),
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

    ('SAF-03', 'Safety', 'FasilitasKeselamatan',
     'Tangga Darurat', 'Setiap sisi gedung lantai 1-11',
     'Jalur evakuasi vertikal dengan pintu tahan api.',
     'Tangga Darurat', 4, 'BAIK', NULL,
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

    ('SAF-04', 'Safety', 'FasilitasKeselamatan',
     'Titik Kumpul', 'Halaman depan gedung J6',
     'Area aman untuk berkumpul setelah evakuasi.',
     'Titik Kumpul', 2, 'BAIK', NULL,
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

    ('ZR-01', 'Safety', 'ZonaRisiko',
     'Laboratorium Listrik Lantai 6', 'Lantai 6 - sayap kanan',
     'Zona dengan risiko sengatan listrik dan kebakaran.',
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'TINGGI',
     'Matikan sumber listrik, gunakan APAR CO2, evakuasi lewat tangga darurat.',
     'Halaman depan gedung J6', NULL, NULL),

    ('E-01', 'Environment', 'FasilitasLingkungan',
     'Pom Bensin Kampus', 'Area belakang gedung J6',
     'Pom bensin internal untuk kendaraan operasional kampus.',
     'Pom Bensin', NULL, NULL, NULL, NULL, NULL, NULL, NULL,
     NULL, NULL, '07.00-19.00', 'Bagian Umum'),

    ('E-02', 'Environment', 'FasilitasLingkungan',
     'Toko Drum & Plastik', 'Area belakang gedung J6',
     'Penyediaan drum dan plastik untuk kebutuhan sarana kampus.',
     'Toko Drum & Plastik', NULL, NULL, NULL, NULL, NULL, NULL, NULL,
     NULL, NULL, '08.00-17.00', 'Bagian Sarana Prasarana');

-- Video tutorial
INSERT INTO video_tutorial (id, judul, topik, durasi_detik, url) VALUES
    ('V-01', 'Cara Menggunakan APAR', 'Safety', 480,
     'https://video.kampus.ac.id/apar'),
    ('V-02', 'Simulasi Evakuasi Kebakaran', 'Safety', 600,
     'https://video.kampus.ac.id/evakuasi');

-- Denah lantai 1-11
INSERT INTO denah_gedung (lantai, nama_file, deskripsi) VALUES
    (1,  'denah-lantai-1.png',  'Denah evakuasi dan fasilitas keselamatan lantai 1'),
    (2,  'denah-lantai-2.png',  'Denah evakuasi dan fasilitas keselamatan lantai 2'),
    (3,  'denah-lantai-3.png',  'Denah evakuasi dan fasilitas keselamatan lantai 3'),
    (4,  'denah-lantai-4.png',  'Denah evakuasi dan fasilitas keselamatan lantai 4'),
    (5,  'denah-lantai-5.png',  'Denah evakuasi dan fasilitas keselamatan lantai 5'),
    (6,  'denah-lantai-6.png',  'Denah evakuasi dan fasilitas keselamatan lantai 6'),
    (7,  'denah-lantai-7.png',  'Denah evakuasi dan fasilitas keselamatan lantai 7'),
    (8,  'denah-lantai-8.png',  'Denah evakuasi dan fasilitas keselamatan lantai 8'),
    (9,  'denah-lantai-9.png',  'Denah evakuasi dan fasilitas keselamatan lantai 9'),
    (10, 'denah-lantai-10.png', 'Denah evakuasi dan fasilitas keselamatan lantai 10'),
    (11, 'denah-lantai-11.png', 'Denah evakuasi dan fasilitas keselamatan lantai 11');

INSERT INTO denah_fasilitas (lantai, urutan, nama_fasilitas)
SELECT d.lantai, f.urutan, f.nama_fasilitas
FROM denah_gedung d
CROSS JOIN (
    SELECT 1 AS urutan, 'Tangga darurat' AS nama_fasilitas
    UNION ALL SELECT 2, 'APAR'
    UNION ALL SELECT 3, 'Titik kumpul terdekat'
) f;

-- Contoh pengunjung
INSERT INTO pengunjung
    (id, nama, nomor_identitas, no_telepon, instansi, tujuan_kunjungan, tanggal_daftar, sudah_induksi)
VALUES
    ('PGN-001', 'Dimas Aryo Pratama', '2021031001', '081234567890',
     'Universitas Gunadarma', 'Mengikuti seminar keamanan kampus', CURDATE(), 1),
    ('PGN-002', 'Budi Santoso', '2021031002', '081234567891',
     'Universitas Gunadarma', 'Mengikuti praktikum laboratorium', CURDATE(), 0),
    ('PGN-003', 'Siti Rahma', '2021031003', '081298765432',
     'PT Maju Jaya', 'Menghadiri rapat koordinasi', CURDATE(), 0);

-- Contoh visitor pass
INSERT INTO visitor_pass
    (nomor_dokumen, tanggal_terbit, diterbitkan_oleh, pengunjung_id,
     tanggal_kunjungan, jam_masuk, masa_berlaku_hari, area_tujuan)
VALUES
    (CONCAT('VP-', DATE_FORMAT(CURDATE(), '%Y%m%d'), '-001'),
     CURDATE(), 'Rina Kartika', 'PGN-001',
     CURDATE(), '09:15:00', 1, 'Lantai 6 - Ruang Seminar');

-- Contoh sertifikat induksi
INSERT INTO sertifikat_induksi
    (nomor_dokumen, tanggal_terbit, diterbitkan_oleh, pengunjung_id,
     tanggal_selesai, nama_fasilitator)
VALUES
    (CONCAT('SERT-', DATE_FORMAT(CURDATE(), '%Y%m%d'), '-001'),
     CURDATE(), 'Rina Kartika', 'PGN-001',
     CURDATE(), 'Rina Kartika, S.K.M.');

INSERT INTO sertifikat_materi (nomor_sertifikat, urutan, materi)
SELECT CONCAT('SERT-', DATE_FORMAT(CURDATE(), '%Y%m%d'), '-001'), urutan, materi
FROM (
    SELECT 1 AS urutan, 'Penggunaan APAR (Alat Pemadam Api Ringan)' AS materi
    UNION ALL SELECT 2, 'Prosedur Evakuasi Kebakaran'
    UNION ALL SELECT 3, 'Mitigasi dan Evakuasi Gempa Bumi'
    UNION ALL SELECT 4, 'Tindakan Saat Terjebak di Lift'
) m;

-- Contoh laporan inspeksi
INSERT INTO laporan_inspeksi
    (nomor_dokumen, tanggal_terbit, diterbitkan_oleh, fasilitas_kode,
     tanggal_cek, kondisi, masa_kedaluwarsa, nama_petugas, tindakan, catatan)
VALUES
    (CONCAT('INS-', DATE_FORMAT(CURDATE(), '%Y%m%d'), '-001'),
     CURDATE(), 'Rina Kartika', 'SAF-01',
     CURDATE(), 'BAIK', DATE_ADD(CURDATE(), INTERVAL 6 MONTH),
     'Rina Kartika',
     'Membersihkan tabung dan memeriksa pressure gauge',
     'Tekanan berada pada zona hijau, segel masih utuh.');

-- Contoh laporan insiden
INSERT INTO laporan_insiden
    (nomor_dokumen, tanggal_terbit, diterbitkan_oleh, nama_pelapor, kontak_pelapor,
     lokasi, deskripsi, waktu_kejadian, status, catatan_admin, ditangani_oleh)
VALUES
    (CONCAT('INC-', DATE_FORMAT(CURDATE(), '%Y%m%d'), '-001'),
     CURDATE(), 'Sistem I-HSSE (pelaporan publik)', 'Siti Rahma', '081298765432',
     'Lantai 3 - Tangga Darurat',
     'Ada ceceran air di anak tangga sehingga berpotensi menyebabkan terpeleset.',
     DATE_SUB(NOW(), INTERVAL 2 HOUR), 'BARU', '-', NULL);

-- Sequence setelah seed
UPDATE nomor_urut SET nilai = 3 WHERE nama = 'pengunjung';
UPDATE nomor_urut SET nilai = 1 WHERE nama = 'visitor_pass';
UPDATE nomor_urut SET nilai = 1 WHERE nama = 'sertifikat';
UPDATE nomor_urut SET nilai = 1 WHERE nama = 'inspeksi';
UPDATE nomor_urut SET nilai = 1 WHERE nama = 'insiden';

-- ============================================================
-- Selesai.
-- Login GUI: admin / admin123
-- ============================================================
