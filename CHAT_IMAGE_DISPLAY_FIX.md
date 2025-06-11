# Chat Image Display Fix

## Vấn đề đã được sửa:
Hình ảnh trong chat không hiển thị mặc dù API gửi thành công và server trả về đúng URL hình ảnh.

## Nguyên nhân:
Trong `ChatAdapter`, logic hiển thị hình ảnh đang kiểm tra `message.getImageUrl()` nhưng server trả về URL hình ảnh trong field `content` chứ không phải `imageUrl`.

**Từ log API response:**
```json
{
  "content": "https://zn8vnhrf-8080.asse.devtunnels.ms/uploads/chat-images/30845dd9-1c60-4e8e-a8de-6f43792747a9.jpg",
  "type": "IMAGE"
}
```

## Thay đổi đã thực hiện:

### 1. Cập nhật logic trong `ChatAdapter.java`:

**Trước:**
```java
// Handle image messages
if (message.isImage() && message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
    holder.ivImage.setVisibility(View.VISIBLE);
    Glide.with(context)
            .load(message.getImageUrl())
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.placeholder)
            .into(holder.ivImage);
} else {
    holder.ivImage.setVisibility(View.GONE);
}
```

**Sau:**
```java
// Handle image messages
if (message.isImage()) {
    // For image messages, content contains the image URL
    String imageUrl = message.getContent();
    if (imageUrl != null && !imageUrl.isEmpty()) {
        holder.ivImage.setVisibility(View.VISIBLE);
        holder.tvContent.setVisibility(View.GONE); // Hide text content for images
        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .centerCrop()
                .into(holder.ivImage);
    } else {
        holder.ivImage.setVisibility(View.GONE);
        holder.tvContent.setVisibility(View.VISIBLE);
    }
} else {
    // For text messages, show content and hide image
    holder.ivImage.setVisibility(View.GONE);
    holder.tvContent.setVisibility(View.VISIBLE);
}
```

### 2. Cải tiến:

- **Sửa nguồn dữ liệu**: Sử dụng `message.getContent()` thay vì `message.getImageUrl()` cho tin nhắn hình ảnh
- **Ẩn/hiện đúng views**: Ẩn `tvContent` khi hiển thị hình ảnh và ngược lại
- **Thêm centerCrop**: Để hình ảnh hiển thị đẹp hơn
- **Logic rõ ràng**: Phân biệt rõ ràng giữa tin nhắn text và image

## Kết quả mong đợi:
- Hình ảnh trong chat sẽ hiển thị bình thường
- Layout chat message sẽ ẩn text khi hiển thị hình ảnh
- Tin nhắn text vẫn hoạt động bình thường

## Files đã chỉnh sửa:
1. `app/src/main/java/com/example/ok/adapter/ChatAdapter.java` - Sửa logic hiển thị hình ảnh

## Test:
- Build project thành công
- Không có lỗi compilation
- Logic hiển thị đã được cập nhật

**Ghi chú**: Vấn đề này xảy ra do sự không nhất quán giữa server API (trả về URL trong `content`) và client code (đọc từ `imageUrl`).
