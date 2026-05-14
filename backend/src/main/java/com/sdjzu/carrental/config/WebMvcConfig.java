package com.sdjzu.carrental.config;

import com.sdjzu.carrental.security.AuthInterceptor;
import com.sdjzu.carrental.service.MediaService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final MediaService mediaService;

    public WebMvcConfig(AuthInterceptor authInterceptor, MediaService mediaService) {
        this.authInterceptor = authInterceptor;
        this.mediaService = mediaService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(mediaService.getMediaAccessPrefix() + "/**")
                .addResourceLocations("file:" + mediaService.getMediaBasePath().toString().replace("\\", "/") + "/");
    }
}
