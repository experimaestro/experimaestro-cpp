package net.bpiwowar.xpm.manager.tasks;

import net.bpiwowar.xpm.manager.Typename;

/**
 * Value type for task inputs
 */
public interface TaskInputType {

    /** Input stream type */
    class InputStream implements TaskInputType {
        public final Typename inputstream;

        public InputStream(Typename inputstream) {
            this.inputstream = inputstream;
        }
    }

    /** Simple JSON type */
    class Json implements TaskInputType {
        public final Typename value;

        public Json(Typename value) {
            this.value = value;
        }
    }
}
