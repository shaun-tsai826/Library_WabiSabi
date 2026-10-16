package com.wabisabi.library.dto;

import com.wabisabi.library.entity.Book;
import com.wabisabi.library.entity.Favorite;

/**
 * 收藏清單中的一筆（含書籍資料）：
 * {"id": 5, "isbn": "9789861343722", "title": "...", "author": "...", "publishDate": "2020-05-01"}
 * id 是「收藏」的編號，前端按「刪除」時用它呼叫 DELETE /api/favorites/{id}。
 */
public class FavoriteResponse {

	private Long id;
	private String isbn;
	private String title;
	private String author;
	private String publishDate;

	public FavoriteResponse() {
	}

	public FavoriteResponse(Favorite favorite) {
		Book book = favorite.getBook();
		this.id = favorite.getId();
		this.isbn = book.getIsbn();
		this.title = book.getTitle();
		this.author = book.getAuthor();
		this.publishDate = book.getPublishDate();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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
}
