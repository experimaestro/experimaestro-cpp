package sf.net.experimaestro.scheduler;

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

import sf.net.experimaestro.utils.IdentityHashSet;
import sf.net.experimaestro.utils.log.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.RollbackException;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A JPA transaction that can be used in try-resource blocks
 */
final public class Transaction implements AutoCloseable {
    final static private Logger LOGGER = Logger.getLogger();
    /**
     * The underlying transaction
     */
    private final EntityTransaction transaction;

    /**
     * Old transaction
     */
    private final Transaction old;

    /**
     * Current status of the transaction
     */
    Status status;

    /**
     * The attached entity manager
     */
    private EntityManager entityManager;

    /**
     * Do we own the entity manager ?
     */
    private boolean ownEntityManager;

    /**
     * Methods to evaluate after commit
     */
    IdentityHashSet<PostCommitListener> listeners = null;

    /**
     * List of locks on entities
     */
    private HashMap<Object, EntityLock> locks = new HashMap<>();

    static private ThreadLocal<Transaction> currentTransaction = new ThreadLocal<>();

    public Transaction(EntityManager entityManager, boolean ownEntityManager) {
        this.entityManager = entityManager;
        this.ownEntityManager = ownEntityManager;

        transaction = entityManager.getTransaction();
        transaction.begin();
        old = currentTransaction.get();
        currentTransaction.set(this);
        status = Status.BEGIN;
        LOGGER.debug("Transaction %s begins", System.identityHashCode(this));
    }

    static public Transaction current() {
        return currentTransaction.get();
    }

    public static Transaction create() {
        EntityManager em = Scheduler.manager();
        return new Transaction(em, true);
    }

    public static Transaction of(EntityManager em) {
        return new Transaction(em, false);
    }

    /**
     * Run a transaction and commits
     */
    public static void run(Consumer<EntityManager> f) {
        try (Transaction transaction = create()) {
            f.accept(transaction.em());
            transaction.commit();
        }
    }

    /**
     * Run a transaction and commits
     */
    public static <T> T evaluate(Function<EntityManager, T> f) {
        try (Transaction transaction = create()) {
            T t = f.apply(transaction.em());
            transaction.commit();
            return t;
        }
    }

    /**
     * Run a transaction and commits
     */
    public static <T> T evaluate(BiFunction<EntityManager, Transaction, T> f) {
        try (Transaction transaction = create()) {
            T t = f.apply(transaction.em(), transaction);
            transaction.commit();
            return t;
        }
    }


    /**
     * Run a transaction and commits
     */
    public static void run(BiConsumer<EntityManager, Transaction> f) {
        try (Transaction transaction = create()) {
            f.accept(transaction.em(), transaction);
            transaction.commit();
        }
    }


    public EntityManager em() {
        return entityManager;
    }

    @Override
    public void close() throws RollbackException {
        try {
            // Rollback if an error occurred
            if (status == Status.BEGIN) {
                LOGGER.debug("Transaction %s rollback", System.identityHashCode(this));
                transaction.rollback();
            }

            // Close the entity manager if necessary
            if (ownEntityManager) {
                entityManager.close();
            }

            currentTransaction.set(old);
        } finally {
            for (EntityLock lock : locks.values()) {
                lock.close();
            }
            locks.clear();
        }
    }

    public void commit() {
        try {
            if (status == Status.BEGIN) {
                LOGGER.debug("Transaction %s commits", System.identityHashCode(this));
                transaction.commit();
                status = Status.COMMIT;
                if (listeners != null) {
                    listeners.forEach(f -> f.postCommit(this));
                }
            }
        } catch (RollbackException e) {
            status = Status.ROLLBACK;
            throw e;
        } finally {
            clearLocks();
        }
    }


    /**
     * Commits & prepare a new transaction
     */
    public void boundary() {
        boundary(false);
    }

    /**
     * Commits and prepapres a new transaction
     *
     * @param keepLocks If true, all the locks are kept
     */
    public void boundary(boolean keepLocks) {
        try {
            LOGGER.debug("Transaction %s boundary (commit and begin)", System.identityHashCode(this));
            commit();
            transaction.begin();
            status = Status.BEGIN;
        } finally {
            if (!keepLocks) {
                clearLocks();
            }
        }
    }

    public void registerPostCommit(PostCommitListener f) {
        if (listeners == null)
            listeners = new IdentityHashSet<>();
        listeners.add(f);
    }

    public void clearLocks() {
        locks.values().forEach(sf.net.experimaestro.scheduler.EntityLock::close);
        locks.clear();
    }

    static public class SharedLongReference {
        SharedLongLocks locks;
        long id;

        public SharedLongReference(SharedLongLocks locks, long id) {
            this.locks = locks;
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || !(o instanceof SharedLongReference))
                return false;

            SharedLongReference that = (SharedLongReference) o;

            if (id != that.id) return false;
            return locks.equals(that.locks);

        }

        @Override
        public int hashCode() {
            int result = locks.hashCode();
            result = 31 * result + (int) (id ^ (id >>> 32));
            return result;
        }
    }

    /**
     * Locks an entity (should implements hash code and equals)
     *
     * @param locks
     * @param id
     * @param exclusive
     * @param timeout
     * @return
     */
    public EntityLock lock(SharedLongLocks locks, long id, boolean exclusive, long timeout) {
        final SharedLongReference key = new SharedLongReference(locks, id);
        EntityLock lock = this.locks.get(key);
        if (lock != null && lock.isClosed()) {
            lock = null;
        }

        if (lock == null) {
            lock = locks.lock(id, exclusive, timeout);
            if (lock == null) {
                return null;
            }
            this.locks.put(key, lock);
        } else {
            if (exclusive) {
                lock.makeExclusive(timeout);
            }
        }

        return lock;
    }

    public enum Status {
        BEGIN,
        COMMIT,
        ROLLBACK
    }

}
