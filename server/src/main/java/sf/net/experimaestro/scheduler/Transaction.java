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
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A JPA transaction that can be used in try-resource blocks
 */
public class Transaction implements AutoCloseable {
    final static private Logger LOGGER = Logger.getLogger();
    /**
     * The underlying transaction
     */
    private final EntityTransaction transaction;

    /** Old transaction */
    private final Transaction old;

    /** Current status of the transaction */
    Status status;

    /** The attached entity manager */
    private EntityManager entityManager;

    /**
     * Do we own the entity manager ?
     */
    private boolean ownEntityManager;

    /** Methods to evaluate after commit  */
    IdentityHashSet<PostCommitListener> listeners = null;

    static private ThreadLocal<Transaction> currentTransaction = new ThreadLocal<>();

    public Transaction(EntityManager entityManager, boolean ownEntityManager) {
        this.entityManager = entityManager;
        this.ownEntityManager = ownEntityManager;

        transaction = entityManager.getTransaction();
        transaction.begin();
        old = currentTransaction.get();
        currentTransaction.set(this);
        status = Status.BEGIN;
        LOGGER.info("Transaction %s begins", System.identityHashCode(this));
    }

    static Transaction current() {
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
        // Rollback if an error occurred
        if (status == Status.BEGIN) {
            LOGGER.info("Transaction %s rollback", System.identityHashCode(this));
            transaction.rollback();
        }

        // Close the entity manager if necessary
        if (ownEntityManager) {
            entityManager.close();
        }

        currentTransaction.set(old);
    }

    public void commit() {
        if (status == Status.BEGIN) {
            LOGGER.info("Transaction %s commits", System.identityHashCode(this));
            transaction.commit();
            status = Status.COMMIT;
            if (listeners != null) {
                listeners.forEach(f -> f.postCommit(this));
            }
        }
    }

    public void boundary() {
        LOGGER.info("Transaction %s boundary (commit and begin)", System.identityHashCode(this));
        try {
            transaction.commit();
        } catch(RollbackException e) {
            status = Status.ROLLBACK;
            throw e;
        }
        transaction.begin();
        status = Status.BEGIN;
    }

    public void registerPostCommit(PostCommitListener f) {
        if (listeners == null)
            listeners = new IdentityHashSet<>();
        listeners.add(f);
    }

    static public enum Status {
        BEGIN,
        COMMIT,
        ROLLBACK
    }

}
