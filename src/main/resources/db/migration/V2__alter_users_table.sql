ALTER TABLE users
    ADD CONSTRAINT uc_users_username UNIQUE (username);