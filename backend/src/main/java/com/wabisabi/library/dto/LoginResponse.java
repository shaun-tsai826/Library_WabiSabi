package com.wabisabi.library.dto;

import com.wabisabi.library.entity.User;

/**
 * 登入成功的回應：{"message": "登入成功", "nickname": "Cooqle", "mobile": "0900123456"}
 * 前端拿到暱稱後，就能馬上更新導覽列的「Cooqle (個人頁)」。
 */
public class LoginResponse {

	private String message;
	private String nickname;
	private String mobile;

	public LoginResponse() {
	}

	public LoginResponse(String message, User user) {
		this.message = message;
		this.nickname = user.getNickname();
		this.mobile = user.getMobile();
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
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
