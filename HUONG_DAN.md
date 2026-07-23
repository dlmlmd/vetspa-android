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

## VI. API có sẵn (dùng chung với PHP backend)

| Endpoint | Method | Mô tả |
|----------|--------|-------|
| `auth_api.php?action=login` | POST | Đăng nhập (username, password) |
| `auth_api.php?action=me` | GET | Lấy thông tin user hiện tại |
| `auth_api.php?action=logout` | POST | Đăng xuất |
| `booking_api.php?action=my` | GET | Lịch của tôi |
| `booking_api.php?action=create` | POST | Tạo lịch mới |
| `staff_api.php?action=available` | GET | Nhân viên rảnh |
| `staff_api.php?action=list` | GET | Tất cả nhân viên |
| `packages_api.php?action=list` | GET | Danh sách gói dịch vụ |
| `packages_api.php?action=my` | GET | Gói đã mua |
| `fcm_api.php?action=register` | POST | Đăng ký FCM token |
| `notification_api.php?action=poll` | GET | Kiểm tra thông báo mới |

Xem chi tiết tại CLAUDE.md của project PHP gốc.
