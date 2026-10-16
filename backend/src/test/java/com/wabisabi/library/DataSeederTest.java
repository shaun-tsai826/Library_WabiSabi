package com.wabisabi.library;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.wabisabi.library.config.DataSeeder;
import com.wabisabi.library.entity.Book;
import com.wabisabi.library.entity.User;
import com.wabisabi.library.repository.BookRepository;
import com.wabisabi.library.repository.UserRepository;

/**
 * 驗證種子資料（對應 BDD.md 的 Background）。
 * 測試啟動時 DataSeeder 已經自動執行過一次，資料寫在 H2 記憶體資料庫。
 */
@SpringBootTest
class DataSeederTest {

	@Autowired
	private DataSeeder dataSeeder;

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	@DisplayName("Background：系統中存在 2 本書籍資料")
	void backgroundBooksExist() {
		Book wabiSabi = bookRepository.findById("9789861343722").orElseThrow();
		assertThat(wabiSabi.getTitle()).isEqualTo("侘寂：尋找日常中的不完美之美");
		assertThat(wabiSabi.getAuthor()).isEqualTo("雷納·克魯克");
		assertThat(wabiSabi.getPublishDate()).isEqualTo("2020-05-01");

		Book question = bookRepository.findById("9789863980315").orElseThrow();
		assertThat(question.getTitle()).isEqualTo("提問的設計");
		assertThat(question.getAuthor()).isEqualTo("安齋勇樹");
		assertThat(question.getPublishDate()).isEqualTo("2021-09-10");
	}

	@Test
	@DisplayName("Background：系統中已存在會員 Cooqle / 0900123456")
	void backgroundUserExists() {
		User cooqle = userRepository.findByMobile("0900123456").orElseThrow();
		assertThat(cooqle.getNickname()).isEqualTo("Cooqle");
		assertThat(cooqle.getNicknameLower()).isEqualTo("cooqle");
		assertThat(cooqle.getCreatedAt()).isNotNull();
	}

	@Test
	@DisplayName("種子資料已存在時再執行一次，不會重複新增")
	void seederDoesNotDuplicate() {
		long booksBefore = bookRepository.count();
		long usersBefore = userRepository.count();

		dataSeeder.run();

		assertThat(bookRepository.count()).isEqualTo(booksBefore);
		assertThat(userRepository.count()).isEqualTo(usersBefore);
	}
}
