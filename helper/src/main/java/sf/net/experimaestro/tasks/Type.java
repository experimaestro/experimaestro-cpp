package sf.net.experimaestro.tasks;

/**
 * Created by bpiwowar on 7/10/14.
 */
public @interface Type {
    String type();

    boolean resource() default false;
}
