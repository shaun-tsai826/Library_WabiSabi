package com.wabisabi.library.common;

import java.util.List;

/**
 * 收藏書籍的種類清單（BDD v1.4.0 需求 9）。
 * 整個專案只在這裡定義一份：前端透過 GET /api/categories 取得，後端修改種類時也用它檢查。
 * 要增減種類，只改這個檔案。
 */
public final class Categories {

	private Categories() {
		// 只放常數，不需要建立物件
	}

	/** 預設種類：新加入的收藏、以及資料庫裡種類是空值的收藏，都顯示成它 */
	public static final String DEFAULT = "未分類";

	/** 全部種類，依畫面上顯示的順序排列（List.of 建立的清單不能被修改） */
	public static final List<String> ALL = List.of(
			DEFAULT, "小說", "散文", "詩", "人文歷史", "心理勵志",
			"商業理財", "科學", "電腦資訊", "藝術設計", "其他");

	/** 是不是清單裡的種類 */
	public static boolean isValid(String category) {
		return category != null && ALL.contains(category);
	}

	/** 資料庫的空值轉成「未分類」，其他照原樣 */
	public static String orDefault(String category) {
		return (category == null || category.isBlank()) ? DEFAULT : category;
	}
}
