package net.bpiwowar.xpm.manager.tasks;

import net.bpiwowar.xpm.manager.QName;

/**
 * Value type for task inputs
 */
@ClassChooser(inner = true, mode = ClassChooserMode.FIELDS)
public interface TaskInputType {

    /** Input stream type */
    @ClassChooserInstance
    class InputStream implements TaskInputType {
        public final QName inputstream;

        public InputStream(QName inputstream) {
            this.inputstream = inputstream;
        }
    }

    /** Simple JSON type */
    @ClassChooserInstance
    class Json implements TaskInputType {
        public final QName value;

        public Json(QName value) {
            this.value = value;
        }
    }
}
