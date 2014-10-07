package net.bpiwowar.experimaestro.tasks;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.lang.reflect.Field;

/**
 * A task
 */
public abstract class AbstractTask {
    /** The working directory */
    protected File workingDirectory;

    /**
     * Execute the task
     * @param r The returned object pre-filled with values
     * @return A json object corresponding to the task
     * @throws Throwable Any error that occurs should be reported through exceptions
     */
    public abstract JsonElement execute(JsonObject r) throws Throwable;

    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
}
