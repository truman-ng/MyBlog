# MyBlog — A Native Android Blog Application

A feature-rich native Android blog client offering a smooth experience for **writing**, **reading**, and **social interaction**. Built with a modern Android architecture (**MVVM**) and **Firebase** backend services.
![M_LOGIN.png](screenshot/M_LOGIN.png) ![M_BLOGS.png](screenshot/M_BLOGS.png) ![M_MY.png](screenshot/M_MY.png) ![M_WRITE.png](screenshot/M_WRITE.png) ![M_BOOKMARK.png](screenshot/M_BOOKMARK.png) ![M_FOLLOWING.png](screenshot/M_FOLLOWING.png)

---

## ✨ Key Features

### Blog Browsing & Creation

* **Global Feed**: Discover and read public posts from all users in the main feed.
* **Markdown Support**: Compose and publish posts using Markdown (rich text, code blocks, images).
* **Drafts**: Save unfinished posts as drafts and continue editing later.

### User Authentication & Profile

* **Secure Auth**: Reliable email/password registration and login powered by Firebase Auth.
* **User Profile**: View and edit avatar, display name, and bio.

### Personalized “My” Page

* **Reading History**: Automatically records and displays posts the user has viewed.
* **Bookmarks**: Manually save favorite posts for later reading.
* **Following List**: See all authors you follow.
* **Likes**: Centralized place for posts you liked.

### Social Interaction

* **Follow/Unfollow** favorite authors.
* **Like** posts you enjoy.

### Elegant UI/UX

* **Material Design 3** for a modern, consistent look.
* Smooth bottom-navigation experience.
* Friendly loading, empty, and error states.

---

## 🧱 Tech Stack & Architecture

**Architecture**

* **MVVM (Model–View–ViewModel)**: Clear separation of UI and business logic.
* **Single-Activity, Multi-Fragment** using Android Navigation Component.

**UI**

* **Material Design 3**
* **ViewBinding**
* **RecyclerView**
* **Glide** for image loading
* **Markwon** for Markdown rendering

**Backend (Firebase)**

* **Realtime Database** (core data store & sync)
* **Authentication** (email/password)
* **Storage** (user-uploaded media)

**Async & Reactivity**

* **LiveData** (View ↔ ViewModel communication)
* Firebase SDK async APIs (non-blocking UI thread)

---

## ⚙️ How to Run

1. **Clone**

```bash
git clone https://github.com/your-username/MyBlog.git
```

2. **Configure Firebase**

* Create a new Android project in **Firebase Console**.
* Download `google-services.json` from project settings.
* Place it in your app module directory: `app/google-services.json`.
* Enable **Authentication (Email/Password)** and **Realtime Database**.

3. **Build & Run**

* Open with **Android Studio**.
* Wait for Gradle sync.
* Click **Run 'app'** to deploy on an emulator or a device.


---

# （中文）MyBlog — 安卓原生博客应用

一个功能丰富的安卓原生博客客户端，专注于**写作**、**阅读**与**社交互动**的流畅体验。项目采用现代化安卓架构（**MVVM**）与 **Firebase** 后端服务。

![M_LOGIN.png](screenshot/M_LOGIN.png) ![M_BLOGS.png](screenshot/M_BLOGS.png) ![M_MY.png](screenshot/M_MY.png) ![M_WRITE.png](screenshot/M_WRITE.png) ![M_BOOKMARK.png](screenshot/M_BOOKMARK.png) ![M_FOLLOWING.png](screenshot/M_FOLLOWING.png)


---

## ✨ 功能亮点

### 博客浏览与创作

* **全局信息流**：在主信息流发现并阅读所有用户发布的公开博客。
* **Markdown 支持**：使用 Markdown 撰写与发布，支持富文本、代码块与图片。
* **草稿箱**：将未完成文章保存为草稿，随时继续编辑。

### 用户认证与资料

* **安全认证**：基于 Firebase Auth 的邮箱/密码注册登录。
* **个人资料**：查看与编辑头像、昵称与简介。

### 个性化「我的」

* **阅读历史**：自动记录并展示用户浏览过的文章。
* **书签**：手动收藏喜欢的文章，便于回看。
* **关注列表**：查看已关注的博主。
* **点赞汇总**：集中管理所有点赞过的文章。

### 社交互动

* **关注/取关**喜欢的博主。
* **点赞**认可的文章。

### 优雅 UI/UX

* **Material Design 3** 设计语言。
* 流畅的底部导航与切换体验。
* 友好的加载、空状态与错误提示。

---

## 🧱 技术栈与架构

**架构**

* **MVVM**：清晰分离 UI 与业务逻辑。
* **单 Activity，多 Fragment**，使用导航组件管理跳转。

**UI**

* **Material Design 3**
* **ViewBinding**
* **RecyclerView**
* **Glide**（图片加载）
* **Markwon**（Markdown 渲染）

**后端（Firebase）**

* **Realtime Database**（核心数据存储与同步）
* **Authentication**（邮箱/密码）
* **Storage**（用户上传媒体）

**异步与响应式**

* **LiveData**（View 与 ViewModel 通信）
* Firebase SDK 异步 API（避免阻塞 UI 线程）

---

## ⚙️ 如何运行

1）**克隆项目**

```bash
git clone https://github.com/your-username/MyBlog.git
```

2）**配置 Firebase**

* 在 **Firebase 控制台** 新建 Android 项目。
* 下载 `google-services.json`。
* 放入应用模块目录：`app/google-services.json`。
* 在控制台启用 **Authentication（邮箱/密码）** 与 **Realtime Database**。

3）**构建与运行**

* 使用 **Android Studio** 打开项目。
* 等待 Gradle 同步完成。
* 点击 **Run 'app'**，在模拟器或真机上运行。
