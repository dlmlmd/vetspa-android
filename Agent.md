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

---

## Kế hoạch sẽ làm

### ⏳ Bước 6 — Deep link từ notification

- Khi nhận FCM → mở đúng tab/màn hình (lịch, tin nhắn...)
- Xử lý `message.data` để lấy action + params

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
