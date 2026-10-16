package com.wabisabi.library.client;

/**
 * Google Books 無法使用：連不上、逾時、回傳錯誤狀態碼（例如 429 查詢太頻繁）或回傳內容看不懂。
 * BookService 接到它之後，會回給前端「查詢失敗，請稍後再試」（503）。
 */
public class GoogleBooksUnavailableException extends RuntimeException {

	public GoogleBooksUnavailableException(Throwable cause) {
		super("Google Books 無法使用", cause);
	}
}
