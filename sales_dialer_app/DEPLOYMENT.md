# 智能电销王 - 部署指南

## 📦 完整部署流程

本指南将帮助您完成从开发到生产环境的完整部署流程。

---

## 🎯 部署架构

```
┌────────────────────────────────────────────────────┐
│           生产环境部署架构                          │
├────────────────────────────────────────────────────┤
│                                                    │
│  ┌─────────────┐      ┌──────────────┐           │
│  │  Android    │──────>│   Odoo       │           │
│  │  APP (APK)  │ HTTPS │   Server     │           │
│  └─────────────┘      │  (客户管理)   │           │
│       ↓               └──────────────┘           │
│  [Google Play]              ↓                     │
│  [内部分发]           ┌──────────────┐           │
│                       │  PostgreSQL  │           │
│                       │   Database   │           │
│                       └──────────────┘           │
└────────────────────────────────────────────────────┘
```

---

## 📋 部署清单

### 前置条件

- [ ] Odoo服务器已部署并运行
- [ ] 客户管理模块已安装
- [ ] 服务器可通过HTTPS访问
- [ ] 已准备签名密钥
- [ ] 已配置应用图标和资源

---

## 🔧 步骤1: 配置生产环境

### 1.1 配置服务器地址

编辑 `app/src/main/java/com/salesdialer/config/ApiConfig.java`:

```java
public class ApiConfig {
    // 生产服务器地址
    public static final String BASE_URL = "https://your-production-server.com";
    
    // 生产数据库名称
    public static final String DATABASE = "production_db";
    
    // API版本
    public static final String API_VERSION = "v1";
    
    // 超时设置（毫秒）
    public static final int CONNECT_TIMEOUT = 30000;
    public static final int READ_TIMEOUT = 30000;
    public static final int WRITE_TIMEOUT = 30000;
}
```

### 1.2 配置应用信息

编辑 `app/build.gradle`:

```gradle
android {
    defaultConfig {
        applicationId "com.yourcompany.salesdialer"  // 修改为您的包名
        versionCode 1
        versionName "1.0.0"
        
        // 添加构建配置
        buildConfigField "String", "SERVER_URL", "\"https://your-server.com\""
        buildConfigField "boolean", "DEBUG_MODE", "false"
    }
}
```

### 1.3 配置ProGuard混淆

编辑 `app/proguard-rules.pro`:

```proguard
# 保留Retrofit相关
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# 保留Gson相关
-keep class com.google.gson.** { *; }
-keep class com.salesdialer.model.** { *; }

# 保留Room相关
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
```

---

## 🔑 步骤2: 生成签名密钥

### 2.1 创建密钥库

```bash
keytool -genkey -v -keystore sales-dialer-release.jks \
  -alias sales-dialer \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# 按提示输入以下信息：
# - 密钥库密码（请妥善保管）
# - 您的姓名
# - 组织单位
# - 组织名称
# - 城市
# - 省份
# - 国家代码（如CN）
```

### 2.2 配置签名

创建 `keystore.properties` 文件：

```properties
storePassword=your_store_password
keyPassword=your_key_password
keyAlias=sales-dialer
storeFile=../sales-dialer-release.jks
```

⚠️ **重要**: 将 `keystore.properties` 添加到 `.gitignore`

### 2.3 在build.gradle中引用

编辑 `app/build.gradle`:

```gradle
// 加载keystore配置
def keystorePropertiesFile = rootProject.file("keystore.properties")
def keystoreProperties = new Properties()
keystoreProperties.load(new FileInputStream(keystorePropertiesFile))

android {
    signingConfigs {
        release {
            storeFile file(keystoreProperties['storeFile'])
            storePassword keystoreProperties['storePassword']
            keyAlias keystoreProperties['keyAlias']
            keyPassword keystoreProperties['keyPassword']
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

---

## 📱 步骤3: 构建Release APK

### 3.1 清理项目

```bash
./gradlew clean
```

### 3.2 构建Release版本

```bash
./gradlew assembleRelease

# 或者使用Android Studio:
# Build -> Generate Signed Bundle / APK -> APK
# 选择密钥库和配置
# 选择release构建变体
```

### 3.3 验证APK

```bash
# 查看APK信息
aapt dump badging app/build/outputs/apk/release/app-release.apk

# 验证签名
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk
```

---

## 🎨 步骤4: 优化APK

### 4.1 对齐APK

```bash
zipalign -v -p 4 \
  app/build/outputs/apk/release/app-release.apk \
  app/build/outputs/apk/release/app-release-aligned.apk
```

### 4.2 查看APK大小

```bash
# 分析APK内容
./gradlew :app:analyzeReleaseBundle

# 使用Android Studio:
# Build -> Analyze APK
```

### 4.3 减小APK大小

编辑 `app/build.gradle`:

```gradle
android {
    buildTypes {
        release {
            // 启用资源压缩
            shrinkResources true
            
            // 启用代码压缩
            minifyEnabled true
            
            // 移除未使用的资源
            resValue "string", "app_name", "智能电销王"
        }
    }
    
    // 只保留必要的语言
    bundle {
        language {
            enableSplit = false
        }
    }
    
    // ABI拆分
    splits {
        abi {
            enable true
            reset()
            include 'armeabi-v7a', 'arm64-v8a'
            universalApk true
        }
    }
}
```

---

## 🧪 步骤5: 测试Release版本

### 5.1 安装测试

```bash
# 卸载现有版本
adb uninstall com.yourcompany.salesdialer

# 安装Release版本
adb install app/build/outputs/apk/release/app-release.apk
```

### 5.2 功能测试清单

**基础功能**
- [ ] 登录功能正常
- [ ] 客户列表加载正常
- [ ] 拨号功能正常
- [ ] 跟进记录保存正常
- [ ] 数据同步正常

**自动拨号**
- [ ] 自动拨号启动正常
- [ ] 拨号间隔控制正常
- [ ] 暂停/恢复功能正常
- [ ] 跳过功能正常
- [ ] 统计数据准确

**性能测试**
- [ ] 启动速度 < 3秒
- [ ] 列表滚动流畅
- [ ] 内存使用正常
- [ ] 电池消耗合理

### 5.3 兼容性测试

在不同设备上测试：
- [ ] Android 6.0
- [ ] Android 8.0
- [ ] Android 10
- [ ] Android 12+

在不同屏幕尺寸上测试：
- [ ] 小屏手机 (< 5寸)
- [ ] 普通手机 (5-6寸)
- [ ] 大屏手机 (> 6寸)

---

## 🚀 步骤6: 分发APK

### 方式1: 内部分发（推荐起步）

#### 6.1 创建下载页面

```html
<!DOCTYPE html>
<html>
<head>
    <title>智能电销王 - 下载</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body>
    <h1>智能电销王</h1>
    <p>版本: 1.0.0</p>
    <p>更新时间: 2025-01-30</p>
    
    <h2>下载安装</h2>
    <a href="app-release.apk" class="download-btn">
        下载APK (15MB)
    </a>
    
    <h2>安装说明</h2>
    <ol>
        <li>下载APK文件</li>
        <li>允许安装未知来源应用</li>
        <li>安装应用</li>
        <li>授予必要权限</li>
    </ol>
    
    <h2>更新日志</h2>
    <ul>
        <li>✨ 首次发布</li>
        <li>✨ 支持自动拨号</li>
        <li>✨ 支持跟进管理</li>
    </ul>
</body>
</html>
```

#### 6.2 配置Web服务器

```nginx
# Nginx配置示例
server {
    listen 80;
    server_name download.yourcompany.com;
    
    root /var/www/sales-dialer-download;
    index index.html;
    
    location /app-release.apk {
        add_header Content-Type application/vnd.android.package-archive;
        add_header Content-Disposition 'attachment; filename="sales-dialer.apk"';
    }
}
```

### 方式2: Google Play发布

#### 6.1 准备材料

- [ ] 应用图标 (512x512 PNG)
- [ ] 功能图形 (1024x500 PNG)
- [ ] 屏幕截图 (至少4张)
- [ ] 应用描述
- [ ] 隐私政策URL
- [ ] 联系邮箱

#### 6.2 创建应用

1. 访问 Google Play Console
2. 创建新应用
3. 填写应用详情
4. 上传APK或AAB

#### 6.3 内容分级

按照Google的问卷填写内容分级信息

#### 6.4 定价和分发

设置应用为免费或付费，选择分发国家/地区

---

## 🔄 步骤7: 版本更新

### 7.1 版本号管理

```gradle
// app/build.gradle
android {
    defaultConfig {
        versionCode 2      // 每次发布递增
        versionName "1.0.1" // 语义化版本号
    }
}
```

版本号规则：
- **主版本号**: 重大功能变更
- **次版本号**: 新功能添加
- **修订号**: Bug修复

### 7.2 更新检查

在应用中添加更新检查：

```java
public class UpdateChecker {
    private static final String UPDATE_URL = "https://api.yourcompany.com/app/version";
    
    public void checkUpdate(Context context) {
        // 获取当前版本
        int currentVersion = BuildConfig.VERSION_CODE;
        
        // 请求服务器获取最新版本
        // 如果有新版本，提示用户下载
    }
}
```

### 7.3 强制更新策略

```java
if (serverVersionCode > currentVersionCode) {
    if (isForceUpdate) {
        // 显示不可关闭的更新对话框
        showForceUpdateDialog();
    } else {
        // 显示可选更新对话框
        showOptionalUpdateDialog();
    }
}
```

---

## 📊 步骤8: 监控和分析

### 8.1 集成Firebase

```gradle
// app/build.gradle
dependencies {
    implementation 'com.google.firebase:firebase-analytics:21.5.0'
    implementation 'com.google.firebase:firebase-crashlytics:18.6.0'
}
```

### 8.2 添加分析事件

```java
// 记录拨号事件
Bundle params = new Bundle();
params.putString("customer_level", customer.getLevel());
params.putBoolean("auto_dial", isAutoDial);
FirebaseAnalytics.getInstance(context).logEvent("dial_made", params);
```

### 8.3 崩溃报告

```java
// 记录自定义崩溃信息
FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
crashlytics.setCustomKey("user_id", userId);
crashlytics.setCustomKey("dial_mode", dialMode);
crashlytics.log("Custom log message");
```

---

## 🔐 步骤9: 安全加固

### 9.1 代码混淆

确保ProGuard配置正确，避免关键代码被反编译

### 9.2 网络安全

```java
// 使用证书固定
CertificatePinner certificatePinner = new CertificatePinner.Builder()
    .add("your-server.com", "sha256/AAAA...")
    .build();

OkHttpClient client = new OkHttpClient.Builder()
    .certificatePinner(certificatePinner)
    .build();
```

### 9.3 数据加密

```java
// 加密本地敏感数据
public class CryptoUtils {
    public static String encrypt(String data) {
        // AES加密实现
    }
    
    public static String decrypt(String encrypted) {
        // AES解密实现
    }
}
```

---

## 📝 步骤10: 文档和培训

### 10.1 用户手册

创建详细的用户使用手册，包括：
- 安装指南
- 功能说明
- 常见问题
- 故障排除

### 10.2 培训材料

准备培训PPT和视频教程：
- 基础操作演示
- 高级功能讲解
- 最佳实践分享

### 10.3 技术文档

维护技术文档：
- API文档
- 架构设计文档
- 部署文档
- 维护手册

---

## ✅ 部署检查表

### 部署前
- [ ] 所有测试通过
- [ ] 代码审查完成
- [ ] 版本号已更新
- [ ] 更新日志已编写
- [ ] 签名密钥已备份
- [ ] 服务器环境已准备
- [ ] 监控已配置

### 部署后
- [ ] APK已分发
- [ ] 用户已通知
- [ ] 监控数据正常
- [ ] 无严重错误
- [ ] 用户反馈收集
- [ ] 文档已更新

---

## 🆘 故障处理

### 常见问题

**1. 安装失败**
```
原因: 签名不匹配
解决: 卸载旧版本后重新安装
```

**2. 无法连接服务器**
```
原因: 证书问题或网络限制
解决: 检查HTTPS证书配置
```

**3. 拨号功能异常**
```
原因: 权限未授予
解决: 引导用户到设置中授权
```

---

## 📞 技术支持

### 联系方式
- **技术支持邮箱**: support@yourcompany.com
- **技术支持电话**: 400-xxx-xxxx
- **工作时间**: 周一至周五 9:00-18:00

### 反馈渠道
- 应用内反馈功能
- 邮件反馈
- 电话咨询
- 在线客服

---

**部署文档版本**: v1.0  
**更新日期**: 2025-01-30  
**适用版本**: APP v1.0.0
