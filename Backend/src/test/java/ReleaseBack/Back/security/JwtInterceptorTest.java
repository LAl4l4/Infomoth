package ReleaseBack.Back.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private JwtInterceptor interceptor;

    @Test
    void acceptsValidAuthCookie() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/data/currencies");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("authToken", "jwt-token")});
        when(jwtUtil.parseId("jwt-token")).thenReturn(7);

        assertTrue(interceptor.preHandle(request, response, new Object()));
        verify(request).setAttribute("userId", 7);
    }

    @Test
    void rejectsRequestsWithoutAuthCookie() throws Exception {
        StringWriter body = new StringWriter();
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/data/currencies");
        when(request.getCookies()).thenReturn(null);
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        assertFalse(interceptor.preHandle(request, response, new Object()));
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(jwtUtil, never()).parseId(anyString());
    }
}
