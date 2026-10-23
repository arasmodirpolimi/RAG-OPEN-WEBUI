package com.ragplatform.api.config;

import com.ragplatform.api.web.security.ApiKeyFilter;
import com.ragplatform.impl.config.RagProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class WebConfiguration {

    @Bean
    FilterRegistrationBean<ApiKeyFilter> apiKeyFilter(RagProperties props) {
        FilterRegistrationBean<ApiKeyFilter> registration = new FilterRegistrationBean<>(
                new ApiKeyFilter(props.security().openaiApiKey(), props.security().adminApiKey()));
        registration.addUrlPatterns("/v1/*", "/api/*");
        registration.setOrder(1);
        return registration;
    }
}
