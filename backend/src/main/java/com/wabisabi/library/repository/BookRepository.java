package com.wabisabi.library.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wabisabi.library.entity.Book;

/**
 * 書籍的資料庫操作。主鍵是 ISBN（String），所以第二個型別寫 String。
 */
public interface BookRepository extends JpaRepository<Book, String> {
}
