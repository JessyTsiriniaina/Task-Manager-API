-- V1: Initial schema for Task Manager API
-- Generated to match the Hibernate entity mapping (PostgreSQL / H2 in PostgreSQL mode).

-- Identity / sequence helpers
create sequence blocked_tokens_seq start with 1 increment by 50;
create sequence refresh_tokens_seq start with 1 increment by 50;
create sequence task_seq start with 1 increment by 50;
create sequence users_seq start with 1 increment by 50;

-- users
create table users (
    created_at timestamp(6),
    id bigint not null,
    updated_at timestamp(6),
    username varchar(100) not null unique,
    email varchar(255) not null unique,
    password varchar(255) not null,
    primary key (id)
);

-- task
create table task (
    priority smallint not null check (priority between 0 and 2),
    status smallint not null check (status between 0 and 2),
    created_at timestamp(6),
    due_date timestamp(6) not null,
    id bigint not null,
    updated_at timestamp(6),
    user_id bigint not null,
    title varchar(100) not null,
    description varchar(1000),
    primary key (id)
);

-- refresh_tokens
create table refresh_tokens (
    created_at timestamp(6),
    expires_at timestamp(6) not null,
    id bigint not null,
    revoked_at timestamp(6),
    user_id bigint not null,
    token_hash varchar(64) not null unique,
    primary key (id)
);

-- blocked_tokens
create table blocked_tokens (
    blocked_at timestamp(6),
    expires_at timestamp(6) not null,
    id bigint not null,
    jti varchar(64) not null unique,
    primary key (id)
);

-- Foreign keys
alter table if exists refresh_tokens
    add constraint fk_refresh_tokens_user
    foreign key (user_id)
    references users;

alter table if exists task
    add constraint fk_task_user
    foreign key (user_id)
    references users;
