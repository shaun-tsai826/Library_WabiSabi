package com.wabisabi.library.dto;

/**
 * 修改收藏種類時前端送來的資料：{"category": "散文"}
 */
public class UpdateCategoryRequest {

	private String category;

	public UpdateCategoryRequest() {
	}

	public UpdateCategoryRequest(String category) {
		this.category = category;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}
}
