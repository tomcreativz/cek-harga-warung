API Data Produk Indonesia
==========================
Integrasikan data produk Indonesia dengan mudah! Hanya menggunakan kata kunci dan barcode. Hemat ruang, praktis, dan gratis! 🚀 #APIProdukIndonesia

API Produk Indonesia adalah layanan gratis yang menyediakan data produk Indonesia dalam format JSON dengan total data 50.600. 

API ini memungkinkan pengembang untuk mengakses data produk tanpa harus menyimpannya secara lokal di server aplikasi itu sendiri. Dengan begitu, ruang di database aplikasi Anda tetap terjaga.

## ENDPOINTS

#### 1. Mencari Product berdasarkan barcode

```
GET https://api-products.alpha-projects.cloud/api/v1/products-barcode?barcode={barcode}&generateBarcode={boolean}
```

Contoh Response:

```
{
    "id": "ea3a0720-68e6-4313-a7e9-3844da694f1e",
    "barcode": "089686386219",
    "name": "BUMBU RACIK TUMIS",
    "price": 3500,
    "uom": "PCS",
    "imgBarcode": null
}
```

#### 2. Mencari Product berdasarkan nama

```
GET https://api-products.alpha-projects.cloud/api/v1/products?name={namaProduk}
```

Contoh Response:

```
[
    {
        "id": "112a323c-f226-4f81-a2fa-8dd2d099b92b",
        "barcode": "089686017755",
        "name": "SARIMI ISI2 KOYA JERUK",
        "price": 3500,
        "uom": "PCS",
        "imgBarcode": null
    },
    ...
]

```


#### Untuk price merupakan angka random
#### Tidak perlu Authentication

#### Perubahan terbaru

- `generateBarcode` kini opsional (default `false`), tidak lagi error bila tidak dikirim. Gambar hanya dibuat untuk barcode 12/13 digit; selain itu `imgBarcode` bernilai `null`.
- Barcode tidak ditemukan -> HTTP `404` dengan body `{status, error, message, timestamp}`. Input tidak valid -> `400`. Terlalu banyak request -> `429` (+ header `Retry-After`).
- `GET /products` mendukung paging opsional: `page` (default 0) dan `size` (default 50, maks 100). Respons tetap berupa array; total ada di header `X-Total-Count` dan `X-Total-Pages`. `name` minimal 2 karakter.
- Data dengan `deleted_at` terisi tidak lagi ikut muncul.
- Konfigurasi lewat environment variable (lihat bawah). `show-sql` default mati.

## Konfigurasi

| Variabel | Default | Keterangan |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/products` | JDBC URL PostgreSQL |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` / kosong | Kredensial DB |
| `CORS_ALLOWED_ORIGINS` | `*` | Pisahkan koma, mis. `https://USERNAME.github.io` |
| `RATE_LIMIT_ENABLED` | `true` | Batas request per IP |
| `RATE_LIMIT_MAX_REQUESTS` / `RATE_LIMIT_WINDOW_SECONDS` | `120` / `60` | Batas per jendela waktu |
| `SHOW_SQL` | `false` | Log SQL Hibernate |

Jalankan `src/main/resources/db/indexes.sql` sekali di PostgreSQL untuk mempercepat pencarian (indeks barcode dan trigram nama).

Menjalankan test: `./mvnw test` (memakai H2 in-memory).

## PWA (frontend)

Folder `pwa/` berisi aplikasi web yang bisa dipasang (installable) dan jalan offline sebagian: cari nama/barcode, scan barcode lewat kamera (browser yang mendukung `BarcodeDetector`, mis. Chrome Android), riwayat pencarian, dan URL API yang bisa diganti di menu Pengaturan.

Hosting di GitHub Pages:

1. Push repo ke GitHub (branch `main`).
2. Di **Settings -> Pages**, pilih **Source: GitHub Actions**.
3. Workflow `.github/workflows/pages.yml` akan men-deploy `pwa/` otomatis setiap push. Alamat: `https://USERNAME.github.io/NAMA-REPO/`.
4. Bila API di-host sendiri, set `CORS_ALLOWED_ORIGINS=https://USERNAME.github.io`, lalu isi URL API di Pengaturan aplikasi (atau ubah `DEFAULT_API` di `pwa/app.js`).

Setelah mengubah file di `pwa/`, naikkan `VERSION` di `pwa/sw.js` agar cache lama dibersihkan.

Backend Spring Boot **tidak** bisa berjalan di GitHub Pages (hanya hosting statis); deploy ke server/PaaS lain yang punya PostgreSQL.
