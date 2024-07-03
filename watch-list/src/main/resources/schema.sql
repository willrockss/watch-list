create table if not exists telegram_session (
    id integer primary key generated always as identity,
    username varchar(255) unique not null,
    chat_id bigint not null,
    last_message_received_at timestamptz default current_timestamp not null
);