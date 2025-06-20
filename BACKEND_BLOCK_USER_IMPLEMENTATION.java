// ===========================================
// SAMPLE BACKEND ENDPOINTS FOR BLOCK USER
// ===========================================
// Framework: Spring Boot (Java) hoặc Node.js/Express
// Database: MySQL với schema đã tạo ở file DATABASE_BLOCK_MODERATION_SYSTEM.sql

// ===========================================
// 1. BLOCK USER ENDPOINTS
// ===========================================

/* 
Spring Boot Controller Example:

@RestController
@RequestMapping("/api/users")
public class UserBlockController {
    
    @Autowired
    private UserBlockService userBlockService;
    
    // Path-based endpoint: POST /api/users/{userId}/block/{targetUserId}
    @PostMapping("/{userId}/block/{targetUserId}")
    public ResponseEntity<ApiResponse> blockUserPath(
            @PathVariable Long userId,
            @PathVariable Long targetUserId,
            @RequestBody(required = false) BlockUserRequest request) {
        
        try {
            // Validate users exist and are not the same
            if (userId.equals(targetUserId)) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Không thể tự chặn chính mình"));
            }
            
            // Check if already blocked
            if (userBlockService.isBlocked(userId, targetUserId)) {
                return ResponseEntity.status(409)
                    .body(new ApiResponse(false, "Đã chặn người dùng này trước đó"));
            }
            
            // Perform block
            String reason = request != null ? request.getReason() : null;
            userBlockService.blockUser(userId, targetUserId, reason);
            
            return ResponseEntity.ok(new ApiResponse(true, "Đã chặn người dùng thành công"));
            
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound()
                .build();
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ApiResponse(false, "Lỗi server: " + e.getMessage()));
        }
    }
    
    // Query-based endpoint: POST /api/users/block?userId=&targetUserId=
    @PostMapping("/block")
    public ResponseEntity<ApiResponse> blockUserQuery(
            @RequestParam Long userId,
            @RequestParam Long targetUserId,
            @RequestBody(required = false) BlockUserRequest request) {
        
        // Same logic as path-based endpoint
        return blockUserPath(userId, targetUserId, request);
    }
    
    // Unblock user
    @PostMapping("/{userId}/unblock/{targetUserId}")
    public ResponseEntity<ApiResponse> unblockUser(
            @PathVariable Long userId,
            @PathVariable Long targetUserId) {
        
        try {
            userBlockService.unblockUser(userId, targetUserId);
            return ResponseEntity.ok(new ApiResponse(true, "Đã bỏ chặn người dùng"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ApiResponse(false, "Lỗi server: " + e.getMessage()));
        }
    }
    
    // Get blocked users list
    @GetMapping("/{userId}/blocked")
    public ResponseEntity<List<BlockedUserDto>> getBlockedUsers(@PathVariable Long userId) {
        try {
            List<BlockedUserDto> blockedUsers = userBlockService.getBlockedUsers(userId);
            return ResponseEntity.ok(blockedUsers);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}

// ===========================================
// 2. SERVICE LAYER
// ===========================================

@Service
public class UserBlockService {
    
    @Autowired
    private UserBlockRepository userBlockRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public void blockUser(Long blockerId, Long blockedId, String reason) {
        // Validate users exist
        if (!userRepository.existsById(blockerId) || !userRepository.existsById(blockedId)) {
            throw new UserNotFoundException("Người dùng không tồn tại");
        }
        
        // Check if already blocked
        if (isBlocked(blockerId, blockedId)) {
            throw new IllegalStateException("Đã chặn người dùng này trước đó");
        }
        
        // Create block record
        UserBlock userBlock = new UserBlock();
        userBlock.setBlockerId(blockerId);
        userBlock.setBlockedId(blockedId);
        userBlock.setReason(reason);
        userBlock.setStatus(BlockStatus.ACTIVE);
        
        userBlockRepository.save(userBlock);
    }
    
    public void unblockUser(Long blockerId, Long blockedId) {
        UserBlock userBlock = userBlockRepository
            .findByBlockerIdAndBlockedIdAndStatus(blockerId, blockedId, BlockStatus.ACTIVE)
            .orElseThrow(() -> new IllegalStateException("Chưa chặn người dùng này"));
        
        userBlock.setStatus(BlockStatus.REMOVED);
        userBlockRepository.save(userBlock);
    }
    
    public boolean isBlocked(Long blockerId, Long blockedId) {
        return userBlockRepository
            .existsByBlockerIdAndBlockedIdAndStatus(blockerId, blockedId, BlockStatus.ACTIVE);
    }
    
    public List<BlockedUserDto> getBlockedUsers(Long userId) {
        return userBlockRepository.findActiveBlocksByBlockerId(userId)
            .stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    private BlockedUserDto convertToDto(UserBlock userBlock) {
        // Convert entity to DTO
        return new BlockedUserDto(
            userBlock.getBlockedId(),
            userBlock.getBlockedUser().getDisplayName(),
            userBlock.getReason(),
            userBlock.getCreatedAt()
        );
    }
}

// ===========================================
// 3. REPOSITORY LAYER
// ===========================================

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {
    
    boolean existsByBlockerIdAndBlockedIdAndStatus(Long blockerId, Long blockedId, BlockStatus status);
    
    Optional<UserBlock> findByBlockerIdAndBlockedIdAndStatus(Long blockerId, Long blockedId, BlockStatus status);
    
    @Query("SELECT ub FROM UserBlock ub WHERE ub.blockerId = :blockerId AND ub.status = 'ACTIVE'")
    List<UserBlock> findActiveBlocksByBlockerId(@Param("blockerId") Long blockerId);
    
    @Query("SELECT COUNT(ub) FROM UserBlock ub WHERE ub.blockerId = :blockerId AND ub.status = 'ACTIVE'")
    long countActiveBlocksByBlockerId(@Param("blockerId") Long blockerId);
}

// ===========================================
// 4. ENTITY CLASSES
// ===========================================

@Entity
@Table(name = "user_blocks")
public class UserBlock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "blocker_id", nullable = false)
    private Long blockerId;
    
    @Column(name = "blocked_id", nullable = false)
    private Long blockedId;
    
    @Column(name = "reason", length = 500)
    private String reason;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BlockStatus status = BlockStatus.ACTIVE;
    
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Getters and setters...
}

public enum BlockStatus {
    ACTIVE, REMOVED
}

// ===========================================
// 5. DTO CLASSES
// ===========================================

public class BlockUserRequest {
    private String reason;
    
    // Getters and setters...
}

public class BlockedUserDto {
    private Long userId;
    private String displayName;
    private String reason;
    private LocalDateTime blockedAt;
    
    // Constructors, getters and setters...
}

public class ApiResponse {
    private boolean success;
    private String message;
    private Object data;
    
    // Constructors, getters and setters...
}

// ===========================================
// 6. SECURITY CONSIDERATIONS
// ===========================================

/*
1. Authentication: Đảm bảo user đã đăng nhập
2. Authorization: Chỉ có thể block từ chính tài khoản của mình
3. Rate Limiting: Giới hạn số lần block để tránh spam
4. Validation: Validate input parameters
5. Logging: Log tất cả actions để audit

@PreAuthorize("authentication.name == #userId.toString() or hasRole('ADMIN')")
public ResponseEntity<ApiResponse> blockUser(@PathVariable Long userId, ...) {
    // Implementation
}
*/

// ===========================================
// 7. TESTING ENDPOINTS
// ===========================================

/*
Test với curl:

1. Block user (Path-based):
curl -X POST "http://localhost:8080/api/users/10/block/9" \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer <your-token>" \
     -d '{"reason": "Spam messages"}'

2. Block user (Query-based):
curl -X POST "http://localhost:8080/api/users/block?userId=10&targetUserId=9" \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer <your-token>" \
     -d '{"reason": "Inappropriate behavior"}'

3. Unblock user:
curl -X POST "http://localhost:8080/api/users/10/unblock/9" \
     -H "Authorization: Bearer <your-token>"

4. Get blocked users:
curl -X GET "http://localhost:8080/api/users/10/blocked" \
     -H "Authorization: Bearer <your-token>"
*/

// ===========================================
// 8. INTEGRATION WITH EXISTING FEATURES
// ===========================================

/*
1. Chat System: Không cho phép gửi tin nhắn đến người bị block
2. Listings: Ẩn listings của người bị block
3. Search: Loại bỏ kết quả từ người bị block
4. Notifications: Không gửi notification từ người bị block

Example query with block filtering:
SELECT l.* FROM listings l 
WHERE l.user_id NOT IN (
    SELECT blocked_id FROM user_blocks 
    WHERE blocker_id = ? AND status = 'ACTIVE'
)
AND l.status = 'AVAILABLE';
*/

*/
