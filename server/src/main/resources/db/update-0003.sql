-- Adds a new column to see the last update


ALTER TABLE Processes ADD COLUMN progress DOUBLE DEFAULT 0 NOT NULL;
ALTER TABLE Processes ADD COLUMN last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL;

UPDATE Processes p SET progress = (SELECT j.progress FROM Jobs j WHERE j.id = p.resource);
ALTER TABLE Jobs DROP COLUMN progress;
