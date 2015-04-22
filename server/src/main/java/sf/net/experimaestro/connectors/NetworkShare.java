package sf.net.experimaestro.connectors;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
 *
 * experimaestro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * experimaestro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 */

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Defines relationships between single host connectors and network shares
 */
@Entity
@Table(
        name = "shares",
        uniqueConstraints = @UniqueConstraint(name = "shares", columnNames = {"host", "name"})
)
public class NetworkShare {
    @Id
    @GeneratedValue
    private long key;

    @Version
    private long version;

    @OneToMany(fetch = FetchType.LAZY)
    private Collection<NetworkShareAccess> access;

    /**
     * The name of the logical host
     */
    String host;

    /**
     * The share name on that host
     */
    String name;

    public NetworkShare() {
    }

    public NetworkShare(String host, String share) {
        this.host = host;
        this.name = share;
        this.access = new ArrayList<>();
    }

    public Collection<NetworkShareAccess> getAccess() {
        return access;
    }

    public void add(NetworkShareAccess networkShareAccess) {
        access.add(networkShareAccess);
    }

    /**
     * Find a connector given its string ID
     *
     * @param em   The entity manager
     * @param host The host name
     * @param name The share name
     * @return The connector in database or null if none exist
     */
    public static NetworkShare find(EntityManager em, String host, String name) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        final CriteriaQuery<NetworkShare> q = cb.createQuery(NetworkShare.class);
        final Root<NetworkShare> shares = q.from(NetworkShare.class);
        q.select(shares)
                .where(shares.get(NetworkShare_.host).in(host))
                .where(shares.get(NetworkShare_.name).in(name));

        final List<NetworkShare> resultList = em.createQuery(q).getResultList();
        assert (resultList.size() <= 1);
        if (resultList.isEmpty())
            return null;
        return resultList.get(0);
    }
}
