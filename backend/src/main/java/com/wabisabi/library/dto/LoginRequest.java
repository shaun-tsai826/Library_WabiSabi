package com.wabisabi.library.dto;

/**
 * 登入/註冊時前端送來的資料：{"nickname": "Cooqle", "mobile": "0900123456"}
 * Jackson 會用 setter 把 JSON 的值填進來。
 */
public class LoginRequest {

	private String nickname;
	private String mobile;

	public LoginRequest() {
	}

	public LoginRequest(String nickname, String mobile) {
		this.nickname = nickname;
		this.mobile = mobile;
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
