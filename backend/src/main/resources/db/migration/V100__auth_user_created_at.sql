-- T01 auth: account creation time, so JWTs issued before an account was (re)created are rejected.
ALTER TABLE `user`
    ADD COLUMN `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
