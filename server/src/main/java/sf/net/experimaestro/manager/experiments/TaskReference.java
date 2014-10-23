package sf.net.experimaestro.manager.experiments;

import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.scheduler.Resource;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A task contains resources and is linked to other dependent tasks
 */
@Entity
public class TaskReference {
    @Id
    long id = -1;

    /**
     * The parents
     */
    @OneToMany
    private final Collection<TaskReference> parents = new ArrayList<>();

    /**
     * The children
     */
    @OneToMany
    private final Collection<TaskReference> children = new ArrayList<>();

    /**
     * The ID
     */
    QName taskId;

    /**
     * The experiment ID
     */
    @ManyToOne(fetch = FetchType.EAGER)
    Experiment experiment;

    /**
     * The associated resources
     */
    @OneToMany
    Collection<Resource> resources = new ArrayList<>();

    public TaskReference() {
    }

    public TaskReference(Experiment experiment, QName taskId) {
        this.experiment = experiment;
        this.taskId = taskId;
    }

    /**
     * Add a parent
     */
    public void addParent(TaskReference parent) {
        assert parent.id != -1;
        assert this.id != -1;

        parent.children.add(this);
        parents.add(parent);
    }

    /**
     * Associate a resource to this task reference
     *
     * @param resource The resource to add
     */
    public void add(Resource resource) {
        resources.add(resource);
    }
}
