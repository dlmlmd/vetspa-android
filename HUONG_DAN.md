# Hướng dẫn triển khai VetSpa Android Native

## I. Tạo GitHub repository

```bash
cd D:\WinNMP\WWW\vetspa-android-native

# Khởi tạo git
git init
git add .
git commit -m "init: VetSpa Android native app"

# Đăng nhập GitHub CLI (nếu chưa)
gh auth login

# Tạo repo mới trên GitHub (public)
gh repo create vetspa-android --public --source=. --remote=origin --push
```

Sau lệnh cuối, code tự động push lên `https://github.com/<USER>/vetspa-android.git`

> Nếu không dùng `gh`, tạo repo thủ công trên GitHub rồi:
```bash
git remote add origin https://github.com/dlmlmd/vetspa-android.git
git branch -M main
git push -u origin main
```

---

## II. Cấu hình Firebase

Dự án dùng chung Firebase project `vetspa-e5074` với PHP backend.

### 1. google-services.json (Android)

File `app/google-services.json` đã có sẵn (placeholder). Nếu cần file thật:
- Firebase Console → Project settings → General → Your apps → thêm app Android
- Package name: `com.vetspa.nativeapp`
- Tải file `google-services.json` → thay thế file hiện tại

Nếu build qua GitHub Actions:
- GitHub repo → Settings → Secrets and variables → Actions
- New secret: tên `GOOGLE_SERVICES_JSON`, paste nội dung file json

### 2. Service Account (PHP server)

Đã có trong DB (`fcm_service_account`) — không cần thay đổi.

---

## III. Build APK

### Local (cần Android Studio / SDK)

```bash
cd D:\WinNMP\WWW\vetspa-android-native

# Tạo local.properties (nếu chưa có)
echo "sdk.dir=C:\\Users\\<USER>\\AppData\\Local\\Android\\Sdk" > local.properties

# Build debug
./gradlew assembleDebug

# APK ở: app/build/outputs/apk/debug/
```

### GitHub Actions (tự động)

Push code lên GitHub → vào tab **Actions** → workflow "Build Android APK" tự chạy.

APK được upload dưới dạng artifact `vetspa-native-debug-apk`.

---

## IV. Cấu hình API URL

Sửa trong `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://vetspa.free.je/api/\"")
```

Thay domain bằng domain thật của bạn.

---

## V. Cấu trúc thư mục

```
vetspa-android-native/
├── .github/workflows/build-apk.yml   # CI/CD build APK
├── build.gradle.kts                   # Project-level Gradle
├── settings.gradle.kts                # Settings
├── gradle.properties                  # Gradle properties
├── gradle/wrapper/                    # Gradle wrapper
├── .gitignore
├── HUONG_DAN.md                       # File này
├── app/
│   ├── build.gradle.kts               # App-level Gradle
│   ├── google-services.json           # Firebase config
│   ├── proguard-rules.pro             # ProGuard
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/                       # Resources
│       └── java/com/vetspa/nativeapp/
│           ├── VetSpaApp.kt           # Application class
│           ├── data/
│           │   ├── api/
│           │   │   ├── VetSpaApi.kt    # Retrofit API interface
│           │   │   └── MyCookieJar.kt  # Cookie persistence
│           │   └── model/
│           │       └── ApiResponse.kt  # Data models
│           ├── service/
│           │   └── FcmService.kt       # FCM push handler
│           ├── ui/
│           │   └── MainActivity.kt     # Main activity
│           └── util/
│               └── NotifHelper.kt      # Notification helper
```

---

## VI. API dùng cho Android Native

Android app gọi riêng các endpoint `android_*` — tách biệt hoàn toàn khỏi API web, không ảnh hưởng backend hiện tại.

### Android Auth API — `android_auth_api.php`

| Action | Method | Input | Output |
|--------|--------|-------|--------|
| `login` | POST | `{username, password}` (JSON body) | `{ok, user: {id, username, fullname, role, email, phone, profile_code}}` |
| `me` | GET | — | `{ok, user: {...}}` |
| `logout` | POST | — | `{ok}` |
| `register_fcm` | POST | `{token, platform}` | `{ok}` |

### Android Booking API — `android_booking_api.php`

| Action | Method | Input | Output |
|--------|--------|-------|--------|
| `my_bookings` | GET | — | `{ok, bookings: [{date, items: [Booking]}]}` — nhóm theo ngày |
| `available_beds` | GET | `start, end` | `{ok, beds: [1,3,4], total_beds, booked_beds}` |
| `create` | POST | `{staff_id, bed_id, start_time, end_time, package_id, note?}` | `{ok, booking}` — 2 lớp chống trùng |
| `cancel` | POST | `{booking_id}` | `{ok}` — hoàn trả 1 buổi |
| `detail` | GET | `id` | `{ok, booking: {staff_name, staff_phone, staff_avatar, ...}}` |

### Android Staff API — `android_staff_api.php`

| Action | Method | Input | Output |
|--------|--------|-------|--------|
| `available` | GET | `start, end` | `{ok, staff: [...], busy_staff_ids}` — có `is_favorite` |
| `list` | GET | — | `{ok, staff: [...]}` — tất cả NV |

### Android Packages API — `android_packages_api.php`

| Action | Method | Input | Output |
|--------|--------|-------|--------|
| `list` | GET | — | `{ok, packages: [{id, name, price, total_sessions, duration_per_session, ...}]}` |
| `my` | GET | — | `{ok, packages: [{package_id, package_name, sessions_remaining, sessions, status}]}` |

### Ghi chú quan trọng

- **Cookie:** Retrofit tự động đính kèm session cookie qua `MyCookieJar` (dùng chung với WebView)
- **Conflict 2 lớp:** `available_beds` + `available_staff` chặn client → server validate lại → 409
- **Không sửa API gốc:** Mọi endpoint `android_*` đều là file mới, không ảnh hưởng web
- **Base URL:** `https://spa.vetmedia.vn/api/` (cấu hình trong `BuildConfig.API_BASE_URL`)

Xem chi tiết backend tại `CLAUDE.md` của project PHP gốc (thư mục `D:\WinNMP\WWW\vetspa\`).
