package net.bpiwowar.xpm.manager.tasks;

import net.bpiwowar.xpm.manager.TypeName;

/**
 * Value type for task inputs
 */
@ClassChooser(inner = true, mode = ClassChooserMode.FIELDS)
public interface TaskInputType {

    /** Input stream type */
    @ClassChooserInstance
    class InputStream implements TaskInputType {
        public final TypeName inputstream;

        public InputStream(TypeName inputstream) {
            this.inputstream = inputstream;
        }
    }

    /** Simple JSON type */
    @ClassChooserInstance
    class Json implements TaskInputType {
        public final TypeName value;

        public Json(TypeName value) {
            this.value = value;
        }
    }
}
