-- Tags associated to a given resource
CREATE TABLE ResourceTags (
  -- The task associated to those tags
  resource BIGINT NOT NULL,
  -- The tag name
  tag      varchar(256) NOT NULL,
  -- The tag value
  value    varchar(256) NOT NULL,

  PRIMARY KEY (resource, tag),

  -- Remove tags if task goes away...
  FOREIGN KEY (resource) REFERENCES Resources
    ON DELETE CASCADE
);


----- Update experiment tasks

ALTER TABLE Experiments ADD COLUMN last BOOLEAN DEFAULT TRUE NOT NULL;
CREATE INDEX LastExperiments ON Experiments (last);
UPDATE Experiments as e SET last=false WHERE timestamp<>(SELECT max(timestamp) FROM Experiments e2 WHERE e2.name=e.name);


