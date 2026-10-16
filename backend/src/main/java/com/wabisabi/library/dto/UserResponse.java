package com.wabisabi.library.dto;

import com.wabisabi.library.entity.User;

/**
 * 回傳給前端的會員資料（GET /api/auth/me）：{"nickname": "Cooqle", "mobile": "0900123456"}
 * 不包含 id、nickname_lower 這些前端用不到的欄位。
 */
public class UserResponse {

	private String nickname;
	private String mobile;

	public UserResponse() {
	}

	public UserResponse(User user) {
		this.nickname = user.getNickname();
		this.mobile = user.getMobile();
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}
}
