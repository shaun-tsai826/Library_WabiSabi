package com.wabisabi.library.common;

/**
 * Session 裡存放資料用的「名字」。集中寫在這裡，存和取才不會因為拼錯字而對不上。
 */
public final class SessionKeys {

	private SessionKeys() {
	}

	/** 登入者的會員 id（Long） */
	public static final String USER_ID = "loginUserId";
}
