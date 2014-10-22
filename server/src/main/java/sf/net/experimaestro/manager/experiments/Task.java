package sf.net.experimaestro.manager.experiments;

import com.sleepycat.persist.model.*;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.scheduler.Resource;

import java.util.ArrayList;

/**
 * A task contains resources and is linked to other dependent tasks
 */
@Entity
public class Task {

    /** The parents */
    private final ArrayList<Task> parents = new ArrayList<>();

    /** The children */
    private final ArrayList<Task> children = new ArrayList<>();

    /** The associated resources */
    @SecondaryKey(name = "resources", relate = Relationship.ONE_TO_MANY, relatedEntity = Resource.class, onRelatedEntityDelete = DeleteAction.ABORT)
    ArrayList<Long> resources = new ArrayList<>();

    /** The ID */
    @PrimaryKey
    final QName identifier;

    public Task(QName identifier) {
        this.identifier = identifier;
    }

    /** Add a parent */
    public void addParent(Task parent) {
        parent.children.add(this);
        parents.add(parent);
    }
}
