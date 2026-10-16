package com.wabisabi.library.service;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.wabisabi.library.client.GoogleBooksClient;
import com.wabisabi.library.client.GoogleBooksUnavailableException;
import com.wabisabi.library.common.Messages;
import com.wabisabi.library.dto.BookResponse;
import com.wabisabi.library.entity.Book;
import com.wabisabi.library.exception.ApiException;
import com.wabisabi.library.repository.BookRepository;

/**
 * 書籍查詢的規則都在這裡。
 */
@Service
public class BookService {

	private final BookRepository bookRepository;
	private final GoogleBooksClient googleBooksClient;

	public BookService(BookRepository bookRepository, GoogleBooksClient googleBooksClient) {
		this.bookRepository = bookRepository;
		this.googleBooksClient = googleBooksClient;
	}

	/**
	 * 用 ISBN 查書。順序：
	 * 1. 去除前後空白，檢查是不是空的、是不是 13 碼數字
	 * 2. 先查本機 books 表，有就直接回傳
	 * 3. 本機沒有 → 問 Google Books；查到就存回本機再回傳
	 */
	public BookResponse findByIsbn(String rawIsbn) {
		// 1. 格式檢查（正常情況前端已經擋掉空白，這裡是第二道防線）
		String isbn = normalizeIsbn(rawIsbn);

		// 2. 先查本機
		Optional<Book> localBook = bookRepository.findById(isbn);
		if (localBook.isPresent()) {
			return new BookResponse(localBook.get());
		}

		// 3. 本機沒有，問 Google Books
		Optional<Book> googleBook;
		try {
			googleBook = googleBooksClient.findByIsbn(isbn);
		} catch (GoogleBooksUnavailableException e) {
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, Messages.BOOK_LOOKUP_FAILED);
		}
		if (googleBook.isEmpty()) {
			throw new ApiException(HttpStatus.NOT_FOUND, Messages.BOOK_NOT_FOUND);
		}

		// 查到了：存回本機，下次就不用再問 Google
		Book saved = bookRepository.save(googleBook.get());
		return new BookResponse(saved);
	}

	/**
	 * ISBN 格式規則（查書與加入收藏共用）：去除前後空白後，必須是 13 碼數字。
	 *
	 * @return 去除空白後的 ISBN
	 * @throws ApiException 空白或格式錯誤時（400）
	 */
	public static String normalizeIsbn(String rawIsbn) {
		String isbn = (rawIsbn == null) ? "" : rawIsbn.trim();
		if (isbn.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, Messages.ISBN_REQUIRED);
		}
		if (!isbn.matches("[0-9]{13}")) {
			throw new ApiException(HttpStatus.BAD_REQUEST, Messages.ISBN_INVALID);
		}
		return isbn;
	}
}
