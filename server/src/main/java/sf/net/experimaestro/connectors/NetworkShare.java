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

import sf.net.experimaestro.fs.XPMPath;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
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

    /**
     * Find a network share for
     *
     * @param em        The entity manager
     * @param connector The single host connector
     * @param path      The network share path <code>share://host/root/path</code> path
     * @return
     */
    public static NetworkShareAccess find(EntityManager em, SingleHostConnector connector, XPMPath path) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        final CriteriaQuery<NetworkShareAccess> q = cb.createQuery(NetworkShareAccess.class);
        final Root<NetworkShareAccess> accesses = q.from(NetworkShareAccess.class);

        // Select those with our connector
        q.where(cb.equal(accesses.get(NetworkShareAccess_.connector), connector));

        // Join with network share
        Join<NetworkShareAccess, NetworkShare> share = accesses.join(NetworkShareAccess_.share);
        share.on(cb.equal(share.get(NetworkShare_.host), path.getHostName()),
                cb.equal(share.get(NetworkShare_.name), path.getShareName()));

        final TypedQuery<NetworkShareAccess> query = em.createQuery(q);
        final List<NetworkShareAccess> resultList = query.getResultList();
        assert (resultList.size() <= 1);
        if (resultList.isEmpty())
            return null;
        return resultList.get(0);
    }
}
