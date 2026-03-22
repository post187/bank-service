package com.example.Service.Implementation;

import com.example.Model.Dto.Request.LoginRequest;
import com.example.Model.Entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final StringRedisTemplate redis;

    private static final Duration SESSION_TTL = Duration.ofHours(2);

    private String sessionKey(String sessionId) {
        return "session:" + sessionId;
    }

    private String userSessionsKey(Long userId) {
        return "user_sessions:" + userId;
    }

    // ===== 1. CREATE SESSION =====
    public String createSession(User user, LoginRequest request) {

        String sessionId = UUID.randomUUID().toString();

        String key = sessionKey(sessionId);

        Map<String, String> data = new HashMap<>();
        data.put("userId", user.getUserId().toString());
        data.put("deviceId", request.getDeviceId());
        data.put("deviceName", request.getDeviceName());
        data.put("ip", request.getIpAddress());
        data.put("createdAt", LocalDateTime.now().toString());

        redis.opsForHash().putAll(key, data);
        redis.expire(key, SESSION_TTL);

        // mapping user → session
        redis.opsForSet().add(userSessionsKey(user.getUserId()), sessionId);

        return sessionId;
    }

    // ===== 2. CHECK SESSION =====
    public boolean isSessionValid(String sessionId) {
        return Boolean.TRUE.equals(redis.hasKey(sessionKey(sessionId)));
    }

    // ===== 3. GET SESSION INFO =====
    public Map<Object, Object> getSession(String sessionId) {
        return redis.opsForHash().entries(sessionKey(sessionId));
    }

    // ===== 4. EXTEND 1 SESSION =====
    public void extendSession(String sessionId) {
        String key = sessionKey(sessionId);
        if (Boolean.TRUE.equals(redis.hasKey(key))) {
            redis.expire(key, SESSION_TTL);
        }
    }

    // ===== 5. LOGOUT 1 SESSION =====
    public void deleteSession(Long userId, String sessionId) {

        redis.delete(sessionKey(sessionId));

        redis.opsForSet().remove(userSessionsKey(userId), sessionId);
    }

    // ===== 6. LOGOUT ALL =====
    public void deleteAllSessions(Long userId) {

        Set<String> sessions = redis.opsForSet().members(userSessionsKey(userId));

        if (sessions != null) {
            for (String sessionId : sessions) {
                redis.delete(sessionKey(sessionId));
            }
        }

        redis.delete(userSessionsKey(userId));
    }

    public void deleteOtherSessions(Long userId, String currentSessionId) {
        String userKey = userSessionsKey(userId);

        Set<String> allSessionIds = redis.opsForSet().members(userKey);

        if (allSessionIds != null && !allSessionIds.isEmpty()) {
            for (String sid : allSessionIds) {
                if (!sid.equals(currentSessionId)) {
                    redis.delete(sessionKey(sid));

                    redis.opsForSet().remove(userKey, sid);
                }
            }
        }
        extendSession(currentSessionId);
    }

    // ===== 7. GET ALL DEVICES =====
    public List<Map<Object, Object>> getUserSessions(Long userId) {

        Set<String> sessionIds = redis.opsForSet().members(userSessionsKey(userId));

        List<Map<Object, Object>> result = new ArrayList<>();

        if (sessionIds != null) {
            for (String sessionId : sessionIds) {

                Map<Object, Object> sessionData =
                        redis.opsForHash().entries(sessionKey(sessionId));

                if (!sessionData.isEmpty()) {
                    sessionData.put("sessionId", sessionId);
                    result.add(sessionData);
                }
            }
        }

        return result;
    }
}
