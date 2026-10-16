package com.wabisabi.library.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.wabisabi.library.common.Messages;
import com.wabisabi.library.dto.MessageResponse;

/**
 * 全域例外處理：所有 Controller 丟出來、沒人接住的例外，最後都會來到這裡，
 * 統一轉成 {"message": "..."} 的 JSON 回傳。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/** 1. 我們自己丟的「預料中的錯誤」：狀態碼和訊息照它帶來的回傳 */
	@ExceptionHandler(ApiException.class)
	public ResponseEntity<MessageResponse> handleApiException(ApiException e) {
		return ResponseEntity.status(e.getStatus()).body(new MessageResponse(e.getMessage()));
	}

	/**
	 * 2. 請求內容看不懂：
	 * - HttpMessageNotReadableException：送來的不是正確的 JSON（例如少了括號）
	 * - MethodArgumentTypeMismatchException：網址參數型態不對（例如 DELETE /api/favorites/abc，id 應該是數字）
	 */
	@ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
	public ResponseEntity<MessageResponse> handleBadRequest(Exception e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(Messages.REQUEST_FAILED));
	}

	/** 3. 其他所有例外 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<MessageResponse> handleOther(Exception e) {
		// Spring 自己擋下的請求（網址不存在 404、HTTP 方法不對 405 等）會帶著狀態碼，照用
		if (e instanceof ErrorResponse errorResponse) {
			HttpStatusCode status = errorResponse.getStatusCode();
			return ResponseEntity.status(status).body(new MessageResponse(Messages.REQUEST_FAILED));
		}
		// 真正沒預料到的錯誤：把完整錯誤印在後端主控台方便除錯，前端只看到通用訊息
		log.error("未預期的錯誤", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse(Messages.SERVER_ERROR));
	}
}
