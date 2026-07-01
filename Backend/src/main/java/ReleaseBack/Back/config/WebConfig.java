package ReleaseBack.Back.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ReleaseBack.Back.security.JwtInterceptor;
import lombok.RequiredArgsConstructor;

import org.springframework.lang.NonNull;

@RequiredArgsConstructor
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    private final JwtInterceptor jwtInterceptor;

    private final AppConfigProvider appConfigProvider;
    
    @SuppressWarnings("null")
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**") // intercept all requests
                .excludePathPatterns( // except public endpoints
                        "/auth/login",
                        "/auth/register",
                        "/config/baseurl"
                );

    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        String[] allowedOrigins = appConfigProvider.getCorsAllowedOrigins();
        if (allowedOrigins == null) {
            throw new IllegalStateException("CORS allowed origins cannot be null");
        }
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("*")
                .allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/Config/**")
                .addResourceLocations("file:../Config/");
    }
}
