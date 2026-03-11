CREATE TABLE IF NOT EXISTS oscar_chat_messages (
    id          BIGSERIAL PRIMARY KEY,
    session_id  VARCHAR(36)  NOT NULL,
    user_id     VARCHAR(255) NOT NULL,
    role        VARCHAR(10)  NOT NULL,
    contents    TEXT         NOT NULL,
    response_type VARCHAR(20) NOT NULL,
    ts_millis   BIGINT       NOT NULL
);

CREATE INDEX IF NOT EXISTS oscar_chat_messages_session_ts_idx
    ON oscar_chat_messages (session_id, ts_millis);
