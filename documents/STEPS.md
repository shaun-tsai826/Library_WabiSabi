# STEPS.md：LIBRARY / 侘寂書閣 開發步驟紀錄

> 對應文件：BDD.md v1.1.0、DESIGN.md v1.1.0、PROMPT.md v1.3.0
> 這份文件記錄每一步「做了什麼、為什麼這樣做、怎麼確認成功」。照著從頭做一遍，就能自己重建整個專案。

---

## 目錄

- [開始之前：整體架構一張圖](#開始之前整體架構一張圖)
- [Spring Boot 4.x 與網路教學（3.x）的差異](#spring-boot-4x-與網路教學3x的差異)
- [步驟 0：建立 .gitignore](#步驟-0建立-gitignore)
- [步驟 1：建立後端專案並連上資料庫](#步驟-1建立後端專案並連上資料庫)
- [步驟 2：建立資料表（Entity）與種子資料](#步驟-2建立資料表entity與種子資料)
- [步驟 3：查書 API、Google Books 與全域例外處理](#步驟-3查書-apigoogle-books-與全域例外處理)
- [步驟 4：登入/註冊、Session 與攔截器](#步驟-4登入註冊session-與攔截器)
- [步驟 5：收藏 API（加入、清單、刪除）](#步驟-5收藏-api加入清單刪除)
- 步驟 6～11：（完成後陸續補上）
- 如何快速上手：（全部完成後補上）

---

## 開始之前：整體架構一張圖

```
 瀏覽器                     Vite 開發伺服器（5173）          Spring Boot（8080）              MySQL
┌──────────┐   打開網頁   ┌──────────────────┐  /api/...  ┌────────────────────┐  JPA  ┌──────────┐
│  Vue 畫面 │ ──────────▶ │ 提供 Vue 網頁     │ ─────────▶ │ Controller          │ ────▶ │ library  │
│          │             │ 把 /api 轉給後端   │  (proxy)   │  → Service          │       │ 資料庫    │
└──────────┘             └──────────────────┘            │  → Repository       │       └──────────┘
                                                          │ GoogleBooksClient ──┼──▶ Google Books API
                                                          └────────────────────┘
```

**生活化比喻：餐廳**
- **前端（Vue）** 是「菜單與點餐畫面」，客人只看得到它。
- **Controller** 是「櫃檯服務生」：接單、把結果端給客人，自己不做菜。
- **Service** 是「廚師」：真正的規則（驗證、判斷能不能登入）都在這裡。
- **Repository** 是「倉庫管理員」：只負責到資料庫拿東西、放東西。
- **Entity** 是「倉庫裡每一格的標籤」：一個 Java 類別對應一張資料表。

這種分層叫 **MVC 分層**。好處是每一層只做一件事，出錯時很容易知道該去哪一層找。

---

## Spring Boot 4.x 與網路教學（3.x）的差異

網路上的教學大多是 Spring Boot 3.x。本專案用 **4.1.1**，遇到下面這些地方，照本專案的寫法即可：

| 項目 | 3.x 教學常見寫法 | 本專案（4.x）寫法 |
|---|---|---|
| Web starter 名稱 | `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| RestClient | 包含在 web starter 裡 | 要另外加 `spring-boot-starter-restclient` |
| 測試 starter | 一個 `spring-boot-starter-test` 全包 | 依功能拆開：`spring-boot-starter-webmvc-test`、`spring-boot-starter-data-jpa-test` |
| 測試中模擬（mock）某個元件 | `@MockBean` | `@MockitoBean`（package：`org.springframework.test.context.bean.override.mockito`） |
| `@AutoConfigureMockMvc` 的 package | `org.springframework.boot.test.autoconfigure.web.servlet` | `org.springframework.boot.webmvc.test.autoconfigure` |
| Jackson（JSON 工具）| Jackson 2：`com.fasterxml.jackson.databind.*` | Jackson 3：`tools.jackson.databind.*`（但註解 `com.fasterxml.jackson.annotation.*` 沒變） |
| `JsonNode` 取出文字 | `node.asText()` | `node.asString()`（Jackson 3 改名） |
| Hibernate | 6.x | 7.x（寫法大致相同） |
| 版本號 | `3.x.x` | `4.1.1`（pom.xml 寫 `4.1.1`，不要寫成 `4.1.1.RELEASE`） |

> 小提醒：如果照網路教學貼了程式碼卻出現「找不到類別」，先檢查是不是上表的 package 名稱不同。

---

## 步驟 0：建立 .gitignore

### 目標
告訴 git「哪些檔案不要記錄」：編譯產生的檔案、Eclipse 設定檔、套件資料夾，以及**最重要的：放資料庫密碼的 `application-local.properties`**。

### 建立的檔案
- `.gitignore`（專案根目錄）

### 完整程式碼

```gitignore
# ===== 後端（Spring Boot / Maven / Eclipse）=====
backend/target/
backend/.classpath
backend/.project
backend/.settings/
backend/.factorypath
backend/.apt_generated/
# 資料庫帳號密碼：只放在自己電腦，絕對不能進 git
backend/src/main/resources/application-local.properties

# ===== 前端（Vue / Vite）=====
frontend/node_modules/
frontend/dist/

# ===== 作業系統與編輯器 =====
.DS_Store
.idea/
.vscode/
*.log
```

### 白話解釋
| 項目 | 為什麼不進 git |
|---|---|
| `backend/target/` | Maven 編譯出來的檔案，隨時可以重新產生 |
| `.classpath`、`.project`、`.settings/` | Eclipse 匯入專案時自動產生的個人設定，每台電腦不一樣 |
| `application-local.properties` | 裡面有資料庫密碼。密碼一旦推上 GitHub，就算之後刪掉，歷史紀錄裡還是查得到 |
| `node_modules/` | 前端套件，動輒上萬個檔案，用 `npm install` 就能裝回來 |
| `dist/` | 前端打包結果，可重新產生 |
| `.DS_Store` | macOS 自動產生的資料夾顯示設定 |

### 對應的 Scenario
無（專案基礎建設）。

### 怎麼驗證成功
步驟 1 建好檔案後，在專案根目錄執行：
```bash
git status --ignored
```
`application-local.properties` 應該出現在「Ignored files」區塊，而**不是**「Untracked files」。

---

## 步驟 1：建立後端專案並連上資料庫

### 目標
1. 用官方工具產生 Spring Boot 4.1.1 專案（含 Maven Wrapper）。
2. 設定好「正式執行連 MySQL、跑測試用 H2」兩套環境。
3. 確認專案能編譯、測試能通過、能在 Eclipse 匯入、能連上 MySQL 啟動。

### 名詞先解釋
- **Maven**：Java 的「採購＋組裝」工具。你在 `pom.xml` 寫下需要哪些套件，它會自動下載並編譯專案。
- **Maven Wrapper（`mvnw`）**：專案附帶的小腳本。電腦**不用先安裝 Maven**，第一次執行時它會自己下載正確版本的 Maven。
- **Starter**：Spring Boot 的「套餐」。例如 `spring-boot-starter-webmvc` 一次幫你帶齊寫 REST API 需要的所有套件，不用一個一個挑。
- **Profile（設定檔）**：同一個程式在不同場合用不同設定。就像手機的「上班模式／睡眠模式」。本專案啟用 `local` 模式，Spring Boot 就會多讀一個 `application-local.properties`。
- **H2**：一個用 Java 寫的小型資料庫，可以只存在記憶體裡。測試時用它，測完資料就消失，不會弄髒你的 MySQL。

### 建立或修改的檔案
```
backend/
├── .gitattributes                         （產生器附帶，讓 mvnw 在各系統換行正確）
├── .mvn/wrapper/maven-wrapper.properties  （Maven Wrapper 設定：要用哪一版 Maven）
├── mvnw、mvnw.cmd                          （Maven Wrapper 腳本：macOS 用 mvnw、Windows 用 mvnw.cmd）
├── pom.xml                                 （修改：套件清單）
└── src/
    ├── main/java/com/wabisabi/library/LibraryApplication.java   （程式進入點）
    ├── main/resources/application.properties                    （修改：共用設定）
    ├── main/resources/application-local.properties.example      （新增：資料庫設定範本）
    ├── test/java/com/wabisabi/library/LibraryApplicationTests.java
    └── test/resources/application.properties                    （新增：測試專用設定）
```

### 1-1 用 start.spring.io 產生專案

到 <https://start.spring.io> 選擇：

| 欄位 | 值 |
|---|---|
| Project | Maven |
| Language | Java |
| Spring Boot | 4.1.1 |
| Group | `com.wabisabi` |
| Artifact | `library` |
| Package name | `com.wabisabi.library` |
| Packaging | Jar |
| Java | 21 |
| Dependencies | Spring Web、Spring Data JPA、MySQL Driver、H2 Database、HTTP Client |

按「GENERATE」下載 zip，解壓縮後把內容放進 `backend/`。
產生器附帶的 `.gitignore` 和 `HELP.md` **不用複製**（根目錄已經有 `.gitignore` 了，一個專案只維護一份比較單純）。

### 1-2 修改 pom.xml

產生器的預設值有三個地方要調整：
1. 版本號 `4.1.1.RELEASE` 改成 `4.1.1`（Maven 中央倉庫上的正式名稱沒有 `.RELEASE`）。
2. H2 從 `runtime` 改成 `test`，並刪掉 `spring-boot-h2console`：H2 只給測試用，正式執行不需要它。
3. 刪掉 `spring-boot-starter-restclient-test`：我們會用 Mockito 直接模擬自己寫的 Google Books 類別，用不到它。

**完整內容：`backend/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<!-- 繼承 Spring Boot 的「家長設定」：所有套件的版本都由它決定，下面就不用自己寫版本號 -->
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>4.1.1</version>
		<relativePath/>
	</parent>

	<groupId>com.wabisabi</groupId>
	<artifactId>library</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>library</name>
	<description>LIBRARY / 侘寂書閣 後端</description>

	<properties>
		<java.version>21</java.version>
	</properties>

	<dependencies>
		<!-- 寫 REST API 用（Spring Boot 3.x 叫 spring-boot-starter-web，4.x 改名為 webmvc） -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc</artifactId>
		</dependency>

		<!-- JPA：用 Java 類別操作資料表，不用自己寫 SQL -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>

		<!-- RestClient：呼叫 Google Books API 用（4.x 獨立成一個 starter） -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-restclient</artifactId>
		</dependency>

		<!-- MySQL 驅動程式：讓 Java 能連上 MySQL（只在執行時需要） -->
		<dependency>
			<groupId>com.mysql</groupId>
			<artifactId>mysql-connector-j</artifactId>
			<scope>runtime</scope>
		</dependency>

		<!-- ===== 以下只在跑測試時使用 ===== -->

		<!-- MockMvc（模擬發送 HTTP 請求）+ JUnit 5 + Mockito -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc-test</artifactId>
			<scope>test</scope>
		</dependency>

		<!-- JPA 相關的測試支援 -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa-test</artifactId>
			<scope>test</scope>
		</dependency>

		<!-- H2 記憶體資料庫：測試時用，測完就消失，不會碰到 MySQL -->
		<dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>

</project>
```

**白話解釋**
- `<parent>`：像加入一個「會員制量販店」，店裡每樣商品（套件）的版本都已經搭配好，彼此不會打架。所以下面的 `<dependency>` 都不用寫版本號。
- `<scope>`：這個套件「什麼時候需要」。
  - 不寫 = 編譯、執行都需要。
  - `runtime` = 寫程式時用不到它的類別，執行時才需要（例如 MySQL 驅動）。
  - `test` = 只有跑測試時才需要，打包正式程式時不會帶進去。
- 沒有 Lombok、沒有 Spring Security、沒有 Bean Validation：照新手友善原則，所有 getter/setter、驗證都手寫，看得見、好除錯。

### 1-3 程式進入點（產生器已建立，不用修改）

**`backend/src/main/java/com/wabisabi/library/LibraryApplication.java`**

```java
package com.wabisabi.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LibraryApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibraryApplication.class, args);
	}

}
```

**白話解釋**
- `main` 方法：Java 程式的「大門」，執行程式就是從這裡進來。
- `@SpringBootApplication`：告訴 Spring「從這個 package（`com.wabisabi.library`）開始，往下找所有我寫的類別，自動組裝起來」。
  所以之後所有類別都要放在 `com.wabisabi.library` 底下（例如 `com.wabisabi.library.controller`），否則 Spring 會找不到。

### 1-4 共用設定：application.properties

**`backend/src/main/resources/application.properties`**

```properties
# ===== 基本設定 =====
spring.application.name=library
# 後端埠號（Vite 開發伺服器會把 /api 轉到這裡）
server.port=8080

# 啟用 local 設定檔：Spring Boot 會額外讀取 application-local.properties
# （資料庫網址、帳號、密碼放在那裡，不進 git）
spring.profiles.active=local

# ===== JPA 設定 =====
# update：啟動時依照 Entity 類別自動建立／補上資料表欄位。
# 注意：僅限練習使用！正式專案應改用 Flyway 等工具管理資料表變更。
spring.jpa.hibernate.ddl-auto=update
# 關閉 open-in-view：資料庫連線只在 Service 裡使用，避免新手踩到隱藏的效能問題
spring.jpa.open-in-view=false
# 想看 JPA 實際執行的 SQL 時，把下面改成 true
spring.jpa.show-sql=false
```

**白話解釋**
- `spring.profiles.active=local`：Spring Boot 會讀 `application.properties`，**再加讀** `application-local.properties`。這樣密碼就能放在另一個不進 git 的檔案裡。
- `ddl-auto=update`：DDL 是「建表、改表」的 SQL。設成 `update` 後，Spring 啟動時會比對你的 Java 類別和資料表，**缺什麼就補什麼**（但不會刪除欄位）。
  ⚠️ **只適合練習**：正式專案改資料表要很謹慎，通常會用 Flyway 這類工具，把每一次變更寫成有編號的 SQL 檔案。
- `open-in-view=false`：預設值 `true` 會讓資料庫連線一路開到畫面輸出完才關，容易藏著效能問題。關掉之後，資料庫操作都要在 Service 裡完成，規則比較清楚。

### 1-5 資料庫設定範本

**`backend/src/main/resources/application-local.properties.example`**（會進 git）

```properties
# 使用方式：
# 1. 複製這個檔案，改名為 application-local.properties（放在同一個資料夾）
# 2. 填入你自己的資料庫連線資訊
# 3. application-local.properties 已列在 .gitignore，不會被提交到 git
#
# 網址範例：jdbc:mysql://localhost:3306/library?serverTimezone=Asia/Taipei
spring.datasource.url=
spring.datasource.username=
spring.datasource.password=
```

**白話解釋**
- `.example` 是「空白表格」，讓別人（或未來的你）知道要填哪些欄位；真正填好的 `application-local.properties` 只留在自己電腦。
- `serverTimezone=Asia/Taipei`：告訴驅動程式資料庫用台灣時間，`created_at` 才不會差 8 小時。

**你要自己做的事（只需要做一次）：**
1. 用 MySQL Workbench 或其他工具建立資料庫和專用帳號（**不要用 root**）。參考 SQL（密碼請自己換掉）：
   ```sql
   CREATE DATABASE library CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'library_app'@'localhost' IDENTIFIED BY '請換成你自己的密碼';
   GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
     ON library.* TO 'library_app'@'localhost';
   ```
   - `utf8mb4`：完整支援中文和 emoji 的字元集。
   - 權限包含 `CREATE`、`ALTER`、`INDEX`、`REFERENCES`，因為 `ddl-auto=update` 要能建表、建索引和外鍵。
2. 把範本複製成 `application-local.properties`，填入網址、帳號、密碼。

### 1-6 測試專用設定

**`backend/src/test/resources/application.properties`**

```properties
# ===== 測試專用設定 =====
# 跑測試時，這個檔案會「蓋掉」src/main/resources/application.properties，
# 而且這裡沒有啟用 local 設定檔，所以測試完全不會讀取 application-local.properties，也不會連 MySQL。

spring.application.name=library-test

# H2 記憶體資料庫：MODE=MySQL 讓語法盡量接近 MySQL；程式結束資料就消失
spring.datasource.url=jdbc:h2:mem:librarytest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
spring.datasource.username=sa
spring.datasource.password=

# 每次測試啟動時重新建表，結束時刪除
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.open-in-view=false
```

**白話解釋**
- 跑測試時，`src/test/resources` 排在 `src/main/resources` 前面。兩邊都有 `application.properties` 時，Spring Boot 只讀**排前面的那一份**，也就是測試版。
- 測試版沒寫 `spring.profiles.active=local`，所以不會讀密碼檔，也不會連 MySQL。就算你的電腦沒有 MySQL，測試照樣能跑。
- `create-drop`：每次測試都從空的資料表開始，結果不會被上一次測試影響。
- `DB_CLOSE_DELAY=-1`：同一次測試過程中，資料庫不會因為暫時沒有連線就被清掉。

### 1-7 在 Eclipse 匯入專案

1. Eclipse 選單：`File` → `Import...` → `Maven` → `Existing Maven Projects` → `Next`。
2. `Root Directory` 選 `Library_WabiSabi/backend`，勾選 `/pom.xml` → `Finish`。
3. 等右下角進度條跑完（第一次會下載套件，需要幾分鐘）。
4. 對專案按右鍵 → `Properties` → `Java Build Path` → `Libraries`，確認 JRE 是 **JavaSE-21**。
5. 執行：對 `LibraryApplication.java` 按右鍵 → `Run As` → `Java Application`（或 `Spring Boot App`）。

> Eclipse 版本要 2023-12（4.30）以上才完整支援 Java 21。本機是 4.38，沒問題。

### 對應的 Scenario
無（專案基礎建設）。這一步完成後，後面所有 Scenario 的測試才有地方跑。

### 怎麼驗證成功

**驗證 1：測試能通過（不需要 MySQL）**
```bash
cd backend
./mvnw test
```
預期結果（2026-10-16 實測）：
```
:: Spring Boot ::                (v4.1.1)
... Starting LibraryApplicationTests using Java 21.0.11
... No active profile set, falling back to 1 default profile: "default"   ← 沒啟用 local，代表沒讀密碼檔 ✅
... Added connection conn0: url=jdbc:h2:mem:librarytest                   ← 連的是 H2 ✅
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```
> 會看到一段 `Mockito is currently self-attaching...` 和 `WARNING: A Java agent has been loaded dynamically` 的警告：這是 Mockito 在新版 JDK 上的提醒，**不影響測試結果**，可以先忽略。

**驗證 2：連上 MySQL 啟動**
建好 `application-local.properties` 後：
```bash
cd backend
./mvnw spring-boot:run
```
預期看到 `The following 1 profile is active: "local"` 和 `Tomcat started on port 8080`，而且程式沒有結束。按 `Ctrl + C` 停止。
（現在還沒有任何 API，所以用瀏覽器開 `http://localhost:8080` 會看到 404 錯誤頁，這是正常的。）

### 常見錯誤

**還沒建立 `application-local.properties` 就啟動**（2026-10-16 實際跑出來的訊息）：
```
The following 1 profile is active: "local"
APPLICATION FAILED TO START
Description:
Failed to configure a DataSource: 'url' attribute is not specified and no embedded datasource could be configured.
```
→ 意思是「找不到資料庫網址」。檢查檔案是否存在、檔名是否拼對（`application-local.properties`，不是 `.example`）、`spring.datasource.url` 是否有填。

**`Access denied for user ...`** → 帳號或密碼錯誤，或帳號沒有 `library` 資料庫的權限。

**`Unknown database 'library'`** → 資料庫還沒建立。

**`Communications link failure`** → MySQL 沒有啟動，或網址的埠號不是 3306。

### 實測結果（2026-10-16）
```
The following 1 profile is active: "local"
Database JDBC URL [jdbc:mysql://localhost:3306/library?serverTimezone=Asia/Taipei]
Database version: 8.0.46
Tomcat started on port 8080 (http) with context path '/'
Started LibraryApplication in 3.16 seconds
```

---

## 步驟 2：建立資料表（Entity）與種子資料

### 目標
1. 寫 3 個 Entity 類別（User、Book、Favorite），讓 JPA 自動在 MySQL 建出 `users`、`books`、`favorites` 三張表。
2. 寫 3 個 Repository，之後就能用 Java 方法存取資料。
3. 寫 DataSeeder，啟動時自動放入 BDD.md Background 的 2 本書和會員 Cooqle。

### 名詞先解釋：JPA 是什麼？

**JPA（Java Persistence API）** 是一套「讓 Java 物件和資料表互相轉換」的規則；**Hibernate** 是實際做這件事的工具；**Spring Data JPA** 再幫你把常用操作包裝好。

**比喻：翻譯員。** 你只會說 Java，資料庫只聽得懂 SQL。JPA 就是站在中間的翻譯員：
- 你說 `userRepository.save(user)` → 翻譯員對資料庫說 `INSERT INTO users ...`
- 你說 `bookRepository.findById("978...")` → 翻譯員說 `SELECT * FROM books WHERE isbn = '978...'`

| 你寫的 Java | JPA 幫你對應到 |
|---|---|
| 一個 `@Entity` 類別 | 一張資料表 |
| 類別裡的一個欄位（field） | 資料表的一個欄位（column） |
| 一個物件（`new Book(...)`） | 資料表的一列（row） |
| `Repository` 介面 | 這張表的「增刪改查」工具 |

### 建立的檔案
```
backend/src/main/java/com/wabisabi/library/
├── entity/
│   ├── User.java           會員 → users 表
│   ├── Book.java           書籍 → books 表
│   └── Favorite.java       收藏 → favorites 表
├── repository/
│   ├── UserRepository.java
│   ├── BookRepository.java
│   └── FavoriteRepository.java
└── config/
    └── DataSeeder.java     種子資料
backend/src/test/java/com/wabisabi/library/
└── DataSeederTest.java     Background 測試
```

### 2-1 會員：User.java

**`backend/src/main/java/com/wabisabi/library/entity/User.java`**

```java
package com.wabisabi.library.entity;

import java.time.LocalDateTime;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 會員：對應資料表 users。
 */
@Entity
@Table(name = "users")
public class User {

	/** 會員編號：資料庫自動遞增（1, 2, 3...） */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 暱稱原樣（例如 Cooqle），用來顯示 */
	@Column(name = "nickname", nullable = false, length = 20)
	private String nickname;

	/** 暱稱小寫（例如 cooqle），用來比對；唯一，所以大小寫不同也算同一個暱稱 */
	@Column(name = "nickname_lower", nullable = false, length = 20, unique = true)
	private String nicknameLower;

	/** 手機號碼：用字串存，開頭的 0 才不會消失；唯一 */
	@Column(name = "mobile", nullable = false, length = 10, unique = true)
	private String mobile;

	/** 建立時間 */
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	/** JPA 規定一定要有一個沒有參數的建構子（它從資料庫讀資料時會用到） */
	public User() {
	}

	/** 建立新會員時用：自動填好小寫暱稱與建立時間 */
	public User(String nickname, String mobile) {
		setNickname(nickname);
		this.mobile = mobile;
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNickname() {
		return nickname;
	}

	/** 設定暱稱時，同時更新小寫版本，兩個欄位永遠一致 */
	public void setNickname(String nickname) {
		this.nickname = nickname;
		this.nicknameLower = nickname.toLowerCase(Locale.ROOT);
	}

	public String getNicknameLower() {
		return nicknameLower;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
```

**白話解釋**
- `@Entity`：貼上這張標籤，JPA 就知道「這個類別要對應到一張資料表」。
- `@Table(name = "users")`：表名叫 `users`。不用 `user`，因為 `user` 在很多資料庫是保留字，容易出錯。
- `@Id` ＋ `@GeneratedValue(strategy = GenerationType.IDENTITY)`：這是主鍵，號碼交給 MySQL 的 `AUTO_INCREMENT` 自動發，就像銀行的號碼牌。
- `@Column(...)`：
  - `name`：資料表裡的欄位名稱。Java 習慣寫 `nicknameLower`（駝峰式），資料庫習慣寫 `nickname_lower`（底線式），這裡明確寫出來，一看就知道對應關係。
  - `nullable = false`：不可以是空值（NOT NULL）。
  - `length = 20`：`VARCHAR(20)`。
  - `unique = true`：整張表不能有重複值。
- **為什麼暱稱要存兩欄？** 規則是「大小寫視為相同」，但畫面要顯示使用者原本輸入的樣子。
  - `nickname`：`Cooqle`（顯示用）
  - `nickname_lower`：`cooqle`（比對用，唯一）
  有人用 `COOQLE` 註冊時，轉小寫後是 `cooqle`，跟現有資料重複，資料庫就會擋下來。
- **`setNickname` 為什麼要同時改兩欄？** 這樣兩欄**永遠不會不一致**。所以刻意不寫 `setNicknameLower`，避免有人只改了其中一欄。
- `Locale.ROOT`：轉小寫時不受電腦語系影響（例如在土耳其語系下，`I` 轉小寫不是 `i`）。
- **為什麼要有空的建構子 `public User() {}`？** JPA 從資料庫讀資料時，會先 `new User()` 再一欄一欄填值，所以一定要有它。
- **手機為什麼用 `String`？** 如果用數字型態，`0900123456` 會變成 `900123456`，開頭的 0 就消失了。

### 2-2 書籍：Book.java

**`backend/src/main/java/com/wabisabi/library/entity/Book.java`**

```java
package com.wabisabi.library.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 書籍：對應資料表 books。
 */
@Entity
@Table(name = "books")
public class Book {

	/** ISBN：13 碼數字，直接當主鍵（每本書的 ISBN 本來就不重複） */
	@Id
	@Column(name = "isbn", length = 13)
	private String isbn;

	@Column(name = "title", nullable = false, length = 255)
	private String title;

	/** 作者：多位作者用「、」連接 */
	@Column(name = "author", length = 255)
	private String author;

	/** 出版日期：用字串，因為 Google Books 可能只給「2020」或「2020-05」 */
	@Column(name = "publish_date", length = 20)
	private String publishDate;

	public Book() {
	}

	public Book(String isbn, String title, String author, String publishDate) {
		this.isbn = isbn;
		this.title = title;
		this.author = author;
		this.publishDate = publishDate;
	}

	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getPublishDate() {
		return publishDate;
	}

	public void setPublishDate(String publishDate) {
		this.publishDate = publishDate;
	}
}
```

**白話解釋**
- ISBN 本身就是全世界唯一的書號，所以直接拿來當主鍵，不另外加流水號。也因為是我們自己指定的值，所以**沒有** `@GeneratedValue`。
- `publishDate` 用 `String` 而不是日期型態：Google Books 有時只回「2020」，日期型態存不下「只有年份」的資料。

### 2-3 收藏：Favorite.java

**`backend/src/main/java/com/wabisabi/library/entity/Favorite.java`**

```java
package com.wabisabi.library.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 收藏：對應資料表 favorites，記錄「哪位會員收藏了哪本書」。
 * 同一位會員不能重複收藏同一本書：(user_id, isbn) 設為唯一鍵。
 */
@Entity
@Table(name = "favorites",
		uniqueConstraints = @UniqueConstraint(name = "uk_favorites_user_isbn", columnNames = { "user_id", "isbn" }))
public class Favorite {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 多筆收藏 → 屬於一位會員。資料表裡存的是 user_id 欄位（外鍵，指向 users.id） */
	@ManyToOne(optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	/** 多筆收藏 → 指向一本書。資料表裡存的是 isbn 欄位（外鍵，指向 books.isbn） */
	@ManyToOne(optional = false)
	@JoinColumn(name = "isbn", nullable = false)
	private Book book;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	public Favorite() {
	}

	/** 新增收藏時用：自動填好建立時間 */
	public Favorite(User user, Book book) {
		this.user = user;
		this.book = book;
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Book getBook() {
		return book;
	}

	public void setBook(Book book) {
		this.book = book;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
```

**白話解釋**
- **外鍵（Foreign Key）是什麼？** 就像借書單上寫的「借書證號碼」，一定要是真的存在的會員。資料庫會幫你檢查：`favorites.user_id` 必須是 `users` 表裡真的有的 id，`favorites.isbn` 必須是 `books` 表裡真的有的書。
- `@ManyToOne`：「多對一」。一位會員可以有**多筆**收藏，但每一筆收藏只屬於**一位**會員。
  - 在 Java 裡，欄位型態是 `User`（整個會員物件），寫 `favorite.getUser().getNickname()` 就能拿到暱稱。
  - 在資料表裡，只存一個 `user_id` 數字。`@JoinColumn(name = "user_id")` 指定這個欄位的名稱。
  - JPA 看到 `@ManyToOne` 會自動建外鍵，這也是我們用它、而不是只存一個 `Long userId` 的原因。
- `@UniqueConstraint(columnNames = { "user_id", "isbn" })`：「會員＋書」這個組合不能重複。Cooqle 可以收藏很多本書，但同一本書只能收藏一次。這是資料庫層級的最後一道防線；步驟 5 的 Service 會先檢查並回傳友善的提示訊息。
- `optional = false`：一筆收藏一定要有會員和書，不可以是空的。

### 2-4 Repository：資料庫的「倉庫管理員」

**`backend/src/main/java/com/wabisabi/library/repository/UserRepository.java`**

```java
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
}
```

**`backend/src/main/java/com/wabisabi/library/repository/BookRepository.java`**

```java
package com.wabisabi.library.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wabisabi.library.entity.Book;

/**
 * 書籍的資料庫操作。主鍵是 ISBN（String），所以第二個型別寫 String。
 */
public interface BookRepository extends JpaRepository<Book, String> {
}
```

**`backend/src/main/java/com/wabisabi/library/repository/FavoriteRepository.java`**

```java
package com.wabisabi.library.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wabisabi.library.entity.Favorite;

/**
 * 收藏的資料庫操作。步驟 5 做收藏功能時再加上查詢方法。
 */
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
}
```

**白話解釋**
- 這三個都是 `interface`（介面），**沒有寫任何實作**，卻能直接使用。因為 Spring Data JPA 在啟動時會自動幫你產生實作類別。
- `JpaRepository<User, Long>`：第一個是「管哪個 Entity」，第二個是「主鍵的型別」。
- 繼承後免費得到的常用方法：

  | 方法 | 作用 |
  |---|---|
  | `save(物件)` | 新增或更新 |
  | `findById(主鍵)` | 用主鍵找一筆，回傳 `Optional` |
  | `existsById(主鍵)` | 這筆存不存在（true/false） |
  | `findAll()` | 全部找出來 |
  | `count()` | 總共幾筆 |
  | `delete(物件)` | 刪除 |

- **用方法名稱查詢**：`findByMobile(String mobile)`，Spring 會把名稱拆開讀成「find（找）By（條件是）Mobile（mobile 欄位）」，自動產生 `WHERE mobile = ?`。名稱一定要跟 Entity 的欄位名稱對得上，拼錯的話啟動時就會報錯。
- **`Optional` 是什麼？** 一個「可能有、也可能是空的盒子」。查不到資料時，不會給你 `null`（容易造成 NullPointerException），而是給一個空盒子：
  ```java
  Optional<User> box = userRepository.findByMobile("0900123456");
  if (box.isPresent()) {          // 盒子裡有東西嗎？
      User user = box.get();      // 拿出來
  }
  ```

### 2-5 種子資料：DataSeeder.java

**`backend/src/main/java/com/wabisabi/library/config/DataSeeder.java`**

```java
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
```

**白話解釋**
- **種子資料**：程式第一次跑起來就要有的資料，像新開的書店要先擺幾本書上架。
- `CommandLineRunner`：Spring Boot 啟動完成後，會自動呼叫所有實作它的類別的 `run` 方法，而且只呼叫一次。
- `@Component`：告訴 Spring「這個類別交給你管理」。Spring 會自動建立它，不用自己 `new`。
- **建構子注入（Dependency Injection）**：我們沒有寫 `new BookRepository()`，而是在建構子參數列出「我需要什麼」，Spring 就會自動遞進來。比喻：去餐廳不用自己帶碗筷，坐下來服務生就會擺好。
- **「不存在才寫入」**：`existsById` / `findByMobile` 先檢查，所以重複啟動 10 次也只會有 2 本書、1 位會員。
- `log.info(...)`：在主控台印出訊息，比 `System.out.println` 多了時間和來源類別，方便除錯。`{}` 是佔位符號，會依序換成後面的參數。

### 2-6 Background 測試：DataSeederTest.java

**`backend/src/test/java/com/wabisabi/library/DataSeederTest.java`**

```java
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
```

**白話解釋**
- `@SpringBootTest`：把整個 Spring 程式啟動起來再測試，跟正式執行幾乎一樣，只是資料庫換成 H2。
- `@Autowired`：在測試類別裡，請 Spring 把需要的元件遞進來（測試類別不能用建構子注入，所以用這個寫法）。
- `@DisplayName`：測試報告上顯示的名字。我們用它標註對應的 BDD Scenario。
- `orElseThrow()`：從 `Optional` 盒子拿東西；盒子是空的就直接讓測試失敗。
- `assertThat(實際值).isEqualTo(預期值)`：「我斷言兩個值相等」，不相等測試就會失敗並印出差異。

### 對應的 Scenario
- **Background**（2 本書＋會員 Cooqle）✅：BDD 每一個 Scenario 都建立在它之上。

### 怎麼驗證成功

**驗證 1：跑測試**
```bash
cd backend
./mvnw test
```
實測結果（2026-10-16）：
```
種子資料：新增書籍 9789861343722（侘寂：尋找日常中的不完美之美）
種子資料：新增書籍 9789863980315（提問的設計）
種子資料：新增會員 Cooqle
種子資料：書籍 9789861343722 已存在，略過     ← 第 3 個測試手動再跑一次 seeder
...
Tests run: 1, Failures: 0, Errors: 0 -- in LibraryApplicationTests
Tests run: 3, Failures: 0, Errors: 0 -- in DataSeederTest
```

**驗證 2：連 MySQL 啟動，看 JPA 建了哪些表**
想看 JPA 實際送出的 SQL，可以暫時打開 show-sql：
```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.jpa.show-sql=true"
```
第 1 次啟動（實測，節錄）：
```
Hibernate: create table books (isbn varchar(13) not null, author varchar(255), publish_date varchar(20), title varchar(255) not null, primary key (isbn)) engine=InnoDB
Hibernate: create table favorites (id bigint not null auto_increment, created_at datetime(6) not null, isbn varchar(13) not null, user_id bigint not null, primary key (id)) engine=InnoDB
Hibernate: create table users (id bigint not null auto_increment, created_at datetime(6) not null, mobile varchar(10) not null, nickname varchar(20) not null, nickname_lower varchar(20) not null, primary key (id)) engine=InnoDB
Hibernate: alter table favorites add constraint uk_favorites_user_isbn unique (user_id, isbn)
Hibernate: alter table users add constraint UK63cf... unique (mobile)
Hibernate: alter table users add constraint UKs5c3... unique (nickname_lower)
Hibernate: alter table favorites add constraint FK3i6p... foreign key (isbn) references books (isbn)
Hibernate: alter table favorites add constraint FKk7du... foreign key (user_id) references users (id)
種子資料：新增書籍 9789861343722（侘寂：尋找日常中的不完美之美）
種子資料：新增書籍 9789863980315（提問的設計）
種子資料：新增會員 Cooqle
```
第 2 次啟動：
```
種子資料：書籍 9789861343722 已存在，略過
種子資料：書籍 9789863980315 已存在，略過
種子資料：會員 Cooqle 已存在，略過
```

**看懂這些 SQL**
- 3 個 `create table`：對應 3 個 Entity，欄位型態都跟 `@Column` 寫的一樣（例如 `mobile varchar(10)`）。
- `unique (...)`：唯一鍵。`@Column(unique = true)` 產生的名稱是亂碼般的 `UK63cf...`，那是 Hibernate 自動取的名字，不影響功能；收藏表用 `@UniqueConstraint` 自己取了名字 `uk_favorites_user_isbn`，比較好認。
- `foreign key ... references ...`：兩個外鍵，由 `@ManyToOne` 產生。
- 第 1 次啟動時，你可能還會看到 `alter table ... drop index ...`：這是 Hibernate 「先刪掉舊的再重建」的習慣，表是新建的就沒東西可刪，**不是錯誤**。

**驗證 3（選做）：用 MySQL Workbench 看資料**
```sql
USE library;
SHOW TABLES;                 -- 應該有 books、favorites、users
SELECT * FROM books;         -- 2 本書，中文正常顯示
SELECT * FROM users;         -- Cooqle / cooqle / 0900123456
```

---

## 步驟 3：查書 API、Google Books 與全域例外處理

### 目標
1. 完成 `GET /api/books/{isbn}`：先查本機，查不到再問 Google Books，查到就存回本機。
2. 建立「所有提示訊息集中一處」的 `Messages`。
3. 建立全域例外處理：任何錯誤都統一回傳 `{"message": "..."}`。
4. 依 BDD 需求 1 的 Scenario 寫 MockMvc 測試，Google Books 用 Mockito 模擬。

### 名詞先解釋

**DTO（Data Transfer Object，資料傳輸物件）**
專門用來「打包要回傳給前端的資料」的類別。比喻：Entity 是倉庫裡的原始貨品，DTO 是「出貨用的包裝盒」。
為什麼不直接回傳 Entity？
- 可以只挑前端需要的欄位（例如會員的 `nickname_lower` 不需要給前端看）。
- 之後改資料表時，前端拿到的 JSON 格式不會跟著亂變。
- 避免 `@ManyToOne` 互相參照造成 JSON 無限循環。

**全域例外處理（Global Exception Handler）**
- **例外（Exception）**：程式執行時發生的「意外狀況」，例如找不到書。Java 用 `throw` 把它「丟出去」。
- 比喻：**百貨公司的客服中心**。各樓層櫃位（Service、Controller）遇到問題，不用各自想辦法跟客人解釋，只要把問題單「丟」出去，就會一路送到客服中心。客服中心用**統一的格式**回覆客人：`{"message": "找不到此書籍"}`。
- 好處：
  1. Service 只要寫一行 `throw new ApiException(HttpStatus.NOT_FOUND, Messages.BOOK_NOT_FOUND);`，不用管 JSON 怎麼組。
  2. 不管哪支 API 出錯，前端拿到的格式都一樣，只要讀 `message` 就能顯示提示。
  3. 沒預料到的錯誤（程式 bug）也會被接住，前端只看到「系統發生錯誤，請稍後再試」，不會看到一大串技術細節；完整錯誤則印在後端主控台給你除錯。

**HTTP 狀態碼**：伺服器回應時附上的「結果代號」。
| 代號 | 意思 | 本專案用在 |
|---|---|---|
| 200 | 成功 | 查到書 |
| 400 | 你送來的資料有問題 | ISBN 格式錯誤 |
| 404 | 找不到 | 找不到此書籍 |
| 503 | 伺服器暫時無法提供服務 | Google Books 無法使用 |

**RestClient**：Spring 內建的「打電話給別的網站」工具。我們用它呼叫 Google Books。

**Mockito／模擬（Mock）**：測試時用「假的替身」取代真的元件。比喻：拍電影的**替身演員**，危險動作（連網、可能失敗、可能被限流）交給替身，劇本由你決定，例如「這次就回答查不到」或「這次假裝斷線」。

### 建立的檔案
```
backend/src/main/java/com/wabisabi/library/
├── common/Messages.java                       所有提示訊息
├── exception/ApiException.java                「預料中的錯誤」
├── exception/GlobalExceptionHandler.java      全域例外處理
├── dto/MessageResponse.java                   {"message": "..."}
├── dto/BookResponse.java                      書籍回應 DTO
├── client/GoogleBooksClient.java              呼叫 Google Books
├── client/GoogleBooksUnavailableException.java Google 無法使用
├── service/BookService.java                   查書規則
└── controller/BookController.java             GET /api/books/{isbn}
backend/src/test/java/com/wabisabi/library/
├── BookApiTest.java                           需求 1 的 Scenario 測試
└── client/GoogleBooksClientTest.java          JSON 轉換規則的單元測試
```

### 一個請求的旅程

```
GET /api/books/9780000000000
   │
   ▼
BookController.findByIsbn()          櫃檯：收單，交給廚師
   │
   ▼
BookService.findByIsbn()             廚師：
   ├─ 去空白、檢查 13 碼數字 ──── 不合格 → throw ApiException(400, ISBN_INVALID)
   ├─ bookRepository.findById() ── 有 → 回傳 BookResponse（200）
   └─ googleBooksClient.findByIsbn()
         ├─ 連不上／429 ─────────── throw ApiException(503, BOOK_LOOKUP_FAILED)
         ├─ 查不到 ──────────────── throw ApiException(404, BOOK_NOT_FOUND)
         └─ 查到 → 存回 books 表 → 回傳 BookResponse（200）
   │
   ▼（如果有 throw）
GlobalExceptionHandler               客服中心：轉成 {"message": "找不到此書籍"} + 404
```

### 3-1 所有提示訊息：Messages.java

**`backend/src/main/java/com/wabisabi/library/common/Messages.java`**

```java
package com.wabisabi.library.common;

/**
 * 所有提示訊息集中在這裡（文字來自 BDD.md）。
 * 要改文字只改這一個檔案，其他類別一律寫 Messages.XXX，不要直接寫中文字串。
 */
public final class Messages {

	private Messages() {
		// 只放常數，不需要建立物件
	}

	// ===== 書籍查詢 =====
	public static final String ISBN_REQUIRED = "請輸入 ISBN 碼";
	public static final String ISBN_INVALID = "ISBN 碼格式錯誤或無效";
	public static final String BOOK_NOT_FOUND = "找不到此書籍";
	public static final String BOOK_LOOKUP_FAILED = "查詢失敗，請稍後再試";

	// ===== 登入 / 註冊 =====
	public static final String NICKNAME_REQUIRED = "請輸入暱稱";
	public static final String MOBILE_REQUIRED = "請輸入手機號碼";
	public static final String NICKNAME_INVALID = "暱稱僅限英文、數字、- 與 _";
	public static final String NICKNAME_TOO_LONG = "暱稱長度不可超過 20 個字元";
	public static final String MOBILE_INVALID = "請輸入有效的手機號碼格式 (10碼)";
	public static final String NICKNAME_MOBILE_MISMATCH = "暱稱與手機號碼不符";
	public static final String NICKNAME_TAKEN = "此暱稱已被使用";
	public static final String LOGIN_SUCCESS = "登入成功";
	public static final String LOGOUT_SUCCESS = "已成功登出";
	/** 未登入呼叫需要登入的 API（401） */
	public static final String LOGIN_REQUIRED = "請先註冊/登入";

	// ===== 收藏 =====
	public static final String FAVORITE_ADDED = "已成功加入收藏";
	public static final String FAVORITE_DUPLICATE = "此書籍已在收藏清單中，請勿重複收藏";
	public static final String FAVORITE_REMOVED = "已移除收藏";
	/** 刪除時找不到收藏，或收藏不是自己的（BDD 未定義，計畫階段補充） */
	public static final String FAVORITE_NOT_FOUND = "找不到此收藏";

	// ===== 通用（BDD 未定義，計畫階段補充） =====
	/** 程式沒預料到的錯誤（500） */
	public static final String SERVER_ERROR = "系統發生錯誤，請稍後再試";
	/** 網址不存在、HTTP 方法不對、JSON 格式壞掉等 Spring 自己擋下的請求 */
	public static final String REQUEST_FAILED = "無法處理此請求";
}
```

**白話解釋**
- `public static final String`：「公開的、屬於類別本身的、不可改的字串」，也就是常數。用法：`Messages.BOOK_NOT_FOUND`。
- `private Messages() {}`：把建構子設成私有，別人就不能 `new Messages()`。這個類別只是「訊息清單」，不需要物件。
- 為什麼要集中？想改文字時只改這裡；Service 裡寫的是有意義的名字（`BOOK_NOT_FOUND`），比直接寫中文更不容易打錯字。
- 這次就把後面步驟會用到的訊息一次列齊，方便一眼看完整份清單。
- 標註「BDD 未定義」的 4 則（`LOGIN_REQUIRED`、`FAVORITE_NOT_FOUND`、`SERVER_ERROR`、`REQUEST_FAILED`）是計畫階段補充的。
- 前端的提示文字之後會集中在 `frontend/src/messages.js`（步驟 6）。

### 3-2 預料中的錯誤：ApiException.java

**`backend/src/main/java/com/wabisabi/library/exception/ApiException.java`**

```java
package com.wabisabi.library.exception;

import org.springframework.http.HttpStatus;

/**
 * 「預料中的錯誤」：例如 ISBN 格式錯誤、找不到書、重複收藏。
 * 丟出時帶上 HTTP 狀態碼和要給使用者看的訊息，
 * GlobalExceptionHandler 會把它轉成 {"message": "..."} 回傳給前端。
 *
 * 用法：throw new ApiException(HttpStatus.NOT_FOUND, Messages.BOOK_NOT_FOUND);
 */
public class ApiException extends RuntimeException {

	private final HttpStatus status;

	public ApiException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}
}
```

**白話解釋**
- `extends RuntimeException`：繼承「執行期例外」。好處是丟出時不用在每個方法上寫 `throws`，程式比較乾淨。
- 它多帶了一個 `status`（HTTP 狀態碼），客服中心就知道要回 400、404 還是 409。
- `super(message)`：把訊息交給父類別保存，之後用 `e.getMessage()` 就能取回。
- 整個專案**只有這一個**自訂錯誤類別給 Controller 使用，新手只要記住它就好。

### 3-3 統一的訊息回應：MessageResponse.java

**`backend/src/main/java/com/wabisabi/library/dto/MessageResponse.java`**

```java
package com.wabisabi.library.dto;

/**
 * 只有一段訊息的回應，轉成 JSON 後長這樣：{"message": "找不到此書籍"}
 * 錯誤回應、登入成功、加入收藏成功等都用它。
 */
public class MessageResponse {

	private String message;

	public MessageResponse() {
	}

	public MessageResponse(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
```

**白話解釋**
- Spring 會用 **Jackson**（內建的 JSON 工具）把物件轉成 JSON。它看的是 **getter**：有 `getMessage()`，JSON 就會有 `"message"` 欄位。
- 所以 DTO 一定要寫 getter，否則 JSON 會是空的 `{}`。

### 3-4 全域例外處理：GlobalExceptionHandler.java

**`backend/src/main/java/com/wabisabi/library/exception/GlobalExceptionHandler.java`**

```java
package com.wabisabi.library.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.wabisabi.library.common.Messages;
import com.wabisabi.library.dto.MessageResponse;

/**
 * 全域例外處理：所有 Controller 丟出來、沒人接住的例外，最後都會來到這裡，
 * 統一轉成 {"message": "..."} 的 JSON 回傳。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/** 1. 我們自己丟的「預料中的錯誤」：狀態碼和訊息照它帶來的回傳 */
	@ExceptionHandler(ApiException.class)
	public ResponseEntity<MessageResponse> handleApiException(ApiException e) {
		return ResponseEntity.status(e.getStatus()).body(new MessageResponse(e.getMessage()));
	}

	/** 2. 請求內容不是正確的 JSON（例如少了括號） */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<MessageResponse> handleBadJson(HttpMessageNotReadableException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(Messages.REQUEST_FAILED));
	}

	/** 3. 其他所有例外 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<MessageResponse> handleOther(Exception e) {
		// Spring 自己擋下的請求（網址不存在 404、HTTP 方法不對 405 等）會帶著狀態碼，照用
		if (e instanceof ErrorResponse errorResponse) {
			HttpStatusCode status = errorResponse.getStatusCode();
			return ResponseEntity.status(status).body(new MessageResponse(Messages.REQUEST_FAILED));
		}
		// 真正沒預料到的錯誤：把完整錯誤印在後端主控台方便除錯，前端只看到通用訊息
		log.error("未預期的錯誤", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse(Messages.SERVER_ERROR));
	}
}
```

**白話解釋**
- `@RestControllerAdvice`：「給所有 Controller 的建議」。貼上它，這個類別就變成全站的客服中心。
- `@ExceptionHandler(ApiException.class)`：「遇到 `ApiException` 就交給這個方法處理」。
- Spring 會挑**最精確**的方法：丟出 `ApiException` 就走第 1 個方法，而不是第 3 個（`Exception` 是所有例外的祖先，最籠統）。
- `ResponseEntity`：可以同時設定「狀態碼」和「內容」的回應。`ResponseEntity.status(404).body(...)` 就是「回 404，內容是這個物件」。
- 第 3 個方法的 `instanceof ErrorResponse`：網址打錯（404）、用錯 HTTP 方法（405）這類錯誤是 Spring 自己擋下的，它們本身就帶著正確的狀態碼，直接沿用就好，不要誤報成 500。
- `log.error("未預期的錯誤", e)`：把完整的錯誤堆疊印在後端主控台。**前端只看到通用訊息，細節留給開發者看**，這也是安全上的好習慣。

### 3-5 書籍回應 DTO：BookResponse.java

**`backend/src/main/java/com/wabisabi/library/dto/BookResponse.java`**

```java
package com.wabisabi.library.dto;

import com.wabisabi.library.entity.Book;

/**
 * 回傳給前端的書籍資料。轉成 JSON 後長這樣：
 * {"isbn": "9789861343722", "title": "...", "author": "...", "publishDate": "2020-05-01"}
 */
public class BookResponse {

	private String isbn;
	private String title;
	private String author;
	private String publishDate;

	public BookResponse() {
	}

	/** 從 Entity 複製資料過來 */
	public BookResponse(Book book) {
		this.isbn = book.getIsbn();
		this.title = book.getTitle();
		this.author = book.getAuthor();
		this.publishDate = book.getPublishDate();
	}

	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getPublishDate() {
		return publishDate;
	}

	public void setPublishDate(String publishDate) {
		this.publishDate = publishDate;
	}
}
```

**白話解釋**
- `BookResponse(Book book)`：從 Entity「抄」一份資料到包裝盒。Controller 回傳它，Jackson 轉成 JSON：
  `{"isbn":"9789861343722","title":"侘寂：尋找日常中的不完美之美","author":"雷納·克魯克","publishDate":"2020-05-01"}`

### 3-6 Google 無法使用：GoogleBooksUnavailableException.java

**`backend/src/main/java/com/wabisabi/library/client/GoogleBooksUnavailableException.java`**

```java
package com.wabisabi.library.client;

/**
 * Google Books 無法使用：連不上、逾時、回傳錯誤狀態碼（例如 429 查詢太頻繁）或回傳內容看不懂。
 * BookService 接到它之後，會回給前端「查詢失敗，請稍後再試」（503）。
 */
public class GoogleBooksUnavailableException extends RuntimeException {

	public GoogleBooksUnavailableException(Throwable cause) {
		super("Google Books 無法使用", cause);
	}
}
```

**白話解釋**
- 為什麼不在 GoogleBooksClient 裡直接丟 `ApiException(503)`？因為 Client 的工作只是「跟 Google 講電話」，不該決定要回給使用者什麼訊息，那是 Service 的工作。分清楚之後，測試時也能精準模擬「Google 斷線」的情境。

### 3-7 呼叫 Google Books：GoogleBooksClient.java

**`backend/src/main/java/com/wabisabi/library/client/GoogleBooksClient.java`**

```java
package com.wabisabi.library.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.wabisabi.library.entity.Book;

import tools.jackson.databind.JsonNode;

/**
 * 呼叫 Google Books API，用 ISBN 查書。
 * 不使用 API 金鑰：匿名呼叫和全世界的匿名使用者共用每日額度，額度用完時 Google 會回 429，
 * 這時我們回給前端「查詢失敗，請稍後再試」。
 *
 * 呼叫的網址：https://www.googleapis.com/books/v1/volumes?q=isbn:9789861343722
 */
@Component
public class GoogleBooksClient {

	private static final Logger log = LoggerFactory.getLogger(GoogleBooksClient.class);

	private final RestClient restClient;

	public GoogleBooksClient(RestClient.Builder builder) {
		// 設定逾時：Google 連不上時最多等幾秒就放棄，不會讓使用者一直卡在「查詢中...」
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(3));
		requestFactory.setReadTimeout(Duration.ofSeconds(5));

		this.restClient = builder
				.baseUrl("https://www.googleapis.com/books/v1")
				.requestFactory(requestFactory)
				.build();
	}

	/**
	 * 用 ISBN 查書。
	 *
	 * @return 查到 → 裝著 Book 的 Optional；Google 沒有這本書 → 空的 Optional
	 * @throws GoogleBooksUnavailableException Google 無法使用時
	 */
	public Optional<Book> findByIsbn(String isbn) {
		JsonNode root;
		try {
			root = restClient.get()
					.uri("/volumes?q=isbn:{isbn}", isbn)
					.retrieve()
					.body(JsonNode.class);
		} catch (RestClientException e) {
			// 把原因印在後端主控台（例如 429 Too Many Requests、Connect timed out），方便除錯
			log.warn("Google Books 查詢失敗（ISBN {}）：{}", isbn, e.getMessage());
			throw new GoogleBooksUnavailableException(e);
		}
		return toBook(isbn, root);
	}

	/**
	 * 把 Google 回傳的 JSON 轉成 Book。（package 層級可見，方便單元測試直接呼叫）
	 *
	 * Google 回傳的 JSON 大概長這樣（只列出我們用到的部分）：
	 * {
	 *   "totalItems": 1,
	 *   "items": [
	 *     { "volumeInfo": { "title": "書名", "subtitle": "副標題",
	 *                       "authors": ["作者甲", "作者乙"], "publishedDate": "2020-05" } }
	 *   ]
	 * }
	 */
	static Optional<Book> toBook(String isbn, JsonNode root) {
		if (root == null) {
			return Optional.empty();
		}
		JsonNode items = root.path("items");
		if (!items.isArray() || items.size() == 0) {
			return Optional.empty(); // Google 也查不到
		}

		JsonNode info = items.path(0).path("volumeInfo");

		// 書名只取 title，不含副標題（subtitle）
		String title = text(info.path("title"));
		if (title.isEmpty()) {
			return Optional.empty(); // 沒有書名的資料不可靠，當作查不到
		}

		// 多位作者用「、」連接
		List<String> authors = new ArrayList<>();
		for (JsonNode author : info.path("authors")) {
			String name = text(author);
			if (!name.isEmpty()) {
				authors.add(name);
			}
		}

		// 出版日期照原樣（可能是 "2020"、"2020-05" 或 "2020-05-01"）
		String publishDate = text(info.path("publishedDate"));

		// 長度不超過資料表欄位上限，避免存檔失敗
		return Optional.of(new Book(isbn, limit(title, 255), limit(String.join("、", authors), 255),
				limit(publishDate, 20)));
	}

	/** 超過長度就截斷 */
	private static String limit(String value, int maxLength) {
		return value.length() > maxLength ? value.substring(0, maxLength) : value;
	}

	/** 取出文字；欄位不存在或是 null 時回傳空字串 */
	private static String text(JsonNode node) {
		if (node.isMissingNode() || node.isNull()) {
			return "";
		}
		return node.asString().trim();
	}
}
```

**白話解釋**
- `RestClient.Builder builder`：Spring Boot 已經準備好的 RestClient「組裝器」（內建 Jackson 轉換功能），我們只要設定網址和逾時再 `build()`。
- **逾時（timeout）**：
  - `setConnectTimeout(3 秒)`：撥電話 3 秒沒人接就掛斷。
  - `setReadTimeout(5 秒)`：接通了但對方 5 秒都不說話也掛斷。
  沒設定的話，網路異常時使用者可能要等很久。
- `.uri("/volumes?q=isbn:{isbn}", isbn)`：`{isbn}` 是佔位符號，會換成實際的 ISBN，並自動處理特殊字元。
- `.retrieve().body(JsonNode.class)`：送出請求，把回傳的 JSON 讀成 `JsonNode`（一棵可以一層一層往下走的樹）。
- **`retrieve()` 遇到 4xx/5xx 會自動丟出 `RestClientException`**；連不上、逾時也是丟這個。所以只要接住它，就能處理所有「Google 無法使用」的情況，包括 **429 Too Many Requests**（查詢太頻繁／額度用完）。
- `JsonNode.path("items")`：往下走一層。欄位不存在時**不會**回傳 `null`，而是回傳一個「MissingNode」（空節點），所以可以放心一直 `.path().path()` 往下走，不會出現 NullPointerException。
- `asString()`：取出文字。⚠️ **Jackson 3 的新名字**；Jackson 2 的教學寫 `asText()`（3.x 仍可用但已不建議）。
- `import tools.jackson.databind.JsonNode;`：⚠️ **Jackson 3 的 package 是 `tools.jackson`**，Jackson 2 是 `com.fasterxml.jackson`。
- `toBook` 刻意寫成 `static` 而且不加 `private`（package 層級可見），同一個 package 的單元測試才能直接餵假 JSON 給它。
- `limit(...)`：Google 偶爾會有超長的書名或作者清單，截斷到欄位上限，避免存檔失敗。

### 3-8 查書規則：BookService.java

**`backend/src/main/java/com/wabisabi/library/service/BookService.java`**

```java
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
		String isbn = (rawIsbn == null) ? "" : rawIsbn.trim();

		// 1. 格式檢查（正常情況前端已經擋掉空白，這裡是第二道防線）
		if (isbn.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, Messages.ISBN_REQUIRED);
		}
		if (!isbn.matches("[0-9]{13}")) {
			throw new ApiException(HttpStatus.BAD_REQUEST, Messages.ISBN_INVALID);
		}

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
}
```

**白話解釋**
- `@Service`：跟 `@Component` 一樣讓 Spring 管理，只是名字更清楚地表示「這是放規則的地方」。
- `isbn.matches("[0-9]{13}")`：**正規表示式（Regular Expression）**，用來描述「字串長什麼樣子」。
  - `[0-9]`：一個 0～9 的數字
  - `{13}`：剛好 13 個
  - `matches` 要求**整個字串**都符合，所以 `978986134372`（12 碼）和 `INVALID_ISBN` 都不會通過。
- 檢查順序就是設計決策 1、2 的順序：格式 → 本機 → Google。
- `try { ... } catch (GoogleBooksUnavailableException e) { throw new ApiException(503, ...); }`：把「Google 出問題」翻譯成「給使用者看的訊息」。
- `bookRepository.save(...)`：存回本機。下次查同一本書就不用再問 Google，也比較不會碰到 Google 的次數限制。

### 3-9 書籍 API：BookController.java

**`backend/src/main/java/com/wabisabi/library/controller/BookController.java`**

```java
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
```

**白話解釋**
- `@RestController`：這個類別的方法回傳值會自動轉成 JSON。
- `@RequestMapping("/api/books")`：這個類別所有 API 的網址都以 `/api/books` 開頭。加 `/api` 是為了不要跟前端頁面網址（`/favorites`、`/profile`）撞名。
- `@GetMapping("/{isbn}")` ＋ `@PathVariable String isbn`：網址 `/api/books/9789861343722` 的最後一段會被拿出來，放進 `isbn` 參數。
- Controller 裡**沒有任何 if**：它只負責轉交，所有規則都在 Service。這就是分層的精神。

### 3-10 測試：BookApiTest.java

**`backend/src/test/java/com/wabisabi/library/BookApiTest.java`**

```java
package com.wabisabi.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.wabisabi.library.client.GoogleBooksClient;
import com.wabisabi.library.client.GoogleBooksUnavailableException;
import com.wabisabi.library.entity.Book;
import com.wabisabi.library.repository.BookRepository;

/**
 * 需求 1：首頁查詢書籍（GET /api/books/{isbn}）。
 * Google Books 用 Mockito 模擬（@MockitoBean），測試時不會真的連網。
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookApiTest {

	/** Google Books 有收錄、但本機沒有的測試用 ISBN */
	private static final String GOOGLE_ONLY_ISBN = "9780306406157";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BookRepository bookRepository;

	/** 用假的 GoogleBooksClient 取代真的，可以自由決定它要回傳什麼 */
	@MockitoBean
	private GoogleBooksClient googleBooksClient;

	@BeforeEach
	void setUp() {
		// 每個測試開始前，確保這本書不在本機（避免被上一個測試影響）
		bookRepository.deleteById(GOOGLE_ONLY_ISBN);
	}

	@Test
	@DisplayName("Scenario: 成功查詢到存在的書籍資訊")
	void findExistingBook() throws Exception {
		mockMvc.perform(get("/api/books/{isbn}", "9789861343722"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isbn").value("9789861343722"))
				.andExpect(jsonPath("$.title").value("侘寂：尋找日常中的不完美之美"))
				.andExpect(jsonPath("$.author").value("雷納·克魯克"))
				.andExpect(jsonPath("$.publishDate").value("2020-05-01"));

		// 本機就有，不應該去問 Google
		verify(googleBooksClient, never()).findByIsbn(anyString());
	}

	@Test
	@DisplayName("Scenario: 本機沒有但 Google Books 有收錄的書籍，可查詢並存回本機（出版日期只有年，照原樣顯示）")
	void findBookFromGoogleAndSave() throws Exception {
		when(googleBooksClient.findByIsbn(GOOGLE_ONLY_ISBN))
				.thenReturn(Optional.of(new Book(GOOGLE_ONLY_ISBN, "Google 測試書", "作者甲、作者乙", "2004")));

		mockMvc.perform(get("/api/books/{isbn}", GOOGLE_ONLY_ISBN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Google 測試書"))
				.andExpect(jsonPath("$.author").value("作者甲、作者乙"))
				.andExpect(jsonPath("$.publishDate").value("2004"));

		// 應該已經存進本機 books 表
		Book saved = bookRepository.findById(GOOGLE_ONLY_ISBN).orElseThrow();
		assertThat(saved.getTitle()).isEqualTo("Google 測試書");
		assertThat(saved.getPublishDate()).isEqualTo("2004");
	}

	@Test
	@DisplayName("Scenario: 本機沒有且 Google Books 無法連線時顯示錯誤")
	void googleUnavailable() throws Exception {
		when(googleBooksClient.findByIsbn("9780000000001"))
				.thenThrow(new GoogleBooksUnavailableException(new RuntimeException("模擬連線失敗")));

		mockMvc.perform(get("/api/books/{isbn}", "9780000000001"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.message").value("查詢失敗，請稍後再試"))
				.andExpect(jsonPath("$.title").doesNotExist());

		assertThat(bookRepository.existsById("9780000000001")).isFalse();
	}

	@Test
	@DisplayName("Scenario Outline: 輸入無效或不存在的 ISBN 碼 | 9780000000000 → 找不到此書籍")
	void bookNotFound() throws Exception {
		when(googleBooksClient.findByIsbn("9780000000000")).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/books/{isbn}", "9780000000000"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("找不到此書籍"))
				.andExpect(jsonPath("$.title").doesNotExist());
	}

	@Test
	@DisplayName("Scenario Outline: 輸入無效或不存在的 ISBN 碼 | INVALID_ISBN → ISBN 碼格式錯誤或無效")
	void invalidIsbnLetters() throws Exception {
		mockMvc.perform(get("/api/books/{isbn}", "INVALID_ISBN"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("ISBN 碼格式錯誤或無效"))
				.andExpect(jsonPath("$.title").doesNotExist());

		// 格式錯誤就不該去問 Google
		verify(googleBooksClient, never()).findByIsbn(anyString());
	}

	@Test
	@DisplayName("Scenario Outline: 輸入無效或不存在的 ISBN 碼 | 978986134372（12 碼）→ ISBN 碼格式錯誤或無效")
	void invalidIsbnTwelveDigits() throws Exception {
		mockMvc.perform(get("/api/books/{isbn}", "978986134372"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("ISBN 碼格式錯誤或無效"));
	}

	@Test
	@DisplayName("Scenario Outline: 輸入無效或不存在的 ISBN 碼 | 空白 → 請輸入 ISBN 碼（後端第二道防線，正常由前端擋下）")
	void blankIsbn() throws Exception {
		mockMvc.perform(get("/api/books/{isbn}", "   "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("請輸入 ISBN 碼"));
	}

	@Test
	@DisplayName("規則：ISBN 前後空白會先去除")
	void isbnIsTrimmed() throws Exception {
		mockMvc.perform(get("/api/books/{isbn}", "  9789861343722  "))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isbn").value("9789861343722"));
	}
}
```

**白話解釋**
- **MockMvc**：「模擬的瀏覽器」。不用真的啟動伺服器，就能發送 HTTP 請求並檢查回應。
- `@AutoConfigureMockMvc`：請 Spring 準備好 MockMvc。⚠️ **4.x 的 package 是 `org.springframework.boot.webmvc.test.autoconfigure`**。
- `@MockitoBean`：用替身取代 Spring 裡真正的 `GoogleBooksClient`。⚠️ **3.x 教學寫 `@MockBean`，4.x 已移除，要改用 `@MockitoBean`**。
- `when(替身.方法(參數)).thenReturn(結果)`：寫劇本，「有人這樣問你，你就這樣回答」。
- `thenThrow(...)`：劇本改成「有人問你，你就假裝斷線」。
- `verify(替身, never()).findByIsbn(anyString())`：事後檢查「替身完全沒被呼叫過」。用來證明本機有書時真的沒去問 Google。
- `jsonPath("$.title")`：從回應 JSON 取出 `title` 欄位。`$` 代表整個 JSON 的最外層。
- `doesNotExist()`：確認回應**沒有**書籍資料，對應 BDD 的「頁面不應顯示任何書籍詳細資訊」。
- `@BeforeEach`：每個測試開始前都會執行一次。先把測試用的書刪掉，每個測試就都從同樣的狀態開始。
- `@DisplayName`：測試報告上的名字，標註對應的 BDD Scenario。

### 3-11 測試：GoogleBooksClientTest.java

**`backend/src/test/java/com/wabisabi/library/client/GoogleBooksClientTest.java`**

```java
package com.wabisabi.library.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.wabisabi.library.entity.Book;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 單元測試：檢查「Google 回傳的 JSON → Book」的轉換規則（設計決策 12）。
 * 不啟動 Spring、不連網，直接餵假的 JSON 字串給 toBook。
 */
class GoogleBooksClientTest {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	private Optional<Book> parse(String json) {
		JsonNode root = jsonMapper.readTree(json);
		return GoogleBooksClient.toBook("9780306406157", root);
	}

	@Test
	@DisplayName("書名只取 title，多位作者用「、」連接，出版日期照原樣")
	void mapsFields() {
		Optional<Book> book = parse("""
				{"totalItems": 1, "items": [{"volumeInfo": {
				  "title": "主書名", "subtitle": "副標題",
				  "authors": ["作者甲", "作者乙", "作者丙"],
				  "publishedDate": "2020-05"
				}}]}
				""");

		assertThat(book).isPresent();
		assertThat(book.get().getIsbn()).isEqualTo("9780306406157");
		assertThat(book.get().getTitle()).isEqualTo("主書名");
		assertThat(book.get().getAuthor()).isEqualTo("作者甲、作者乙、作者丙");
		assertThat(book.get().getPublishDate()).isEqualTo("2020-05");
	}

	@Test
	@DisplayName("出版日期只有年份時照原樣")
	void yearOnlyDate() {
		Optional<Book> book = parse("""
				{"items": [{"volumeInfo": {"title": "書", "authors": ["某人"], "publishedDate": "2004"}}]}
				""");
		assertThat(book.get().getPublishDate()).isEqualTo("2004");
	}

	@Test
	@DisplayName("沒有作者或出版日期時存空字串")
	void missingAuthorAndDate() {
		Optional<Book> book = parse("""
				{"items": [{"volumeInfo": {"title": "只有書名"}}]}
				""");
		assertThat(book.get().getAuthor()).isEmpty();
		assertThat(book.get().getPublishDate()).isEmpty();
	}

	@Test
	@DisplayName("Google 查無資料（totalItems 0、沒有 items）→ 查不到")
	void noItems() {
		assertThat(parse("""
				{"kind": "books#volumes", "totalItems": 0}
				""")).isEmpty();
	}

	@Test
	@DisplayName("沒有書名 → 當作查不到")
	void missingTitle() {
		assertThat(parse("""
				{"items": [{"volumeInfo": {"authors": ["某人"]}}]}
				""")).isEmpty();
	}
}
```

**白話解釋**
- 這是**單元測試**：沒有 `@SpringBootTest`，不啟動 Spring、不連網，只測 `toBook` 這一個方法，所以跑得非常快（0.02 秒）。
- 為什麼需要它？`BookApiTest` 裡的 GoogleBooksClient 整個被替身取代，「JSON → Book」的轉換規則完全沒被測到，所以另外補上。
- `"""` ... `"""`：Java 的**文字區塊（Text Block）**，可以直接寫多行字串，不用一直加 `\n` 和 `+`。
- 放在 `com.wabisabi.library.client` package，才能呼叫 package 層級可見的 `toBook`。

### 對應的 Scenario

| BDD Scenario | 狀態 | 測試方法 |
|---|---|---|
| 成功查詢到存在的書籍資訊 | ✅ 後端通過 | `findExistingBook` |
| 本機沒有但 Google Books 有收錄的書籍，可查詢並存回本機 | ✅ 後端通過（Mock） | `findBookFromGoogleAndSave`、`GoogleBooksClientTest` |
| 本機沒有且 Google Books 無法連線時顯示錯誤 | ✅ 後端通過 | `googleUnavailable` |
| 輸入無效或不存在的 ISBN 碼：9780000000000 | ✅ 後端通過（Mock） | `bookNotFound` |
| 輸入無效或不存在的 ISBN 碼：INVALID_ISBN | ✅ 後端通過 | `invalidIsbnLetters` |
| 輸入無效或不存在的 ISBN 碼：978986134372 | ✅ 後端通過 | `invalidIsbnTwelveDigits` |
| 輸入無效或不存在的 ISBN 碼：空白 | 🔸 由前端處理（步驟 7）；後端有第二道防線 | `blankIsbn` |

「頁面應顯示…」「顯示加入收藏按鈕」這些畫面部分，會在步驟 7 做首頁時一起驗證。

### 怎麼驗證成功

**驗證 1：跑測試**
```bash
cd backend
./mvnw test
```
實測結果（2026-10-16）：
```
Tests run: 8, Failures: 0, Errors: 0 -- in BookApiTest
Tests run: 5, Failures: 0, Errors: 0 -- in GoogleBooksClientTest
Tests run: 3, Failures: 0, Errors: 0 -- in DataSeederTest
Tests run: 1, Failures: 0, Errors: 0 -- in LibraryApplicationTests
```

**驗證 2：啟動程式，用 curl 實際呼叫**
先啟動後端：
```bash
cd backend
./mvnw spring-boot:run
```
再開另一個終端機視窗：
```bash
curl http://localhost:8080/api/books/9789861343722
```
`curl` 是命令列版的「瀏覽器」，可以直接送請求給 API。（也可以直接用瀏覽器打開這個網址。）

實測結果（2026-10-16，連 MySQL）：

| 請求 | 回應 | 說明 |
|---|---|---|
| `/api/books/9789861343722` | 200 `{"author":"雷納·克魯克","isbn":"9789861343722","publishDate":"2020-05-01","title":"侘寂：尋找日常中的不完美之美"}` | 中文從 MySQL 讀出來正常 ✅ |
| `/api/books/%209789863980315%20`（前後有空白） | 200 `提問的設計` | 空白有去掉 ✅ |
| `/api/books/INVALID_ISBN` | 400 `{"message":"ISBN 碼格式錯誤或無效"}` | ✅ |
| `/api/books/978986134372` | 400 `{"message":"ISBN 碼格式錯誤或無效"}` | ✅ |
| `/api/nothing` | 404 `{"message":"無法處理此請求"}` | 全域例外處理接住 Spring 的 404 ✅ |
| `POST /api/books/9789861343722` | 405 `{"message":"無法處理此請求"}` | 全域例外處理接住 405 ✅ |
| `/api/books/9780134685991`（本機沒有） | 503 `{"message":"查詢失敗，請稍後再試"}` | 見下方說明 |

**關於 503：Google 的匿名額度用完了**
實測當天呼叫 Google Books 一律收到 503。後端主控台印出原因：
```
WARN ... GoogleBooksClient : Google Books 查詢失敗（ISBN 9780134685991）：429 Too Many Requests:
  "Quota exceeded for quota metric 'Queries' and limit 'Queries per day' ..."
```
意思是：**不帶 API 金鑰的呼叫，是跟全世界所有匿名使用者共用同一份每日額度**，額度用完時 Google 就回 429。
我們的程式把它轉成「查詢失敗，請稍後再試」，這正是設計上的行為。但「Google 真的查到書並存回本機」這條路徑，當天無法用真實網路驗證，目前只靠 Mock 測試保證。

怎麼自己確認是不是額度問題：
```bash
curl "https://www.googleapis.com/books/v1/volumes?q=isbn:9780134685991"
```
- 回傳書籍資料 → 額度恢復了，可以重新測試我們的 API。
- 回傳 `"code": 429` → 額度還沒恢復（通常隔天會重置）。

> 如果之後想穩定使用，可以申請免費的 Google Books API 金鑰，金鑰會有自己的額度。這會改變設計決策 12（目前規定不使用金鑰），要改之前請先討論。

---

## 步驟 4：登入/註冊、Session 與攔截器

### 目標
1. `POST /api/auth/login`：登入/註冊合一，依 BDD 的順序驗證。
2. 登入成功後用 **Session** 記住「你是誰」。
3. 寫一個 **攔截器（LoginInterceptor）**，擋下沒登入就想用收藏、個人資料、登出 API 的請求，回 401。
4. `GET /api/auth/me`（個人頁、判斷登入狀態用）和 `POST /api/auth/logout`。

### ⚠️ 先說清楚這個設計的限制
- **沒有密碼**：只要知道某人的暱稱和手機就能登入他的帳號。這是作業的簡化設計，**真實網站絕對不能這樣做**。
- **沒有 CSRF 防護**：CSRF（跨站請求偽造）是指壞網站趁你登入中，偷偷用你的身分送請求（例如在它的頁面藏一個表單，把你的收藏刪掉）。Spring Security 內建防護，但本專案為了簡單沒有用。**此做法僅適合練習與作業，正式上線需補強**（例如改用 Spring Security，或設定 Cookie 的 `SameSite` 屬性並加上 CSRF Token）。

### 名詞先解釋：Session 是什麼？

**問題：HTTP 是「健忘」的。** 瀏覽器每送一次請求，伺服器都當作第一次見面。你剛登入完，下一個請求伺服器就不記得你了。

**解法：Session（工作階段）。比喻：健身房的置物櫃手環。**
1. 你第一次到櫃台報到（登入）。櫃台確認身分後，在後面的櫃子開一格（**Session**），把你的會員編號放進去，再給你一個寫著櫃號的手環（**Session ID**）。
2. 手環就是瀏覽器的 **Cookie**：`JSESSIONID=8F3A...`。之後瀏覽器每次送請求都會**自動**戴著它。
3. 你要進重訓區（呼叫收藏 API）時，門口的人看一下手環號碼，打開那格櫃子，就知道「這是會員 1 號」。
4. 離開時歸還手環（**登出**），櫃台把那格清空（`session.invalidate()`）。舊手環號碼就沒用了。

**對應到程式碼：**
| 健身房 | 程式 |
|---|---|
| 開一格櫃子 | `request.getSession()` |
| 放會員編號進去 | `session.setAttribute("loginUserId", 1L)` |
| 從櫃子拿出會員編號 | `session.getAttribute("loginUserId")` |
| 手環 | Cookie `JSESSIONID`（Tomcat 自動處理，我們不用寫） |
| 清空櫃子 | `session.invalidate()` |

**重點：櫃子在伺服器那邊，瀏覽器只拿到號碼。** 所以使用者沒辦法自己改成「我是會員 2 號」。這也是為什麼 Controller **一律從 Session 取會員 id，絕不相信前端傳來的 user_id**：前端送來的任何東西都可能被竄改。

**為什麼登入時要換 Session ID（`request.changeSessionId()`）？**
防止「**Session 固定攻擊**」。比喻：壞人先去櫃台領了一個手環，想辦法塞給你戴上；你戴著它登入，櫃子裡就放了你的會員編號，壞人拿著同號碼的手環也能進去。登入成功時**換一個新號碼**，壞人手上的舊號碼就失效了。

### 名詞先解釋：攔截器（Interceptor）是什麼？

**比喻：大樓門口的警衛。** 每個請求在走進 Controller（辦公室）之前，都要先經過警衛。警衛只問一件事：「你的 Session 裡有會員編號嗎？」
- 有 → 放行（`return true`），請求繼續走到 Controller。
- 沒有 → 擋下來，直接回 401「請先註冊/登入」，Controller 完全不會被執行。

**為什麼不在每個 Controller 方法裡自己檢查？** 收藏有 3 支 API、加上 me 和 logout 共 5 支。每支都寫一樣的檢查，很容易漏掉一支，漏掉的那支就成了安全漏洞。交給一個警衛統一把關，**規則只寫一次**。

```
請求 ──▶ LoginInterceptor（警衛）──▶ Controller ──▶ Service ──▶ Repository
            │
            └─ 沒登入 → throw ApiException(401) ──▶ GlobalExceptionHandler ──▶ {"message":"請先註冊/登入"}
```

**401 和 403 的差別**
- 401 Unauthorized：「我不知道你是誰」，也就是**沒登入**。
- 403 Forbidden：「我知道你是誰，但你沒有權限」。

本專案未登入一律回 401。前端收到 401 時只會顯示提示，**後端不會把你導向任何登入頁**：API 只回資料，換頁是前端的工作。

### 建立或修改的檔案
```
backend/src/main/java/com/wabisabi/library/
├── common/SessionKeys.java          新增：Session 裡存資料用的名字
├── dto/LoginRequest.java            新增：登入請求 {"nickname","mobile"}
├── dto/LoginResponse.java           新增：登入成功回應
├── dto/UserResponse.java            新增：會員資料回應
├── repository/UserRepository.java   修改：加上 existsByNicknameLower
├── service/UserService.java         新增：登入/註冊規則與欄位驗證
├── controller/AuthController.java   新增：login / me / logout
├── config/LoginInterceptor.java     新增：門口警衛
└── config/WebConfig.java            新增：設定警衛要守哪些網址
backend/src/test/java/com/wabisabi/library/
└── AuthApiTest.java                 新增：19 個測試
```

### 4-1 Session 的鑰匙名稱：SessionKeys.java

**`backend/src/main/java/com/wabisabi/library/common/SessionKeys.java`**

```java
package com.wabisabi.library.common;

/**
 * Session 裡存放資料用的「名字」。集中寫在這裡，存和取才不會因為拼錯字而對不上。
 */
public final class SessionKeys {

	private SessionKeys() {
	}

	/** 登入者的會員 id（Long） */
	public static final String USER_ID = "loginUserId";
}
```

**白話解釋**
- Session 就像一個 `Map`，用「名字」存取資料。存的時候寫 `"loginUserId"`、取的時候寫成 `"loginUserID"`，就會永遠拿到 `null`，而且很難發現。所以名字集中寫成常數，存取兩邊都用 `SessionKeys.USER_ID`。

### 4-2 請求與回應的 DTO

**`backend/src/main/java/com/wabisabi/library/dto/LoginRequest.java`**

```java
package com.wabisabi.library.dto;

/**
 * 登入/註冊時前端送來的資料：{"nickname": "Cooqle", "mobile": "0900123456"}
 * Jackson 會用 setter 把 JSON 的值填進來。
 */
public class LoginRequest {

	private String nickname;
	private String mobile;

	public LoginRequest() {
	}

	public LoginRequest(String nickname, String mobile) {
		this.nickname = nickname;
		this.mobile = mobile;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}
}
```

**`backend/src/main/java/com/wabisabi/library/dto/LoginResponse.java`**

```java
package com.wabisabi.library.dto;

import com.wabisabi.library.entity.User;

/**
 * 登入成功的回應：{"message": "登入成功", "nickname": "Cooqle", "mobile": "0900123456"}
 * 前端拿到暱稱後，就能馬上更新導覽列的「Cooqle (個人頁)」。
 */
public class LoginResponse {

	private String message;
	private String nickname;
	private String mobile;

	public LoginResponse() {
	}

	public LoginResponse(String message, User user) {
		this.message = message;
		this.nickname = user.getNickname();
		this.mobile = user.getMobile();
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}
}
```

**`backend/src/main/java/com/wabisabi/library/dto/UserResponse.java`**

```java
package com.wabisabi.library.dto;

import com.wabisabi.library.entity.User;

/**
 * 回傳給前端的會員資料（GET /api/auth/me）：{"nickname": "Cooqle", "mobile": "0900123456"}
 * 不包含 id、nickname_lower 這些前端用不到的欄位。
 */
public class UserResponse {

	private String nickname;
	private String mobile;

	public UserResponse() {
	}

	public UserResponse(User user) {
		this.nickname = user.getNickname();
		this.mobile = user.getMobile();
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}
}
```

**白話解釋**
- `LoginRequest`：前端送來的 JSON 會被 Jackson 「倒進」這個物件。Jackson 先 `new LoginRequest()`（所以要有空的建構子），再呼叫 `setNickname(...)`、`setMobile(...)`。
- `LoginResponse` 多帶了 `nickname`：前端登入後馬上就能把導覽列改成「Cooqle (個人頁)」，不用再多問一次。
- 這三個 DTO 都**不包含會員 id**：前端不需要知道，也就不會有人拿它來假冒別人。

### 4-3 Repository 加一個方法

**`backend/src/main/java/com/wabisabi/library/repository/UserRepository.java`**（修改後全文）

```java
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
```

**白話解釋**
- `existsByNicknameLower`：方法名稱拆開讀是「exists（存在嗎）By（條件）NicknameLower（nicknameLower 欄位）」，回傳 `boolean`。只回答「有或沒有」，不用把整筆資料讀出來。

### 4-4 登入/註冊規則：UserService.java

**`backend/src/main/java/com/wabisabi/library/service/UserService.java`**

```java
package com.wabisabi.library.service;

import java.util.Locale;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.wabisabi.library.common.Messages;
import com.wabisabi.library.dto.LoginRequest;
import com.wabisabi.library.entity.User;
import com.wabisabi.library.exception.ApiException;
import com.wabisabi.library.repository.UserRepository;

/**
 * 會員的規則：登入/註冊合一（沒有密碼，只用暱稱＋手機）。
 */
@Service
public class UserService {

	/** 暱稱只能是英文字母、數字、- 與 _ */
	private static final String NICKNAME_PATTERN = "^[A-Za-z0-9_-]+$";
	private static final int NICKNAME_MAX_LENGTH = 20;
	/** 手機：剛好 10 碼數字 */
	private static final String MOBILE_PATTERN = "^[0-9]{10}$";

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * 登入/註冊合一。判斷順序：
	 * a. 欄位驗證
	 * b. 用手機找會員：找到且暱稱（不分大小寫）相同 → 登入；暱稱不同 → 「暱稱與手機號碼不符」
	 * c. 手機找不到：暱稱已被別人使用 → 「此暱稱已被使用」；否則建立新會員
	 *
	 * @return 登入（或剛註冊）的會員
	 */
	public User login(LoginRequest request) {
		// 先去除前後空白（null 當作空字串）
		String nickname = trim(request.getNickname());
		String mobile = trim(request.getMobile());

		// a. 欄位驗證
		validate(nickname, mobile);

		String nicknameLower = nickname.toLowerCase(Locale.ROOT);

		// b. 用手機找會員
		Optional<User> found = userRepository.findByMobile(mobile);
		if (found.isPresent()) {
			User user = found.get();
			if (user.getNicknameLower().equals(nicknameLower)) {
				return user; // 暱稱相同（不分大小寫）→ 登入
			}
			throw new ApiException(HttpStatus.CONFLICT, Messages.NICKNAME_MOBILE_MISMATCH);
		}

		// c. 手機是新的：檢查暱稱有沒有被別人用走
		if (userRepository.existsByNicknameLower(nicknameLower)) {
			throw new ApiException(HttpStatus.CONFLICT, Messages.NICKNAME_TAKEN);
		}

		// 全新的暱稱＋手機 → 自動註冊
		return userRepository.save(new User(nickname, mobile));
	}

	/** 依 id 取得會員（GET /api/auth/me 用）；找不到回傳空的 Optional */
	public Optional<User> findById(Long id) {
		return userRepository.findById(id);
	}

	/**
	 * 欄位驗證，順序：暱稱空白 → 手機空白 → 暱稱格式 → 暱稱長度 → 手機格式。
	 * 一發現錯誤就丟出例外，所以使用者一次只會看到一則訊息。
	 */
	private void validate(String nickname, String mobile) {
		if (nickname.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, Messages.NICKNAME_REQUIRED);
		}
		if (mobile.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, Messages.MOBILE_REQUIRED);
		}
		if (!nickname.matches(NICKNAME_PATTERN)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, Messages.NICKNAME_INVALID);
		}
		if (nickname.length() > NICKNAME_MAX_LENGTH) {
			throw new ApiException(HttpStatus.BAD_REQUEST, Messages.NICKNAME_TOO_LONG);
		}
		if (!mobile.matches(MOBILE_PATTERN)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, Messages.MOBILE_INVALID);
		}
	}

	private String trim(String value) {
		return (value == null) ? "" : value.trim();
	}
}
```

**白話解釋**
- **驗證順序**（計畫階段 Q1 確認）：暱稱空白 → 手機空白 → 暱稱格式 → 暱稱長度 → 手機格式。一發現錯誤就 `throw`，後面的檢查不會執行，所以使用者一次只會看到一則訊息。
- **正規表示式 `^[A-Za-z0-9_-]+$`**：
  - `^` 開頭、`$` 結尾：從頭到尾整串都要符合。
  - `[A-Za-z0-9_-]`：一個英文字母（大小寫）、數字、底線或減號。
  - `+`：一個以上。
  - 所以 `Coo qle`（有空白）、`Cooqle!`（有驚嘆號）、`小明`（中文）都不會通過。
- **為什麼格式排在長度前面？** 輸入一長串中文時，先提示「只能用英文數字」比先說「太長了」更有用，因為改短了還是不合格。
- **`trim()` 去除前後空白**（計畫階段 Q4 確認）：使用者不小心多打的空白不該害他登入失敗。中間的空白（`Coo qle`）不會被去掉，所以照樣算格式錯誤。
- **判斷流程**（設計決策 5）：
  ```
  手機找得到會員嗎？
   ├─ 找得到 ── 小寫暱稱相同？ ── 是 → 登入
   │                         └─ 否 → 409「暱稱與手機號碼不符」
   └─ 找不到 ── 小寫暱稱被用過？ ── 是 → 409「此暱稱已被使用」
                               └─ 否 → 建立新會員 → 登入
  ```
- `409 Conflict`：「你的資料跟現有資料衝突」。
- 新會員註冊後的提示文字跟一般登入一樣是「登入成功」（設計決策 11），所以 Service 只回傳 `User`，不用區分是登入還是剛註冊。

### 4-5 API：AuthController.java

**`backend/src/main/java/com/wabisabi/library/controller/AuthController.java`**

```java
package com.wabisabi.library.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wabisabi.library.common.Messages;
import com.wabisabi.library.common.SessionKeys;
import com.wabisabi.library.dto.LoginRequest;
import com.wabisabi.library.dto.LoginResponse;
import com.wabisabi.library.dto.MessageResponse;
import com.wabisabi.library.dto.UserResponse;
import com.wabisabi.library.entity.User;
import com.wabisabi.library.exception.ApiException;
import com.wabisabi.library.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 登入/註冊、登出、取得目前登入者。
 * /api/auth/login 公開；/api/auth/me 與 /api/auth/logout 需要登入（由 LoginInterceptor 把關）。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final UserService userService;

	public AuthController(UserService userService) {
		this.userService = userService;
	}

	/**
	 * POST /api/auth/login，內容：{"nickname": "Cooqle", "mobile": "0900123456"}
	 * 成功 200；欄位錯誤 400；暱稱與手機不符、暱稱已被使用 409。
	 */
	@PostMapping("/login")
	public LoginResponse login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
		// 1. 驗證並找出（或建立）會員；失敗會丟出 ApiException，下面就不會執行
		User user = userService.login(loginRequest);

		// 2. 取得 Session（沒有就建立一個），再換一個新的 Session ID，防止「Session 固定攻擊」
		HttpSession session = request.getSession();
		request.changeSessionId();

		// 3. 把會員 id 存進 Session：之後的請求就靠它認出「你是誰」
		session.setAttribute(SessionKeys.USER_ID, user.getId());

		return new LoginResponse(Messages.LOGIN_SUCCESS, user);
	}

	/**
	 * GET /api/auth/me：回傳目前登入者的暱稱與手機。
	 * 前端打開網頁時會先呼叫它，判斷「現在有沒有登入」。
	 */
	@GetMapping("/me")
	public UserResponse me(HttpSession session) {
		// 一律從 Session 取得登入者 id，不相信前端傳來的任何 id
		Long userId = (Long) session.getAttribute(SessionKeys.USER_ID);

		User user = userService.findById(userId).orElse(null);
		if (user == null) {
			// Session 裡有 id，但資料庫已經沒有這位會員：清掉 Session，當作未登入
			session.invalidate();
			throw new ApiException(HttpStatus.UNAUTHORIZED, Messages.LOGIN_REQUIRED);
		}
		return new UserResponse(user);
	}

	/** POST /api/auth/logout：清除 Session，回傳「已成功登出」 */
	@PostMapping("/logout")
	public MessageResponse logout(HttpSession session) {
		session.invalidate();
		return new MessageResponse(Messages.LOGOUT_SUCCESS);
	}
}
```

**白話解釋**
- `@RequestBody LoginRequest loginRequest`：把請求內容的 JSON 轉成 `LoginRequest` 物件。
- 方法參數寫 `HttpServletRequest request` 或 `HttpSession session`，Spring 就會自動把目前的請求或 Session 遞進來。
- **login 的順序很重要**：
  1. 先 `userService.login(...)`。驗證失敗會直接丟出例外，**不會**建立 Session。
  2. `request.getSession()`：取得 Session，沒有就建一個。
  3. `request.changeSessionId()`：換新號碼（防 Session 固定攻擊）。櫃子裡的東西不變，只是換號碼。
  4. `session.setAttribute(...)`：把會員 id 放進櫃子。
- **me**：能走到這裡，代表警衛已經確認 Session 裡有 id，所以可以放心直接取。萬一這位會員已經被從資料庫刪掉，就清掉 Session、回 401。
- **logout**：`session.invalidate()` 讓整個 Session 作廢，舊的 `JSESSIONID` 從此無效。
- `(Long) session.getAttribute(...)`：Session 裡存的東西型態都是 `Object`，取出來要**轉型**回 `Long`。

### 4-6 門口警衛：LoginInterceptor.java

**`backend/src/main/java/com/wabisabi/library/config/LoginInterceptor.java`**

```java
package com.wabisabi.library.config;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.wabisabi.library.common.Messages;
import com.wabisabi.library.common.SessionKeys;
import com.wabisabi.library.exception.ApiException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 登入檢查的「門口警衛」：請求進到 Controller 之前先檢查 Session 裡有沒有會員 id。
 * 沒有 → 丟出 401，請求不會進到 Controller。
 * 要保護哪些網址，在 WebConfig 設定。
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		// getSession(false)：只拿「已經存在」的 Session，沒有就回傳 null，不會順手建立新的
		HttpSession session = request.getSession(false);

		if (session == null || session.getAttribute(SessionKeys.USER_ID) == null) {
			// 交給 GlobalExceptionHandler 轉成 401 + {"message": "請先註冊/登入"}
			throw new ApiException(HttpStatus.UNAUTHORIZED, Messages.LOGIN_REQUIRED);
		}
		return true; // true = 放行，繼續往 Controller 走
	}
}
```

**白話解釋**
- `implements HandlerInterceptor`：實作 Spring 的攔截器介面。`preHandle` 的意思是「在 Controller 之前執行」。
- `request.getSession(false)`：
  - `getSession()` 或 `getSession(true)`：沒有就**新建**一個。
  - `getSession(false)`：沒有就回傳 `null`。警衛只是檢查，不該幫沒登入的人開新櫃子，所以用 `false`。
- 擋人的方式是 `throw new ApiException(401, ...)`：攔截器丟出的例外一樣會被 GlobalExceptionHandler 接住，回傳格式跟其他錯誤完全一致。不用自己寫 JSON。
- `@Component`：讓 Spring 管理它，WebConfig 才能用建構子注入拿到它。

### 4-7 設定警衛守哪些門：WebConfig.java

**`backend/src/main/java/com/wabisabi/library/config/WebConfig.java`**

```java
package com.wabisabi.library.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 的設定：在這裡告訴 Spring「LoginInterceptor 要守哪些網址」。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final LoginInterceptor loginInterceptor;

	public WebConfig(LoginInterceptor loginInterceptor) {
		this.loginInterceptor = loginInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(loginInterceptor)
				// 用「白名單式」只列出需要登入的網址：
				// 查書（/api/books/**）和登入（/api/auth/login）必須保持公開，否則沒人能登入
				.addPathPatterns(
						"/api/favorites", // 查詢自己的收藏
						"/api/favorites/**", // 新增、刪除收藏
						"/api/auth/me", // 取得目前登入者
						"/api/auth/logout"); // 登出
	}
}
```

**白話解釋**
- `@Configuration`：「這是設定類別」，Spring 啟動時會讀取它。
- `implements WebMvcConfigurer`：Spring MVC 提供的「設定掛勾」。覆寫 `addInterceptors` 方法，就能把攔截器註冊進去。只寫 `LoginInterceptor` 而沒有註冊的話，它**完全不會執行**。
- **為什麼這樣設定路徑？**
  - 用 `addPathPatterns` **只列出需要登入的網址**，其他網址預設公開。
  - 一定要公開的：`/api/books/**`（未登入也能查書）、`/api/auth/login`（要登入當然不能先要求登入）。如果把攔截器套在 `/api/**` 全部，就沒有人能登入了。
  - `/api/favorites` 和 `/api/favorites/**` 兩個都列：前者是「查詢收藏清單」，後者是 `/api/favorites/add`、`/api/favorites/5` 這類子路徑。`**` 代表「底下任意層」。（其實 `/**` 也包含零層，但兩個都寫比較一目了然。）
  - `/api/auth/me`、`/api/auth/logout`：要知道「你是誰」或「把你登出」，前提當然是已經登入。

### 4-8 測試：AuthApiTest.java

**`backend/src/test/java/com/wabisabi/library/AuthApiTest.java`**

```java
package com.wabisabi.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.wabisabi.library.common.SessionKeys;
import com.wabisabi.library.entity.User;
import com.wabisabi.library.repository.FavoriteRepository;
import com.wabisabi.library.repository.UserRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * 需求 5～8：登入/註冊、個人頁（/api/auth/me）、登出。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

	private static final String SEED_MOBILE = "0900123456";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private FavoriteRepository favoriteRepository;

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	@BeforeEach
	void setUp() {
		// 每個測試都從「只有種子會員 Cooqle」的狀態開始
		favoriteRepository.deleteAll();
		for (User user : userRepository.findAll()) {
			if (!user.getMobile().equals(SEED_MOBILE)) {
				userRepository.delete(user);
			}
		}
	}

	// ===== 小工具 =====

	/** 送出登入請求 */
	private ResultActions login(String nickname, String mobile) throws Exception {
		String json = jsonMapper.writeValueAsString(Map.of("nickname", nickname, "mobile", mobile));
		return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	/** 登入成功後，取出這次登入的 Session（之後的請求帶著它，就等於「已登入」） */
	private MockHttpSession loginAndGetSession(String nickname, String mobile) throws Exception {
		MvcResult result = login(nickname, mobile).andExpect(status().isOk()).andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	/** 這次請求結束後，Session 裡有沒有登入者 id */
	private boolean isLoggedIn(MvcResult result) {
		var session = result.getRequest().getSession(false);
		return session != null && session.getAttribute(SessionKeys.USER_ID) != null;
	}

	// ===== 登入 / 註冊 =====

	@Test
	@DisplayName("Scenario: 使用暱稱與手機號碼成功登入")
	void loginSuccess() throws Exception {
		MvcResult result = login("Cooqle", "0900123456")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("登入成功"))
				.andExpect(jsonPath("$.nickname").value("Cooqle"))
				.andReturn();

		Long cooqleId = userRepository.findByMobile(SEED_MOBILE).orElseThrow().getId();
		assertThat(result.getRequest().getSession().getAttribute(SessionKeys.USER_ID)).isEqualTo(cooqleId);
	}

	@Test
	@DisplayName("Scenario: 暱稱大小寫視為相同，可登入同一個帳號")
	void loginCaseInsensitive() throws Exception {
		long countBefore = userRepository.count();

		MvcResult result = login("cooqle", "0900123456")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("登入成功"))
				.andExpect(jsonPath("$.nickname").value("Cooqle")) // 顯示的是註冊時的原樣
				.andReturn();

		assertThat(isLoggedIn(result)).isTrue();
		assertThat(userRepository.count()).isEqualTo(countBefore); // 沒有建立新會員
	}

	@Test
	@DisplayName("Scenario: 使用全新暱稱與手機號碼自動完成註冊並登入")
	void registerNewUser() throws Exception {
		MvcResult result = login("Wabi_Sabi", "0987654321")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("登入成功"))
				.andExpect(jsonPath("$.nickname").value("Wabi_Sabi"))
				.andReturn();

		User created = userRepository.findByMobile("0987654321").orElseThrow();
		assertThat(created.getNickname()).isEqualTo("Wabi_Sabi");
		assertThat(created.getNicknameLower()).isEqualTo("wabi_sabi");
		assertThat(isLoggedIn(result)).isTrue();
	}

	@ParameterizedTest(name = "Scenario Outline: 登入/註冊時輸入無效資料 | 暱稱「{0}」手機「{1}」→ {2}")
	@CsvSource({
			"'',                          0900123456, 請輸入暱稱",
			"Cooqle,                      '',         請輸入手機號碼",
			"Cooqle,                      12345,      請輸入有效的手機號碼格式 (10碼)",
			"小明,                         0900123456, 暱稱僅限英文、數字、- 與 _",
			"Coo qle,                     0900123456, 暱稱僅限英文、數字、- 與 _",
			"Cooqle!,                     0900123456, 暱稱僅限英文、數字、- 與 _",
			"ThisNicknameIsWayTooLong123, 0900123456, 暱稱長度不可超過 20 個字元"
	})
	void invalidInput(String nickname, String mobile, String expectedMessage) throws Exception {
		long countBefore = userRepository.count();

		MvcResult result = login(nickname, mobile)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(expectedMessage))
				.andReturn();

		assertThat(isLoggedIn(result)).isFalse(); // 不應執行登入
		assertThat(userRepository.count()).isEqualTo(countBefore);
	}

	@Test
	@DisplayName("Scenario: 手機號碼已註冊但暱稱不同")
	void mobileExistsButNicknameDiffers() throws Exception {
		long countBefore = userRepository.count();

		MvcResult result = login("Another_One", "0900123456")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("暱稱與手機號碼不符"))
				.andReturn();

		assertThat(isLoggedIn(result)).isFalse();
		assertThat(userRepository.count()).isEqualTo(countBefore);
	}

	@Test
	@DisplayName("Scenario: 手機號碼是新的但暱稱已被使用 (不分大小寫)")
	void nicknameTaken() throws Exception {
		long countBefore = userRepository.count();

		MvcResult result = login("COOQLE", "0955000111")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("此暱稱已被使用"))
				.andReturn();

		assertThat(isLoggedIn(result)).isFalse();
		assertThat(userRepository.count()).isEqualTo(countBefore);
	}

	@Test
	@DisplayName("規則：暱稱與手機前後空白會先去除")
	void inputIsTrimmed() throws Exception {
		login("  Cooqle  ", " 0900123456 ")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nickname").value("Cooqle"));
	}

	@Test
	@DisplayName("規則：暱稱剛好 20 個字元可以註冊，21 個字元不行")
	void nicknameLengthBoundary() throws Exception {
		login("A2345678901234567890", "0911000001").andExpect(status().isOk());
		login("A23456789012345678901", "0911000002")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("暱稱長度不可超過 20 個字元"));
	}

	@Test
	@DisplayName("規則：登入成功後會更換 Session ID（防止 Session 固定攻擊）")
	void sessionIdChangesOnLogin() throws Exception {
		MockHttpSession oldSession = new MockHttpSession();
		String oldId = oldSession.getId();

		String json = jsonMapper.writeValueAsString(Map.of("nickname", "Cooqle", "mobile", "0900123456"));
		MvcResult result = mockMvc.perform(post("/api/auth/login")
				.session(oldSession).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isOk())
				.andReturn();

		assertThat(result.getRequest().getSession().getId()).isNotEqualTo(oldId);
	}

	// ===== 個人頁 =====

	@Test
	@DisplayName("Scenario: 在個人頁面檢視個人資訊（GET /api/auth/me）")
	void meReturnsProfile() throws Exception {
		MockHttpSession session = loginAndGetSession("Cooqle", "0900123456");

		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nickname").value("Cooqle"))
				.andExpect(jsonPath("$.mobile").value("0900123456"));
	}

	@Test
	@DisplayName("Scenario: 未登入使用者嘗試訪問個人頁面被拒絕（API 回 401）")
	void meRequiresLogin() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("請先註冊/登入"))
				.andExpect(jsonPath("$.nickname").doesNotExist());
	}

	// ===== 登出 =====

	@Test
	@DisplayName("Scenario: 在個人頁面登出後無法再查看個人資訊（Session 已清除）")
	void logoutClearsSession() throws Exception {
		MockHttpSession session = loginAndGetSession("Cooqle", "0900123456");

		mockMvc.perform(post("/api/auth/logout").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("已成功登出"));

		assertThat(session.isInvalid()).isTrue();

		// 拿舊的 Session 再來，應該被當作未登入
		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("規則：未登入時呼叫登出 API 回 401")
	void logoutRequiresLogin() throws Exception {
		mockMvc.perform(post("/api/auth/logout"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("請先註冊/登入"));
	}
}
```

**白話解釋**
- `@ParameterizedTest` ＋ `@CsvSource`：**參數化測試**，同一段測試程式跑很多組資料，剛好對應 BDD 的 Scenario Outline（Examples 表格）。每一行是一組「暱稱, 手機, 預期訊息」。
  - `''`（兩個單引號）代表空字串，對應 BDD 表格裡空白的格子。
- `MockHttpSession`：模擬瀏覽器的 Session。登入成功後把它存起來，之後的請求 `.session(session)` 帶著它，就等於「已登入的瀏覽器」。
- `isLoggedIn(result)`：檢查請求結束後 Session 裡有沒有會員 id，對應 BDD 的「系統不應執行登入」。
- `userRepository.count()` 前後比較：對應 BDD 的「系統不應建立新的會員帳號」。
- `@BeforeEach` 清掉種子以外的會員和所有收藏：讓每個測試都從 Background 的狀態開始，測試之間不會互相影響。
- `session.isInvalid()`：確認登出後 Session 真的作廢了。

### 對應的 Scenario

| BDD Scenario | 狀態 | 測試方法 |
|---|---|---|
| 使用暱稱與手機號碼成功登入 | ✅ 後端通過 | `loginSuccess` |
| 暱稱大小寫視為相同，可登入同一個帳號 | ✅ 後端通過 | `loginCaseInsensitive` |
| 使用全新暱稱與手機號碼自動完成註冊並登入 | ✅ 後端通過 | `registerNewUser` |
| 登入/註冊時輸入無效資料（7 組） | ✅ 後端通過 | `invalidInput` ×7 |
| 手機號碼已註冊但暱稱不同 | ✅ 後端通過 | `mobileExistsButNicknameDiffers` |
| 手機號碼是新的但暱稱已被使用 (不分大小寫) | ✅ 後端通過 | `nicknameTaken` |
| 在個人頁面檢視個人資訊 | ✅ 後端 API 通過（畫面在步驟 10） | `meReturnsProfile` |
| 未登入使用者嘗試訪問個人頁面被拒絕 | ✅ 後端 API 回 401（提示與導向在前端，步驟 6） | `meRequiresLogin` |
| 在個人頁面登出後無法再查看收藏內容 | 🔸 部分：登出與 Session 清除已通過；「看不到收藏」在步驟 5 補測 | `logoutClearsSession` |

「跳出提示」「跳轉至首頁」這些畫面行為會在前端步驟驗證。

### 怎麼驗證成功

**驗證 1：跑測試**
```bash
cd backend
./mvnw test
```
實測結果（2026-10-16）：
```
Tests run: 19, Failures: 0, Errors: 0 -- in AuthApiTest
Tests run: 8,  Failures: 0, Errors: 0 -- in BookApiTest
Tests run: 5,  Failures: 0, Errors: 0 -- in GoogleBooksClientTest
Tests run: 3,  Failures: 0, Errors: 0 -- in DataSeederTest
Tests run: 1,  Failures: 0, Errors: 0 -- in LibraryApplicationTests
```

**驗證 2：用 curl 模擬瀏覽器（連 MySQL）**
curl 預設不會記住 Cookie。加上 `-c cookies.txt`（把收到的 Cookie 存起來）和 `-b cookies.txt`（送請求時帶上 Cookie），它就像瀏覽器一樣會「戴手環」。

```bash
# 登入（Cookie 存進 cookies.txt）
curl -c cookies.txt -b cookies.txt -X POST -H "Content-Type: application/json" \
     -d '{"nickname":"cooqle","mobile":"0900123456"}' http://localhost:8080/api/auth/login

# 帶著 Cookie 查詢自己的資料
curl -c cookies.txt -b cookies.txt http://localhost:8080/api/auth/me

# 登出
curl -c cookies.txt -b cookies.txt -X POST http://localhost:8080/api/auth/logout
```

實測結果（2026-10-16）：

| # | 請求 | 回應 |
|---|---|---|
| 1 | 未登入 `GET /api/auth/me` | 401 `{"message":"請先註冊/登入"}` |
| 2 | 登入：暱稱空白 | 400 `{"message":"請輸入暱稱"}` |
| 3 | 登入：暱稱「小明」 | 400 `{"message":"暱稱僅限英文、數字、- 與 _"}` |
| 4 | 登入：手機「12345」 | 400 `{"message":"請輸入有效的手機號碼格式 (10碼)"}` |
| 5 | 登入：Another_One / 0900123456 | 409 `{"message":"暱稱與手機號碼不符"}` |
| 6 | 登入：COOQLE / 0955000111 | 409 `{"message":"此暱稱已被使用"}` |
| 7 | 登入：JSON 格式壞掉 | 400 `{"message":"無法處理此請求"}` |
| 8 | 登入：cooqle / 0900123456 | 200 `{"message":"登入成功","mobile":"0900123456","nickname":"Cooqle"}`，回應標頭有 `Set-Cookie: JSESSIONID=...; Path=/; HttpOnly` |
| 9 | 已登入 `GET /api/auth/me` | 200 `{"mobile":"0900123456","nickname":"Cooqle"}` |
| 10 | 登出 | 200 `{"message":"已成功登出"}` |
| 11 | 登出後 `GET /api/auth/me` | 401 `{"message":"請先註冊/登入"}` |

- 第 8 步的 `HttpOnly`：這個 Cookie 不能被網頁的 JavaScript 讀取，可以降低被惡意程式偷走的風險（Tomcat 預設就會加）。
- 第 8 步輸入小寫 `cooqle`，回應的是原樣 `Cooqle`：暱稱大小寫視為相同 ✅

---

## 步驟 5：收藏 API（加入、清單、刪除）

### 目標
1. `POST /api/favorites/add`：加入收藏（201），重複收藏回 409，書不存在回 404。
2. `GET /api/favorites`：只回傳**自己的**收藏（含書籍資料）。
3. `DELETE /api/favorites/{id}`：只能刪自己的，不存在或不是自己的**一律回 404**。
4. 這 3 支 API 都由步驟 4 的 LoginInterceptor 把關，未登入回 401。

後端到這一步全部完成。

### 名詞先解釋
- **201 Created**：「已建立」。新增資料成功時用它，比 200 更精確。
- **為什麼刪別人的收藏回 404，而不是 403？** 回 403 等於告訴對方「這筆收藏存在，只是不是你的」，等於洩漏了資訊。一律回 404「找不到此收藏」，對方就無從判斷。BDD 允許 403 或 404，計畫階段已確定用 404。
- **「不相信前端」原則**：加入收藏的請求內容只有 `isbn`，**沒有** `userId`。就算有人在 JSON 裡偷塞 `"userId": 2`，後端也完全不看，收藏一律記在 Session 裡的登入者名下。測試 `ignoreUserIdFromClient` 專門驗證這件事。

### 建立或修改的檔案
```
backend/src/main/java/com/wabisabi/library/
├── repository/FavoriteRepository.java          修改：加上 3 個查詢方法
├── dto/AddFavoriteRequest.java                 新增：{"isbn": "..."}
├── dto/FavoriteResponse.java                   新增：收藏清單的一筆
├── service/BookService.java                    修改：抽出 normalizeIsbn() 給收藏共用
├── service/FavoriteService.java                新增：收藏規則
├── controller/FavoriteController.java          新增：3 支收藏 API
└── exception/GlobalExceptionHandler.java       修改：網址參數型態錯誤改回 400
backend/src/test/java/com/wabisabi/library/
└── FavoriteApiTest.java                        新增：13 個測試
```

### 5-1 Repository：FavoriteRepository.java（修改後全文）

**`backend/src/main/java/com/wabisabi/library/repository/FavoriteRepository.java`**

```java
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
```

**白話解釋**
- Favorite 裡沒有 `userId` 欄位，只有 `user`（整個 User 物件）。方法名稱寫 `UserId` 時，Spring 會理解成「`user` 的 `id`」，自動產生 `WHERE user_id = ?`。同理，`BookIsbn` 就是 `book.isbn`。
- `findByUserIdOrderByIdAsc`：`OrderByIdAsc` 代表「依 id 由小到大排序」，也就是先收藏的排前面。（`Desc` 則是由大到小。）
- `findByIdAndUserId`：**兩個條件都要符合**才找得到。拿別人的收藏 id 來查，因為 `user_id` 對不上，結果就是「找不到」。用一個查詢同時完成「找資料」和「檢查擁有者」。

### 5-2 DTO

**`backend/src/main/java/com/wabisabi/library/dto/AddFavoriteRequest.java`**

```java
package com.wabisabi.library.dto;

/**
 * 加入收藏時前端送來的資料：{"isbn": "9789861343722"}
 * 刻意沒有 userId 欄位：「是誰收藏的」一律從 Session 取得。
 */
public class AddFavoriteRequest {

	private String isbn;

	public AddFavoriteRequest() {
	}

	public AddFavoriteRequest(String isbn) {
		this.isbn = isbn;
	}

	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}
}
```

**`backend/src/main/java/com/wabisabi/library/dto/FavoriteResponse.java`**

```java
package com.wabisabi.library.dto;

import com.wabisabi.library.entity.Book;
import com.wabisabi.library.entity.Favorite;

/**
 * 收藏清單中的一筆（含書籍資料）：
 * {"id": 5, "isbn": "9789861343722", "title": "...", "author": "...", "publishDate": "2020-05-01"}
 * id 是「收藏」的編號，前端按「刪除」時用它呼叫 DELETE /api/favorites/{id}。
 */
public class FavoriteResponse {

	private Long id;
	private String isbn;
	private String title;
	private String author;
	private String publishDate;

	public FavoriteResponse() {
	}

	public FavoriteResponse(Favorite favorite) {
		Book book = favorite.getBook();
		this.id = favorite.getId();
		this.isbn = book.getIsbn();
		this.title = book.getTitle();
		this.author = book.getAuthor();
		this.publishDate = book.getPublishDate();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getPublishDate() {
		return publishDate;
	}

	public void setPublishDate(String publishDate) {
		this.publishDate = publishDate;
	}
}
```

**白話解釋**
- `FavoriteResponse` 把「收藏」和「書」的資料**攤平**成一層，前端用起來比較直覺：`item.title`，而不是 `item.book.title`。
- 這裡的 `id` 是**收藏的編號**（favorites.id），不是書的 ISBN。前端按「刪除」時要用它。

### 5-3 BookService.java（修改後全文）

加入收藏也要檢查 ISBN 格式。為了不把同樣的規則寫兩次，把檢查抽成 `normalizeIsbn()`：

**`backend/src/main/java/com/wabisabi/library/service/BookService.java`**

```java
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
```

**白話解釋**
- `public static`：不用建立 BookService 物件就能呼叫，寫法是 `BookService.normalizeIsbn(...)`。它只是一條「規則」，不需要用到 Repository。
- 規則只寫在一個地方，以後要改（例如改成也接受 10 碼 ISBN）只要改這裡。

### 5-4 收藏規則：FavoriteService.java

**`backend/src/main/java/com/wabisabi/library/service/FavoriteService.java`**

```java
package com.wabisabi.library.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.wabisabi.library.common.Messages;
import com.wabisabi.library.dto.FavoriteResponse;
import com.wabisabi.library.entity.Book;
import com.wabisabi.library.entity.Favorite;
import com.wabisabi.library.entity.User;
import com.wabisabi.library.exception.ApiException;
import com.wabisabi.library.repository.BookRepository;
import com.wabisabi.library.repository.FavoriteRepository;
import com.wabisabi.library.repository.UserRepository;

/**
 * 收藏的規則。每個方法的第一個參數 userId 都來自 Session（Controller 傳進來），不是前端給的。
 */
@Service
public class FavoriteService {

	private final FavoriteRepository favoriteRepository;
	private final BookRepository bookRepository;
	private final UserRepository userRepository;

	public FavoriteService(FavoriteRepository favoriteRepository, BookRepository bookRepository,
			UserRepository userRepository) {
		this.favoriteRepository = favoriteRepository;
		this.bookRepository = bookRepository;
		this.userRepository = userRepository;
	}

	/**
	 * 加入收藏。順序：ISBN 格式 → 書存在嗎 → 已經收藏過嗎 → 新增。
	 */
	public void add(Long userId, String rawIsbn) {
		String isbn = BookService.normalizeIsbn(rawIsbn);

		// 書一定要先在本機 books 表（使用者是先查詢、再按加入收藏，查詢時就已經存進來了）
		Book book = bookRepository.findById(isbn)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Messages.BOOK_NOT_FOUND));

		// Session 裡的會員已經不在資料庫：當作未登入
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, Messages.LOGIN_REQUIRED));

		if (favoriteRepository.existsByUserIdAndBookIsbn(userId, isbn)) {
			throw new ApiException(HttpStatus.CONFLICT, Messages.FAVORITE_DUPLICATE);
		}

		try {
			// saveAndFlush：立刻寫進資料庫，若違反唯一鍵會馬上在這裡丟出例外
			favoriteRepository.saveAndFlush(new Favorite(user, book));
		} catch (DataIntegrityViolationException e) {
			// 極少見：同一時間按了兩次，兩個請求都通過上面的檢查，資料庫的唯一鍵擋下第二筆
			throw new ApiException(HttpStatus.CONFLICT, Messages.FAVORITE_DUPLICATE);
		}
	}

	/** 取得這位會員自己的收藏清單 */
	public List<FavoriteResponse> list(Long userId) {
		List<FavoriteResponse> result = new ArrayList<>();
		for (Favorite favorite : favoriteRepository.findByUserIdOrderByIdAsc(userId)) {
			result.add(new FavoriteResponse(favorite));
		}
		return result;
	}

	/**
	 * 刪除收藏：只能刪自己的。
	 * 收藏不存在、或不是自己的，一律回 404（不透露「這筆收藏存在但屬於別人」）。
	 */
	public void remove(Long userId, Long favoriteId) {
		Favorite favorite = favoriteRepository.findByIdAndUserId(favoriteId, userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Messages.FAVORITE_NOT_FOUND));
		favoriteRepository.delete(favorite);
	}
}
```

**白話解釋**
- `.orElseThrow(() -> new ApiException(...))`：從 `Optional` 盒子拿東西，盒子是空的就丟出指定的例外。比寫 `if (box.isEmpty()) throw ...; Book book = box.get();` 簡潔。
  - `() -> new ApiException(...)` 叫做 **Lambda（匿名函式）**，可以讀成「需要的時候，就執行箭頭右邊這件事」。
- **加入收藏的檢查順序**：ISBN 格式 → 書在本機嗎 → 會員還在嗎 → 收藏過了嗎 → 新增。
- **兩道防線防重複**：
  1. `existsByUserIdAndBookIsbn`：先問一次，有就回 409 和友善的提示。
  2. 資料庫的唯一鍵 `(user_id, isbn)`：萬一使用者快速連按兩次，兩個請求可能同時通過第 1 道檢查。這時資料庫會擋下第二筆並丟出 `DataIntegrityViolationException`，我們接住它，一樣回 409。
- `saveAndFlush`：`save` 有時會晚一點才真的寫進資料庫；`saveAndFlush` 會**立刻寫入**，所以違反唯一鍵時，例外會在 `try` 裡面發生，才接得到。
- 為什麼加入收藏時**不去問 Google**？使用者一定是先查詢、再按「加入收藏」，查詢時書就已經存進本機了。所以這裡找不到書，代表請求不正常，直接回 404。

### 5-5 收藏 API：FavoriteController.java

**`backend/src/main/java/com/wabisabi/library/controller/FavoriteController.java`**

```java
package com.wabisabi.library.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wabisabi.library.common.Messages;
import com.wabisabi.library.common.SessionKeys;
import com.wabisabi.library.dto.AddFavoriteRequest;
import com.wabisabi.library.dto.FavoriteResponse;
import com.wabisabi.library.dto.MessageResponse;
import com.wabisabi.library.service.FavoriteService;

import jakarta.servlet.http.HttpSession;

/**
 * 收藏 API：全部需要登入（LoginInterceptor 已經在門口檢查過）。
 * 「是誰」一律從 Session 取得，不相信前端傳來的任何 user_id。
 */
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

	private final FavoriteService favoriteService;

	public FavoriteController(FavoriteService favoriteService) {
		this.favoriteService = favoriteService;
	}

	/** GET /api/favorites：自己的收藏清單 */
	@GetMapping
	public List<FavoriteResponse> list(HttpSession session) {
		return favoriteService.list(loginUserId(session));
	}

	/**
	 * POST /api/favorites/add，內容：{"isbn": "9789861343722"}
	 * 成功 201；書不存在 404；重複收藏 409。
	 */
	@PostMapping("/add")
	public ResponseEntity<MessageResponse> add(@RequestBody AddFavoriteRequest request, HttpSession session) {
		favoriteService.add(loginUserId(session), request.getIsbn());
		return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse(Messages.FAVORITE_ADDED));
	}

	/** DELETE /api/favorites/5：刪除自己的一筆收藏；不存在或不是自己的回 404 */
	@DeleteMapping("/{id}")
	public MessageResponse remove(@PathVariable Long id, HttpSession session) {
		favoriteService.remove(loginUserId(session), id);
		return new MessageResponse(Messages.FAVORITE_REMOVED);
	}

	/** 從 Session 取出登入者 id */
	private Long loginUserId(HttpSession session) {
		return (Long) session.getAttribute(SessionKeys.USER_ID);
	}
}
```

**白話解釋**
- `@GetMapping`（沒寫路徑）：對應 `@RequestMapping` 的 `/api/favorites` 本身。
- `@DeleteMapping("/{id}")` ＋ `@PathVariable Long id`：網址 `/api/favorites/5` 的 `5` 會自動轉成 `Long`。
- `ResponseEntity.status(HttpStatus.CREATED)`：要回 201 而不是預設的 200，就得用 `ResponseEntity` 指定狀態碼。
- `loginUserId(session)`：3 支 API 都從 Session 取 id，寫成一個小方法共用。能走到這裡，代表攔截器已經確認過有登入，所以一定拿得到。

### 5-6 GlobalExceptionHandler.java（修改後全文）

用 curl 實測時發現：`DELETE /api/favorites/abc`（id 不是數字）會回 **500「系統發生錯誤」**。這不對，錯的是請求，不是伺服器。
原因：Spring 把 `abc` 轉成 `Long` 失敗時，丟出的 `MethodArgumentTypeMismatchException` 沒有帶狀態碼，所以掉進了「其他所有例外」，被當成 500。
修正方式：把它和「JSON 格式錯誤」放在同一個處理方法，回 400。

**`backend/src/main/java/com/wabisabi/library/exception/GlobalExceptionHandler.java`**

```java
package com.wabisabi.library.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.wabisabi.library.common.Messages;
import com.wabisabi.library.dto.MessageResponse;

/**
 * 全域例外處理：所有 Controller 丟出來、沒人接住的例外，最後都會來到這裡，
 * 統一轉成 {"message": "..."} 的 JSON 回傳。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/** 1. 我們自己丟的「預料中的錯誤」：狀態碼和訊息照它帶來的回傳 */
	@ExceptionHandler(ApiException.class)
	public ResponseEntity<MessageResponse> handleApiException(ApiException e) {
		return ResponseEntity.status(e.getStatus()).body(new MessageResponse(e.getMessage()));
	}

	/**
	 * 2. 請求內容看不懂：
	 * - HttpMessageNotReadableException：送來的不是正確的 JSON（例如少了括號）
	 * - MethodArgumentTypeMismatchException：網址參數型態不對（例如 DELETE /api/favorites/abc，id 應該是數字）
	 */
	@ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
	public ResponseEntity<MessageResponse> handleBadRequest(Exception e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(Messages.REQUEST_FAILED));
	}

	/** 3. 其他所有例外 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<MessageResponse> handleOther(Exception e) {
		// Spring 自己擋下的請求（網址不存在 404、HTTP 方法不對 405 等）會帶著狀態碼，照用
		if (e instanceof ErrorResponse errorResponse) {
			HttpStatusCode status = errorResponse.getStatusCode();
			return ResponseEntity.status(status).body(new MessageResponse(Messages.REQUEST_FAILED));
		}
		// 真正沒預料到的錯誤：把完整錯誤印在後端主控台方便除錯，前端只看到通用訊息
		log.error("未預期的錯誤", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse(Messages.SERVER_ERROR));
	}
}
```

**白話解釋**
- `@ExceptionHandler({ A.class, B.class })`：大括號裡可以列好幾種例外，這幾種都交給同一個方法處理。
- **這就是全域例外處理的好處**：修一個地方，全站所有 API 遇到同樣問題都會一起變正確。

### 5-7 測試：FavoriteApiTest.java

**`backend/src/test/java/com/wabisabi/library/FavoriteApiTest.java`**

```java
package com.wabisabi.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.wabisabi.library.entity.Favorite;
import com.wabisabi.library.entity.User;
import com.wabisabi.library.repository.FavoriteRepository;
import com.wabisabi.library.repository.UserRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * 需求 2、3、4、8：加入收藏、收藏清單、刪除收藏、未登入的權限控管。
 */
@SpringBootTest
@AutoConfigureMockMvc
class FavoriteApiTest {

	private static final String SEED_MOBILE = "0900123456";
	private static final String WABI_SABI_ISBN = "9789861343722";
	private static final String QUESTION_ISBN = "9789863980315";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private FavoriteRepository favoriteRepository;

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	@BeforeEach
	void setUp() {
		// 每個測試都從「只有種子會員 Cooqle、沒有任何收藏」的狀態開始
		favoriteRepository.deleteAll();
		for (User user : userRepository.findAll()) {
			if (!user.getMobile().equals(SEED_MOBILE)) {
				userRepository.delete(user);
			}
		}
	}

	// ===== 小工具 =====

	/** 登入（或註冊）並取得 Session */
	private MockHttpSession login(String nickname, String mobile) throws Exception {
		String json = jsonMapper.writeValueAsString(Map.of("nickname", nickname, "mobile", mobile));
		return (MockHttpSession) mockMvc
				.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isOk())
				.andReturn().getRequest().getSession(false);
	}

	/** 加入收藏 */
	private ResultActions addFavorite(MockHttpSession session, String isbn) throws Exception {
		String json = jsonMapper.writeValueAsString(Map.of("isbn", isbn));
		return mockMvc.perform(post("/api/favorites/add")
				.session(session).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private long favoriteCountOf(String mobile) {
		Long userId = userRepository.findByMobile(mobile).orElseThrow().getId();
		return favoriteRepository.findByUserIdOrderByIdAsc(userId).size();
	}

	// ===== 需求 2：加入收藏 =====

	@Test
	@DisplayName("Scenario: 已登入會員成功將書籍加入個人收藏")
	void addFavoriteSuccess() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);

		addFavorite(cooqle, WABI_SABI_ISBN)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("已成功加入收藏"));

		Long cooqleId = userRepository.findByMobile(SEED_MOBILE).orElseThrow().getId();
		assertThat(favoriteRepository.existsByUserIdAndBookIsbn(cooqleId, WABI_SABI_ISBN)).isTrue();
	}

	@Test
	@DisplayName("Scenario: 已登入會員重複收藏書籍時跳出提示")
	void addFavoriteDuplicate() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);
		addFavorite(cooqle, WABI_SABI_ISBN).andExpect(status().isCreated());

		addFavorite(cooqle, WABI_SABI_ISBN)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("此書籍已在收藏清單中，請勿重複收藏"));

		assertThat(favoriteCountOf(SEED_MOBILE)).isEqualTo(1); // 數量沒有增加
	}

	@Test
	@DisplayName("規則：加入本機沒有的書 → 404 找不到此書籍")
	void addFavoriteBookNotFound() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);

		addFavorite(cooqle, "9780000000000")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("找不到此書籍"));
	}

	@Test
	@DisplayName("規則：加入收藏時 ISBN 格式錯誤 → 400")
	void addFavoriteInvalidIsbn() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);

		addFavorite(cooqle, "INVALID_ISBN")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("ISBN 碼格式錯誤或無效"));
	}

	@Test
	@DisplayName("規則：不相信前端傳來的 user_id，收藏一律記在登入者名下")
	void ignoreUserIdFromClient() throws Exception {
		login("Reader_B", "0911222333"); // 先註冊 Reader_B
		Long readerBId = userRepository.findByMobile("0911222333").orElseThrow().getId();
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);

		// Cooqle 登入中，卻在內容裡偷塞 Reader_B 的 id
		String json = jsonMapper.writeValueAsString(Map.of("isbn", WABI_SABI_ISBN, "userId", readerBId));
		mockMvc.perform(post("/api/favorites/add").session(cooqle).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isCreated());

		assertThat(favoriteCountOf(SEED_MOBILE)).isEqualTo(1);
		assertThat(favoriteCountOf("0911222333")).isZero();
	}

	// ===== 需求 3：收藏清單 =====

	@Test
	@DisplayName("Scenario: 已登入會員在收藏頁面檢視自己的收藏清單")
	void listOwnFavorites() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);
		addFavorite(cooqle, WABI_SABI_ISBN).andExpect(status().isCreated());
		addFavorite(cooqle, QUESTION_ISBN).andExpect(status().isCreated());

		// 別的會員的收藏不應該出現在 Cooqle 的清單
		MockHttpSession readerB = login("Reader_B", "0911222333");
		addFavorite(readerB, QUESTION_ISBN).andExpect(status().isCreated());

		mockMvc.perform(get("/api/favorites").session(cooqle))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].isbn").value(WABI_SABI_ISBN))
				.andExpect(jsonPath("$[0].title").value("侘寂：尋找日常中的不完美之美"))
				.andExpect(jsonPath("$[0].author").value("雷納·克魯克"))
				.andExpect(jsonPath("$[0].id").isNumber()) // 「刪除」按鈕要用的收藏編號
				.andExpect(jsonPath("$[1].isbn").value(QUESTION_ISBN))
				.andExpect(jsonPath("$[1].title").value("提問的設計"));
	}

	@Test
	@DisplayName("規則：還沒有收藏時回傳空陣列")
	void listEmpty() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);

		mockMvc.perform(get("/api/favorites").session(cooqle))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	// ===== 需求 8：未登入的權限控管 =====

	@Test
	@DisplayName("Scenario: 未登入直接呼叫收藏相關 API 被拒絕 (查詢、新增、刪除都回 401)")
	void favoritesRequireLogin() throws Exception {
		// 先讓 Cooqle 有一筆收藏，確認未登入的回應裡不會洩漏它
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);
		addFavorite(cooqle, WABI_SABI_ISBN).andExpect(status().isCreated());
		Long favoriteId = favoriteRepository.findAll().get(0).getId();

		mockMvc.perform(get("/api/favorites"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("請先註冊/登入"))
				.andExpect(content().string(not(containsString("侘寂"))));

		mockMvc.perform(post("/api/favorites/add").contentType(MediaType.APPLICATION_JSON)
				.content("{\"isbn\":\"" + QUESTION_ISBN + "\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("請先註冊/登入"));

		mockMvc.perform(delete("/api/favorites/{id}", favoriteId))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("請先註冊/登入"));

		assertThat(favoriteRepository.count()).isEqualTo(1); // 什麼都沒被改到
	}

	@Test
	@DisplayName("Scenario: 在個人頁面登出後無法再查看收藏內容")
	void cannotSeeFavoritesAfterLogout() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);
		addFavorite(cooqle, WABI_SABI_ISBN).andExpect(status().isCreated());

		mockMvc.perform(post("/api/auth/logout").session(cooqle))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("已成功登出"));

		// 帶著登出前的 Session 再來查收藏
		mockMvc.perform(get("/api/favorites").session(cooqle))
				.andExpect(status().isUnauthorized())
				.andExpect(content().string(not(containsString("侘寂"))));
	}

	// ===== 需求 4：刪除收藏 =====

	@Test
	@DisplayName("Scenario: 會員從收藏頁面成功刪除書籍")
	void removeFavoriteSuccess() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);
		addFavorite(cooqle, WABI_SABI_ISBN).andExpect(status().isCreated());
		Long favoriteId = favoriteRepository.findAll().get(0).getId();

		mockMvc.perform(delete("/api/favorites/{id}", favoriteId).session(cooqle))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("已移除收藏"));

		mockMvc.perform(get("/api/favorites").session(cooqle))
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("Scenario: 會員無法刪除其他會員的收藏（回 404）")
	void cannotRemoveOthersFavorite() throws Exception {
		MockHttpSession readerB = login("Reader_B", "0911222333");
		addFavorite(readerB, QUESTION_ISBN).andExpect(status().isCreated());
		Favorite readerBFavorite = favoriteRepository.findAll().get(0);

		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);
		mockMvc.perform(delete("/api/favorites/{id}", readerBFavorite.getId()).session(cooqle))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("找不到此收藏"));

		// Reader_B 的收藏清單維持不變
		assertThat(favoriteRepository.existsById(readerBFavorite.getId())).isTrue();
		assertThat(favoriteCountOf("0911222333")).isEqualTo(1);
	}

	@Test
	@DisplayName("規則：刪除時 id 不是數字 → 400（不是 500）")
	void removeWithNonNumericId() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);

		mockMvc.perform(delete("/api/favorites/{id}", "abc").session(cooqle))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("無法處理此請求"));
	}

	@Test
	@DisplayName("規則：刪除不存在的收藏 → 404")
	void removeNonExisting() throws Exception {
		MockHttpSession cooqle = login("Cooqle", SEED_MOBILE);

		mockMvc.perform(delete("/api/favorites/{id}", 999999).session(cooqle))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("找不到此收藏"));
	}
}
```

**白話解釋**
- `jsonPath("$.length()")`：回應是陣列時，取得它有幾個元素。
- `jsonPath("$[0].title")`：陣列第 1 個元素的 `title`（從 0 開始數）。
- `content().string(not(containsString("侘寂")))`：整個回應內容**不包含**「侘寂」這兩個字。對應 BDD 的「回應內容不應包含任何收藏資料」和「無法看到任何先前收藏的書籍內容」。
- `listOwnFavorites` 刻意讓 Reader_B 也收藏一本書，再確認 Cooqle 的清單只有 2 本：證明「只回傳自己的收藏」。
- `cannotRemoveOthersFavorite` 完全照 BDD 的 Given：Reader_B（0911222333）收藏了「提問的設計」，Cooqle 嘗試刪除它。

### 對應的 Scenario

| BDD Scenario | 狀態 | 測試方法 |
|---|---|---|
| 已登入會員成功將書籍加入個人收藏 | ✅ 後端通過 | `addFavoriteSuccess` |
| 已登入會員重複收藏書籍時跳出提示 | ✅ 後端通過 | `addFavoriteDuplicate` |
| 已登入會員在收藏頁面檢視自己的收藏清單 | ✅ 後端通過 | `listOwnFavorites` |
| 未登入直接呼叫收藏相關 API 被拒絕 | ✅ 後端通過（查詢、新增、刪除都測） | `favoritesRequireLogin` |
| 會員從收藏頁面成功刪除書籍 | ✅ 後端通過（確認視窗在前端，步驟 9） | `removeFavoriteSuccess` |
| 會員無法刪除其他會員的收藏 | ✅ 後端通過（404） | `cannotRemoveOthersFavorite` |
| 在個人頁面登出後無法再查看收藏內容 | ✅ 後端通過（補上步驟 4 缺的「看不到收藏」） | `cannotSeeFavoritesAfterLogout` |

**後端 Scenario 總覽（步驟 1～5 完成後）**

| 需求 | Scenario 數 | 後端 API 測試 |
|---|---|---|
| Background | — | ✅ |
| 需求 1：查詢書籍 | 4（Outline 展開為 7 組） | ✅ 全部（空白 ISBN 主要由前端處理） |
| 需求 5、6：登入/註冊 | 7（Outline 展開為 13 組） | ✅ 全部 |
| 需求 2：加入收藏 | 2 | ✅ 全部 |
| 需求 3、8：收藏頁與權限 | 3 | ✅ API 部分（路由守衛在前端） |
| 需求 4：刪除收藏 | 2 | ✅ 全部 |
| 需求 7、8：個人頁與登出 | 3 | ✅ API 部分（路由守衛在前端） |

「未登入點加入收藏」「未登入訪問收藏頁／個人頁」這 3 個 Scenario 是純前端行為（提示＋導向），會在步驟 6～10 完成，並列入手動測試清單。

### 怎麼驗證成功

**驗證 1：跑測試**
```bash
cd backend
./mvnw test
```
實測結果（2026-10-16）：共 **49** 個測試全部通過。
```
Tests run: 13, Failures: 0, Errors: 0 -- in FavoriteApiTest
Tests run: 19, Failures: 0, Errors: 0 -- in AuthApiTest
Tests run: 8,  Failures: 0, Errors: 0 -- in BookApiTest
Tests run: 5,  Failures: 0, Errors: 0 -- in GoogleBooksClientTest
Tests run: 3,  Failures: 0, Errors: 0 -- in DataSeederTest
Tests run: 1,  Failures: 0, Errors: 0 -- in LibraryApplicationTests
```

**驗證 2：用 curl 實測（連 MySQL）**
```bash
# 登入
curl -c cookies.txt -b cookies.txt -X POST -H "Content-Type: application/json" \
     -d '{"nickname":"Cooqle","mobile":"0900123456"}' http://localhost:8080/api/auth/login
# 加入收藏
curl -c cookies.txt -b cookies.txt -X POST -H "Content-Type: application/json" \
     -d '{"isbn":"9789861343722"}' http://localhost:8080/api/favorites/add
# 收藏清單
curl -c cookies.txt -b cookies.txt http://localhost:8080/api/favorites
# 刪除（把 1 換成清單裡的 id）
curl -c cookies.txt -b cookies.txt -X DELETE http://localhost:8080/api/favorites/1
```

實測結果（2026-10-16）：

| # | 請求 | 回應 |
|---|---|---|
| 1 | 未登入 `GET /api/favorites` | 401 `{"message":"請先註冊/登入"}` |
| 2 | 未登入 `POST /api/favorites/add` | 401 `{"message":"請先註冊/登入"}` |
| 3 | 未登入 `DELETE /api/favorites/1` | 401 `{"message":"請先註冊/登入"}` |
| 4 | 登入 Cooqle | 200 `登入成功` |
| 5 | 加入 9789861343722 | 201 `{"message":"已成功加入收藏"}` |
| 6 | 再加一次 | 409 `{"message":"此書籍已在收藏清單中，請勿重複收藏"}` |
| 7 | 加入 9789863980315 | 201 `已成功加入收藏` |
| 8 | 加入 9780000000000（本機沒有） | 404 `{"message":"找不到此書籍"}` |
| 9 | `GET /api/favorites` | 200 `[{"author":"雷納·克魯克","id":1,"isbn":"9789861343722",...},{"author":"安齋勇樹","id":2,...}]` |
| 10 | `DELETE /api/favorites/999999` | 404 `{"message":"找不到此收藏"}` |
| 11 | `DELETE /api/favorites/abc` | 修正前 500 ❌ → 修正後 400 `{"message":"無法處理此請求"}` ✅ |
| 12 | 刪除 id 1、2 | 200 `{"message":"已移除收藏"}` |
| 13 | 刪除後 `GET /api/favorites` | 200 `[]` |
| 14 | 登出 | 200 `已成功登出` |
| 15 | 登出後 `GET /api/favorites` | 401 `{"message":"請先註冊/登入"}` |

> 實測時加入的 2 筆收藏已在第 12 步刪除，MySQL 回到只有種子資料的狀態。

---

<!-- 步驟 6 之後的內容會在完成時陸續補上 -->

## 如何快速上手

（全部步驟完成後補上）
