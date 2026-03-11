CREATE TABLE IF NOT EXISTS okapi_oscar.oscar_chat_messages (
    id               BIGSERIAL PRIMARY KEY,
    session_id       VARCHAR(36)  NOT NULL,
    user_id          VARCHAR(255) NOT NULL,
    role             VARCHAR(10)  NOT NULL,
    contents         TEXT         NOT NULL,
    event_stream_id  BIGINT  NOT NULL,
    response_type    VARCHAR(64),
    ts_millis        BIGINT       NOT NULL
);

CREATE INDEX IF NOT EXISTS oscar_chat_messages_session_ts_idx
    ON okapi_oscar.oscar_chat_messages (session_id, ts_millis);

CREATE TABLE IF NOT EXISTS okapi_oscar.oscar_stream_meta (
    stream_id   BIGSERIAL PRIMARY KEY,
    session_id  VARCHAR(36)  NOT NULL,
    start_time  BIGINT       NOT NULL,
    state       VARCHAR(10)  NOT NULL
);

CREATE TABLE IF NOT EXISTS okapi_oscar.oscar_session_meta (
    session_id          VARCHAR(36)  PRIMARY KEY,
    state               VARCHAR(10)  NOT NULL,
    start_time          BIGINT       NOT NULL,
    last_recorded_ping  BIGINT       NOT NULL,
    owner_id            VARCHAR(36)  NOT NULL,
    session_title       TEXT         NOT NULL,
    ongoing_stream_id   BIGINT       REFERENCES okapi_oscar.oscar_stream_meta(stream_id)
);
