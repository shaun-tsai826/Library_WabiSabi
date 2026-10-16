package com.wabisabi.library.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wabisabi.library.common.Messages;
import com.wabisabi.library.common.SessionKeys;
import com.wabisabi.library.dto.LoginRequest;
import com.wabisabi.library.dto.LoginResponse;
import com.wabisabi.library.dto.MessageResponse;
import com.wabisabi.library.dto.UserResponse;
import com.wabisabi.library.entity.User;
import com.wabisabi.library.exception.ApiException;
import com.wabisabi.library.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 登入/註冊、登出、取得目前登入者。
 * /api/auth/login 公開；/api/auth/me 與 /api/auth/logout 需要登入（由 LoginInterceptor 把關）。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final UserService userService;

	public AuthController(UserService userService) {
		this.userService = userService;
	}

	/**
	 * POST /api/auth/login，內容：{"nickname": "Cooqle", "mobile": "0900123456"}
	 * 成功 200；欄位錯誤 400；暱稱與手機不符、暱稱已被使用 409。
	 */
	@PostMapping("/login")
	public LoginResponse login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
		// 1. 驗證並找出（或建立）會員；失敗會丟出 ApiException，下面就不會執行
		User user = userService.login(loginRequest);

		// 2. 取得 Session（沒有就建立一個），再換一個新的 Session ID，防止「Session 固定攻擊」
		HttpSession session = request.getSession();
		request.changeSessionId();

		// 3. 把會員 id 存進 Session：之後的請求就靠它認出「你是誰」
		session.setAttribute(SessionKeys.USER_ID, user.getId());

		return new LoginResponse(Messages.LOGIN_SUCCESS, user);
	}

	/**
	 * GET /api/auth/me：回傳目前登入者的暱稱與手機。
	 * 前端打開網頁時會先呼叫它，判斷「現在有沒有登入」。
	 */
	@GetMapping("/me")
	public UserResponse me(HttpSession session) {
		// 一律從 Session 取得登入者 id，不相信前端傳來的任何 id
		Long userId = (Long) session.getAttribute(SessionKeys.USER_ID);

		User user = userService.findById(userId).orElse(null);
		if (user == null) {
			// Session 裡有 id，但資料庫已經沒有這位會員：清掉 Session，當作未登入
			session.invalidate();
			throw new ApiException(HttpStatus.UNAUTHORIZED, Messages.LOGIN_REQUIRED);
		}
		return new UserResponse(user);
	}

	/** POST /api/auth/logout：清除 Session，回傳「已成功登出」 */
	@PostMapping("/logout")
	public MessageResponse logout(HttpSession session) {
		session.invalidate();
		return new MessageResponse(Messages.LOGOUT_SUCCESS);
	}
}
