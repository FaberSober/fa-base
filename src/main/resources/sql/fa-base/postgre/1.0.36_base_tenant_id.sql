-- ------------------------- info -------------------------
-- @@ver: 1_000_036
-- @@info: 统一 Telemetry 租户ID字段类型
-- ------------------------- info -------------------------

ALTER TABLE "base_client_error_event"
    ALTER COLUMN "tenant_id" TYPE varchar(32)
    USING "tenant_id"::varchar(32);

ALTER TABLE "base_stat_event"
    ALTER COLUMN "tenant_id" TYPE varchar(32)
    USING "tenant_id"::varchar(32);
