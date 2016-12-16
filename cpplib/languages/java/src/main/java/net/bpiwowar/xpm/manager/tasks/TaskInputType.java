package net.bpiwowar.xpm.manager.tasks;

import net.bpiwowar.xpm.manager.TypeName;

/**
 * Value type for task inputs
 */
public interface TaskInputType {

    /** Input stream type */
    class InputStream implements TaskInputType {
        public final TypeName inputstream;

        public InputStream(TypeName inputstream) {
            this.inputstream = inputstream;
        }
    }

    /** Simple JSON type */
    class Json implements TaskInputType {
        public final TypeName value;

        public Json(TypeName value) {
            this.value = value;
        }
    }
}
