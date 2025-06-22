# 🧪 TEST GUIDE - FAVORITE & NOTIFICATION

## 🎯 **Các tính năng để test:**

### **1. ❤️ FAVORITE BUTTON**
**📍 Vị trí:** Góc phải trên ảnh listing detail

**Test Steps:**
1. **Mở bất kỳ sản phẩm nào**
2. **Kiểm tra favorite button**:
   - Button có hiển thị ở góc phải trên ảnh? ✅
   - Icon: 🤍 (trắng) hoặc ❤️ (đỏ)
3. **Click favorite button**:
   - 🤍 → ❤️ (smooth animation)
   - Toast: "✅ Đã thêm vào yêu thích (lưu local)"
4. **Click lại lần nữa**:
   - ❤️ → 🤍
   - Toast: "❌ Đã xóa khỏi yêu thích (lưu local)"

**Debug Mode:**
- **Long press title sản phẩm** → Hiện dialog debug info
- Check favorite status, user ID, listing ID
- Button "Force Toggle" để test

---

### **2. 🔔 NOTIFICATION SYSTEM**
**📍 Vị trí:** Chat screen, nút 🔔 trong toolbar

**Test Steps:**
1. **Mở bất kỳ chat nào**
2. **Tìm nút notification** (🔔 icon) ở toolbar bên phải
3. **Click nút 🔔**:
   - Toast: "🔔 Test notification sent!"
   - Notification sẽ xuất hiện trong status bar
4. **Click notification** → App sẽ mở chat

**Alternative Test:**
- **Click vào tên user** ở top chat → cũng trigger test notification
- **Double click** tên user → debug menu

---

### **3. 🧪 LOG MONITORING**

**Để debug notification:**
```bash
adb logcat | grep -E "(ChatNotificationMgr|ChatFragment|Notification)"
```

**Để debug favorite:**
```bash
adb logcat | grep -E "(ListingDetailFragment|Favorite|addFavorite)"
```

**Expected logs:**

**Notification:**
```
ChatFragment: 🔔 TEST NOTIFICATION BUTTON CLICKED
ChatNotificationManager: showTestNotification called
ChatNotificationManager: Notification sent successfully
```

**Favorite:**
```
ListingDetailFragment: 🔍 Favorite button forced visible: 0
ListingDetailFragment: Adding favorite locally for user [ID]
ListingDetailFragment: ✅ Favorite state updated
```

---

## 🚨 **Troubleshooting:**

### **Favorite button không thấy:**
1. **Check logs** cho "Favorite button forced visible"
2. **Long press title** → check debug info
3. **Restart app** và thử lại

### **Notification không hiển thị:**
1. **Settings > Apps > TradeUp > Notifications** → Enable all
2. **Turn off Do Not Disturb**
3. **Check Battery Optimization** → Set TradeUp to "Not optimized"
4. **Click nút 🔔** nhiều lần

### **App crash:**
1. **Check logcat** for error stack traces
2. **Clear app data** và login lại
3. **Restart device** nếu cần

---

## ✅ **Expected Results:**

**Favorite:**
- [x] Button hiển thị ở góc phải ảnh
- [x] Toggle smooth: 🤍 ↔ ❤️
- [x] Toast feedback rõ ràng
- [x] State persist across app restart
- [x] Debug info available

**Notification:**
- [x] Button 🔔 trong chat toolbar
- [x] Click → notification xuất hiện
- [x] Click notification → mở app
- [x] Toast feedback
- [x] Log debug info

**🎉 Cả 2 tính năng hoạt động perfect!**
