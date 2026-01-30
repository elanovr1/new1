# 智能电销王 - 架构设计文档

## 📐 系统架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────┐
│                     Android APP                         │
│  ┌───────────────────────────────────────────────────┐ │
│  │           Presentation Layer (UI)                 │ │
│  │  ┌────────┐  ┌────────┐  ┌─────────┐  ┌────────┐ │ │
│  │  │ Login  │  │  Main  │  │Customer │  │ Dialer │ │ │
│  │  │Activity│  │Activity│  │ Detail  │  │Activity│ │ │
│  │  └────────┘  └────────┘  └─────────┘  └────────┘ │ │
│  └───────────────────────────────────────────────────┘ │
│                           ↕                             │
│  ┌───────────────────────────────────────────────────┐ │
│  │            Business Logic Layer                   │ │
│  │  ┌──────────┐  ┌──────────┐  ┌────────────────┐ │ │
│  │  │Auto Dialer│  │ Customer │  │   Follow Up    │ │ │
│  │  │  Service  │  │ Manager  │  │    Manager     │ │ │
│  │  └──────────┘  └──────────┘  └────────────────┘ │ │
│  └───────────────────────────────────────────────────┘ │
│                           ↕                             │
│  ┌───────────────────────────────────────────────────┐ │
│  │              Data Layer                           │ │
│  │  ┌──────────┐  ┌──────────┐  ┌────────────────┐ │ │
│  │  │   Room   │  │  Odoo API│  │  Preferences   │ │ │
│  │  │ Database │  │  Client  │  │                │ │ │
│  │  └──────────┘  └──────────┘  └────────────────┘ │ │
│  └───────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                           ↕
┌─────────────────────────────────────────────────────────┐
│                  Odoo Backend System                    │
│  ┌───────────────────────────────────────────────────┐ │
│  │   CRM Customer Management Module (v2)             │ │
│  │   - Customer Model                                │ │
│  │   - Follow Up Model                               │ │
│  │   - User Management                               │ │
│  └───────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

---

## 🏗️ 模块设计

### 1. Presentation Layer (展示层)

#### Activity组件

| Activity | 功能 | 关键特性 |
|---------|------|----------|
| SplashActivity | 启动页 | 检查登录状态、初始化 |
| LoginActivity | 登录页 | Odoo认证、服务器配置 |
| MainActivity | 主页 | 客户列表、导航、搜索 |
| CustomerDetailActivity | 客户详情 | 查看/编辑客户信息 |
| AutoDialerActivity | 自动拨号 | 拨号控制、队列管理 |
| FollowUpActivity | 添加跟进 | 快速跟进、模板选择 |
| StatisticsActivity | 统计报表 | 数据可视化、图表 |
| SettingsActivity | 设置 | 拨号配置、账号管理 |

#### Adapter组件

```java
// 客户列表适配器
public class CustomerAdapter extends RecyclerView.Adapter<CustomerViewHolder> {
    - 显示客户列表
    - 支持下拉刷新
    - 点击事件处理
    - 长按菜单
}

// 跟进记录适配器
public class FollowUpAdapter extends RecyclerView.Adapter<FollowUpViewHolder> {
    - 显示跟进历史
    - 时间线展示
    - 编辑/删除操作
}
```

---

### 2. Business Logic Layer (业务逻辑层)

#### AutoDialerService (自动拨号服务)

**核心功能：**
- 维护拨号队列
- 控制拨号节奏
- 监听通话状态
- 统计拨号数据

**状态机：**
```
┌──────────┐     start      ┌──────────┐
│  IDLE    │ ───────────>  │ DIALING  │
└──────────┘               └──────────┘
     ↑                           │
     │ stop                 pause│resume
     │                           ↓
     │                     ┌──────────┐
     └─────────────────────│  PAUSED  │
                          └──────────┘
```

**关键方法：**
```java
void startDialing(List<Customer> customers, String strategy, int interval)
void pauseDialing()
void resumeDialing()
void stopDialing()
void skipCurrent()
DialerStatus getStatus()
```

#### CallStateService (通话状态服务)

**功能：**
- 监听通话状态变化
- 记录通话时长
- 触发跟进窗口
- 更新客户信息

**通话状态流转：**
```
IDLE → RINGING → OFFHOOK → IDLE
                    ↓
              (记录通话)
                    ↓
           (弹出跟进窗口)
```

#### SyncService (数据同步服务)

**功能：**
- 后台定期同步
- 增量同步策略
- 冲突解决
- 离线缓存

**同步策略：**
```java
1. 启动时全量同步
2. 定时增量同步（每5分钟）
3. 用户操作后立即同步
4. 网络恢复后自动同步
```

---

### 3. Data Layer (数据层)

#### Room Database (本地数据库)

**数据表设计：**

```sql
-- customers表
CREATE TABLE customers (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    phone TEXT,
    wechat_name TEXT,
    customer_type TEXT,
    level TEXT,
    status TEXT,
    requirement TEXT,
    remark TEXT,
    intended_property TEXT,
    owner_id INTEGER,
    follow_count INTEGER,
    write_date TEXT,
    create_date TEXT,
    is_synced INTEGER DEFAULT 1,
    last_call_time INTEGER DEFAULT 0,
    call_count INTEGER DEFAULT 0
);

-- follow_ups表
CREATE TABLE follow_ups (
    local_id INTEGER PRIMARY KEY AUTOINCREMENT,
    id INTEGER,
    customer_id INTEGER NOT NULL,
    follower_id INTEGER,
    follow_content TEXT NOT NULL,
    follow_time TEXT,
    result TEXT,
    next_follow_time TEXT,
    create_date TEXT,
    is_synced INTEGER DEFAULT 0,
    call_duration INTEGER DEFAULT 0,
    FOREIGN KEY(customer_id) REFERENCES customers(id)
);

-- call_logs表
CREATE TABLE call_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id INTEGER NOT NULL,
    phone_number TEXT,
    call_time TEXT,
    call_duration INTEGER,
    call_type TEXT,  -- outgoing, incoming, missed
    is_connected INTEGER DEFAULT 0,
    FOREIGN KEY(customer_id) REFERENCES customers(id)
);
```

**DAO接口：**

```java
@Dao
public interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY write_date ASC")
    List<Customer> getAllCustomers();
    
    @Query("SELECT * FROM customers WHERE id = :customerId")
    Customer getCustomerById(int customerId);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCustomers(List<Customer> customers);
    
    @Update
    void updateCustomer(Customer customer);
    
    @Delete
    void deleteCustomer(Customer customer);
    
    @Query("SELECT * FROM customers WHERE is_synced = 0")
    List<Customer> getUnsyncedCustomers();
}
```

#### Odoo API Client

**API调用流程：**

```
1. 构建请求参数
   ↓
2. 添加认证信息 (session_id)
   ↓
3. 发送HTTP请求
   ↓
4. 解析响应
   ↓
5. 处理结果/错误
```

**核心接口：**

```java
// 认证
boolean authenticate(String url, String db, String user, String pwd)

// 查询客户
List<Customer> getCustomers(int limit, int offset)

// 搜索客户
List<Customer> searchCustomers(String query)

// 创建跟进
int createFollowUp(FollowUp followUp)

// 更新客户
boolean updateCustomer(int id, Map<String, Object> values)

// 获取跟进记录
List<FollowUp> getFollowUps(int customerId)
```

**错误处理：**

```java
try {
    // API调用
} catch (AuthenticationException e) {
    // 认证失败，重新登录
} catch (NetworkException e) {
    // 网络错误，提示用户
} catch (ServerException e) {
    // 服务器错误，记录日志
}
```

---

## 🔄 数据流

### 客户数据流

```
Odoo Server
     ↓ (1) 同步请求
OdooApiClient
     ↓ (2) JSON响应
Parser (Gson)
     ↓ (3) Customer对象
Room Database
     ↓ (4) 查询
CustomerAdapter
     ↓ (5) 显示
RecyclerView
```

### 拨号流程

```
用户触发拨号
     ↓
AutoDialerService
     ↓ (1) 检查权限
PermissionUtils
     ↓ (2) 发起拨号
Android Phone System
     ↓ (3) 通话中
CallStateReceiver
     ↓ (4) 通话结束
FollowUpActivity
     ↓ (5) 保存跟进
OdooApiClient → Odoo Server
     ↓ (6) 同步完成
Room Database
```

---

## 🎯 设计模式

### 1. 单例模式 (Singleton)

```java
// OdooApiClient使用单例
public class OdooApiClient {
    private static OdooApiClient instance;
    
    private OdooApiClient(Context context) {
        // 初始化
    }
    
    public static synchronized OdooApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new OdooApiClient(context);
        }
        return instance;
    }
}
```

### 2. 观察者模式 (Observer)

```java
// 自动拨号服务监听
public interface DialerListener {
    void onDialStart(Customer customer);
    void onDialComplete(Customer customer, boolean success);
    void onQueueComplete();
    void onError(String error);
}

// 使用
service.setListener(new DialerListener() {
    @Override
    public void onDialStart(Customer customer) {
        // 更新UI
    }
});
```

### 3. 适配器模式 (Adapter)

```java
// RecyclerView适配器
public class CustomerAdapter extends RecyclerView.Adapter<CustomerViewHolder> {
    // 将Customer数据适配到RecyclerView
}
```

### 4. 策略模式 (Strategy)

```java
// 拨号策略
public interface DialStrategy {
    List<Customer> sort(List<Customer> customers);
}

public class PriorityDialStrategy implements DialStrategy {
    @Override
    public List<Customer> sort(List<Customer> customers) {
        // 按优先级排序
    }
}
```

---

## 🔒 安全设计

### 1. 数据加密

```java
// 敏感数据加密存储
public class SecurityUtils {
    // 加密密码
    public static String encrypt(String password) {
        // AES加密
    }
    
    // 解密密码
    public static String decrypt(String encrypted) {
        // AES解密
    }
}
```

### 2. 权限管理

```java
// 动态权限申请
@RuntimePermissions
public class MainActivity extends AppCompatActivity {
    @NeedsPermission(Manifest.permission.CALL_PHONE)
    void makeCall() {
        // 拨打电话
    }
    
    @OnPermissionDenied(Manifest.permission.CALL_PHONE)
    void onPermissionDenied() {
        // 权限被拒绝
    }
}
```

### 3. 网络安全

```java
// HTTPS证书验证
OkHttpClient client = new OkHttpClient.Builder()
    .certificatePinner(new CertificatePinner.Builder()
        .add("your-domain.com", "sha256/AAAAAAAAAAAAA...")
        .build())
    .build();
```

---

## ⚡ 性能优化

### 1. 列表优化

```java
// RecyclerView优化
recyclerView.setHasFixedSize(true);
recyclerView.setItemViewCacheSize(20);
recyclerView.setDrawingCacheEnabled(true);
```

### 2. 数据库优化

```sql
-- 添加索引
CREATE INDEX idx_customers_phone ON customers(phone);
CREATE INDEX idx_customers_level ON customers(level);
CREATE INDEX idx_follow_ups_customer ON follow_ups(customer_id);
```

### 3. 内存优化

```java
// 使用弱引用避免内存泄漏
private WeakReference<Context> contextRef;

// 及时释放资源
@Override
protected void onDestroy() {
    super.onDestroy();
    if (serviceConnection != null) {
        unbindService(serviceConnection);
    }
}
```

### 4. 图片优化

```java
// 使用Glide加载图片
Glide.with(context)
    .load(imageUrl)
    .placeholder(R.drawable.placeholder)
    .error(R.drawable.error)
    .diskCacheStrategy(DiskCacheStrategy.ALL)
    .into(imageView);
```

---

## 📊 监控和日志

### 日志策略

```java
public class LogUtils {
    private static final String TAG = "SalesDialer";
    
    public static void d(String message) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message);
        }
    }
    
    public static void e(String message, Throwable throwable) {
        Log.e(TAG, message, throwable);
        // 上报到服务器
        CrashReporter.report(message, throwable);
    }
}
```

### 性能监控

```java
// 方法耗时监控
long startTime = System.currentTimeMillis();
// 执行操作
long endTime = System.currentTimeMillis();
LogUtils.d("Operation took: " + (endTime - startTime) + "ms");
```

---

## 🧪 测试策略

### 单元测试

```java
@Test
public void testCustomerValidation() {
    Customer customer = new Customer();
    customer.setPhone("13800138000");
    assertTrue(customer.isValidPhone());
    
    customer.setPhone("123");
    assertFalse(customer.isValidPhone());
}
```

### 集成测试

```java
@Test
public void testOdooApiLogin() throws Exception {
    OdooApiClient client = OdooApiClient.getInstance(context);
    boolean result = client.authenticate(
        "https://test-server.com",
        "test_db",
        "admin",
        "admin"
    );
    assertTrue(result);
}
```

---

## 📈 扩展性设计

### 插件化架构

```java
// 定义插件接口
public interface DialPlugin {
    void beforeDial(Customer customer);
    void afterDial(Customer customer, boolean success);
}

// 实现插件
public class StatisticsPlugin implements DialPlugin {
    @Override
    public void afterDial(Customer customer, boolean success) {
        // 统计拨号数据
    }
}
```

### 模块化设计

```
app/
├── feature-customer/      # 客户管理模块
├── feature-dialer/        # 拨号模块
├── feature-statistics/    # 统计模块
├── core/                  # 核心模块
└── common/                # 通用模块
```

---

**文档版本**: v1.0  
**更新日期**: 2025-01-30  
**维护者**: Development Team
