package sf.net.experimaestro.manager.js;

import com.google.common.collect.Iterables;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * Created by bpiwowar on 10/9/14.
 */
public class ConstructorFunction extends GenericFunction {
    private String className;
    ArrayList<Constructor<?>> constructors = new ArrayList<>();

    public ConstructorFunction(String className, ArrayList<Constructor<?>> constructors) {
        this.className = className;
        this.constructors = constructors;
    }

    static public class ConstructorDeclaration extends Declaration<Constructor> {
        Constructor<?> constructor;

        public ConstructorDeclaration(Constructor constructor) {
            super(constructor);
            this.constructor = constructor;
        }

        @Override
        public Object invoke(Object[] transformedArgs) throws IllegalAccessException, InvocationTargetException, InstantiationException {
            return constructor.newInstance(transformedArgs);
        }

    }


    @Override
    protected String getName() {
        return "new " + className;
    }

    @Override
    protected Iterable<ConstructorDeclaration> declarations() {
        return Iterables.transform(constructors, ConstructorDeclaration::new);
    }
}
