CREATE TABLE UserCache (
  identifier VARCHAR(256),
  keyhash CHAR(32),
  validity TIMESTAMP NOT NULL,
  jsonkey BLOB NOT NULL,
  jsondata BLOB NOT NULL,

  PRIMARY KEY (identifier, keyhash)
);