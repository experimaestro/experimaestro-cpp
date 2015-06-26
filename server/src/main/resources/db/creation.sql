--
-- Connectors and shares
--


CREATE TABLE Connectors (
  id       IDENTITY,
  type     BIGINT        NOT NULL,
  uri      VARCHAR(4096) NOT NULL,
  data     BLOB          NOT NULL
);

CREATE TABLE NetworkShares (
  id       IDENTITY,
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


--
-- Resources
--

CREATE TABLE Resources (
  id IDENTITY,
  path      VARCHAR(4096),
  connector BIGINT,
  status    BIGINT,
  type      BIGINT,
  priority  INT DEFAULT 0 NOT NULL,
  data      BLOB          NOT NULL,

  FOREIGN KEY (connector) REFERENCES Connectors
    ON DELETE RESTRICT
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

-- Job resource
CREATE TABLE Job (
  id          BIGINT NOT NULL PRIMARY KEY,
  submitted   TIMESTAMP,
  start       TIMESTAMP,
  end         TIMESTAMP,
  unsatisfied INT    NOT NULL,
  holding     INT    NOT NULL,
  priority    INT    NOT NULL,
  progress    DOUBLE NOT NULL,

  FOREIGN KEY (id) REFERENCES Resources
    ON DELETE CASCADE
);

--
-- Other
--


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

---
--- A lock
---

CREATE TABLE Locks (
  id IDENTITY,
  type BIGINT NOT NULL,
  data BLOB   NOT NULL
);


-- Ensures that shares are not removed if a lock references it
CREATE TABLE LockShares (
  lock    BIGINT NOT NULL,
  share   BIGINT NOT NULL,
  path    VARCHAR(4096) NOT NULL,

  -- do not delete a share if it is referenced
  FOREIGN KEY (lock) REFERENCES Locks ON DELETE CASCADE,
  FOREIGN KEY (share) REFERENCES NetworkShares ON DELETE RESTRICT
);


--
-- Process
--

CREATE TABLE Processes (
  resource  BIGINT NOT NULL,
  type      BIGINT NOT NULL,
  connector BIGINT,
  pid       VARCHAR(255),
  data      BLOB   NOT NULL,
  FOREIGN KEY (resource) REFERENCES Resources
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  FOREIGN KEY (connector) REFERENCES Connectors
    ON DELETE RESTRICT
    ON UPDATE CASCADE
);

CREATE TABLE ProcessLocks (
  process BIGINT NOT NULL,
  lock    BIGINT NOT NULL,

  -- Ensures that no locks are left behind
  CONSTRAINT ProcessLocks_process FOREIGN KEY (process) REFERENCES Resources
    ON DELETE RESTRICT,

  -- Removing a lock will remove the process lock
  CONSTRAINT ProcessLocks_lock FOREIGN KEY (lock) REFERENCES Locks
    ON DELETE CASCADE
);



--
-- Experiments
--

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


