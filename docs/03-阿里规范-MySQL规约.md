# 阿里巴巴 Java 开发规范 — MySQL 规约

> 来源：《阿里巴巴Java开发手册》| 约束力：【强制】>【推荐】>【参考】

---

## MySQL 数据库规约

| 规约 | 说明 |
|------|------|
| 【强制】表名/字段名用小写，下划线分隔 | `user_info`，禁止大写 |
| 【强制】表达"是与否"用 `is_xxx` | 类型用 `tinyint` |
| 【强制】小数用 `decimal` | 不用 `float`/`double` |
| 【强制】索引命名：`idx_xxx` / `uk_xxx` | 区分普通索引和唯一索引 |
| 【推荐】每张表必须有主键 `id` | 推荐自增 |

---

## 补充说明

### 表名与字段命名

```sql
-- 正例
CREATE TABLE user_info (
    id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username VARCHAR(50)  NOT NULL                COMMENT '用户名',
    is_deleted TINYINT    NOT NULL DEFAULT 0      COMMENT '是否删除 0-否 1-是',
    price    DECIMAL(10,2) NOT NULL               COMMENT '价格',
    PRIMARY KEY (id),
    INDEX idx_username (username),
    UNIQUE KEY uk_username (username)
);

-- 反例
CREATE TABLE UserInfo (     -- ❌ 大驼峰
    isDeleted INT,          -- ❌ 驼峰命名
    price FLOAT             -- ❌ 浮点类型存金额
);
```

### 索引命名规范

| 索引类型 | 命名格式 | 示例 |
|----------|----------|------|
| 普通索引 | `idx_{字段}` | `idx_user_id` |
| 唯一索引 | `uk_{字段}` | `uk_mobile` |
| 联合索引 | `idx_{字段1}_{字段2}` | `idx_user_id_status` |

---

*来源：《阿里巴巴Java开发手册》*
