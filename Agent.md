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

### ✅ Bước 1 — Google Services (2026-07-23)

- Tạo placeholder google-services.json (cấu trúc đúng, package `com.vetspa.nativeapp`)
- CI đọc từ secret `GOOGLESERVICES_JSON` thay thế placeholder

---

## Kế hoạch sẽ làm

### ⏳ Bước 2 — Push lên GitHub

1. `cd D:\WinNMP\WWW\vetspa-android-native`
2. `git init && git add . && git commit -m "init: VetSpa Android native app"`
3. Tạo repo trên GitHub hoặc dùng `gh repo create`
4. `git remote add origin <url> && git push -u origin main`
5. Vào GitHub → Settings → Secrets → thêm `GOOGLESERVICES_JSON`

### ⏳ Bước 3 — Fix build thử nghiệm

1. Sửa `buildConfigField("String", "API_BASE_URL", "...")` với domain thật
2. Local build: `./gradlew assembleDebug`
3. Cài APK lên máy thật test login + load WebView

### ⏳ Bước 4 — UI gốc Kotlin (native screens)

- Thêm Jetpack Compose hoặc Fragment-based UI
- Màn hình Login (dùng Retrofit login API)
- Màn hình Dashboard (thống kê, nút mở WebView)
- Navigation graph (NavComponent)

### ⏳ Bước 5 — Tích hợp đầy đủ API endpoints

- Packages + Voucher + Trial Voucher screens
- Kế toán (Accounting) charts

### ⏳ Bước 6 — Push Notification full flow

- Khi nhận FCM → mở deep link vào booking/tin nhắn cụ thể
- Notification channel grouping (thông báo đặt lịch, khuyến mãi, hệ thống)

### ⏳ Bước 7 — Tối ưu WebView

- Cache WebView resources (Glide + OkHttp cache)
- HTTPS tránh mixed content
- Pull-to-refresh gesture
- Loading progress bar

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
