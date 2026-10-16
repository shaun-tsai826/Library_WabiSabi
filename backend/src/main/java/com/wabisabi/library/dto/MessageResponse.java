package com.wabisabi.library.dto;

/**
 * 只有一段訊息的回應，轉成 JSON 後長這樣：{"message": "找不到此書籍"}
 * 錯誤回應、登入成功、加入收藏成功等都用它。
 */
public class MessageResponse {

	private String message;

	public MessageResponse() {
	}

	public MessageResponse(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
