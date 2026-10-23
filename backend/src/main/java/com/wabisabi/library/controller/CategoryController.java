package com.wabisabi.library.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wabisabi.library.common.Categories;

/**
 * 書籍種類清單 API（公開，不需要登入）。
 * 前端的下拉選單與種類按鈕都從這裡取得清單，種類只在後端定義一份。
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

	/** GET /api/categories → ["未分類", "小說", "散文", ...] */
	@GetMapping
	public List<String> list() {
		return Categories.ALL;
	}
}
