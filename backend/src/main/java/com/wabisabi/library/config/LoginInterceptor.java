package com.wabisabi.library.config;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.wabisabi.library.common.Messages;
import com.wabisabi.library.common.SessionKeys;
import com.wabisabi.library.exception.ApiException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 登入檢查的「門口警衛」：請求進到 Controller 之前先檢查 Session 裡有沒有會員 id。
 * 沒有 → 丟出 401，請求不會進到 Controller。
 * 要保護哪些網址，在 WebConfig 設定。
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		// getSession(false)：只拿「已經存在」的 Session，沒有就回傳 null，不會順手建立新的
		HttpSession session = request.getSession(false);

		if (session == null || session.getAttribute(SessionKeys.USER_ID) == null) {
			// 交給 GlobalExceptionHandler 轉成 401 + {"message": "請先註冊/登入"}
			throw new ApiException(HttpStatus.UNAUTHORIZED, Messages.LOGIN_REQUIRED);
		}
		return true; // true = 放行，繼續往 Controller 走
	}
}
