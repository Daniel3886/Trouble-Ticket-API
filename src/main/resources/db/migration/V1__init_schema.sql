CREATE TABLE services (
    id        BIGINT       PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    active    BOOLEAN      NOT NULL
);

CREATE TABLE trouble_tickets (
    id          VARCHAR(36)   PRIMARY KEY,
    tenant_id   VARCHAR(255)  NOT NULL,
    external_id VARCHAR(255)  NOT NULL,
    service_id  BIGINT        NOT NULL,
    description VARCHAR(5000) NOT NULL,
    status      VARCHAR(20)   NOT NULL,
    version     BIGINT        NOT NULL,


    CONSTRAINT unique_tenant_external_id UNIQUE (tenant_id, external_id)
);

CREATE TABLE notes (
    id                VARCHAR(36)              PRIMARY KEY,
    text              VARCHAR(5000)            NOT NULL,
    date              TIMESTAMP WITH TIME ZONE NOT NULL,
    trouble_ticket_id VARCHAR(36)              NOT NULL REFERENCES trouble_tickets (id)
);

CREATE INDEX idx_notes_trouble_ticket ON notes (trouble_ticket_id, date);
