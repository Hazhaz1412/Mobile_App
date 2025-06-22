# 🔧 DEBUG NOTIFICATION SYSTEM - QUICK TEST GUIDE

## 📱 URGENT TESTING STEPS

Bạn đã có app build mới với các fixes về thông báo. Hãy test ngay như sau:

### **🔍 BƯỚC 1: Test local notification đơn giản**

1. **Mở app TradeUp**
2. **Vào bất kỳ chat nào** 
3. **Click vào tên người dùng ở top bar** 
4. ➡️ **Expected**: Xuất hiện notification test "🔔 Test từ [tên user]"

**Nếu bước này WORK** ✅ → Hệ thống notification cơ bản hoạt động
**Nếu bước này FAIL** ❌ → Vấn đề ở notification system settings

---

### **🔍 BƯỚC 2: Test real-time notifications**

#### **Setup Test Environment:**
- **Device A**: Your main account
- **Device B**: Another account hoặc emulator với account khác

#### **Test Case 1: Background notification**
1. **Device A**: Mở chat với user B 
2. **Device A**: **Press HOME button** (app vào background)
3. **Device B**: Gửi tin nhắn cho user A
4. ➡️ **Expected**: Device A hiện notification

#### **Test Case 2: Fragment not visible**
1. **Device A**: Từ chat, navigate đến fragment khác (Home, Profile...)
2. **Device B**: Gửi tin nhắn
3. ➡️ **Expected**: Device A hiện notification

#### **Test Case 3: No notification when visible**
1. **Device A**: Đang xem chat với user B
2. **Device B**: Gửi tin nhắn  
3. ➡️ **Expected**: Device A **KHÔNG** có notification (đang xem chat)

---

### **🔍 BƯỚC 3: Check system settings nếu không hoạt động**

#### **Android System Settings:**
```
Settings > Apps > TradeUp > Notifications > ENABLED
Settings > Apps > TradeUp > Notifications > Messages Channel > ENABLED & HIGH
Settings > Sound > Do Not Disturb > OFF (hoặc thêm TradeUp vào exceptions)
Settings > Battery > Battery Optimization > TradeUp > Not optimized
```

#### **Debug trong app:**
1. Mở chat bất kỳ
2. Click tên user ở top để trigger test notification
3. Nếu không có notification, kiểm tra **LogCat:**

---

### **🔍 BƯỚC 4: LogCat Debug Commands**

Nếu có ADB setup:
```bash
adb logcat -c
adb logcat | grep -E "(ChatNotificationMgr|FCMService|NotificationChannels)"
```

Look for:
- `✅ Test notification sent`
- `✅ FCM token sent to server successfully`
- `Global notifications enabled: true`
- `Messages channel importance: 4` (HIGH)

---

### **🔍 BƯỚC 5: Backend Integration Check**

Đảm bảo backend gửi FCM notifications với format đúng:

#### **Required FCM data fields:**
```json
{
  "data": {
    "type": "message",
    "senderId": "123",
    "senderName": "User Name", 
    "roomId": "456",
    "content": "Message content"
  },
  "notification": {
    "title": "User Name",
    "body": "Message content"
  }
}
```

#### **Backend endpoint cần kiểm tra:**
- `POST /api/users/{userId}/fcm-token` - Register FCM token
- FCM notification sending logic when new messages created

---

## 🚨 COMMON ISSUES & FIXES

### **Issue 1: "Token chưa được gửi lên server"**
- Check: Log in/register flow có call `updateFcmToken` API
- Fix: Đã fix trong NotificationHelper.java

### **Issue 2: "Notification channel không tồn tại"**
- Check: Application class có call `createNotificationChannels()`
- Fix: Đã có trong OkApplication.onCreate()

### **Issue 3: "Fragment visibility không đúng"**
- Check: onResume/onPause có set `isFragmentVisible`
- Fix: Đã implement trong ChatFragment lifecycle

### **Issue 4: "FCM Service không nhận messages"**
- Check: AndroidManifest có khai báo MyFirebaseMessagingService
- Check: google-services.json có đúng package name
- Fix: Đã có trong manifest

---

## 💡 EXPECTED RESULTS

### **✅ Working Scenario:**
1. Test notification: Click tên user → notification xuất hiện
2. Background: Press HOME → nhận notification khi có tin nhắn mới
3. Fragment switch: Chuyển tab → nhận notification 
4. Clear notification: Mở chat → notification biến mất

### **❌ Not Working → Check:**
1. System notification settings disabled
2. FCM token not sent to server
3. Backend not sending FCM notifications
4. Do Not Disturb mode enabled
5. Battery optimization blocking notifications

---

**Test ngay và cho tôi biết kết quả!** 🚀

Nếu vẫn không work, share LogCat output từ test notification đầu tiên.
