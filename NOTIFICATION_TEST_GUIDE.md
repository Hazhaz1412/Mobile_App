# Hướng dẫn Test Thông báo

## 🧪 TEST THÔNG BÁO THỦ CÔNG

### Bước 1: Test cơ bản
1. **Mở ứng dụng và vào bất kỳ cuộc trò chuyện nào**
2. **Nhấp vào tên người dùng ở đầu màn hình chat** 
3. **Kiểm tra xem có thông báo test xuất hiện không**

### Bước 2: Test với tin nhắn thật
1. **Mở chat với một người khác**
2. **Thoát khỏi màn hình chat (quay về màn hình chính)**
3. **Yêu cầu người khác gửi tin nhắn**
4. **Chờ 5 giây để hệ thống polling hoạt động**
5. **Kiểm tra thông báo**

## 🔍 KIỂM TRA CÀI ĐẶT

### Android Settings
- Settings → Apps → [Tên app] → Notifications → **BẬT**
- Settings → Apps → [Tên app] → Battery → **Tắt Battery Optimization**
- Settings → Do Not Disturb → **TẮT** hoặc cho phép app

### Firebase
- Kiểm tra xem FCM token có được tạo không (xem logs)
- Kiểm tra kết nối mạng

## 📱 COMMANDS ĐỂ KIỂM TRA

### Kiểm tra notification permission:
```bash
adb shell dumpsys notification
```

### Xem logs:
```bash
adb logcat | findstr "ChatFragment\|FCMService\|NotificationTester"
```
