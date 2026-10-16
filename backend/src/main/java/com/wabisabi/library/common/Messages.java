package com.wabisabi.library.common;

/**
 * 所有提示訊息集中在這裡（文字來自 BDD.md）。
 * 要改文字只改這一個檔案，其他類別一律寫 Messages.XXX，不要直接寫中文字串。
 */
public final class Messages {

	private Messages() {
		// 只放常數，不需要建立物件
	}

	// ===== 書籍查詢 =====
	public static final String ISBN_REQUIRED = "請輸入 ISBN 碼";
	public static final String ISBN_INVALID = "ISBN 碼格式錯誤或無效";
	public static final String BOOK_NOT_FOUND = "找不到此書籍";
	public static final String BOOK_LOOKUP_FAILED = "查詢失敗，請稍後再試";

	// ===== 登入 / 註冊 =====
	public static final String NICKNAME_REQUIRED = "請輸入暱稱";
	public static final String MOBILE_REQUIRED = "請輸入手機號碼";
	public static final String NICKNAME_INVALID = "暱稱僅限英文、數字、- 與 _";
	public static final String NICKNAME_TOO_LONG = "暱稱長度不可超過 20 個字元";
	public static final String MOBILE_INVALID = "請輸入有效的手機號碼格式 (10碼)";
	public static final String NICKNAME_MOBILE_MISMATCH = "暱稱與手機號碼不符";
	public static final String NICKNAME_TAKEN = "此暱稱已被使用";
	public static final String LOGIN_SUCCESS = "登入成功";
	public static final String LOGOUT_SUCCESS = "已成功登出";
	/** 未登入呼叫需要登入的 API（401） */
	public static final String LOGIN_REQUIRED = "請先註冊/登入";

	// ===== 收藏 =====
	public static final String FAVORITE_ADDED = "已成功加入收藏";
	public static final String FAVORITE_DUPLICATE = "此書籍已在收藏清單中，請勿重複收藏";
	public static final String FAVORITE_REMOVED = "已移除收藏";
	/** 刪除時找不到收藏，或收藏不是自己的（BDD 未定義，計畫階段補充） */
	public static final String FAVORITE_NOT_FOUND = "找不到此收藏";

	// ===== 通用（BDD 未定義，計畫階段補充） =====
	/** 程式沒預料到的錯誤（500） */
	public static final String SERVER_ERROR = "系統發生錯誤，請稍後再試";
	/** 網址不存在、HTTP 方法不對、JSON 格式壞掉等 Spring 自己擋下的請求 */
	public static final String REQUEST_FAILED = "無法處理此請求";
}
