package com.wabisabi.library.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wabisabi.library.entity.Favorite;

/**
 * 收藏的資料庫操作。
 * 方法名稱裡的 UserId 指的是「favorite.user.id」，BookIsbn 指的是「favorite.book.isbn」，
 * Spring 會沿著 @ManyToOne 的關係自動找到對應的欄位（user_id、isbn）。
 */
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

	/** 某位會員的所有收藏，依收藏順序（id 由小到大）排列 */
	List<Favorite> findByUserIdOrderByIdAsc(Long userId);

	/** 這位會員是不是已經收藏過這本書 */
	boolean existsByUserIdAndBookIsbn(Long userId, String isbn);

	/** 找「這個 id、而且屬於這位會員」的收藏；不是他的就找不到 */
	Optional<Favorite> findByIdAndUserId(Long id, Long userId);
}
