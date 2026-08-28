package blackdemise.cp.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

import blackdemise.cp.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

// Returned when a request has no (or an invalid/expired) access token - the frontend should
// call /refresh once, then force the user back to login if that also fails.
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                jsonMapper.writeValue(
                response.getWriter(),
                ApiResponse.error(HttpServletResponse.SC_UNAUTHORIZED,
                    "Authentication is required to access this resource"));
    }
}
