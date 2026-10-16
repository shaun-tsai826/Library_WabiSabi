package com.wabisabi.library.entity;

import java.time.LocalDateTime;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 會員：對應資料表 users。
 */
@Entity
@Table(name = "users")
public class User {

	/** 會員編號：資料庫自動遞增（1, 2, 3...） */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 暱稱原樣（例如 Cooqle），用來顯示 */
	@Column(name = "nickname", nullable = false, length = 20)
	private String nickname;

	/** 暱稱小寫（例如 cooqle），用來比對；唯一，所以大小寫不同也算同一個暱稱 */
	@Column(name = "nickname_lower", nullable = false, length = 20, unique = true)
	private String nicknameLower;

	/** 手機號碼：用字串存，開頭的 0 才不會消失；唯一 */
	@Column(name = "mobile", nullable = false, length = 10, unique = true)
	private String mobile;

	/** 建立時間 */
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	/** JPA 規定一定要有一個沒有參數的建構子（它從資料庫讀資料時會用到） */
	public User() {
	}

	/** 建立新會員時用：自動填好小寫暱稱與建立時間 */
	public User(String nickname, String mobile) {
		setNickname(nickname);
		this.mobile = mobile;
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNickname() {
		return nickname;
	}

	/** 設定暱稱時，同時更新小寫版本，兩個欄位永遠一致 */
	public void setNickname(String nickname) {
		this.nickname = nickname;
		this.nicknameLower = nickname.toLowerCase(Locale.ROOT);
	}

	public String getNicknameLower() {
		return nicknameLower;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
