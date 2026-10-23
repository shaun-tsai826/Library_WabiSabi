package com.wabisabi.library;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 需求 9：書籍種類清單（GET /api/categories）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class CategoryApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("規則：種類清單公開，不需要登入，依固定順序回傳 11 種（第一個是「未分類」）")
	void listCategories() throws Exception {
		mockMvc.perform(get("/api/categories")) // 沒有帶 Session = 未登入
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(11))
				.andExpect(jsonPath("$[0]").value("未分類"))
				.andExpect(jsonPath("$[1]").value("小說"))
				.andExpect(jsonPath("$[2]").value("散文"))
				.andExpect(jsonPath("$[3]").value("詩"))
				.andExpect(jsonPath("$[4]").value("人文歷史"))
				.andExpect(jsonPath("$[5]").value("心理勵志"))
				.andExpect(jsonPath("$[6]").value("商業理財"))
				.andExpect(jsonPath("$[7]").value("科學"))
				.andExpect(jsonPath("$[8]").value("電腦資訊"))
				.andExpect(jsonPath("$[9]").value("藝術設計"))
				.andExpect(jsonPath("$[10]").value("其他"));
	}
}
