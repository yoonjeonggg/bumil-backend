package com.example.bumil_backend.config;

import com.example.bumil_backend.common.exception.JwtAuthenticationException;
import com.example.bumil_backend.common.exception.JwtTokenNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import java.security.Principal;


@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {
    private final TokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";


    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (token == null || !tokenProvider.validateToken(token)) {
                throw new JwtAuthenticationException("유효하지 않은 토큰입니다.");
            }

            String username = tokenProvider.extractEmail(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            accessor.setUser(authentication);
            accessor.getSessionAttributes().put("userPrincipal", authentication);
        }

        if (StompCommand.SEND.equals(accessor.getCommand()) ||
                StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

            if (accessor.getUser() == null) {
                Object saved = accessor.getSessionAttributes().get("userPrincipal");
                if (saved instanceof Principal savedUser) {
                    accessor.setUser(savedUser);
                }
            }

            // 여전히 유저 정보가 없다면 인증 실패 처리
            if (accessor.getUser() == null) {
                throw new AccessDeniedException("인증 정보가 없습니다. 다시 로그인해주세요.");
            }
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader(AUTH_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;

    }
}


