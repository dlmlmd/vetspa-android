# Agent.md — VetSpa Android Native

## Nhật ký công việc đã làm

### ✅ Bước 0 — Khởi tạo project (2026-07-23)

**Mục tiêu:** Dự án Android Native App riêng cho VetSpa, tách biệt hoàn toàn khỏi PHP backend, có Gradle + Kotlin + FCM + WorkManager.

**Kiến trúc:**
- Gradle Kotlin DSL
- API giao tiếp qua Retrofit + OkHttp (có CookieJar lưu SharedPreferences)
- FCM push từ Firebase Cloud Messaging
- WorkManager poller (kiểm tra thông báo mới mỗi 5 phút)
- WebView vẫn là thành phần giao diện chính (MainActivity dùng WebView)

**Files tạo:**
| File | Chức năng |
|------|-----------|
| `build.gradle.kts` | Project-level (Kotlin 1.9.0, google-services 4.4.0) |
| `settings.gradle.kts` | Config repositories + rootProject.name |
| `gradle.properties` | AndroidX + heap 2048m |
| `.gitignore` | Chuẩn Android + Gradle |
| `app/build.gradle.kts` | Dependencies (Kotlin, Retrofit, OkHttp, Glide, Firebase, WorkManager, Gson) |
| `app/proguard-rules.pro` | ProGuard giữ model class |
| `app/google-services.json` | Firebase project `vetspa-e5074`, package `com.vetspa.nativeapp` |
| `app/src/main/AndroidManifest.xml` | Quyền INTERNET, POST_NOTIFICATIONS (API 33+), FcmService |
| `app/src/main/java/.../VetSpaApp.kt` | Application class: init MyCookieJar, WorkManager poller |
| `app/src/main/java/.../data/api/VetSpaApi.kt` | Retrofit interface (base_url, login, me, bookings, staff, packages, fcm, notifications) |
| `app/src/main/java/.../data/api/MyCookieJar.kt` | CookieJar lưu SharedPreferences (đồng bộ cookie với WebView) |
| `app/src/main/java/.../data/model/ApiResponse.kt` | Data models (LoginResponse, UserResponse, BookingResponse, NotificationResponse, FcmTokenRequest) |
| `app/src/main/java/.../service/FcmService.kt` | FCM: onNewToken (register), onMessageReceived (show notification) |
| `app/src/main/java/.../ui/MainActivity.kt` | WebView activity (placeholder) |
| `app/src/main/java/.../util/NotifHelper.kt` | Notification channel + builder |
| `app/src/main/res/layout/activity_main.xml` | WebView layout |
| `app/src/main/res/values/{colors,strings,themes}.xml` | Theme brand (xanh #0074e0) |
| `app/src/main/res/drawable/ic_launcher_{foreground,background}.xml` | Adaptive icon vector |
| `app/src/main/res/xml/network_security_config.xml` | Cho phép HTTP cho API domain |
| `.github/workflows/build-apk.yml` | GitHub Actions build APK (debug) + artifact |
| `HUONG_DAN.md` | Hướng dẫn chi tiết từ tạo repo → build |
| `Agent.md` | File này |

### ✅ Bước 1 — Google Services + CI build APK (2026-07-23)

- Tạo placeholder google-services.json (cấu trúc đúng, package `com.vetspa.nativeapp`)
- CI đọc từ secret `GOOGLESERVICES_JSON` thay thế placeholder

### ✅ Bước 2 — Push GitHub + Fix build (2026-07-23)

- Push code lên `dlmlmd/vetspa-android`
- Fix: `paths` filter không trigger workflow → bỏ filter
- Fix: thiếu Gradle wrapper → tải `gradlew`, `gradlew.bat`, `gradle-wrapper.jar`
- Fix: `./gradlew: Permission denied` → thêm `chmod +x gradlew`

### ✅ Bước 3 — Fix lỗi compile Kotlin (2026-07-23)

- `MyCookieJar.kt`: `url.host()` → `url.host` (property)
- `VetSpaApi.kt`: xoá `@Body` thừa, thêm `import com.google.gson.annotations.SerializedName`
- `MainActivity.kt`: `settings` → `this.userAgentString` (apply context)

### ✅ Bước 4 — WebView hoàn chỉnh + icon (2026-07-23)

- `activity_main.xml`: TextView → WebView full màn
- `MainActivity.kt`: WebView setup (JS, cookie, bridge, FCM injection)
- `app/build.gradle.kts`: thêm `BuildConfig.WEB_APP_URL`
- Icon lá xanh (`#10b981`) background kem

### 🟢 Kết quả thực tế (2026-07-23)
- APK build thành công qua GitHub Actions
- Cài lên máy thật, WebView load được `index2.php` từ backend
- Ứng dụng hoạt động

### ✅ Bước 5 — Native Login + Bottom Navigation (2026-07-23)

- **LoginActivity**: màn hình native đăng nhập (Material OutlinedTextInput + button xanh)
- Retrofit gọi `auth_api.php?action=login` (FormUrlEncoded)
- Lưu user vào SharedPreferences (id, fullname, role, email...)
- **BottomNavigationView**: 3 tab (Trang chủ, Lịch của tôi, Tài khoản)
- **HomeFragment**: WebView load `index2.php?from=android`
- **BookingsFragment**: WebView load `bookings.php`
- **ProfileFragment**: native UI hiển thị thông tin user + nút Đăng xuất
- Đồng bộ cookie Retrofit → WebView qua `MyCookieJar.getSessionCookies()` → `CookieManager`
- LoginActivity là LAUNCHER, MainActivity kiểm tra login ở onCreate

### ⚠️ Vấn đề Cloudflare (2026-07-23)

- Host `vetspa.free.je` có Cloudflare proxy (WAF) chặn bot
- Retrofit/OkHttp gọi API thẳng bị 400 + JS challenge
- Không thể dùng native Retrofit login khi có Cloudflare
- **Giải pháp tạm thời:** LoginActivity dùng WebView load `index2.php`
- WebView tự xử lý Cloudflare challenge → đăng nhập web → bắt cookie → qua MainActivity
- **Giải pháp lâu dài:** Mua VPS riêng, tắt Cloudflare proxy để Retrofit hoạt động

### 🟢 Kết quả thực tế (2026-07-23)
- APK build thành công qua GitHub Actions
- App chạy trên máy thật
- WebView bypass Cloudflare OK
- Retrofit tạm thời không dùng được (đợi VPS)

### 🟢 Kết quả thực tế (2026-07-24) — VPS + Native Login
- Đã mua VPS `spa.vetmedia.vn` — hết Cloudflare
- Retrofit login hoạt động native, không cần WebView workaround
- Cookie tự động lưu qua MyCookieJar, đồng bộ WebView
- Login form Material Design đẹp, responsive
- FCM token register hoạt động

---

## Kế hoạch sắp tới

### ✅ Bước 6 — Mua VPS + chuyển hosting (2026-07-24)

- Mua VPS tại spa.vetmedia.vn (hết Cloudflare)
- Cập nhật `API_BASE_URL` → `https://spa.vetmedia.vn/api/`
- Cập nhật `WEB_APP_URL` → `https://spa.vetmedia.vn/index2.php`
- Cập nhật `network_security_config.xml` domain mới
- Cập nhật `FcmService.kt` domain check

### ✅ Bước 7 — Native Login thật (Retrofit) (2026-07-24)

- `activity_login.xml` — form Material Design OutlinedTextInput
- `LoginActivity.kt` — Retrofit login + auto-save user/cookie
- `VetSpaApi.kt` — login trả `Response<LoginResponse>` thay `ResponseBody`
- `MyCookieJar.init()` trong `VetSpaApp.onCreate()`
- Cookie đồng bộ Retrofit → WebView qua `syncCookiesToWebView()`
- Staff login: mở browser ngoài (`staff_login.php`)
- Lưu SharedPreferences: id, username, fullname, role, email, phone, profile_code

### ✅ Bước 8 — Tách API Android riêng (android_* prefix) (2026-07-24)

**Vấn đề:** Android app gọi thẳng API cũ (booking_api.php, staff_api.php...) gây rủi ro ảnh hưởng web workflows.

**Giải pháp:** Tạo API file riêng với tiền tố `android_`, không sửa file gốc.

**Files PHP backend mới (tại `D:\WinNMP\WWW\vetspa\api\`):**

| File | Actions | Mô tả |
|------|---------|-------|
| `android_auth_api.php` | login, me, logout, register_fcm | Xác thực + FCM token |
| `android_staff_api.php` | available, list | NV rảnh + danh sách |
| `android_packages_api.php` | list, my | Gói dịch vụ + gói đã mua |
| `android_booking_api.php` | my_bookings, available_beds, create, cancel, detail | Booking native |

**Tính năng mới cho Android:**

- **`action=my_bookings`**: nhóm theo ngày (DateGroup), trả `{date, items: [...]}`
- **`action=available_beds`**: trả list giường trống cho [start, end] → client chặn trùng lớp 1
- **`action=create`**: gọn nhẹ, không voucher/walk-in (giữ web ổn định)
- **`action=detail`**: chi tiết booking + thông tin nhân viên

**Cơ chế chống trùng booking (2 lớp):**
1. **Client:** `available_beds` + `available_staff` → dropdown chỉ hiện options rảnh
2. **Server:** conflict query `(user_id OR bed_id OR staff_id)` → 409 nếu trùng

**Files Android sửa:**

| File | Thay đổi |
|------|----------|
| `VetSpaApi.kt` | Gọi `android_*` endpoints + model mới (DateGroup, AvailableBedsResponse, ...) |
| `LoginActivity.kt` | Dùng `@Body LoginRequest` thay `@Field` (JSON body) |
| `FcmService.kt` | Gọi `android_auth_api?action=register_fcm` |
| `ApiResponse.kt` | Thêm `UserPackage`, mở rộng `Booking` (packageId, packagePrice, staff*) |

**Kiến trúc API mới:**
```
Android App → ApiClient.api.*()
  → OkHttp + MyCookieJar
    → spa.vetmedia.vn/api/android_*.php
      → Session từ cookie (giống web)
```

**Hoàn toàn không ảnh hưởng:** booking_api.php, staff_api.php, packages_api.php, auth_api.php gốc.

### ⏳ Bước 9 — Native Booking + Packages screens

- HomeFragment + BookingsFragment → native RecyclerView thay WebView
- Packages + Mua gói + Lịch sử native
- 2 lớp conflict (available_beds API + server validation)

---

## Kiến trúc API

```
VetSpaApi (Retrofit) ← OkHttpClient ← MyCookieJar (SharedPreferences)
                                            ↑
                                    WebView (CookieManager)
```

- Retrofit gọi API PHP backend dùng chung
- Cookie được đồng bộ giữa Retrofit và WebView qua MyCookieJar
- FCM token đăng ký qua `fcm_api.php?action=register`
- WorkManager NotificationPoller chạy background (5 phút), đọc cookie từ SharedPreferences, gọi notification poll API, hiển thị notificaiton native

## Backend dependencies (PHP project)

PHP backend tại `D:\WinNMP\WWW\vetspa\` dùng chung. Android native app KHÔNG thay đổi backend. Mọi endpoint API đều gọi qua `ApiClient.api.*()` với cookie tự động đính kèm.

Xem chi tiết API tại `HUONG_DAN.md` mục VI.
