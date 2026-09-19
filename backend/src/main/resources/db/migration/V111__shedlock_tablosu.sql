-- V111__shedlock_tablosu.sql
-- Zamanlanmis islerin cok instance'li ortamda tek kez calismasi icin ShedLock tablosu.

CREATE TABLE IF NOT EXISTS sistem.shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    CONSTRAINT pk_shedlock PRIMARY KEY (name)
);
