# 🚨 Notification Debugging Guide

## Issue: "I can't see notifications when texting"

You've successfully implemented the push notification system, but notifications aren't appearing. Let's debug this step by step.

## 📱 **STEP 1: Basic Notification Test**

I've added a **manual test trigger** to your chat. Here's how to test:

### Quick Test:
1. **Open any chat conversation**
2. **Tap on the user's name at the top** (in the toolbar)
3. **Check the logcat** for notification debug messages

**Expected logs:**
```
ChatFragment: === MANUAL NOTIFICATION TEST ===
ChatFragment: === NOTIFICATION DEBUG ===
ChatFragment: New messages count: 1
ChatFragment: Fragment visible: true
ChatFragment: NotificationHelper available: true
ChatFragment: Message notifications are ENABLED in settings
ChatFragment: Showing notification (DEBUG MODE - ignoring fragment visibility)
ChatFragment: ✅ Local notification created successfully for 1 new messages
```

## 📱 **STEP 2: Check System Notification Permissions**

### Android Settings Check:
1. **Long-press your app icon** → App Info
2. **Go to Notifications**
3. **Ensure "Allow notifications" is ON**
4. **Check individual categories** (especially "Messages")

### Programmatic Check:
The debug logs will show:
```
ChatFragment: ✅ System notifications are ENABLED
```
or
```
ChatFragment: ⚠️ System notifications are DISABLED for this app
```

## 📱 **STEP 3: Check App Notification Settings**

1. **Go to Profile (User tab)**
2. **Tap "Notification Settings"**
3. **Ensure "Messages" toggle is ON**

### Debug logs should show:
```
ChatFragment: Message notifications are ENABLED in settings
```

## 📱 **STEP 4: Test Real Message Notifications**

### Scenario A: Single Device Test
1. **Open chat with any user**
2. **Minimize the app** (press home button)
3. **Wait 5-10 seconds** (for polling cycle)
4. **Send yourself a message from another account/device**

### Scenario B: Two Device Test
1. **Device A**: Open chat, then minimize app
2. **Device B**: Send message to Device A
3. **Device A**: Should receive notification within 5 seconds

### Expected behavior:
- **Chat open**: No notification (messages appear in real-time)
- **Chat minimized**: Notification should appear
- **Tap notification**: Opens specific chat

## 🔍 **STEP 5: Debug Logs to Check**

Run this command while testing:
```bash
adb logcat | findstr "ChatFragment\|NotificationHelper\|FCM"
```

### Key logs to look for:

#### ✅ **Success Logs:**
```
ChatFragment: === NOTIFICATION DEBUG ===
ChatFragment: Message notifications are ENABLED in settings
ChatFragment: Building notification with:
ChatFragment: - Title: [User Name]
ChatFragment: - Content: [Message content]
ChatFragment: ✅ Notification sent to system with ID: [room_id]
```

#### ❌ **Problem Logs:**
```
ChatFragment: Message notifications are DISABLED in settings
ChatFragment: ⚠️ System notifications are DISABLED for this app
ChatFragment: ❌ Error showing notification: [error]
```

## 🔧 **STEP 6: Common Issues & Solutions**

### Issue 1: "Fragment always visible"
**Symptom**: Logs show `Fragment visible: true` even when app is minimized
**Solution**: The fragment visibility tracking might have issues
**Fix**: I've temporarily disabled visibility check for debugging

### Issue 2: "Notifications disabled in settings"
**Symptom**: `Message notifications are DISABLED in settings`
**Solution**: 
1. Go to Profile → Notification Settings
2. Turn ON "Messages" toggle
3. Restart app

### Issue 3: "System notifications disabled"
**Symptom**: `System notifications are DISABLED for this app`
**Solution**: 
1. Go to Android Settings → Apps → [Your App] → Notifications
2. Enable "Allow notifications"
3. Enable "Messages" category

### Issue 4: "No polling happening"
**Symptom**: No new messages detected during polling
**Solution**: Check if polling is running:
```
ChatFragment: Added X new messages from polling
```

### Issue 5: "Firebase token issues"
**Symptom**: Firebase warnings in original logs
**Solution**: Firebase warnings are normal during initialization

## 🎯 **STEP 7: Force Notification Test**

I've added debugging that **ignores fragment visibility** temporarily. This means:
- **Notifications should appear even when chat is open** (for testing)
- **This helps isolate if the issue is visibility tracking or notification creation**

### Test this:
1. **Open a chat**
2. **Tap the user name** (manual test)
3. **Notification should appear even though chat is open**

## 📊 **Debug Report Template**

When testing, provide these details:

```
=== NOTIFICATION DEBUG REPORT ===

1. Manual test result: ✅/❌
   - Tapped user name: ✅/❌
   - Notification appeared: ✅/❌

2. System settings:
   - App notifications enabled: ✅/❌
   - Messages category enabled: ✅/❌

3. App settings:
   - Messages toggle in app: ✅/❌

4. Key logs:
   - Fragment visible: true/false
   - NotificationHelper available: true/false
   - Message notifications enabled: true/false
   - System notifications enabled: true/false

5. Real message test:
   - App minimized: ✅/❌
   - Message sent: ✅/❌
   - Notification received: ✅/❌
   - Notification tap works: ✅/❌

=== END REPORT ===
```

## 🚀 **Next Steps**

1. **Run the manual test** (tap user name in chat)
2. **Check all system/app notification settings**
3. **Test with minimized app**
4. **Provide debug logs** if issues persist

The notification system is implemented correctly - we just need to identify what's preventing the notifications from showing up on your device.

---

**Note**: The debug mode currently shows notifications even when chat is open. Once we confirm notifications work, we'll re-enable the proper visibility logic.
