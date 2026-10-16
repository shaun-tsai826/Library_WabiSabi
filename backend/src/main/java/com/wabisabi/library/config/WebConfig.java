package com.wabisabi.library.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 的設定：在這裡告訴 Spring「LoginInterceptor 要守哪些網址」。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final LoginInterceptor loginInterceptor;

	public WebConfig(LoginInterceptor loginInterceptor) {
		this.loginInterceptor = loginInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(loginInterceptor)
				// 用「白名單式」只列出需要登入的網址：
				// 查書（/api/books/**）和登入（/api/auth/login）必須保持公開，否則沒人能登入
				.addPathPatterns(
						"/api/favorites", // 查詢自己的收藏
						"/api/favorites/**", // 新增、刪除收藏
						"/api/auth/me", // 取得目前登入者
						"/api/auth/logout"); // 登出
	}
}
