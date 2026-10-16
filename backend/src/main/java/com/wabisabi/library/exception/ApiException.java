package com.wabisabi.library.exception;

import org.springframework.http.HttpStatus;

/**
 * 「預料中的錯誤」：例如 ISBN 格式錯誤、找不到書、重複收藏。
 * 丟出時帶上 HTTP 狀態碼和要給使用者看的訊息，
 * GlobalExceptionHandler 會把它轉成 {"message": "..."} 回傳給前端。
 *
 * 用法：throw new ApiException(HttpStatus.NOT_FOUND, Messages.BOOK_NOT_FOUND);
 */
public class ApiException extends RuntimeException {

	private final HttpStatus status;

	public ApiException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}
}
