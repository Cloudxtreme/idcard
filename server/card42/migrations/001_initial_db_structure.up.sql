
CREATE TABLE buildings (
	id SERIAL PRIMARY KEY,
	name TEXT NOT NULL DEFAULT '',
	num_floors INT NOT NULL DEFAULT 1
);

CREATE TABLE users (
	id SERIAL PRIMARY KEY,
	full_name TEXT UNIQUE NOT NULL,
	intra_login VARCHAR(8) UNIQUE DEFAULT NULL,
	intra_user_id INTEGER
);

CREATE TABLE doors (
	id SERIAL PRIMARY KEY,
	card_door_id INT UNIQUE NOT NULL,
	full_name TEXT UNIQUE NOT NULL,

	dsx_door_num INT,
	dsx_panel_num INT,
	dsx_door_name VARCHAR(32) NOT NULL DEFAULT '',

	building_id INTEGER NOT NULL REFERENCES buildings(id),
	floor_num SMALLINT NOT NULL DEFAULT 1,

	active_config_id INTEGER,

	CONSTRAINT valid_card_id CHECK (card_door_id > 0 AND card_door_id < 256)
);

CREATE TABLE access_groups (
	id SERIAL PRIMARY KEY,
	name TEXT NOT NULL,
	description TEXT NOT NULL DEFAULT '',
	card_group_id INT NOT NULL UNIQUE,
	-- A special AccessGroup has no AccessGroupUser entries.
	is_special BOOLEAN NOT NULL DEFAULT false,

	CONSTRAINT valid_card_id CHECK (card_group_id > 0 AND card_group_id < 65536)
);

CREATE TABLE access_group_users (
	id SERIAL PRIMARY KEY,
	access_group_id INT NOT NULL REFERENCES access_groups(id),
	user_id INT NOT NULL REFERENCES users(id),
	is_manager BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE door_access_groups (
	id SERIAL PRIMARY KEY,
	access_group_id INT NOT NULL REFERENCES access_groups(id),
	door_id INT NOT NULL REFERENCES doors(id),

	action_code INT NOT NULL DEFAULT 0,
	action_order INT NOT NULL,
	group_is_manager BOOLEAN NOT NULL DEFAULT false,

	UNIQUE(door_id, access_group_id)
);
CREATE UNIQUE INDEX door_access_groups_query ON door_access_groups (door_id, action_order);
