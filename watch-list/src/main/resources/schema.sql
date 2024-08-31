create table if not exists telegram_session (
    id bigint primary key generated always as identity,
    username varchar(255) unique not null,
    chat_id bigint not null,
    last_message_received_at timestamptz default current_timestamp not null
);

create table if not exists download_content_process (
    id bigint primary key generated always as identity,
    status varchar(30) not null,
    run_iteration integer not null default 0,
    content_item_identity varchar(255) unique not null,
    torr_file_path varchar(255) not null,
    torr_info_hash varchar(40),
    content_path varchar(255), -- might be null at INITIAL status
    created_at timestamptz default current_timestamp not null,
    updated_at timestamptz default current_timestamp not null,
    next_run_after timestamptz default current_timestamp not null,
    context jsonb
);
