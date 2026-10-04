create extension if not exists pgcrypto;

create table if not exists users (
    id uuid primary key default gen_random_uuid(),
    username varchar(64) unique not null,
    display_name varchar(80) not null,
    password_hash text not null,
    avatar_url text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create unique index if not exists idx_users_display_name_lower on users (lower(display_name));

create table if not exists conversations (
    id uuid primary key default gen_random_uuid(),
    kind varchar(16) not null check (kind in ('private','group')),
    title varchar(120),
    created_at timestamptz not null default now()
);

create table if not exists conversation_members (
    conversation_id uuid not null references conversations(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    role varchar(16) not null default 'member',
    joined_at timestamptz not null default now(),
    primary key (conversation_id, user_id)
);

create table if not exists messages (
    id uuid primary key default gen_random_uuid(),
    conversation_id uuid not null references conversations(id) on delete cascade,
    sender_id uuid not null references users(id) on delete cascade,
    client_message_id varchar(80),
    body text not null,
    created_at timestamptz not null default now(),
    edited_at timestamptz,
    deleted_at timestamptz
);

create index if not exists idx_messages_conversation_created
    on messages(conversation_id, created_at desc);

create table if not exists message_receipts (
    message_id uuid not null references messages(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    status varchar(16) not null,
    updated_at timestamptz not null default now(),
    primary key (message_id, user_id)
);
