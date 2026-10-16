package com.wabisabi.library.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wabisabi.library.dto.BookResponse;
import com.wabisabi.library.service.BookService;

/**
 * 書籍 API（公開，不需要登入）。
 * Controller 只負責「接收請求、呼叫 Service、回傳結果」，規則都寫在 Service。
 */
@RestController
@RequestMapping("/api/books")
public class BookController {

	private final BookService bookService;

	public BookController(BookService bookService) {
		this.bookService = bookService;
	}

	/**
	 * GET /api/books/9789861343722
	 * 成功 200 + 書籍資料；錯誤由 Service 丟出 ApiException，GlobalExceptionHandler 轉成 400/404/503。
	 */
	@GetMapping("/{isbn}")
	public BookResponse findByIsbn(@PathVariable String isbn) {
		return bookService.findByIsbn(isbn);
	}
}
