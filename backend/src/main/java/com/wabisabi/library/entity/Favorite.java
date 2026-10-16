package com.wabisabi.library.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 收藏：對應資料表 favorites，記錄「哪位會員收藏了哪本書」。
 * 同一位會員不能重複收藏同一本書：(user_id, isbn) 設為唯一鍵。
 */
@Entity
@Table(name = "favorites",
		uniqueConstraints = @UniqueConstraint(name = "uk_favorites_user_isbn", columnNames = { "user_id", "isbn" }))
public class Favorite {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 多筆收藏 → 屬於一位會員。資料表裡存的是 user_id 欄位（外鍵，指向 users.id） */
	@ManyToOne(optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	/** 多筆收藏 → 指向一本書。資料表裡存的是 isbn 欄位（外鍵，指向 books.isbn） */
	@ManyToOne(optional = false)
	@JoinColumn(name = "isbn", nullable = false)
	private Book book;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	public Favorite() {
	}

	/** 新增收藏時用：自動填好建立時間 */
	public Favorite(User user, Book book) {
		this.user = user;
		this.book = book;
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Book getBook() {
		return book;
	}

	public void setBook(Book book) {
		this.book = book;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
