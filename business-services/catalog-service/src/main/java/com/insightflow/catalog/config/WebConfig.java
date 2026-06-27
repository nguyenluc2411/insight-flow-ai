package com.insightflow.catalog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Removed to let NewsImageController stream images from MinIO
        // String uploadPath = Paths.get("uploads/news").toAbsolutePath().toUri().toString();
        // registry.addResourceHandler("/api/v1/catalog/public/news/uploads/**")
        //         .addResourceLocations(uploadPath);
    }
}
