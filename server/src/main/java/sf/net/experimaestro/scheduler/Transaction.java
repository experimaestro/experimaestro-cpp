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

import sf.net.experimaestro.utils.log.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.RollbackException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
* A JPA transaction that can be used in try-resource blocks
*/
public class Transaction implements AutoCloseable {
    final static private Logger LOGGER = Logger.getLogger();

    private EntityManager entityManager;

    /** Do we own the entity manager ? */
    private boolean ownEntityManager;

    /** The underlying transaction */
    private final EntityTransaction transaction;

    private int count = 1;

    static public enum Status {
        BEGIN,
        COMMIT,
        ROLLBACK
    };

    Status status;

    public Transaction(EntityManager entityManager, boolean ownEntityManager) {
        this.entityManager = entityManager;
        transaction  = entityManager.getTransaction();
        transaction.begin();
        this.ownEntityManager = ownEntityManager;
    }

    public EntityManager em() {
        return entityManager;
    }

    @Override
    public void close() throws RollbackException {
        // If not commited, we throw an exception
        if (--count == 0) {
            // Rollback if an error occured
            if (status == Status.BEGIN) {
                LOGGER.info("Transaction %s rollback", System.identityHashCode(this));
                transaction.rollback();
            }

            // Close the entity manager if necessary
            if (ownEntityManager) {
                entityManager.close();
            }
        }
    }


    public void commit() {
        LOGGER.info("Transaction %s commits", System.identityHashCode(this));
        transaction.commit();
        status = Status.COMMIT;
    }

    public void boundary() {
        LOGGER.info("Transaction %s boundary (commit and begin)", System.identityHashCode(this));
        transaction.commit();
        transaction.begin();
        status = Status.BEGIN;
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
        try(Transaction transaction = create()) {
            f.accept(transaction.em());
            transaction.commit();
        }
    }

    /**
     * Run a transaction and commits
     */
     public static <T> T evaluate(Function<EntityManager, T> f) {
        try(Transaction transaction = create()) {
            T t = f.apply(transaction.em());
            transaction.commit();
            return t;
        }
    }

    /**
     * Run a transaction and commits
     */
    public static void run(BiConsumer<EntityManager, Transaction> f) {
        try(Transaction transaction = create()) {
            f.accept(transaction.em(), transaction);
            transaction.commit();
        }
    }

    /**
     * Run a transaction and commits
     */
    public static <T> T evaluate(BiFunction<EntityManager, Transaction, T> f) {
        try(Transaction transaction = create()) {
            T t = f.apply(transaction.em(), transaction);
            transaction.commit();
            return t;
        }
    }

}
