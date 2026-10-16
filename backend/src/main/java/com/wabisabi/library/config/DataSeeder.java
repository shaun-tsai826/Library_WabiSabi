package com.wabisabi.library.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.wabisabi.library.entity.Book;
import com.wabisabi.library.entity.User;
import com.wabisabi.library.repository.BookRepository;
import com.wabisabi.library.repository.UserRepository;

/**
 * 種子資料：程式啟動完成後自動執行一次，寫入 BDD.md Background 的書籍與會員。
 * 資料已經存在就略過，所以重複啟動也不會重複新增。
 */
@Component
public class DataSeeder implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

	private final BookRepository bookRepository;
	private final UserRepository userRepository;

	/** 建構子注入：Spring 會自動把 Repository 傳進來 */
	public DataSeeder(BookRepository bookRepository, UserRepository userRepository) {
		this.bookRepository = bookRepository;
		this.userRepository = userRepository;
	}

	@Override
	public void run(String... args) {
		addBookIfMissing(new Book("9789861343722", "侘寂：尋找日常中的不完美之美", "雷納·克魯克", "2020-05-01"));
		addBookIfMissing(new Book("9789863980315", "提問的設計", "安齋勇樹", "2021-09-10"));
		addUserIfMissing("Cooqle", "0900123456");
	}

	private void addBookIfMissing(Book book) {
		if (bookRepository.existsById(book.getIsbn())) {
			log.info("種子資料：書籍 {} 已存在，略過", book.getIsbn());
			return;
		}
		bookRepository.save(book);
		log.info("種子資料：新增書籍 {}（{}）", book.getIsbn(), book.getTitle());
	}

	private void addUserIfMissing(String nickname, String mobile) {
		if (userRepository.findByMobile(mobile).isPresent()) {
			log.info("種子資料：會員 {} 已存在，略過", nickname);
			return;
		}
		userRepository.save(new User(nickname, mobile));
		log.info("種子資料：新增會員 {}", nickname);
	}
}
