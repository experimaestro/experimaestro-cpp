package sf.net.experimaestro.utils;

import sf.net.experimaestro.manager.Mapping;

import java.util.Comparator;

/**
* @author B. Piwowarski <benjamin@bpiwowar.net>
* @date 8/2/13
*/
public class ClassNameComparator implements Comparator<Mapping.Set> {
    @Override
    public int compare(Mapping.Set o1, Mapping.Set o2) {
        if (o1 == null)
            if (o2 == null)
                return 0;
            else
                return -1;
        if (o2 == null)
            return 1;

        return o1.getClass().getCanonicalName().compareTo(o2.getClass().getCanonicalName());
    }
}
