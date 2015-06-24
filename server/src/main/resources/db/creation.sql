-- Main tables

-- Resources

CREATE TABLE Resources (
  id IDENTITY,
  path     VARCHAR(4096),
  status   BIGINT,
  type     BIGINT,
  priority INT DEFAULT 0 NOT NULL,
  data     BLOB not null
);

CREATE INDEX status_index ON resources (status);
CREATE UNIQUE INDEX status_path ON resources (path);
CREATE INDEX status_priority ON resources (priority);

-- Token resource
CREATE TABLE TokenResources (
  id    BIGINT NOT NULL PRIMARY KEY,
  limit INT    NOT NULL,
  used  INT    NOT NULL,
  FOREIGN KEY (id) REFERENCES Resources
    ON DELETE CASCADE
);

-- Dependencies between resources

CREATE TABLE Dependencies (
  fromId BIGINT   NOT NULL,
  toId   BIGINT   NOT NULL,
  status SMALLINT NOT NULL,

  -- Foreign key for the source (restricting deletion)
  FOREIGN KEY (fromId) REFERENCES Resources
    ON DELETE RESTRICT,
  FOREIGN KEY (toId) REFERENCES Resources
    ON DELETE CASCADE
);

-- Process

CREATE TABLE Processes (
  resource BIGINT NOT NULL,
  value    BLOB   NOT NULL,
  FOREIGN KEY (resource) REFERENCES Resources
    ON DELETE RESTRICT
);

-- Connectors

CREATE TABLE Connectors (
  id IDENTITY,
  type  BIGINT        NOT NULL,
  uri   VARCHAR(4096) NOT NULL,
  value BLOB
);

-- Shares

CREATE TABLE NetworkShares (
  id IDENTITY,
  hostname VARCHAR(256) NOT NULL,
  name     VARCHAR(256) NOT NULL
);

CREATE TABLE NetworkShareAccess (
  share     BIGINT        NOT NULL,
  connector BIGINT        NOT NULL,
  path      VARCHAR(4096) NOT NULL,
  priority  INT DEFAULT 0 NOT NULL,

  FOREIGN KEY (share) REFERENCES NetworkShares
    ON DELETE CASCADE,
  FOREIGN KEY (connector) REFERENCES Connectors
    ON DELETE CASCADE
);


-- Experiments

CREATE TABLE Experiments (
  id IDENTITY,
  name      VARCHAR(256)                        NOT NULL,
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX experiment_name ON Experiments (name, timestamp);

CREATE TABLE ExperimentTasks (
  id IDENTITY,
  identifier VARCHAR(256) NOT NULL,
  experiment BIGINT       NOT NULL,
  parent     BIGINT       NOT NULL,

  FOREIGN KEY (experiment) REFERENCES Experiments
    ON DELETE CASCADE,
  FOREIGN KEY (parent) REFERENCES ExperimentTasks
    ON DELETE CASCADE
);

CREATE TABLE ExperimentResources (
  task     BIGINT NOT NULL,
  resource BIGINT NOT NULL,

  FOREIGN KEY (task) REFERENCES ExperimentTasks
    ON DELETE CASCADE,
  FOREIGN KEY (resource) REFERENCES Resources
    ON DELETE RESTRICT
);