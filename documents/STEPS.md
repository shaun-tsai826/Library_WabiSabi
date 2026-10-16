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
- [步驟 6：建立前端專案、共用元件與路由守衛](#步驟-6建立前端專案共用元件與路由守衛)
- [步驟 7：首頁（查詢、錯誤提示、加入收藏）](#步驟-7首頁查詢錯誤提示加入收藏)
- [步驟 8：登入 / 註冊頁](#步驟-8登入--註冊頁)
- [步驟 9：收藏頁（清單、刪除、Session 失效備用畫面）](#步驟-9收藏頁清單刪除session-失效備用畫面)
- [步驟 10：個人頁與登出](#步驟-10個人頁與登出)
- 步驟 11：（完成後補上）
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

## 步驟 6：建立前端專案、共用元件與路由守衛

### 目標
1. 用 create-vue 建立 Vue 3 專案，設定 Vite proxy，把 `/api` 轉給後端。
2. 依 DESIGN.md 第 2 節寫好 CSS 變數，依 3.1 節寫好 RWD 斷點。
3. 寫好全站共用的小工具：提示訊息、呼叫 API、登入狀態、Toast。
4. 做出可重用元件：導覽列、標題、卡片、輸入框、按鈕、錯誤框、空狀態框、Toast。
5. 設定 4 個路由與**路由守衛**：未登入進 `/favorites`、`/profile` 時提示並導向 `/login`。

這一步的 4 個頁面先放「只有標題」的暫時版本，步驟 7～10 再一頁一頁完成。

### 版本資訊（2026-10-16 建立時）
| 套件 | 版本 | 說明 |
|---|---|---|
| create-vue | 3.24.0 | 建立專案的工具 |
| Vue | 3.5 | |
| Vue Router | **5.3** | ⚠️ 網路教學多為 4.x。本專案用到的寫法（`createRouter`、`createWebHistory`、`beforeEach`、`RouterLink`、`RouterView`）在 5.x 都一樣。舊教學的守衛常寫 `next()`，新版建議改成**直接 return**（見 6-7） |
| Vite | 8 | 開發伺服器與打包工具 |
| Node.js | 24（需 22.18 以上或 24.12 以上） | |

### 名詞先解釋
- **Vue**：用「元件」組網頁的框架。一個 `.vue` 檔就是一個元件，裡面有三塊：`<script setup>`（程式）、`<template>`（HTML）、`<style scoped>`（CSS，只影響這個元件）。
- **元件（Component）**：網頁的「樂高積木」。按鈕寫一次，每一頁都能拿來用；要改樣式只改一個地方。
- **Props**：父元件傳給子元件的設定。例如 `<BaseButton variant="danger">` 的 `variant`。
- **Slot（插槽）**：元件裡預留的「空位」，父元件把內容塞進去。例如 `<BaseButton>刪除</BaseButton>` 的「刪除」兩個字。
- **reactive**：把普通物件變成「有變化就會通知畫面更新」的物件。比喻：**公布欄**，誰改了上面的內容，所有看著公布欄的人（元件）都會馬上看到。
- **路由（Router）**：決定「哪個網址顯示哪一頁」。網址從 `/` 變 `/login` 時，不會重新載入整個網頁，只換掉中間的內容。
- **路由守衛（Navigation Guard）**：每次換頁「之前」都會執行的檢查，就像前端的門口警衛。
- **Vite proxy（轉接）**：見 6-2。
- **CORS**：瀏覽器的安全規則，網頁（`localhost:5173`）預設不能隨便呼叫另一個網址（`localhost:8080`）的 API。用 proxy 讓兩者看起來是同一個網址，就不用處理這個問題。

### 6-1 用 create-vue 建立專案

在專案根目錄執行：
```bash
npx create-vue@3.24.0 --router --bare frontend
```
```bash
cd frontend
npm install
```
- `--router`：加入 Vue Router。沒有加的 `--ts`、`--jsx`、`--pinia`、`--vitest`、`--cypress`、`--playwright`、`--eslint` 就是不選（照提示詞的要求）。
- `--bare`：不要範例程式（HelloWorld 那些），省得事後一個一個刪。
- 建好後刪掉 `frontend/.gitignore`（根目錄已經有一份）和 `frontend/README.md`。
- `frontend/.vscode/` 是 create-vue 附的 VS Code 建議設定，已被根目錄 `.gitignore` 排除，留著不影響。

**根目錄 `.gitignore` 補上兩行**（Vite 暫存檔與個人環境變數檔）：
```gitignore
# Vite 偶爾產生的暫存檔、個人環境變數檔
frontend/*.timestamp-*.mjs
frontend/.env.local
```

**前端資料夾結構（這一步完成後）**
```
frontend/
├── index.html                網頁入口
├── vite.config.js            Vite 設定（含 proxy）
├── package.json              套件清單
└── src/
    ├── main.js               程式進入點
    ├── App.vue               整個網站的外框
    ├── messages.js           提示訊息
    ├── api.js                呼叫 API
    ├── auth.js               登入狀態
    ├── toast.js              Toast 狀態
    ├── styles/base.css       CSS 變數與全域樣式
    ├── router/index.js       路由與守衛
    ├── components/           可重用元件（8 個）
    └── views/                4 個頁面
```

### 6-2 Vite 設定：vite.config.js

**`frontend/vite.config.js`**

```js
import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      // 在 import 路徑寫 @ 代表 src 資料夾，例如 import AppToast from '@/components/AppToast.vue'
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    // 開發時的「轉接」：瀏覽器打 http://localhost:5173/api/...，Vite 幫忙轉給後端 http://localhost:8080/api/...
    // 對瀏覽器來說網頁和 API 是同一個網址，所以不用處理 CORS，Session Cookie 也能正常運作
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
```

**白話解釋**
- **proxy 比喻：總機轉接。** 瀏覽器只認識一個電話號碼 `localhost:5173`。打 `/api/...` 開頭的分機時，Vite 總機會幫你轉到後端 `localhost:8080`；其他號碼（網頁本身）就由 Vite 自己接。
  ```
  瀏覽器 ── GET localhost:5173/api/books/978... ──▶ Vite ── 轉接 ──▶ Spring Boot localhost:8080/api/books/978...
  ```
- 對瀏覽器來說，網頁和 API 都來自 `localhost:5173`，所以：
  1. 不會碰到 CORS 限制。
  2. 後端發的 Session Cookie（`JSESSIONID`）會記在 `localhost:5173` 底下，之後呼叫 API 時瀏覽器自動帶上，登入狀態才能維持。
- 這個轉接**只在開發時**（`npm run dev`）有效。正式上線要另外設定，不在這次作業範圍。
- `vueDevTools()`：create-vue 預設附的開發工具，畫面下方會出現一個小圖示，可以查看元件狀態。只在開發時出現。

### 6-3 網頁入口：index.html

**`frontend/index.html`**

```html
<!DOCTYPE html>
<html lang="zh-Hant">
  <head>
    <meta charset="UTF-8">
    <link rel="icon" href="/favicon.ico">
    <!-- RWD 必備：讓手機用實際螢幕寬度顯示，而不是縮小的桌機版 -->
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>LIBRARY / 侘寂書閣</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.js"></script>
  </body>
</html>
```

**白話解釋**
- `lang="zh-Hant"`：告訴瀏覽器這是繁體中文網頁（影響字型選擇與螢幕報讀軟體）。
- `<meta name="viewport" ...>`：**RWD 必備**。沒有這行，手機會把網頁當成 980px 寬的桌機版再縮小顯示，字小到看不清楚。create-vue 預設就有，這裡確認後保留。
- `<div id="app">`：Vue 會把整個網站「掛」在這個 div 裡。

### 6-4 CSS 變數與全域樣式：styles/base.css

**`frontend/src/styles/base.css`**

```css
/*
 * 全域樣式：顏色、尺寸的 CSS 變數（依 DESIGN.md 第 2 節），以及所有頁面共用的基本設定。
 *
 * RWD 斷點（DESIGN.md 3.1 節）：先寫桌機版，再用 @media (max-width: ...) 覆寫較窄的螢幕。
 *   桌機：≥ 1024px（不用寫 @media，就是預設樣式）
 *   平板：768px ～ 1023px  → @media (max-width: 1023px)
 *   手機：< 768px          → @media (max-width: 767px)
 *   極窄：< 360px          → @media (max-width: 359px)（只有個人頁資訊用到）
 * 注意：CSS 變數不能用在 @media 的條件裡，所以斷點直接寫數字。
 */

:root {
  /* ===== 顏色：規範指定色 ===== */
  --color-bg: #F9F8F6;            /* 米白：頁面背景 */
  --color-surface: #EAE6DF;       /* 暖灰：頂部導覽列、卡片背景 */
  --color-text: #4A4643;          /* 炭灰：文字、主要按鈕背景 */
  --color-border: #D3C9BF;        /* 大地淺棕：邊框、分隔線、虛線框 */

  /* ===== 顏色：依線稿估算（約） ===== */
  --color-text-muted: #7A7066;    /* 次要文字：標籤、作者與 ISBN 行 */
  --color-btn-secondary: #8E8475; /* 次要按鈕：「+ 加入收藏」 */
  --color-danger: #A96B6B;        /* 危險按鈕：「刪除」「登出帳號」 */
  --color-danger-bg: #E8D7D7;     /* 錯誤/警示框背景 */
  --color-danger-border: #C39A9A; /* 錯誤/警示框邊框 */
  --color-danger-text: #7A2E2E;   /* 錯誤/警示框文字 */
  --color-input-bg: #FFFFFF;      /* 輸入框背景 */

  /* ===== 線條與圓角 ===== */
  --line: 1.5px;
  --radius-card: 12px;
  --radius-input: 8px;
  --radius-btn: 6px;

  /* ===== 尺寸（會隨螢幕寬度在下方 @media 調整） ===== */
  --page-gutter: 32px;            /* 頁面左右外距 */
  --card-padding: 48px;           /* 卡片內距 */
  --title-size: 32px;             /* 頁面標題字級 */
  --title-underline: 220px;       /* 標題底線寬度 */
  --text-size: 18px;              /* 內文字級 */
  --wide-max: 1200px;             /* 寬容器（首頁、收藏頁）最大寬度 */
  --narrow-max: 720px;            /* 窄卡片（登入、個人頁）最大寬度 */
  --tap-min: 44px;                /* 可點擊區域最小高度 */

  --font-family: "Noto Sans TC", "PingFang TC", "Microsoft JhengHei", sans-serif;
}

/* 平板 */
@media (max-width: 1023px) {
  :root {
    --page-gutter: 24px;
    --card-padding: 32px;
    --title-size: 28px;
    --title-underline: 180px;
  }
}

/* 手機 */
@media (max-width: 767px) {
  :root {
    --page-gutter: 16px;
    --card-padding: 24px;
    --title-size: 24px;
    --title-underline: 120px;
    --text-size: 16px;
  }
}

/* ===== 基本設定 ===== */

/* 讓 width 包含 padding 與 border，計算寬度比較直覺 */
*,
*::before,
*::after {
  box-sizing: border-box;
}

html,
body {
  margin: 0;
  overflow-x: hidden; /* 任何寬度都不出現水平捲軸 */
}

body {
  min-width: 320px; /* 最小支援寬度 */
  background: var(--color-bg);
  color: var(--color-text);
  font-family: var(--font-family);
  font-size: var(--text-size);
  line-height: 1.6;
}

/* 長書名、長暱稱自動換行，不撐破版面 */
h1,
h2,
h3,
p,
span,
div {
  overflow-wrap: anywhere;
}

button,
input {
  font-family: inherit;
  font-size: inherit;
  color: inherit;
}

a {
  color: inherit;
  text-decoration: none;
}

/* ===== 版面容器 ===== */

/* 每一頁的外框：置中、左右留外距 */
.page {
  width: 100%;
  margin: 0 auto;
  padding: 48px var(--page-gutter) 64px;
}

/* 寬容器：首頁、收藏頁 */
.page--wide {
  max-width: calc(var(--wide-max) + var(--page-gutter) * 2);
}

/* 窄容器：登入、個人頁 */
.page--narrow {
  max-width: calc(var(--narrow-max) + var(--page-gutter) * 2);
}

@media (max-width: 767px) {
  .page {
    padding-top: 32px;
    padding-bottom: 48px;
  }
}
```

**白話解釋**
- **CSS 變數**：`--color-text: #4A4643;` 先取名字，之後用 `var(--color-text)` 引用。比喻：**調色盤上的格子**，元件只說「用 3 號格的顏色」，要換顏色時只改調色盤，所有元件一起變。所以元件裡**不會出現任何色碼**。
- `:root`：整份網頁的最上層，定義在這裡的變數全站都能用。
- **RWD 的寫法：在 `@media` 裡改變數的值**。例如手機版把 `--card-padding` 從 48px 改成 24px，所有用到 `var(--card-padding)` 的卡片就會自動跟著縮小，不用每個元件各寫一次 `@media`。
- **為什麼斷點寫數字，不寫成變數？** CSS 規定 `@media (max-width: var(--xxx))` 是無效的，所以斷點直接寫 1023px、767px，並在檔案頂部用註解列出。
- `box-sizing: border-box`：讓 `width: 100%` 包含內距和邊框，不會因為加了 padding 就超出畫面。這是 RWD 最常用的基本設定。
- `overflow-x: hidden` ＋ `min-width: 320px`：任何寬度都不出現水平捲軸，最小支援 320px。
- `overflow-wrap: anywhere`：超長的英文書名或暱稱會自動換行，不會撐破卡片。
- `.page`、`.page--wide`、`.page--narrow`：每一頁最外層的容器。寬的最大 1200px（首頁、收藏頁），窄的最大 720px（登入、個人頁），左右一律保留外距。

### 6-5 提示訊息：messages.js

**`frontend/src/messages.js`**

```js
// 前端用到的提示訊息（文字來自 BDD.md），集中在這裡管理。
// API 的結果訊息（登入成功、已成功加入收藏…）由後端回傳，前端直接顯示 response 的 message，不寫在這裡。
// 這裡只放「前端自己判斷、自己顯示」的訊息；前端欄位驗證的文字與後端 Messages.java 相同。

export const MESSAGES = {
  // ===== 書籍查詢（前端驗證） =====
  ISBN_REQUIRED: '請輸入 ISBN 碼',
  ISBN_INVALID: 'ISBN 碼格式錯誤或無效',

  // ===== 登入 / 註冊（前端驗證） =====
  NICKNAME_REQUIRED: '請輸入暱稱',
  MOBILE_REQUIRED: '請輸入手機號碼',
  NICKNAME_INVALID: '暱稱僅限英文、數字、- 與 _',
  NICKNAME_TOO_LONG: '暱稱長度不可超過 20 個字元',
  MOBILE_INVALID: '請輸入有效的手機號碼格式 (10碼)',

  // ===== 未登入的提示 =====
  /** 未登入點「加入收藏」 */
  FAVORITE_LOGIN_REQUIRED: '請先登入或註冊會員以使用收藏功能',
  /** 未登入進入收藏頁 /favorites */
  FAVORITES_PAGE_LOGIN_REQUIRED: '請先登入後再檢視收藏內容',
  /** 未登入進入個人頁 /profile */
  PROFILE_PAGE_LOGIN_REQUIRED: '請先註冊/登入',

  // ===== 收藏 =====
  /** 刪除前的確認視窗文字（DESIGN.md 4.9） */
  DELETE_CONFIRM: '確定要移除這本書的收藏嗎？',

  // ===== 通用 =====
  /** 連不上後端、或後端回應看不懂時（與後端的 SERVER_ERROR 相同） */
  SERVER_ERROR: '系統發生錯誤，請稍後再試',
}
```

**白話解釋**
- 跟後端的 `Messages.java` 一樣，前端的提示文字都集中在這裡，用法是 `MESSAGES.ISBN_REQUIRED`。
- **分工（計畫階段 Q3 確認）**：
  - API 的結果訊息（「登入成功」「此暱稱已被使用」「已成功加入收藏」…）由後端回傳，前端直接顯示 `result.data.message`，**不寫在這裡**。
  - 這裡只放前端自己判斷的訊息：路由守衛、ISBN 空白、前端欄位驗證、刪除確認文字。
  - 前端欄位驗證的文字和後端一模一樣（因為前後端都要驗證），這是唯一重複的地方，改的時候兩邊都要改。
- `export const`：匯出，其他檔案才能 `import { MESSAGES } from '@/messages'` 拿來用。

### 6-6 呼叫 API：api.js

**`frontend/src/api.js`**

```js
// 呼叫後端 API 的小工具：用瀏覽器內建的 fetch，不用另外安裝 axios。
//
// 每次呼叫都回傳同樣格式的物件，頁面用起來很單純：
//   { ok: true/false, status: HTTP 狀態碼, data: 後端回傳的 JSON }
// 失敗時 data.message 一定有文字，可以直接拿去顯示。
//
// 用法：
//   const result = await api.get('/api/books/9789861343722')
//   if (result.ok) { ...result.data.title... } else { showToast(result.data.message, 'error') }

import { MESSAGES } from '@/messages'

async function request(method, url, body) {
  const options = { method, headers: {} }
  if (body !== undefined) {
    options.headers['Content-Type'] = 'application/json'
    options.body = JSON.stringify(body)
  }

  let response
  try {
    // 網頁和 API 同一個網址（Vite proxy），fetch 預設就會帶上 Session Cookie
    response = await fetch(url, options)
  } catch {
    // 連不上（例如後端沒啟動、網路斷線）
    return { ok: false, status: 0, data: { message: MESSAGES.SERVER_ERROR } }
  }

  // 讀取回應的 JSON；沒有內容或不是 JSON 時當作 null
  let data = null
  try {
    data = await response.json()
  } catch {
    data = null
  }

  // 失敗卻沒有 message（例如 Vite 轉接時後端沒開）：補上通用訊息
  if (!response.ok && !(data && data.message)) {
    data = { message: MESSAGES.SERVER_ERROR }
  }

  return { ok: response.ok, status: response.status, data }
}

export const api = {
  get: (url) => request('GET', url),
  post: (url, body) => request('POST', url, body),
  delete: (url) => request('DELETE', url),
}
```

**白話解釋**
- `fetch(url, options)`：瀏覽器內建的「送請求」功能，不用安裝 axios。
- `async` / `await`：送請求要等網路回應，`await` 的意思是「在這裡等結果回來，再往下執行」，寫起來像一般的順序程式。
- `JSON.stringify(body)`：把 JavaScript 物件轉成 JSON 字串送出；`response.json()` 則是把回應的 JSON 轉回物件。
- **統一回傳 `{ ok, status, data }`**：每一頁都用同樣的方式判斷成功或失敗。
- **三種失敗都整理成「`data.message` 一定有字」**：
  1. 後端回錯誤（400/404/409…）：用後端的 message。
  2. 連不上後端（`fetch` 本身丟出錯誤）：用「系統發生錯誤，請稍後再試」。
  3. 回應沒有 JSON（例如 Vite 轉接時後端沒開）：也用通用訊息。
- fetch 預設的 `credentials: 'same-origin'`：同一個網址時自動帶上 Cookie，所以不用額外設定就能維持登入。

### 6-7 登入狀態：auth.js

**`frontend/src/auth.js`**

```js
// 登入狀態：全站共用的一個變數（不使用 Pinia）。
// reactive 讓它變成「會通知畫面更新」的物件：auth.user 一改，導覽列就會自動跟著變。

import { reactive } from 'vue'
import { api } from '@/api'

export const auth = reactive({
  /** 目前登入的會員 { nickname, mobile }；未登入是 null */
  user: null,
  /** 是否已經問過後端「現在有沒有登入」 */
  ready: false,
})

let loadingPromise = null

/**
 * 打開網頁時呼叫一次 GET /api/auth/me，取得登入狀態。
 * 重新整理頁面後，瀏覽器還留著 Session Cookie，後端就會回傳會員資料，畫面不會被誤判成未登入。
 * 重複呼叫只會真的問後端一次（第二次起直接回傳同一個 Promise）。
 */
export function loadCurrentUser() {
  if (!loadingPromise) {
    loadingPromise = api.get('/api/auth/me').then((result) => {
      auth.user = result.ok ? result.data : null
      auth.ready = true
    })
  }
  return loadingPromise
}

/** 登入成功後呼叫 */
export function setLoggedIn(user) {
  auth.user = { nickname: user.nickname, mobile: user.mobile }
}

/** 登出、或發現 Session 已失效（API 回 401）時呼叫 */
export function setLoggedOut() {
  auth.user = null
}
```

**白話解釋**
- `auth` 是全站共用的「公布欄」：導覽列看著 `auth.user` 決定顯示「登入 / 註冊」還是「Cooqle (個人頁)」；登入頁登入成功後改它，導覽列就自動更新。這就是提示詞說的「登入狀態用簡單的共用變數」，不需要 Pinia。
- **`loadCurrentUser()`（設計決策 13）**：
  - 問題：重新整理頁面時，JavaScript 的變數全部歸零，`auth.user` 會變回 `null`，看起來像沒登入。但其實瀏覽器還留著 Session Cookie，後端仍然記得你。
  - 解法：打開網頁時先呼叫 `GET /api/auth/me` 問後端，路由守衛**等這個答案回來**才決定要不要放行。
- **Promise**：「一張還沒兌現的取貨單」。第一次呼叫時發出請求並記住這張單；之後再呼叫，直接回傳同一張單，不會重複問後端。
- `auth.ready`：還沒問完之前，導覽列先不顯示第三個項目，避免已登入的人看到「登入 / 註冊」閃一下又變成暱稱。
- 未登入時打開網頁，瀏覽器的開發者工具會出現一行紅色的 `401 (Unauthorized)`，那是 `/api/auth/me` 的正常回應（代表「你沒登入」），**不是錯誤**。

### 6-8 Toast 狀態：toast.js

**`frontend/src/toast.js`**

```js
// Toast 提示的狀態：任何頁面呼叫 showToast()，畫面上方就會出現提示，3 秒後自動消失。
// 同一時間只顯示一則，新的會直接取代舊的。

import { reactive } from 'vue'

export const toast = reactive({
  message: '',
  /** 'success'（成功／一般）、'warning'（警告）、'error'（錯誤） */
  type: 'success',
  visible: false,
})

let hideTimer = null

export function showToast(message, type = 'success') {
  toast.message = message
  toast.type = type
  toast.visible = true

  // 重新計時：前一則還沒消失就被新的取代時，從頭算 3 秒
  clearTimeout(hideTimer)
  hideTimer = setTimeout(() => {
    toast.visible = false
  }, 3000)
}
```

**白話解釋**
- 任何頁面只要 `showToast('登入成功')` 就會跳出提示；第二個參數決定樣式：`'success'`（預設）、`'warning'`、`'error'`。
- `setTimeout(..., 3000)`：3 秒後執行，把 `visible` 改成 `false`，Toast 就消失。
- `clearTimeout(hideTimer)`：新提示出現時，取消舊的倒數計時，所以新提示一定會完整顯示 3 秒，而且同時只會有一則。

### 6-9 可重用元件

#### PageTitle.vue：頁面標題

**`frontend/src/components/PageTitle.vue`**

```vue
<!-- 頁面標題：置中粗體＋下方短底線（DESIGN.md 4.2） -->
<script setup>
defineProps({
  title: { type: String, required: true },
})
</script>

<template>
  <h1 class="page-title">{{ title }}</h1>
</template>

<style scoped>
.page-title {
  margin: 0 0 40px;
  font-size: var(--title-size);
  font-weight: 700;
  text-align: center;
  line-height: 1.4;
}

/* 標題下方的短底線 */
.page-title::after {
  content: '';
  display: block;
  width: var(--title-underline);
  max-width: 100%;
  height: var(--line);
  margin: 12px auto 0;
  background: var(--color-text);
}

@media (max-width: 767px) {
  .page-title {
    margin-bottom: 28px;
  }
}
</style>
```

- `defineProps`：宣告這個元件接受哪些設定。`required: true` 代表一定要給。
- `::after`：在標題後面用 CSS「畫」出那條短底線，不用多寫一個 HTML 元素。寬度用 `var(--title-underline)`，所以桌機 220px、平板 180px、手機 120px 會自動切換。

#### BaseCard.vue：卡片

**`frontend/src/components/BaseCard.vue`**

```vue
<!-- 卡片：暖灰背景、細邊框、圓角（DESIGN.md 4.3）。內容用 <slot /> 放進來 -->
<template>
  <div class="card">
    <slot />
  </div>
</template>

<style scoped>
.card {
  background: var(--color-surface);
  border: var(--line) solid var(--color-border);
  border-radius: var(--radius-card);
  padding: var(--card-padding);
}
</style>
```

- `<slot />`：卡片裡要放什麼由使用的人決定。比喻：卡片是**相框**，slot 是放照片的位置。

#### BaseInput.vue：輸入框

**`frontend/src/components/BaseInput.vue`**

```vue
<!--
  輸入框：上方標籤＋白色輸入框（DESIGN.md 4.4）。
  用法：<BaseInput v-model="isbn" label="ISBN 查詢" placeholder="請輸入 13 位數 ISBN 碼..." />
-->
<script setup>
import { useId } from 'vue'

defineProps({
  label: { type: String, required: true },
  placeholder: { type: String, default: '' },
  type: { type: String, default: 'text' },
  /** 手機鍵盤類型，例如 'numeric' 會跳出數字鍵盤 */
  inputmode: { type: String, default: undefined },
  autocomplete: { type: String, default: 'off' },
})

// defineModel：讓父元件可以用 v-model 綁定這個輸入框的值
const model = defineModel({ type: String, default: '' })

// 產生不重複的 id，讓 <label for> 對到 <input id>（點標籤就能聚焦輸入框）
const inputId = useId()
</script>

<template>
  <div class="field">
    <label class="field__label" :for="inputId">{{ label }}</label>
    <input
      :id="inputId"
      v-model="model"
      class="field__input"
      :type="type"
      :placeholder="placeholder"
      :inputmode="inputmode"
      :autocomplete="autocomplete"
    />
  </div>
</template>

<style scoped>
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.field__label {
  color: var(--color-text-muted);
  font-size: 0.95em;
  padding-left: 4px;
}

.field__input {
  width: 100%;
  min-height: 56px;
  padding: 0 24px;
  background: var(--color-input-bg);
  border: var(--line) solid var(--color-border);
  border-radius: var(--radius-input);
  outline: none;
}

.field__input::placeholder {
  color: #A59C92;
}

/* 聚焦時邊框變深，讓使用者知道正在輸入哪一格 */
.field__input:focus {
  border-color: var(--color-text-muted);
}

@media (max-width: 767px) {
  .field__input {
    min-height: 48px;
    padding: 0 16px;
  }
}
</style>
```

- **`defineModel()`**（Vue 3.4 起）：讓元件支援 `v-model`。父元件寫 `<BaseInput v-model="isbn" />`，使用者打字時 `isbn` 會自動更新，反過來改 `isbn` 輸入框也會跟著變。這叫**雙向綁定**。
- `useId()`（Vue 3.5 起）：自動產生不重複的 id。`<label for>` 對到 `<input id>` 之後，點標籤文字就能聚焦輸入框，螢幕報讀軟體也能正確念出欄位名稱。
- `inputmode="numeric"`：手機上會跳出數字鍵盤（輸入 ISBN、手機號碼時好用）。

#### BaseButton.vue：按鈕

**`frontend/src/components/BaseButton.vue`**

```vue
<!--
  按鈕（DESIGN.md 4.5）。
  用法：<BaseButton variant="danger" block @click="logout">登出帳號</BaseButton>
    variant：primary（炭灰，預設）／secondary（棕灰）／danger（紅）
    block：滿版寬度
    spaced：文字加字距（例如「登 入 / 註 冊」）
-->
<script setup>
defineProps({
  variant: { type: String, default: 'primary' },
  type: { type: String, default: 'button' },
  block: { type: Boolean, default: false },
  spaced: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
})
</script>

<template>
  <button
    :type="type"
    :disabled="disabled"
    :class="['btn', `btn--${variant}`, { 'btn--block': block, 'btn--spaced': spaced }]"
  >
    <slot />
  </button>
</template>

<style scoped>
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 56px;
  padding: 0 28px;
  border: none;
  border-radius: var(--radius-btn);
  color: var(--color-bg);
  font-weight: 500;
  cursor: pointer;
}

.btn--primary {
  background: var(--color-text);
}

.btn--secondary {
  background: var(--color-btn-secondary);
}

.btn--danger {
  background: var(--color-danger);
}

.btn--block {
  width: 100%;
}

.btn--spaced {
  letter-spacing: 0.1em;
  font-weight: 700;
}

.btn:hover:not(:disabled) {
  opacity: 0.88;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 767px) {
  .btn {
    min-height: 48px; /* 仍大於 44px，手指好點 */
  }
}
</style>
```

- ``:class="['btn', `btn--${variant}`, {...}]"``：依 props 組出 class。`variant="danger"` 就會有 `btn--danger`，背景變紅。
  - 前面的冒號 `:` 代表「後面是 JavaScript，不是普通文字」。
  - `{ 'btn--block': block }`：`block` 是 true 才加上這個 class。
- `type="button"` 是預設值：放在 `<form>` 裡的按鈕預設是 `submit`，按了會送出表單。要送出的按鈕再明確寫 `type="submit"`。
- 高度 56px（手機 48px），都大於 44px 的最小點擊區域。

#### ErrorBox.vue、EmptyBox.vue：錯誤框與空狀態框

**`frontend/src/components/ErrorBox.vue`**

```vue
<!--
  錯誤／警示框：淡紅底、紅框、深紅粗體字（DESIGN.md 4.6）。
  用法：<ErrorBox>⚠ 錯誤提示：找不到此書籍</ErrorBox>
    center：文字置中（收藏頁的未登入提示用）
-->
<script setup>
defineProps({
  center: { type: Boolean, default: false },
})
</script>

<template>
  <div :class="['error-box', { 'error-box--center': center }]" role="alert">
    <slot />
  </div>
</template>

<style scoped>
.error-box {
  padding: 32px 48px;
  background: var(--color-danger-bg);
  border: var(--line) solid var(--color-danger-border);
  border-radius: var(--radius-input);
  color: var(--color-danger-text);
  font-weight: 700;
}

.error-box--center {
  text-align: center;
}

@media (max-width: 767px) {
  .error-box {
    padding: 24px;
  }
}
</style>
```

**`frontend/src/components/EmptyBox.vue`**

```vue
<!-- 空狀態框：虛線邊框、置中次要色文字（DESIGN.md 4.7） -->
<template>
  <div class="empty-box">
    <slot />
  </div>
</template>

<style scoped>
.empty-box {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  padding: 24px;
  border: var(--line) dashed var(--color-border);
  border-radius: var(--radius-card);
  color: var(--color-text-muted);
  text-align: center;
}
</style>
```

- `role="alert"`：錯誤框出現時，螢幕報讀軟體會主動念出來。

#### AppToast.vue：Toast

**`frontend/src/components/AppToast.vue`**

```vue
<!--
  Toast 提示（DESIGN.md 4.8）：畫面上方置中、3 秒後自動消失、同時只顯示一則。
  只要在 App.vue 放一個；各頁面呼叫 toast.js 的 showToast() 就會出現。
-->
<script setup>
import { toast } from '@/toast'
</script>

<template>
  <!-- Transition：出現與消失時套用淡入淡出（唯一允許的動畫） -->
  <Transition name="toast-fade">
    <div v-if="toast.visible" :class="['toast', `toast--${toast.type}`]" role="status" aria-live="polite">
      {{ toast.message }}
    </div>
  </Transition>
</template>

<style scoped>
.toast {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
  width: max-content;
  max-width: min(560px, 90vw); /* 手機上最多為螢幕寬度的 90% */
  padding: 14px 28px;
  border: var(--line) solid transparent;
  border-radius: var(--radius-input);
  text-align: center;
  font-weight: 500;
}

/* 成功／一般提示 */
.toast--success {
  background: var(--color-text);
  border-color: var(--color-text);
  color: var(--color-bg);
}

/* 警告 */
.toast--warning {
  background: var(--color-border);
  border-color: var(--color-text-muted);
  color: var(--color-text);
}

/* 錯誤：沿用錯誤框的紅色系 */
.toast--error {
  background: var(--color-danger-bg);
  border-color: var(--color-danger-border);
  color: var(--color-danger-text);
  font-weight: 700;
}

/* 淡入淡出 */
.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: opacity 0.3s;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
}
</style>
```

- 整個網站只放**一個** AppToast（在 App.vue），它看著 `toast.js` 的公布欄：`visible` 變 true 就出現。
- `<Transition name="toast-fade">`：Vue 內建的動畫外框。元件出現或消失時，自動套上 `toast-fade-enter-active` 等 class，搭配 CSS 做出 0.3 秒的淡入淡出（DESIGN.md 唯一允許的動畫）。
- `position: fixed` ＋ `left: 50%` ＋ `transform: translateX(-50%)`：固定在畫面上方正中間，捲動頁面也不會跑掉。
- `max-width: min(560px, 90vw)`：最寬 560px，手機上不超過螢幕寬度的 90%（`vw` = 螢幕寬度的 1%）。

#### AppNavbar.vue：頂部導覽列

**`frontend/src/components/AppNavbar.vue`**

```vue
<!--
  頂部導覽列（DESIGN.md 4.1）：所有頁面共用。
  左：LIBRARY / 侘寂書閣（回首頁）；右：首頁 ｜ 我的收藏 ｜ 登入 / 註冊（或「暱稱 (個人頁)」）
-->
<script setup>
import { useRoute } from 'vue-router'
import { auth } from '@/auth'

// 目前所在的網址，用來把「目前頁面」的項目加粗
const route = useRoute()
</script>

<template>
  <header class="navbar">
    <RouterLink to="/" class="navbar__logo">LIBRARY / <strong>侘寂書閣</strong></RouterLink>

    <nav class="navbar__menu">
      <RouterLink to="/" :class="['navbar__item', { 'is-active': route.path === '/' }]">首頁</RouterLink>

      <!-- 未登入點這裡時，路由守衛會提示並導向登入頁（見 router/index.js） -->
      <RouterLink
        to="/favorites"
        :class="['navbar__item', 'navbar__item--divider', { 'is-active': route.path === '/favorites' }]"
      >
        我的收藏
      </RouterLink>

      <!-- 還沒問完後端「有沒有登入」之前先不顯示，避免畫面閃一下「登入 / 註冊」 -->
      <template v-if="auth.ready">
        <RouterLink
          v-if="auth.user"
          to="/profile"
          :class="['navbar__item', 'navbar__item--divider', { 'is-active': route.path === '/profile' }]"
        >
          {{ auth.user.nickname }} (個人頁)
        </RouterLink>
        <RouterLink
          v-else
          to="/login"
          :class="['navbar__item', 'navbar__item--divider', 'navbar__item--muted', { 'is-active': route.path === '/login' }]"
        >
          登入 / 註冊
        </RouterLink>
      </template>
    </nav>
  </header>
</template>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  min-height: 104px;
  padding: 16px max(var(--page-gutter), calc((100% - 1440px) / 2));
  background: var(--color-surface);
}

.navbar__logo {
  font-size: 30px;
  white-space: nowrap;
}

.navbar__logo strong {
  font-weight: 700;
}

.navbar__menu {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 40px;
}

.navbar__item {
  display: inline-flex;
  align-items: center;
  min-height: var(--tap-min); /* 至少 44px 高，方便手指點擊 */
  font-size: 20px;
}

/* 「我的收藏」與第三項前面的細直線 */
.navbar__item--divider::before {
  content: '';
  display: inline-block;
  width: 1px;
  height: 1em;
  margin-right: 14px;
  background: var(--color-text);
}

/* 「登入 / 註冊」平常是較淺的灰色 */
.navbar__item--muted {
  color: var(--color-text-muted);
}

/* 目前所在頁面：加粗 */
.navbar__item.is-active {
  font-weight: 700;
}

/* 平板：字小一點 */
@media (max-width: 1023px) {
  .navbar__logo {
    font-size: 24px;
  }

  .navbar__menu {
    gap: 8px 24px;
  }

  .navbar__item {
    font-size: 18px;
  }
}

/* 手機：logo 一列置中；三個項目在下一列置中（不使用漢堡選單） */
@media (max-width: 767px) {
  .navbar {
    flex-direction: column;
    gap: 4px;
    min-height: auto;
    padding: 16px var(--page-gutter) 8px;
  }

  .navbar__logo {
    font-size: 22px;
  }

  .navbar__menu {
    justify-content: center;
    gap: 0 16px;
  }

  .navbar__item {
    font-size: 16px;
  }

  .navbar__item--divider::before {
    margin-right: 10px;
  }
}
</style>
```

- `RouterLink`：Vue Router 的連結。跟 `<a href>` 很像，但點了不會重新載入整個網頁，只換中間的內容。
- `useRoute()`：取得目前的網址，`route.path === '/favorites'` 就把「我的收藏」加粗。
- `v-if` / `v-else`：已登入顯示「暱稱 (個人頁)」，否則顯示「登入 / 註冊」。暱稱用註冊時的原樣（例如 `Cooqle`）。
- 「我的收藏」**不管有沒有登入都連到 `/favorites`**。未登入時由路由守衛擋下並提示，規則只寫在一個地方。
- **手機版**：`flex-direction: column` 讓 logo 和選單上下排列，選單 `justify-content: center` 置中，不使用漢堡選單。
- `padding: 16px max(var(--page-gutter), calc((100% - 1440px) / 2))`：超寬螢幕時，導覽列內容不會貼到最左右兩邊。

### 6-10 路由與守衛：router/index.js

**`frontend/src/router/index.js`**

```js
// 路由：決定「哪個網址顯示哪一頁」，以及未登入時能不能進去（路由守衛）。

import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import LoginView from '@/views/LoginView.vue'
import FavoritesView from '@/views/FavoritesView.vue'
import ProfileView from '@/views/ProfileView.vue'
import { auth, loadCurrentUser } from '@/auth'
import { showToast } from '@/toast'
import { MESSAGES } from '@/messages'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', component: HomeView },
    { path: '/login', component: LoginView },
    {
      path: '/favorites',
      component: FavoritesView,
      // meta：自己加的備註。requiresAuth = 需要登入；loginMessage = 未登入時的提示
      meta: { requiresAuth: true, loginMessage: MESSAGES.FAVORITES_PAGE_LOGIN_REQUIRED },
    },
    {
      path: '/profile',
      component: ProfileView,
      meta: { requiresAuth: true, loginMessage: MESSAGES.PROFILE_PAGE_LOGIN_REQUIRED },
    },
    // 打錯網址：回首頁
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

// 路由守衛：每次換頁「之前」都會執行
router.beforeEach(async (to) => {
  // 先等後端回答「現在有沒有登入」（只有打開網頁的第一次需要真的等）
  await loadCurrentUser()

  if (to.meta.requiresAuth && !auth.user) {
    showToast(to.meta.loginMessage, 'warning')
    return '/login' // 回傳網址 = 改去這一頁
  }
  // 不回傳任何東西 = 放行
})

export default router
```

**白話解釋**
- `routes`：網址與頁面的對照表。
- `meta`：我們自己在路由上加的「備註」。`requiresAuth: true` 代表需要登入，`loginMessage` 是未登入時要顯示的提示。守衛只要看備註，不用寫死「如果是 /favorites 就…」。
- **守衛的流程**：
  ```
  使用者要去 /favorites
     │
     ▼
  await loadCurrentUser()     ← 第一次打開網頁時，等後端回答有沒有登入
     │
     ├─ 需要登入，但 auth.user 是 null → 跳 Toast「請先登入後再檢視收藏內容」→ return '/login'（改去登入頁）
     └─ 其他情況 → 不 return（放行）
  ```
- **新舊寫法**：舊教學（Vue Router 3、4）常寫 `beforeEach((to, from, next) => { ... next('/login') })`，忘了呼叫 `next()` 畫面就會卡住。新寫法直接 `return` 目標網址，不 return 就是放行，比較不會出錯。Vue Router 5 建議用新寫法。
- `/:pathMatch(.*)*`：「其他所有網址」，打錯網址就導回首頁。

### 6-11 網站外框與進入點：App.vue、main.js

**`frontend/src/App.vue`**

```vue
<!-- 整個網站的外框：導覽列＋目前頁面＋Toast -->
<script setup>
import AppNavbar from '@/components/AppNavbar.vue'
import AppToast from '@/components/AppToast.vue'
</script>

<template>
  <AppNavbar />
  <main>
    <!-- 依網址顯示對應的頁面（router/index.js 決定） -->
    <RouterView />
  </main>
  <AppToast />
</template>
```

**`frontend/src/main.js`**

```js
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
// 全域樣式（CSS 變數、基本設定）：只在這裡載入一次，所有元件都能用
import './styles/base.css'

const app = createApp(App)

app.use(router)

app.mount('#app')
```

- `<RouterView />`：「頁面放這裡」。網址是 `/login` 時，這裡就顯示 LoginView。
- `import './styles/base.css'`：全域樣式在進入點載入一次，所有元件都吃得到 CSS 變數。

### 6-12 暫時的頁面

4 個頁面先只放標題，例如：

**`frontend/src/views/FavoritesView.vue`**（暫時版本）

```vue
<!-- 暫時的頁面：步驟 9 完成 -->
<script setup>
import PageTitle from '@/components/PageTitle.vue'
</script>

<template>
  <div class="page page--wide">
    <PageTitle title="我的收藏 — 書籍清單" />
  </div>
</template>
```

其他三頁（`HomeView.vue`、`LoginView.vue`、`ProfileView.vue`）結構相同，只有標題和寬窄不同：首頁 `page--wide`「首頁 — 書籍查詢」；登入 `page--narrow`「會員 登入 / 註冊」；個人頁 `page--narrow`「個人資訊」。

### 對應的 Scenario

| BDD Scenario | 狀態 | 由誰實作 |
|---|---|---|
| 未登入使用者嘗試訪問收藏頁面被拒絕 | ✅ 前端通過（提示＋導向 /login） | `router/index.js` 守衛 |
| 未登入使用者嘗試訪問個人頁面被拒絕 | ✅ 前端通過（提示「請先註冊/登入」＋導向 /login） | `router/index.js` 守衛 |
| 重新整理後仍維持登入（設計決策 13） | ✅ 通過 | `auth.js` 的 `loadCurrentUser` |

### 怎麼驗證成功

**啟動方式（之後每一步都一樣，要開兩個終端機視窗）**
```bash
cd backend
./mvnw spring-boot:run
```
```bash
cd frontend
npm run dev
```
用瀏覽器打開 <http://localhost:5173>。

**驗證 1：能編譯**
```bash
cd frontend
npm run build
```
預期看到 `✓ built in ...ms`，沒有錯誤。（產生的 `dist/` 資料夾可以刪掉，它已被 `.gitignore` 排除。）

**驗證 2：proxy 有效**
```bash
curl http://localhost:5173/api/books/9789861343722
```
打的是 Vite 的 5173，卻拿到後端的書籍 JSON，代表轉接成功。

**驗證 3：瀏覽器實測（2026-10-16）**

| # | 操作 | 結果 |
|---|---|---|
| 1 | 未登入打開首頁 | 導覽列：「首頁」粗體｜我的收藏｜登入 / 註冊（淺灰） ✅ |
| 2 | 未登入點「我的收藏」 | 網址變 `/login`，上方出現警告 Toast「請先登入後再檢視收藏內容」，3 秒後淡出 ✅ |
| 3 | 未登入直接在網址列輸入 `/profile` | 導向 `/login`，Toast「請先註冊/登入」 ✅ |
| 4 | 登入 Cooqle 後重新整理 `/profile` | 停留在個人頁，導覽列顯示「Cooqle (個人頁)」且為粗體，**沒有**被誤判成未登入 ✅ |
| 5 | 登出後再點「我的收藏」 | 再次被擋下並導向 `/login` ✅ |

> 第 4 步的登入，這一步是在瀏覽器開發者工具的 Console 執行下面這行（步驟 8 做完登入頁後就不用了）：
> ```js
> await fetch('/api/auth/login', {method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({nickname:'Cooqle', mobile:'0900123456'})})
> ```

**驗證 4：RWD（瀏覽器開發者工具的「裝置模式」）**

| 寬度 | 結果 |
|---|---|
| 1280px（桌機） | logo 在左、三個項目在右，同一列 ✅ |
| 768px（平板） | 同桌機，字級略小 ✅ |
| 375px（手機） | logo 一列置中，三個項目在下一列置中；每個項目高 44px ✅ |
| 320px（最小） | 沒有水平捲軸（頁面寬度 = 320px） ✅ |

開啟方式：Chrome 按 `F12`（Mac 是 `Cmd + Option + I`）→ 點左上角的手機平板圖示 → 上方輸入寬度。

---

## 步驟 7：首頁（查詢、錯誤提示、加入收藏）

### 目標
完成首頁 `/` 的三種狀態（DESIGN.md 5.1）與「加入收藏」：
- (a) 初始：虛線空狀態框
- (b) 查詢成功：結果卡片＋「+ 加入收藏」
- (c) 查詢錯誤：紅色錯誤框「⚠ 錯誤提示：{訊息}」
- 查詢中：按鈕顯示「查詢中...」並暫時停用
- 加入收藏：未登入 → 提示並導向登入頁；已登入 → 成功／重複提示

### 名詞先解釋
- **`ref`**：Vue 的「會通知畫面的變數」，適合放單一值（字串、數字、物件）。讀寫時要加 `.value`（`book.value = ...`），但在 `<template>` 裡直接寫 `book` 就好。
  - 和 `reactive` 的差別：`reactive` 用在「一整個物件」（像 `auth`、`toast`），`ref` 用在「單一變數」。
- **`v-if` / `v-else-if` / `v-else`**：條件顯示。像一串 if-else，只會顯示**第一個**成立的區塊。首頁就靠它在三種狀態之間切換。
- **`@submit.prevent`**：表單送出時（按按鈕或按 Enter）執行函式，`.prevent` 阻止瀏覽器預設的「重新整理網頁」。
- **`encodeURIComponent`**：把字串轉成可以安全放進網址的格式（例如空白變 `%20`）。

### 建立或修改的檔案
```
frontend/src/
├── validators.js         新增：前端欄位驗證（這一步先放 ISBN）
└── views/HomeView.vue    修改：完成首頁
```

### 7-1 前端驗證：validators.js

**`frontend/src/validators.js`**

```js
// 前端欄位驗證：規則與後端相同（後端仍會再驗證一次，前端驗證只是讓使用者更快看到提示）。
// 每個函式回傳「錯誤訊息」；沒有錯誤時回傳空字串 ''。

import { MESSAGES } from '@/messages'

/** ISBN：去除前後空白後，必須是 13 碼數字 */
export function validateIsbn(isbn) {
  if (isbn === '') {
    return MESSAGES.ISBN_REQUIRED
  }
  if (!/^[0-9]{13}$/.test(isbn)) {
    return MESSAGES.ISBN_INVALID
  }
  return ''
}
```

**白話解釋**
- `/^[0-9]{13}$/` 是 JavaScript 的正規表示式寫法（前後用斜線包起來），規則和後端的 `[0-9]{13}` 一樣。`.test(isbn)` 回傳 true 或 false。
- **為什麼前後端都要驗證？**
  - 前端驗證：使用者馬上看到提示，不用等網路來回（體驗好）。
  - 後端驗證：前端的程式碼使用者看得到也改得到，有人可以跳過畫面直接呼叫 API（安全）。
  - 所以**後端驗證是必須的，前端驗證是加分**。
- **空白 ISBN 為什麼只能由前端處理？**（設計決策 10）API 網址是 `/api/books/{isbn}`，ISBN 空白時網址變成 `/api/books/`，根本對不到這支 API。所以由前端直接顯示「請輸入 ISBN 碼」，不呼叫 API。
- 之後的登入頁（步驟 8）也會在這個檔案加上暱稱、手機的驗證。

### 7-2 首頁：HomeView.vue

**`frontend/src/views/HomeView.vue`**

```vue
<!--
  首頁 /：用 ISBN 查詢書籍（DESIGN.md 5.1）。
  三種狀態：初始（虛線空狀態框）、查詢成功（結果卡片）、查詢錯誤（紅色錯誤框）。
-->
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import PageTitle from '@/components/PageTitle.vue'
import BaseInput from '@/components/BaseInput.vue'
import BaseButton from '@/components/BaseButton.vue'
import BaseCard from '@/components/BaseCard.vue'
import ErrorBox from '@/components/ErrorBox.vue'
import EmptyBox from '@/components/EmptyBox.vue'
import { api } from '@/api'
import { auth, setLoggedOut } from '@/auth'
import { showToast } from '@/toast'
import { MESSAGES } from '@/messages'
import { validateIsbn } from '@/validators'

const router = useRouter()

// ===== 畫面狀態（ref：值一改，畫面就自動更新） =====
const isbnInput = ref('') // 輸入框的內容（保留使用者輸入的原樣）
const book = ref(null) // 查到的書；null = 沒有結果
const errorMessage = ref('') // 錯誤訊息；'' = 沒有錯誤
const searching = ref(false) // 是否正在查詢（按鈕顯示「查詢中...」）
const adding = ref(false) // 是否正在加入收藏（避免連按）

/** 按下「查詢」 */
async function search() {
  const isbn = isbnInput.value.trim()

  // 每次查詢前先清掉上一次的結果與錯誤
  book.value = null
  errorMessage.value = ''

  // 前端先驗證：空白、格式錯誤都不呼叫 API（設計決策 10）
  const invalid = validateIsbn(isbn)
  if (invalid) {
    errorMessage.value = invalid
    return
  }

  searching.value = true
  const result = await api.get(`/api/books/${encodeURIComponent(isbn)}`)
  searching.value = false

  if (result.ok) {
    book.value = result.data
  } else {
    // 404 找不到此書籍、503 查詢失敗，請稍後再試…都直接顯示後端的訊息
    errorMessage.value = result.data.message
  }
}

/** 按下「+ 加入收藏」 */
async function addFavorite() {
  // 未登入：提示並導向登入/註冊頁
  if (!auth.user) {
    goLogin()
    return
  }

  adding.value = true
  const result = await api.post('/api/favorites/add', { isbn: book.value.isbn })
  adding.value = false

  if (result.ok) {
    showToast(result.data.message) // 已成功加入收藏
  } else if (result.status === 409) {
    showToast(result.data.message, 'warning') // 此書籍已在收藏清單中，請勿重複收藏
  } else if (result.status === 401) {
    // 畫面以為有登入，但 Session 已經失效：當作未登入處理
    setLoggedOut()
    goLogin()
  } else {
    showToast(result.data.message, 'error')
  }
}

function goLogin() {
  showToast(MESSAGES.FAVORITE_LOGIN_REQUIRED, 'warning')
  router.push('/login')
}

/** 欄位沒有資料時顯示「—」 */
function orDash(value) {
  return value ? value : '—'
}
</script>

<template>
  <div class="page page--wide">
    <PageTitle title="首頁 — 書籍查詢" />

    <!-- form + @submit.prevent：按 Enter 也能查詢，而且不會重新整理網頁 -->
    <form class="search" @submit.prevent="search">
      <BaseInput
        v-model="isbnInput"
        class="search__input"
        label="ISBN 查詢"
        placeholder="請輸入 13 位數 ISBN 碼..."
        inputmode="numeric"
      />
      <BaseButton type="submit" class="search__button" spaced :disabled="searching">
        {{ searching ? '查詢中...' : '查詢' }}
      </BaseButton>
    </form>

    <!-- 狀態 (c)：查詢錯誤 -->
    <ErrorBox v-if="errorMessage">⚠ 錯誤提示：{{ errorMessage }}</ErrorBox>

    <!-- 狀態 (b)：查詢成功 -->
    <BaseCard v-else-if="book">
      <p class="result__caption"><strong>查詢結果</strong> (Search Result)</p>
      <h2 class="result__title">書名：{{ book.title }}</h2>
      <p class="result__line">作者：{{ orDash(book.author) }}</p>
      <p class="result__line">出版日期：{{ orDash(book.publishDate) }}</p>
      <p class="result__isbn">ISBN：{{ book.isbn }}</p>
      <BaseButton variant="secondary" class="result__add" :disabled="adding" @click="addFavorite">
        + 加入收藏
      </BaseButton>
    </BaseCard>

    <!-- 狀態 (a)：初始（查詢中不顯示） -->
    <EmptyBox v-else-if="!searching">
      請在上方輸入 ISBN 碼並點擊「查詢」以檢視書籍詳細資訊
    </EmptyBox>
  </div>
</template>

<style scoped>
/* ===== 查詢列：輸入框約 70%＋按鈕約 30%，同一列 ===== */
.search {
  display: flex;
  align-items: flex-end; /* 按鈕對齊輸入框底部（輸入框上方有標籤） */
  gap: 32px;
  margin-bottom: 48px;
}

.search__input {
  flex: 7;
}

.search__button {
  flex: 3;
}

/* ===== 結果卡片 ===== */
.result__caption {
  margin: 0 0 24px;
  color: var(--color-text-muted);
}

.result__title {
  margin: 0 0 20px;
  font-size: 1.3em;
  font-weight: 700;
}

.result__line {
  margin: 0 0 16px;
}

.result__isbn {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.9em;
}

/* 「+ 加入收藏」：靠左、約卡片寬 35%；與 ISBN 行至少隔 16px（DESIGN.md 5.1-b） */
.result__add {
  width: 35%;
  margin-top: 24px;
}

/* 平板：按鈕約卡片寬 50% */
@media (max-width: 1023px) {
  .search {
    gap: 24px;
  }

  .result__add {
    width: 50%;
  }
}

/* 手機：輸入框與按鈕上下排列、各自滿寬；加入收藏按鈕滿寬 */
@media (max-width: 767px) {
  .search {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
    margin-bottom: 32px;
  }

  .result__add {
    width: 100%;
  }
}
</style>
```

**白話解釋**

**畫面狀態**：整個首頁只靠 5 個 `ref` 決定要顯示什麼。
| 變數 | 意思 | 影響畫面 |
|---|---|---|
| `isbnInput` | 輸入框內容 | 保留使用者輸入的原樣（DESIGN 規定錯誤時輸入框要保留） |
| `book` | 查到的書 | 有值 → 顯示結果卡片 |
| `errorMessage` | 錯誤訊息 | 有值 → 顯示錯誤框 |
| `searching` | 查詢中 | 按鈕變「查詢中...」並停用；不顯示空狀態框 |
| `adding` | 加入收藏中 | 按鈕停用，避免連按送出兩次 |

**三種狀態的切換**（`<template>` 裡的 `v-if` 鏈）：
```
errorMessage 有值？ → 錯誤框 (c)
否則 book 有值？    → 結果卡片 (b)
否則不在查詢中？    → 空狀態框 (a)
（查詢中：三個都不顯示）
```

**`search()` 的流程**
1. `trim()` 去除前後空白。
2. 清掉上一次的結果與錯誤，避免新舊畫面混在一起。
3. 前端驗證：不合格就顯示錯誤，`return` 結束，**不呼叫 API**。
4. `searching = true`，按鈕變「查詢中...」。
5. `await api.get(...)` 等後端回應。
6. 成功 → `book` 放資料；失敗 → `errorMessage` 放後端回傳的訊息（找不到此書籍、查詢失敗，請稍後再試…）。

**`addFavorite()` 的流程**
| 情況 | 處理 | 對應 |
|---|---|---|
| 畫面上是未登入（`auth.user` 是 null） | 警告 Toast「請先登入或註冊會員以使用收藏功能」＋ `router.push('/login')` | BDD Scenario |
| 201 成功 | 成功 Toast，顯示後端的「已成功加入收藏」 | BDD Scenario |
| 409 重複 | 警告 Toast，顯示後端的「此書籍已在收藏清單中，請勿重複收藏」 | BDD Scenario（BDD 寫的是「警告訊息」，所以用 warning 樣式） |
| 401 | 畫面以為有登入，其實 Session 已失效：`setLoggedOut()` 後，跟未登入一樣處理 | 計畫階段的預設決定 |
| 其他錯誤 | 錯誤 Toast | — |

- `router.push('/login')`：用程式換頁，效果和點 `RouterLink` 一樣。
- `orDash()`：Google Books 偶爾沒有作者或出版日期，這時顯示「—」，不會只留一個冒號。

**樣式重點**
- 查詢列用 `display: flex`，輸入框 `flex: 7`、按鈕 `flex: 3`，比例約 70%：30%。
- `align-items: flex-end`：輸入框上方有標籤，按鈕對齊輸入框的**底部**才會跟輸入框同高。
- 「+ 加入收藏」寬度：桌機 35%、平板 50%、手機 100%（DESIGN.md 3.1）。
- `.result__add` 的 `margin-top: 24px`：線稿中 ISBN 行和按鈕太近，DESIGN.md 要求至少 16px。
- 手機版：`flex-direction: column` 讓輸入框和按鈕上下排列，`align-items: stretch` 讓兩者都滿寬。

### 對應的 Scenario

| BDD Scenario | 狀態 | 說明 |
|---|---|---|
| 成功查詢到存在的書籍資訊 | ✅ 前後端通過 | 顯示書名、作者、出版日期與「加入收藏」按鈕 |
| 本機沒有但 Google Books 有收錄的書籍 | 🔸 後端以 Mock 通過；畫面沿用同一個結果卡片 | 實測當天 Google 回 429，無法用真實資料測 |
| 本機沒有且 Google Books 無法連線時顯示錯誤 | ✅ 前後端通過 | Google 額度用完（429）剛好就是真實的「無法使用」情境，畫面顯示「查詢失敗，請稍後再試」 |
| 無效或不存在的 ISBN：9780000000000 | 🔸 後端以 Mock 通過 | 真實環境下 Google 回 429，所以畫面顯示的是「查詢失敗，請稍後再試」；Google 恢復後應顯示「找不到此書籍」 |
| 無效或不存在的 ISBN：INVALID_ISBN | ✅ 前後端通過 | 前端直接擋下，沒有呼叫 API |
| 無效或不存在的 ISBN：978986134372 | ✅ 前後端通過 | 同上 |
| 無效或不存在的 ISBN：空白 | ✅ 前端通過 | 前端直接顯示「請輸入 ISBN 碼」 |
| 未登入狀態下點擊「加入收藏」需引導至登入/註冊 | ✅ 前端通過 | Toast＋導向 /login |
| 已登入會員成功將書籍加入個人收藏 | ✅ 前後端通過 | Toast「已成功加入收藏」 |
| 已登入會員重複收藏書籍時跳出提示 | ✅ 前後端通過 | 警告 Toast，收藏數量維持 1 |

### 怎麼驗證成功

啟動後端和前端（見步驟 6），打開 <http://localhost:5173>。

**瀏覽器實測（2026-10-16，桌機 1280px）**

| # | 操作 | 結果 |
|---|---|---|
| 1 | 打開首頁 | 虛線框「請在上方輸入 ISBN 碼並點擊「查詢」以檢視書籍詳細資訊」 ✅ |
| 2 | 輸入 `9789861343722` 按 Enter | 結果卡片：書名（粗體）、作者、出版日期、ISBN（淺色）、「+ 加入收藏」；ISBN 行和按鈕相距 24px ✅ |
| 3 | 輸入 `INVALID_ISBN` 查詢 | 錯誤框「⚠ 錯誤提示：ISBN 碼格式錯誤或無效」，輸入框保留原文 ✅ |
| 4 | 輸入 `978986134372` 查詢 | 同上 ✅ |
| 5 | 只輸入空白查詢 | 「⚠ 錯誤提示：請輸入 ISBN 碼」 ✅ |
| 6 | 第 3～5 步的網路紀錄 | 開發者工具 Network 分頁**沒有**出現 `/api/books` 請求（前端擋下） ✅ |
| 7 | 輸入本機沒有的 `9780306406157` | 等待 Google 時按鈕顯示「查詢中...」且無法點擊；之後顯示「查詢失敗，請稍後再試」（Google 回 429） ✅ |
| 8 | 未登入，查詢後點「+ 加入收藏」 | 警告 Toast「請先登入或註冊會員以使用收藏功能」，導向 `/login` ✅ |
| 9 | 登入 Cooqle，查詢後點「+ 加入收藏」 | Toast「已成功加入收藏」 ✅ |
| 10 | 再點一次 | 警告 Toast「此書籍已在收藏清單中，請勿重複收藏」，收藏數量仍是 1 ✅ |
| 11 | 頁面停留期間 Session 失效，再點「+ 加入收藏」 | Toast「請先登入或註冊會員以使用收藏功能」，導向 `/login`，導覽列變回「登入 / 註冊」 ✅ |

> 第 9 步的登入，在登入頁完成前（步驟 8）用 Console 執行步驟 6 提供的 fetch 指令。實測加入的收藏已刪除。

**RWD 檢查**

| 寬度 | 結果 |
|---|---|
| 1280px | 輸入框約 70%＋按鈕約 30% 同一列；加入收藏按鈕約卡片 35%；卡片內距 48px ✅ |
| 768px | 同一列；加入收藏按鈕 = 卡片內容寬 50%；卡片內距 32px ✅ |
| 375px | 輸入框與「查詢」上下排列且都滿寬；加入收藏按鈕滿寬；長書名自動換行；內文 16px、標題 24px；沒有水平捲軸 ✅ |

---

## 步驟 8：登入 / 註冊頁

### 目標
完成 `/login`（DESIGN.md 5.2）：
- 窄卡片裡放「暱稱」「手機號碼」兩個輸入框，以及滿版的「登入 / 註冊」按鈕。
- 前端先驗證（規則和順序都和後端相同），錯誤用 Toast（錯誤樣式）顯示。
- 成功：更新登入狀態 → Toast「登入成功」→ 跳轉首頁。

### 建立或修改的檔案
```
frontend/src/
├── validators.js        修改：加上 validateLogin
└── views/LoginView.vue  修改：完成登入頁
```

### 8-1 validators.js（修改後全文）

**`frontend/src/validators.js`**

```js
// 前端欄位驗證：規則與後端相同（後端仍會再驗證一次，前端驗證只是讓使用者更快看到提示）。
// 每個函式回傳「錯誤訊息」；沒有錯誤時回傳空字串 ''。

import { MESSAGES } from '@/messages'

/** ISBN：去除前後空白後，必須是 13 碼數字 */
export function validateIsbn(isbn) {
  if (isbn === '') {
    return MESSAGES.ISBN_REQUIRED
  }
  if (!/^[0-9]{13}$/.test(isbn)) {
    return MESSAGES.ISBN_INVALID
  }
  return ''
}

/**
 * 登入/註冊（傳入已去除前後空白的值）。
 * 檢查順序與後端相同：暱稱空白 → 手機空白 → 暱稱格式 → 暱稱長度 → 手機格式
 */
export function validateLogin(nickname, mobile) {
  if (nickname === '') {
    return MESSAGES.NICKNAME_REQUIRED
  }
  if (mobile === '') {
    return MESSAGES.MOBILE_REQUIRED
  }
  if (!/^[A-Za-z0-9_-]+$/.test(nickname)) {
    return MESSAGES.NICKNAME_INVALID
  }
  if (nickname.length > 20) {
    return MESSAGES.NICKNAME_TOO_LONG
  }
  if (!/^[0-9]{10}$/.test(mobile)) {
    return MESSAGES.MOBILE_INVALID
  }
  return ''
}
```

**白話解釋**
- 檢查順序和後端 `UserService.validate()` **完全一樣**：暱稱空白 → 手機空白 → 暱稱格式 → 暱稱長度 → 手機格式。前後端順序一致，同樣的輸入不管被哪一邊擋下，看到的都是同一則訊息。
- 傳進來的值已經在頁面裡 `trim()` 過，所以這裡直接判斷 `=== ''`。
- `/^[A-Za-z0-9_-]+$/`：和後端 `NICKNAME_PATTERN` 相同（說明見步驟 4-4）。
- 「暱稱與手機號碼不符」「此暱稱已被使用」需要查資料庫，前端沒辦法判斷，只能由後端回傳。

### 8-2 登入頁：LoginView.vue

**`frontend/src/views/LoginView.vue`**

```vue
<!--
  登入 / 註冊 /login（DESIGN.md 5.2）：登入與註冊合一，只要暱稱＋手機。
  錯誤訊息一律用 Toast（錯誤樣式）顯示。
-->
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import PageTitle from '@/components/PageTitle.vue'
import BaseCard from '@/components/BaseCard.vue'
import BaseInput from '@/components/BaseInput.vue'
import BaseButton from '@/components/BaseButton.vue'
import { api } from '@/api'
import { setLoggedIn } from '@/auth'
import { showToast } from '@/toast'
import { validateLogin } from '@/validators'

const router = useRouter()

const nickname = ref('')
const mobile = ref('')
const submitting = ref(false) // 送出中：按鈕暫時停用，避免連按

/** 按下「登入 / 註冊」 */
async function submit() {
  const trimmedNickname = nickname.value.trim()
  const trimmedMobile = mobile.value.trim()

  // 前端先驗證（規則與後端相同）
  const invalid = validateLogin(trimmedNickname, trimmedMobile)
  if (invalid) {
    showToast(invalid, 'error')
    return
  }

  submitting.value = true
  const result = await api.post('/api/auth/login', { nickname: trimmedNickname, mobile: trimmedMobile })
  submitting.value = false

  if (result.ok) {
    setLoggedIn(result.data) // 導覽列馬上變成「暱稱 (個人頁)」
    showToast(result.data.message) // 登入成功（新註冊也是這句）
    router.push('/')
  } else {
    // 暱稱與手機號碼不符、此暱稱已被使用…都顯示後端的訊息
    showToast(result.data.message, 'error')
  }
}
</script>

<template>
  <div class="page page--narrow">
    <PageTitle title="會員 登入 / 註冊" />

    <BaseCard>
      <form class="login-form" @submit.prevent="submit">
        <BaseInput v-model="nickname" label="暱稱 (Nickname)" placeholder="例如：Cooqle" autocomplete="nickname" />
        <BaseInput
          v-model="mobile"
          label="手機號碼 (Mobile)"
          placeholder="例如：0900123456"
          type="tel"
          inputmode="numeric"
          autocomplete="tel"
        />
        <BaseButton type="submit" class="login-form__submit" block spaced :disabled="submitting">
          登入 / 註冊
        </BaseButton>
      </form>
    </BaseCard>
  </div>
</template>

<style scoped>
.login-form {
  display: flex;
  flex-direction: column;
  gap: 32px;
  padding: 8px 40px;
}

.login-form__submit {
  margin-top: 16px;
}

/* 平板以下：卡片本身的內距已經變小，表單不再額外內縮 */
@media (max-width: 1023px) {
  .login-form {
    padding: 0;
  }
}

@media (max-width: 767px) {
  .login-form {
    gap: 24px;
  }

  .login-form__submit {
    margin-top: 8px;
  }
}
</style>
```

**白話解釋**
- **`submit()` 的流程**
  1. `trim()` 去除前後空白（計畫階段 Q4）。
  2. 前端驗證，有錯就跳錯誤 Toast，`return` 結束，不呼叫 API。
  3. 呼叫 `POST /api/auth/login`。送出的是 trim 過的值。
  4. 成功：
     - `setLoggedIn(result.data)`：更新全站共用的 `auth.user`，導覽列**立刻**變成「Cooqle (個人頁)」，不用重新整理。
     - Toast 顯示後端的「登入成功」。全新暱稱自動註冊時，後端回的也是同一句（設計決策 11）。
     - `router.push('/')` 跳轉首頁。
  5. 失敗：錯誤 Toast 顯示後端的訊息。
- **`type="tel"` ＋ `inputmode="numeric"`**：手機號碼欄位在手機上會跳出數字鍵盤。用 `tel` 而不是 `number`，因為 `number` 會把開頭的 0 吃掉，還會出現上下調整的小箭頭。
- **`autocomplete`**：讓瀏覽器可以記住、自動填入暱稱和手機。
- **刻意沒有設定 `maxlength="20"`**：設了的話，使用者根本打不出第 21 個字，就看不到 BDD 規定的「暱稱長度不可超過 20 個字元」提示。
- **樣式**：`.login-form` 用 `flex-direction: column` ＋ `gap` 讓欄位上下排列、間距一致。桌機時表單左右再內縮 40px，看起來比較接近線稿（輸入框比卡片窄一圈）。

### 對應的 Scenario

| BDD Scenario | 狀態 | 說明 |
|---|---|---|
| 使用暱稱與手機號碼成功登入 | ✅ 前後端通過 | Toast「登入成功」→ 跳轉首頁 |
| 暱稱大小寫視為相同，可登入同一個帳號 | ✅ 前後端通過 | 輸入 `cooqle`，導覽列顯示原樣 `Cooqle` |
| 使用全新暱稱與手機號碼自動完成註冊並登入 | ✅ 後端通過；🔸 畫面流程與登入完全相同 | 實測時為了不在你的 MySQL 留下測試帳號，沒有在畫面上實際註冊；列入手動測試清單 |
| 登入/註冊時輸入無效資料（7 組） | ✅ 前後端通過 | 7 組都由前端擋下並顯示正確訊息，沒有呼叫 API |
| 手機號碼已註冊但暱稱不同 | ✅ 前後端通過 | 錯誤 Toast「暱稱與手機號碼不符」，維持未登入 |
| 手機號碼是新的但暱稱已被使用 | ✅ 前後端通過 | 錯誤 Toast「此暱稱已被使用」，維持未登入 |

### 怎麼驗證成功

**瀏覽器實測（2026-10-16）**

| # | 輸入（暱稱 / 手機） | 結果 |
|---|---|---|
| 1 | （空白） / 0900123456 | 錯誤 Toast「請輸入暱稱」，停在 `/login` ✅ |
| 2 | Cooqle / （空白） | 「請輸入手機號碼」 ✅ |
| 3 | Cooqle / 12345 | 「請輸入有效的手機號碼格式 (10碼)」 ✅ |
| 4 | 小明 / 0900123456 | 「暱稱僅限英文、數字、- 與 _」 ✅ |
| 5 | Coo qle / 0900123456 | 「暱稱僅限英文、數字、- 與 _」 ✅ |
| 6 | Cooqle! / 0900123456 | 「暱稱僅限英文、數字、- 與 _」 ✅ |
| 7 | ThisNicknameIsWayTooLong123 / 0900123456 | 「暱稱長度不可超過 20 個字元」 ✅ |
| — | 第 1～7 步的網路紀錄 | 沒有任何 `/api/auth/login` 請求（全部由前端擋下） ✅ |
| 8 | Another_One / 0900123456 | 錯誤 Toast「暱稱與手機號碼不符」（後端回 409），`/api/auth/me` 仍是 401 ✅ |
| 9 | COOQLE / 0955000111 | 錯誤 Toast「此暱稱已被使用」（後端回 409） ✅ |
| 10 | cooqle / 0900123456 | 成功 Toast「登入成功」→ 跳到首頁，導覽列「Cooqle (個人頁)」 ✅ |

**RWD 檢查**

| 寬度 | 結果 |
|---|---|
| 1280px | 卡片寬 720px 置中，與線稿 `login.png` 一致 ✅ |
| 768px | 卡片寬 720px（左右各留 24px），卡片內距 32px ✅ |
| 375px | 卡片滿寬（左右各留 16px），內距 24px；輸入框與按鈕高 48px；沒有水平捲軸 ✅ |

> 從這一步開始就可以直接用登入頁登入，不需要再用 Console 的 fetch 指令。

---

## 步驟 9：收藏頁（清單、刪除、Session 失效備用畫面）

### 目標
完成 `/favorites`（DESIGN.md 5.3）：
- (a) 已登入：副標「會員「{暱稱}」的私人藏書 (共 N 本)」＋每本書一張卡片，右側「刪除」。
- 收藏為 0 本：虛線空狀態框「目前還沒有收藏的書籍」。
- 刪除：`window.confirm()` 確認 → Toast「已移除收藏」→ 卡片消失、「共 N 本」更新。
- (b) 未登入提示畫面：**只在停留在本頁時 Session 失效（API 回 401）才顯示**。一般未登入的人在進來之前就會被路由守衛擋下（步驟 6）。

### 名詞先解釋
- **`onMounted`**：Vue 的「生命週期」函式，在頁面**顯示到畫面上之後**執行一次。適合在這裡載入資料。
- **`v-for`**：清單有幾筆就重複畫幾次。`v-for="item in favorites"` 的意思是：每一筆叫做 `item`，各畫一張卡片。
- **`:key`**：給每一筆一個不重複的身分證（這裡用收藏 id）。刪掉其中一筆時，Vue 才知道要移除**哪一張**卡片，不會搞混。
- **`filter`**：JavaScript 陣列的「篩選」。`favorites.filter((f) => f.id !== item.id)` 會留下「id 不是要刪的那一筆」的所有項目，也就是把那一筆拿掉。
- **`window.confirm(訊息)`**：瀏覽器內建的確認視窗，按「確定」回傳 `true`、按「取消」回傳 `false`。不用安裝任何套件（DESIGN.md 4.9）。

### 建立或修改的檔案
```
frontend/src/views/FavoritesView.vue   修改：完成收藏頁
```

### 9-1 收藏頁：FavoritesView.vue

**`frontend/src/views/FavoritesView.vue`**

```vue
<!--
  收藏頁 /favorites（DESIGN.md 5.3）：需要登入（路由守衛把關）。
  (a) 已登入：收藏清單，每本書旁邊有「刪除」；沒有收藏時顯示空狀態框。
  (b) 未登入提示畫面：只在「停留在本頁時 Session 失效（API 回 401）」時顯示（備用）。
-->
<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import PageTitle from '@/components/PageTitle.vue'
import BaseCard from '@/components/BaseCard.vue'
import BaseButton from '@/components/BaseButton.vue'
import ErrorBox from '@/components/ErrorBox.vue'
import EmptyBox from '@/components/EmptyBox.vue'
import { api } from '@/api'
import { auth, setLoggedOut } from '@/auth'
import { showToast } from '@/toast'
import { MESSAGES } from '@/messages'

const router = useRouter()

const favorites = ref([]) // 收藏清單
const loading = ref(true) // 第一次載入中
const sessionExpired = ref(false) // Session 失效 → 顯示未登入提示畫面
const errorMessage = ref('') // 其他錯誤（例如連不上後端）
const deletingId = ref(null) // 正在刪除哪一筆（按鈕暫時停用）

// onMounted：頁面顯示出來之後執行一次
onMounted(loadFavorites)

async function loadFavorites() {
  loading.value = true
  const result = await api.get('/api/favorites')
  loading.value = false

  if (result.ok) {
    favorites.value = result.data
  } else if (result.status === 401) {
    handleSessionExpired()
  } else {
    errorMessage.value = result.data.message
  }
}

/** 按下某本書的「刪除」 */
async function removeFavorite(item) {
  // 瀏覽器內建的確認視窗：按「取消」就什麼都不做（DESIGN.md 4.9）
  if (!window.confirm(MESSAGES.DELETE_CONFIRM)) {
    return
  }

  deletingId.value = item.id
  const result = await api.delete(`/api/favorites/${item.id}`)
  deletingId.value = null

  if (result.ok) {
    showToast(result.data.message) // 已移除收藏
    // 從畫面上的清單拿掉這一筆（「共 N 本」會自動跟著更新）
    favorites.value = favorites.value.filter((f) => f.id !== item.id)
  } else if (result.status === 401) {
    handleSessionExpired()
  } else {
    // 例如另一個分頁已經刪掉了（404 找不到此收藏）：提示後重新載入清單
    showToast(result.data.message, 'error')
    loadFavorites()
  }
}

function handleSessionExpired() {
  setLoggedOut() // 導覽列變回「登入 / 註冊」
  favorites.value = [] // 不再顯示任何收藏內容
  sessionExpired.value = true
}
</script>

<template>
  <div class="page page--wide">
    <PageTitle title="我的收藏 — 書籍清單" />

    <!-- (b) 未登入提示畫面（Session 失效時的備用） -->
    <template v-if="sessionExpired">
      <ErrorBox center class="guest">
        <p class="guest__title">您目前為未登入狀態</p>
        <p class="guest__text">請先登入或註冊會員後，即可檢視與管理您的個人藏書</p>
      </ErrorBox>
      <div class="guest__action">
        <BaseButton class="guest__button" @click="router.push('/login')">前往登入 / 註冊</BaseButton>
      </div>
    </template>

    <!-- 其他錯誤 -->
    <ErrorBox v-else-if="errorMessage">⚠ 錯誤提示：{{ errorMessage }}</ErrorBox>

    <!-- (a) 已登入：清單（載入中先不顯示，避免閃一下「目前還沒有收藏」） -->
    <template v-else-if="!loading">
      <h2 class="summary">會員「{{ auth.user?.nickname }}」的私人藏書 (共 {{ favorites.length }} 本)</h2>

      <EmptyBox v-if="favorites.length === 0">目前還沒有收藏的書籍</EmptyBox>

      <ul v-else class="list">
        <!-- v-for：清單有幾筆就畫幾張卡片；:key 讓 Vue 分辨每一筆 -->
        <li v-for="item in favorites" :key="item.id">
          <BaseCard class="item">
            <div class="item__info">
              <h3 class="item__title">{{ item.title }}</h3>
              <p class="item__meta">作者：{{ item.author || '—' }} | ISBN: {{ item.isbn }}</p>
            </div>
            <BaseButton
              variant="danger"
              class="item__delete"
              :disabled="deletingId === item.id"
              @click="removeFavorite(item)"
            >
              刪除
            </BaseButton>
          </BaseCard>
        </li>
      </ul>
    </template>
  </div>
</template>

<style scoped>
/* 左對齊粗體副標：會員「Cooqle」的私人藏書 (共 2 本) */
.summary {
  margin: 0 0 24px;
  padding-left: 16px;
  font-size: 1.1em;
  font-weight: 700;
}

/* ===== 收藏清單 ===== */
.list {
  display: flex;
  flex-direction: column;
  gap: 24px; /* 卡片間距約 24px */
  margin: 0;
  padding: 0;
  list-style: none;
}

/* 左：書名與資訊；右：「刪除」垂直置中 */
.item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
  padding-top: 40px;
  padding-bottom: 40px;
}

.item__info {
  min-width: 0; /* 讓長書名可以換行，不會把按鈕擠出去 */
}

.item__title {
  margin: 0 0 16px;
  font-size: 1.1em;
  font-weight: 700;
}

.item__meta {
  margin: 0;
  color: var(--color-text-muted);
}

.item__delete {
  flex-shrink: 0;
  width: 220px;
}

/* ===== 未登入提示畫面 ===== */
.guest {
  margin-top: 48px;
}

.guest__title {
  margin: 0 0 8px;
  font-size: 1.15em;
}

.guest__text {
  margin: 0;
  font-weight: 400;
}

.guest__action {
  display: flex;
  justify-content: center;
  margin-top: 56px;
}

.guest__button {
  width: 340px;
  max-width: 100%;
}

/* 平板：按鈕窄一點 */
@media (max-width: 1023px) {
  .item {
    padding-top: 32px;
    padding-bottom: 32px;
  }

  .item__delete {
    width: 160px;
  }
}

/* 手機：書名與資訊在上，「刪除」在下並滿寬 */
@media (max-width: 767px) {
  .summary {
    padding-left: 0;
  }

  .item {
    flex-direction: column;
    align-items: stretch;
    gap: 20px;
    padding-top: 24px;
    padding-bottom: 24px;
  }

  .item__delete {
    width: 100%;
  }

  .guest {
    margin-top: 24px;
  }

  .guest__action {
    margin-top: 32px;
  }
}
</style>
```

**白話解釋**

**畫面狀態**
| 變數 | 意思 |
|---|---|
| `favorites` | 收藏清單（陣列）。長度就是「共 N 本」 |
| `loading` | 第一次載入中：先什麼都不顯示，避免閃一下「目前還沒有收藏的書籍」 |
| `sessionExpired` | Session 失效 → 顯示未登入提示畫面 |
| `errorMessage` | 其他錯誤（例如後端沒開） |
| `deletingId` | 正在刪除哪一筆：那顆按鈕暫時停用，避免連按 |

**畫面切換（`v-if` 鏈）**
```
sessionExpired？  → 未登入提示畫面 (b)
否則 errorMessage？ → 錯誤框
否則載入完成？      → 副標＋（0 本 → 空狀態框；否則 → 清單） (a)
```

**刪除的流程**
1. `window.confirm(...)`：按「取消」就 `return`，什麼都不做。
2. 呼叫 `DELETE /api/favorites/{id}`。
3. 成功 → Toast「已移除收藏」，用 `filter` 從清單拿掉那一筆。「共 N 本」用的是 `favorites.length`，會**自動**跟著變。
4. 401 → `handleSessionExpired()`：清掉登入狀態與清單，改顯示未登入提示畫面。
5. 其他錯誤（例如另一個分頁已經刪掉，後端回 404「找不到此收藏」）→ 錯誤 Toast，並重新載入清單，讓畫面和資料庫一致。

**為什麼未登入提示畫面只是「備用」？**（DESIGN.md 第 7 節已確認）
BDD 規定未登入進入收藏頁時，要「跳出提示並自動導向登入頁」，這由路由守衛完成，一般不會看到這個畫面。只有在使用者**已經在收藏頁上**、而 Session 剛好失效（例如閒置太久、在別的分頁登出）時，按下刪除收到 401，才會切換成這個畫面。這時清單會被清空，所以 BDD 的「無法看到任何先前收藏的書籍內容」也成立。

**樣式重點**
- 卡片內 `display: flex` ＋ `justify-content: space-between` ＋ `align-items: center`：左邊資訊、右邊按鈕，按鈕垂直置中。
- `.item__info { min-width: 0 }`：flex 子元素預設不會比內容窄。加上這行，長書名才會換行，不會把按鈕擠出卡片。
- `.item__delete { flex-shrink: 0 }`：按鈕寬度固定，不會被壓扁。
- 手機版 `flex-direction: column`：資訊在上、按鈕在下並滿寬（DESIGN.md 3.1）。
- `<ul>` / `<li>`：清單用 HTML 的清單標籤，螢幕報讀軟體會念出「清單，共 2 項」。`list-style: none` 拿掉前面的圓點。
- `auth.user?.nickname`：`?.` 是「安全取值」。萬一 `auth.user` 是 null，不會出錯，只會得到 undefined。

### 對應的 Scenario

| BDD Scenario | 狀態 | 說明 |
|---|---|---|
| 已登入會員在收藏頁面檢視自己的收藏清單 | ✅ 前後端通過 | 顯示 2 本書，每本旁邊有「刪除」 |
| 會員從收藏頁面成功刪除書籍 | ✅ 前後端通過 | 確認視窗 → 「已移除收藏」→ 卡片消失 |
| 未登入使用者嘗試訪問收藏頁面被拒絕 | ✅（步驟 6 的路由守衛） | |
| 在個人頁面登出後無法再查看收藏內容 | 🔸 收藏頁部分已完成；「從個人頁登出」在步驟 10 | 登出後進收藏頁會被守衛擋下；停留在收藏頁時 Session 失效會顯示備用畫面且不顯示任何收藏 |

### 怎麼驗證成功

**瀏覽器實測（2026-10-16）**

| # | 操作 | 結果 |
|---|---|---|
| 1 | 登入 Cooqle（沒有收藏）→ 點「我的收藏」 | 「會員「Cooqle」的私人藏書 (共 0 本)」＋虛線框「目前還沒有收藏的書籍」 ✅ |
| 2 | 收藏 2 本書後重新進入 | 「(共 2 本)」，兩張卡片：書名粗體、下一行「作者：雷納·克魯克 \| ISBN: 9789861343722」；「刪除」垂直置中（與卡片中心相差 0px），與線稿 `favorites-list.png` 一致 ✅ |
| 3 | 點「刪除」→ 確認視窗按「取消」 | 確認視窗文字「確定要移除這本書的收藏嗎？」；清單維持 2 本 ✅ |
| 4 | 點「刪除」→ 按「確定」 | Toast「已移除收藏」；該卡片消失；副標變「(共 1 本)」；重新向後端查詢，也只剩 1 本 ✅ |
| 5 | 停留在收藏頁時讓 Session 失效（在 Console 呼叫登出 API），再點「刪除」 | 切換成未登入提示畫面「您目前為未登入狀態」＋「前往登入 / 註冊」按鈕；清單消失；導覽列變回「登入 / 註冊」；與線稿 `favorites-guest.png` 一致 ✅ |
| 6 | 點「前往登入 / 註冊」 | 跳到 `/login` ✅ |

> 第 3、4 步的確認視窗：自動化測試工具無法點擊瀏覽器原生的確認視窗，所以實測時暫時把 `window.confirm` 換成會記錄文字並自動回答的版本。**真正的確認視窗請在手動測試時確認**（列入步驟 11 的手動測試清單）。
> 實測用的收藏已全部刪除，Cooqle 回到 0 本。

**RWD 檢查**

| 寬度 | 結果 |
|---|---|
| 1280px | 左側書名與資訊、右側「刪除」（寬 220px）垂直置中 ✅ |
| 768px | 同一列，「刪除」寬 160px ✅ |
| 375px | 書名與資訊在上、「刪除」在下並滿寬（高 48px）；ISBN 過長時自動換行；未登入提示畫面的文字與按鈕也正常；沒有水平捲軸 ✅ |

---

## 步驟 10：個人頁與登出

### 目標
完成 `/profile`（DESIGN.md 5.4）：
- 窄卡片：置中粗體「會員基本資料」＋分隔線，兩欄顯示「會員暱稱：」「手機號碼：」。
- 全寬危險按鈕「登出帳號」：清除 Session → Toast「已成功登出」→ 回首頁。
- 寬度 < 360px 時，標籤與內容改成上下排列（DESIGN.md 3.1）。

### 名詞先解釋
- **`<dl>` / `<dt>` / `<dd>`**：HTML 的「描述清單」。`dt` 是名稱（會員暱稱：），`dd` 是內容（Cooqle）。這種「標籤：內容」的資料用它最合適，螢幕報讀軟體也會念出兩者的對應關係。
- **CSS Grid（格線排版）**：把區塊切成欄與列的排版方式。`grid-template-columns: max-content 1fr` 代表兩欄：左欄寬度剛好容納最長的標籤，右欄佔滿剩下的空間。

### 建立或修改的檔案
```
frontend/src/
├── messages.js            修改：加上 LOGOUT_SUCCESS
└── views/ProfileView.vue  修改：完成個人頁
```

### 10-1 messages.js（修改後全文）

**`frontend/src/messages.js`**

```js
// 前端用到的提示訊息（文字來自 BDD.md），集中在這裡管理。
// API 的結果訊息（登入成功、已成功加入收藏…）由後端回傳，前端直接顯示 response 的 message，不寫在這裡。
// 這裡只放「前端自己判斷、自己顯示」的訊息；前端欄位驗證的文字與後端 Messages.java 相同。

export const MESSAGES = {
  // ===== 書籍查詢（前端驗證） =====
  ISBN_REQUIRED: '請輸入 ISBN 碼',
  ISBN_INVALID: 'ISBN 碼格式錯誤或無效',

  // ===== 登入 / 註冊（前端驗證） =====
  NICKNAME_REQUIRED: '請輸入暱稱',
  MOBILE_REQUIRED: '請輸入手機號碼',
  NICKNAME_INVALID: '暱稱僅限英文、數字、- 與 _',
  NICKNAME_TOO_LONG: '暱稱長度不可超過 20 個字元',
  MOBILE_INVALID: '請輸入有效的手機號碼格式 (10碼)',

  // ===== 未登入的提示 =====
  /** 未登入點「加入收藏」 */
  FAVORITE_LOGIN_REQUIRED: '請先登入或註冊會員以使用收藏功能',
  /** 未登入進入收藏頁 /favorites */
  FAVORITES_PAGE_LOGIN_REQUIRED: '請先登入後再檢視收藏內容',
  /** 未登入進入個人頁 /profile */
  PROFILE_PAGE_LOGIN_REQUIRED: '請先註冊/登入',

  // ===== 登出 =====
  /** 登出時 Session 早已失效（API 回 401），前端仍當作登出成功（與後端 LOGOUT_SUCCESS 相同） */
  LOGOUT_SUCCESS: '已成功登出',

  // ===== 收藏 =====
  /** 刪除前的確認視窗文字（DESIGN.md 4.9） */
  DELETE_CONFIRM: '確定要移除這本書的收藏嗎？',

  // ===== 通用 =====
  /** 連不上後端、或後端回應看不懂時（與後端的 SERVER_ERROR 相同） */
  SERVER_ERROR: '系統發生錯誤，請稍後再試',
}
```

**白話解釋**
- 新增的 `LOGOUT_SUCCESS` 是唯一一則「API 結果訊息」也放在前端的例外：登出時如果 Session 早已失效，後端回 401，不會給「已成功登出」這句。但對使用者來說他確實已經登出了，所以前端自己顯示這句（計畫階段的預設決定）。

### 10-2 個人頁：ProfileView.vue

**`frontend/src/views/ProfileView.vue`**

```vue
<!--
  個人頁 /profile（DESIGN.md 5.4）：需要登入（路由守衛把關）。
  顯示會員暱稱、手機號碼，以及「登出帳號」按鈕。
-->
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import PageTitle from '@/components/PageTitle.vue'
import BaseCard from '@/components/BaseCard.vue'
import BaseButton from '@/components/BaseButton.vue'
import { api } from '@/api'
import { auth, setLoggedOut } from '@/auth'
import { showToast } from '@/toast'
import { MESSAGES } from '@/messages'

const router = useRouter()
const loggingOut = ref(false)

/** 按下「登出帳號」 */
async function logout() {
  loggingOut.value = true
  const result = await api.post('/api/auth/logout')
  loggingOut.value = false

  // 成功（200）或 Session 早就失效（401），對使用者來說結果都一樣：已經登出了
  if (result.ok || result.status === 401) {
    setLoggedOut() // 導覽列變回「登入 / 註冊」
    showToast(result.ok ? result.data.message : MESSAGES.LOGOUT_SUCCESS) // 已成功登出
    router.push('/')
  } else {
    showToast(result.data.message, 'error')
  }
}
</script>

<template>
  <div class="page page--narrow">
    <PageTitle title="個人資訊" />

    <!-- 路由守衛保證進得來就一定有 auth.user；v-if 是多一層保險 -->
    <BaseCard v-if="auth.user">
      <h2 class="profile__heading">會員基本資料</h2>

      <!-- dl / dt / dd：HTML 的「名稱—內容」清單，剛好對應「標籤：內容」兩欄 -->
      <dl class="profile__info">
        <dt>會員暱稱：</dt>
        <dd class="profile__nickname">{{ auth.user.nickname }}</dd>
        <dt>手機號碼：</dt>
        <dd>{{ auth.user.mobile }}</dd>
      </dl>

      <BaseButton variant="danger" block spaced :disabled="loggingOut" @click="logout">登出帳號</BaseButton>
    </BaseCard>
  </div>
</template>

<style scoped>
/* 置中粗體「會員基本資料」，下方一條分隔線 */
.profile__heading {
  margin: 0 0 32px;
  padding-bottom: 28px;
  border-bottom: var(--line) solid var(--color-border);
  font-size: 1.15em;
  font-weight: 700;
  text-align: center;
}

/* 兩欄：左邊標籤（次要色）、右邊內容 */
.profile__info {
  display: grid;
  grid-template-columns: max-content 1fr;
  gap: 32px 72px;
  margin: 0 0 40px;
  padding: 0 48px;
}

.profile__info dt {
  color: var(--color-text-muted);
}

.profile__info dd {
  margin: 0;
  min-width: 0; /* 長暱稱可以換行 */
}

.profile__nickname {
  font-weight: 700;
}

@media (max-width: 1023px) {
  .profile__info {
    gap: 24px 40px;
    padding: 0 16px;
  }
}

@media (max-width: 767px) {
  .profile__heading {
    margin-bottom: 24px;
    padding-bottom: 20px;
  }

  .profile__info {
    gap: 20px 24px;
    margin-bottom: 32px;
    padding: 0;
  }
}

/* 極窄螢幕（< 360px）：標籤與內容改成上下排列 */
@media (max-width: 359px) {
  .profile__info {
    grid-template-columns: 1fr;
    gap: 4px;
  }

  .profile__info dd {
    margin-bottom: 16px;
  }
}
</style>
```

**白話解釋**
- **資料從哪來？** 直接用 `auth.user`。打開網頁時 `GET /api/auth/me`（步驟 6）或登入成功時（步驟 8）就已經拿到暱稱和手機了，不用再呼叫一次 API。
- **為什麼還要 `v-if="auth.user"`？** 路由守衛保證沒登入進不來。但按下登出後，`auth.user` 會先變成 null，接著才換頁。這一瞬間如果沒有 `v-if`，畫面會嘗試讀 `null.nickname` 而出錯。
- **`logout()` 的流程**
  1. 呼叫 `POST /api/auth/logout`，後端執行 `session.invalidate()`。
  2. 成功（200）或 Session 早已失效（401）都當作登出成功：
     - `setLoggedOut()`：導覽列變回「登入 / 註冊」。
     - Toast「已成功登出」：200 時用後端回傳的訊息，401 時用前端的 `MESSAGES.LOGOUT_SUCCESS`。
     - `router.push('/')` 回首頁。
  3. 其他錯誤（例如後端沒開）：錯誤 Toast，留在個人頁，維持登入狀態。
- **登出後還能看到收藏嗎？** 不能。後端的 Session 已作廢，前端的 `auth.user` 也清空了，再點「我的收藏」會被路由守衛擋下；就算直接呼叫 API，攔截器也會回 401。
- **樣式重點**
  - `.profile__heading` 的 `border-bottom`：「會員基本資料」下方的 1.5px 分隔線。
  - `gap: 32px 72px`：列與列之間 32px、欄與欄之間 72px。
  - `@media (max-width: 359px)`：改成 `grid-template-columns: 1fr`（只剩一欄），標籤和內容就變成上下排列。

### 對應的 Scenario

| BDD Scenario | 狀態 | 說明 |
|---|---|---|
| 在個人頁面檢視個人資訊 | ✅ 前後端通過 | 會員暱稱 Cooqle、手機號碼 0900123456，以及「登出帳號」按鈕 |
| 未登入使用者嘗試訪問個人頁面被拒絕 | ✅（步驟 6 的路由守衛） | |
| 在個人頁面登出後無法再查看收藏內容 | ✅ 前後端通過 | 登出 → 「已成功登出」→ 回首頁 → 點「我的收藏」被導向登入頁，看不到任何收藏 |

**到這一步，BDD.md 所有 Scenario 都已實作完成。** 步驟 11 會整理手動測試清單，並寫「如何快速上手」。

### 怎麼驗證成功

**瀏覽器實測（2026-10-16）**

| # | 操作 | 結果 |
|---|---|---|
| 1 | 登入 Cooqle → 點導覽列「Cooqle (個人頁)」 | 顯示「會員基本資料」、會員暱稱 **Cooqle**、手機號碼 0900123456、「登出帳號」；導覽列第三項為粗體；與線稿 `profile.png` 一致 ✅ |
| 2 | 點「登出帳號」 | 成功 Toast「已成功登出」→ 回首頁；導覽列變回「登入 / 註冊」；`/api/auth/me` 回 401（Session 已清除） ✅ |
| 3 | 接著點「我的收藏」 | 被導向 `/login`，Toast「請先登入後再檢視收藏內容」，看不到任何收藏 ✅ |
| 4 | 登入後進個人頁，讓 Session 先失效，再點「登出帳號」 | 依然顯示「已成功登出」並回首頁 ✅ |

**RWD 檢查**

| 寬度 | 結果 |
|---|---|
| 1280px | 窄卡片 720px 置中，兩欄排列 ✅ |
| 768px | 兩欄排列 ✅ |
| 375px | 兩欄排列；「登出帳號」高 48px；沒有水平捲軸 ✅ |
| 340px（< 360px） | 標籤與內容上下排列；沒有水平捲軸 ✅ |

---

<!-- 步驟 11 之後的內容會在完成時陸續補上 -->

## 如何快速上手

（全部步驟完成後補上）
