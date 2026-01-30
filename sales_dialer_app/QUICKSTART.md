# 智能电销王 - 快速开始指南

## 🚀 5分钟快速上手

### 第一步：环境准备

#### 必需软件
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 11+
- Android SDK (API 23-34)

#### 安装Android Studio
1. 访问 https://developer.android.com/studio
2. 下载并安装最新版本
3. 安装时选择标准安装（Standard Installation）

### 第二步：导入项目

1. **打开Android Studio**
   ```
   File -> Open -> 选择 sales_dialer_app 目录
   ```

2. **等待Gradle同步**
   - Android Studio会自动下载依赖
   - 第一次可能需要5-10分钟
   - 确保网络连接正常

3. **配置SDK**
   ```
   File -> Project Structure -> SDK Location
   确认Android SDK路径正确
   ```

### 第三步：配置Odoo服务器

编辑配置文件（可选，也可以在APP中配置）：

```java
// app/src/main/java/com/salesdialer/config/ApiConfig.java

public class ApiConfig {
    // 您的Odoo服务器地址
    public static final String BASE_URL = "https://your-odoo-server.com";
    
    // 数据库名称
    public static final String DATABASE = "your_database_name";
}
```

### 第四步：运行应用

#### 使用真机（推荐）

1. **启用开发者选项**
   - 设置 -> 关于手机 -> 连续点击"版本号" 7次
   
2. **启用USB调试**
   - 设置 -> 开发者选项 -> USB调试

3. **连接手机**
   - 使用USB线连接电脑
   - 允许USB调试授权

4. **运行**
   - 点击Android Studio的运行按钮（绿色三角形）
   - 选择已连接的设备
   - 等待安装完成

#### 使用模拟器

1. **创建模拟器**
   ```
   Tools -> Device Manager -> Create Device
   ```

2. **选择设备型号**
   - 推荐：Pixel 5 或更高版本
   - 系统镜像：Android 10 或更高版本

3. **启动模拟器**
   - 点击启动按钮
   - 等待模拟器启动完成

4. **运行应用**
   - 点击运行按钮
   - 应用会自动安装到模拟器

### 第五步：首次使用

1. **打开应用**
   - 应用图标：智能电销王

2. **登录**
   - 输入Odoo服务器地址
   - 输入数据库名称
   - 输入用户名和密码
   - 点击登录

3. **授予权限**
   - 拨打电话权限（必需）
   - 读取通话状态（必需）
   - 其他权限按需授予

4. **同步数据**
   - 首次登录会自动同步客户数据
   - 等待同步完成

5. **开始拨号**
   - 进入客户列表
   - 点击客户进行一键拨号
   - 或启动自动拨号模式

---

## 🎯 核心功能使用

### 一键拨号

```
客户列表 -> 点击客户 -> 点击拨号按钮
```

### 自动拨号

```
1. 点击右下角"自动拨号"按钮
2. 选择拨号策略：
   - 优先级优先（推荐A级客户）
   - 时间优先（很久没联系的）
   - 随机拨打
3. 设置拨号间隔：5秒/10秒/30秒
4. 选择客户范围：
   - 全部客户
   - 仅私客
   - 仅公客
   - 仅A级客户
5. 点击"开始拨号"
```

### 添加跟进

```
方式一：通话后自动弹出
1. 选择跟进结果
2. 输入跟进内容
3. 设置下次跟进时间
4. 保存

方式二：手动添加
客户详情 -> 跟进记录 -> 添加跟进
```

### 查看统计

```
主页 -> 统计 -> 查看各项数据
- 今日拨打数量
- 今日接通率
- 本周/本月统计
- 排行榜
```

---

## 🔧 常见问题

### Q1: 无法拨打电话？

**检查清单：**
- [ ] 是否授予了拨打电话权限
- [ ] 手机是否插入SIM卡
- [ ] 手机号码是否正确
- [ ] 是否有网络电话APP干扰

**解决方法：**
```
设置 -> 应用 -> 智能电销王 -> 权限 -> 开启"拨打电话"
```

### Q2: 无法连接Odoo服务器？

**检查清单：**
- [ ] 服务器地址是否正确（包含http://或https://）
- [ ] 数据库名称是否正确
- [ ] 用户名密码是否正确
- [ ] 手机是否连接网络
- [ ] 服务器是否可访问

**测试方法：**
```
在浏览器中访问服务器地址，看是否能打开Odoo登录页
```

### Q3: 自动拨号启动不了？

**检查清单：**
- [ ] 是否有客户数据
- [ ] 客户手机号是否有效
- [ ] 是否授予了必要权限
- [ ] 拨号间隔设置是否合理

**解决方法：**
```
1. 确保至少有一个有效客户
2. 检查筛选条件是否过于严格
3. 尝试重启应用
```

### Q4: 通话记录不同步？

**检查清单：**
- [ ] 网络是否连接
- [ ] 登录是否有效
- [ ] 服务器是否正常

**手动同步：**
```
主页 -> 下拉刷新 -> 等待同步完成
```

---

## 📱 开发测试

### 调试模式

1. **查看日志**
   ```
   Android Studio -> Logcat -> 筛选: salesdialer
   ```

2. **常用TAG**
   ```
   AutoDialerService  - 自动拨号日志
   OdooApiClient     - API请求日志
   MainActivity      - 主页面日志
   ```

### 测试拨号功能

1. **准备测试数据**
   - 在Odoo后台添加几个测试客户
   - 手机号使用自己的号码或测试号码

2. **测试步骤**
   ```
   1. 登录应用
   2. 同步客户数据
   3. 进入客户列表
   4. 点击测试客户
   5. 点击拨号按钮
   6. 观察是否能正常拨打
   ```

3. **测试自动拨号**
   ```
   1. 准备3-5个测试客户
   2. 启动自动拨号
   3. 设置较长间隔（30秒）
   4. 观察拨号流程
   5. 测试暂停/恢复/停止功能
   ```

### 测试权限

1. **首次安装测试**
   ```
   1. 卸载应用
   2. 重新安装
   3. 检查权限申请流程
   4. 逐个授予权限
   5. 验证功能是否正常
   ```

2. **权限撤销测试**
   ```
   设置 -> 应用 -> 智能电销王 -> 权限
   撤销"拨打电话"权限
   再次使用拨号功能，检查提示
   ```

---

## 🔨 自定义配置

### 修改拨号间隔选项

```java
// SettingsActivity.java

private void initDialIntervalSpinner() {
    String[] intervals = {
        "3秒",   // 添加更短的间隔
        "5秒",
        "10秒",
        "30秒",
        "手动控制"
    };
    // ...
}
```

### 修改拨号策略

```java
// AutoDialerActivity.java

private void initStrategySpinner() {
    String[] strategies = {
        "优先级优先",
        "时间优先",
        "随机拨打",
        "自定义策略"  // 添加新策略
    };
    // ...
}
```

### 修改客户筛选

```java
// CustomerListActivity.java

private void applyFilters() {
    // 添加更多筛选条件
    // 例如：按意向楼盘筛选
    if (selectedProperty != null) {
        query = query.equalTo("intendedProperty", selectedProperty);
    }
}
```

---

## 📦 打包发布

### Debug版本（测试用）

```bash
# 生成Debug APK
./gradlew assembleDebug

# 输出位置
app/build/outputs/apk/debug/app-debug.apk
```

### Release版本（正式发布）

1. **生成签名密钥**
   ```bash
   keytool -genkey -v -keystore sales-dialer-key.jks \
     -alias sales-dialer -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **配置签名**
   ```gradle
   // app/build.gradle
   android {
       signingConfigs {
           release {
               storeFile file("../sales-dialer-key.jks")
               storePassword "your-password"
               keyAlias "sales-dialer"
               keyPassword "your-password"
           }
       }
   }
   ```

3. **打包Release**
   ```bash
   ./gradlew assembleRelease
   
   # 输出位置
   app/build/outputs/apk/release/app-release.apk
   ```

4. **安装测试**
   ```bash
   adb install app/build/outputs/apk/release/app-release.apk
   ```

---

## 🎓 学习资源

### Android开发
- [Android官方文档](https://developer.android.com/docs)
- [Material Design指南](https://material.io/design)

### Odoo API
- [Odoo官方文档](https://www.odoo.com/documentation)
- [Odoo API参考](https://www.odoo.com/documentation/16.0/developer/api.html)

### 项目相关
- [项目README](./README.md)
- [API文档](./docs/API.md)
- [架构说明](./docs/ARCHITECTURE.md)

---

## 💡 提示和技巧

### 提高开发效率

1. **使用快捷键**
   - `Ctrl + Click`: 跳转到定义
   - `Ctrl + Alt + L`: 格式化代码
   - `Ctrl + /`: 注释/取消注释

2. **实时预览**
   ```
   Tools -> Device Manager -> 运行模拟器
   保持模拟器运行，修改代码后直接运行
   ```

3. **快速调试**
   ```
   在代码行号处点击添加断点
   点击Debug按钮（🐞）运行
   ```

### 提高拨号效率

1. **优化客户数据**
   - 确保手机号格式正确
   - 及时更新客户状态
   - 添加详细的客户备注

2. **合理设置间隔**
   - 新手建议30秒间隔
   - 熟练后可以减少到10秒
   - 高峰期适当增加间隔

3. **使用拨号策略**
   - 早上：拨打A级客户
   - 下午：拨打B级客户
   - 晚上：拨打C级客户

---

**祝您使用愉快！如有问题，随时查看文档或联系支持。** 🎉
