-- T01 auth: account creation time, so JWTs issued before an account was (re)created are rejected.
-- Accounts that predate this migration get a sentinel in the past so their outstanding tokens stay valid.
ALTER TABLE `user`
    ADD COLUMN `created_at` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00';
ALTER TABLE `user`
    MODIFY COLUMN `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
