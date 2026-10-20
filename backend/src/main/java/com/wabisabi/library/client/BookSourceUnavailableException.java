package com.wabisabi.library.client;

/**
 * 外部書籍來源（Google Books、Open Library）無法使用：
 * 連不上、逾時、回傳錯誤狀態碼（例如 429 查詢太頻繁）或回傳內容看不懂。
 * BookService 接到它之後，會改查下一個來源；所有來源都沒查到時，回給前端「查詢失敗，請稍後再試」（503）。
 */
public class BookSourceUnavailableException extends RuntimeException {

	/**
	 * @param source 來源名稱，例如 "Google Books"、"Open Library"
	 * @param cause  原本的錯誤
	 */
	public BookSourceUnavailableException(String source, Throwable cause) {
		super(source + " 無法使用", cause);
	}
}
