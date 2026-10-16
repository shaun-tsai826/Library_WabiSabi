package com.wabisabi.library.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wabisabi.library.entity.User;

/**
 * 會員的資料庫操作。
 * 繼承 JpaRepository 後，save、findById、count、delete 等常用方法都自動有了。
 */
public interface UserRepository extends JpaRepository<User, Long> {

	/** 用手機號碼找會員。Spring 看方法名稱就會自動產生 SQL：WHERE mobile = ? */
	Optional<User> findByMobile(String mobile);

	/** 這個小寫暱稱有沒有人用過：WHERE nickname_lower = ?，回傳 true / false */
	boolean existsByNicknameLower(String nicknameLower);
}
