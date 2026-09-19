-- 开发阶段手动更新 SQL（PostgreSQL）
-- 适用：已执行 1.0.36，但数据库中缺少租户查询索引的开发环境。
-- 手动执行一次即可；新建数据库直接执行合并后的 1.0.36 脚本即可。

CREATE INDEX IF NOT EXISTS "base_department__idx_tenant_id"
    ON "base_department" ("tenant_id");
CREATE INDEX IF NOT EXISTS "base_rbac_role__idx_tenant_id"
    ON "base_rbac_role" ("tenant_id");
