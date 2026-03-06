CREATE TABLE users
(
    uuid     UUID         NOT NULL,
    username VARCHAR(30)  NOT NULL,
    password VARCHAR(100) NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (uuid)
);