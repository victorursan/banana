
CREATE TABLE location (
    location_id UUID NOT NULL PRIMARY KEY,
    parent_location UUID NOT NULL REFERENCES location(location_id),
    message TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp
);

INSERT INTO location(location_id, parent_location, message, active)
VALUES ('929abc9f-f34f-4a44-9928-863d9dfbe705', '929abc9f-f34f-4a44-9928-863d9dfbe705', 'World', true),
('95c12221-2314-4d1f-bf25-bd30d969c49f', '95c12221-2314-4d1f-bf25-bd30d969c49f', 'NO LOCATION', true);

CREATE TABLE role (
    role_id UUID NOT NULL PRIMARY KEY,
    role_type TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp
);

INSERT INTO role (role_id, role_type, active)
VALUES ('53e07fd5-8deb-4ab6-aedb-cbcdcf28eec1', 'ADMIN', true),
('90642ef3-cd01-4fe5-a789-af915ddeaebc', 'COMMUNITY', true),
('56841b70-d343-445f-b4a7-c0b10ea4e0f6', 'CLEANER', true),
('2a53b2dc-11c3-4de6-a382-b6a9a1e3173e', 'MAINTENANCE', true),
('8981b593-6d7a-45db-bbbe-cbcdd23cc693', 'MEMBER', true),
('2fdeaa40-1e25-4b08-b960-5add7c18d59f', 'NO ROLE', true);

CREATE TABLE personnel (
    personnel_id UUID NOT NULL PRIMARY KEY,
    first_name TEXT,
    last_name TEXT,
    email TEXT,
    checked_in BOOLEAN NOT NULL DEFAULT true,
    location_id UUID NOT NULL DEFAULT '95c12221-2314-4d1f-bf25-bd30d969c49f' REFERENCES location(location_id),
    role_id UUID NOT NULL DEFAULT '2fdeaa40-1e25-4b08-b960-5add7c18d59f' REFERENCES role(role_id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp
);

INSERT INTO personnel (personnel_id, checked_in, location_id, role_id)
VALUES ('cf338d20-073a-4f28-ad68-a104d02eef9d', true, '929abc9f-f34f-4a44-9928-863d9dfbe705', '53e07fd5-8deb-4ab6-aedb-cbcdcf28eec1');

CREATE TABLE sticky (
    sticky_id UUID NOT NULL PRIMARY KEY,
    message TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp
);

CREATE TABLE sticky_location (
    location_id UUID NOT NULL UNIQUE REFERENCES location(location_id),
    sticky_id UUID NOT NULL REFERENCES sticky(sticky_id),
    message TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    PRIMARY KEY(location_id, sticky_id)
);

CREATE TABLE telegram_channel (
    chat_id BIGINT NOT NULL PRIMARY KEY,
    personnel_id UUID NOT NULL REFERENCES personnel(personnel_id),
    username TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp
);

CREATE TABLE chat_message (
    message_id BIGINT NOT NULL,
    chat_id BIGINT NOT NULL REFERENCES telegram_channel(chat_id),
    message TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    PRIMARY KEY(message_id, chat_id)
);

CREATE TABLE sticky_action (
    action_id UUID NOT NULL PRIMARY KEY,
    sticky_id UUID NOT NULL REFERENCES sticky(sticky_id),
    role_id UUID NOT NULL REFERENCES role(role_id),
    message TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp
);

CREATE TYPE STATE AS ENUM ('PENDING', 'ACQUIRED', 'SOLVED');

CREATE TABLE ticket (
    ticket_id UUID NOT NULL PRIMARY KEY,
    action_id UUID NOT NULL REFERENCES sticky_action(action_id),
    location_id UUID NOT NULL REFERENCES sticky_location(location_id),
    message TEXT NOT NULL,
    state STATE NOT NULL,
    aquired_by UUID REFERENCES personnel(personnel_id),
    aquired_at TIMESTAMP WITH TIME ZONE,
    solved_by UUID REFERENCES personnel(personnel_id),
    solved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp
);

CREATE TYPE NOTIFICATION AS ENUM ('CREATED_BY', 'FOLLOWING');

CREATE TABLE personnel_ticket (
    ticket_id UUID NOT NULL,
    personnel_id UUID NOT NULL,
    notification NOTIFICATION NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    PRIMARY KEY(ticket_id, personnel_id)
);

CREATE TABLE chat_ticket_message (
    message_id BIGINT NOT NULL,
    chat_id BIGINT NOT NULL REFERENCES telegram_channel(chat_id),
    ticket_id UUID NOT NULL REFERENCES ticket(ticket_id),
    visible BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    PRIMARY KEY(message_id, chat_id)
);

CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_timestamp
BEFORE UPDATE ON location
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

CREATE TRIGGER set_timestamp
BEFORE UPDATE ON role
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

CREATE TRIGGER set_timestamp
BEFORE UPDATE ON personnel
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

CREATE TRIGGER set_timestamp
BEFORE UPDATE ON sticky
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

CREATE TRIGGER set_timestamp
BEFORE UPDATE ON sticky_location
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

CREATE TRIGGER set_timestamp
BEFORE UPDATE ON telegram_channel
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

CREATE TRIGGER set_timestamp
BEFORE UPDATE ON chat_message
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

CREATE TRIGGER set_timestamp
BEFORE UPDATE ON sticky_action
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

CREATE TRIGGER set_timestamp
BEFORE UPDATE ON ticket
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

CREATE TRIGGER set_timestamp
BEFORE UPDATE ON personnel_ticket
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

CREATE TRIGGER set_timestamp
BEFORE UPDATE ON chat_ticket_message
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();
