package com.wabisabi.library.dto;

/**
 * 加入收藏時前端送來的資料：{"isbn": "9789861343722"}
 * 刻意沒有 userId 欄位：「是誰收藏的」一律從 Session 取得。
 */
public class AddFavoriteRequest {

	private String isbn;

	public AddFavoriteRequest() {
	}

	public AddFavoriteRequest(String isbn) {
		this.isbn = isbn;
	}

	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}
}
