package com.wabisabi.library.dto;

import com.wabisabi.library.entity.Book;

/**
 * 回傳給前端的書籍資料。轉成 JSON 後長這樣：
 * {"isbn": "9789861343722", "title": "...", "author": "...", "publishDate": "2020-05-01",
 *  "publisher": "...", "pageCount": 320, "coverUrl": "https://books.google.com/..."}
 */
public class BookResponse {

	private String isbn;
	private String title;
	private String author;
	private String publishDate;
	private String publisher;
	/** 頁數；來源沒有提供時是 null，轉成 JSON 也是 null */
	private Integer pageCount;
	/** 備用封面網址（Google Books）；沒有時是 null */
	private String coverUrl;

	public BookResponse() {
	}

	/** 從 Entity 複製資料過來 */
	public BookResponse(Book book) {
		this.isbn = book.getIsbn();
		this.title = book.getTitle();
		this.author = book.getAuthor();
		this.publishDate = book.getPublishDate();
		this.publisher = book.getPublisher();
		this.pageCount = book.getPageCount();
		this.coverUrl = book.getCoverUrl();
	}

	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getPublishDate() {
		return publishDate;
	}

	public void setPublishDate(String publishDate) {
		this.publishDate = publishDate;
	}

	public String getPublisher() {
		return publisher;
	}

	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}

	public Integer getPageCount() {
		return pageCount;
	}

	public void setPageCount(Integer pageCount) {
		this.pageCount = pageCount;
	}

	public String getCoverUrl() {
		return coverUrl;
	}

	public void setCoverUrl(String coverUrl) {
		this.coverUrl = coverUrl;
	}
}
