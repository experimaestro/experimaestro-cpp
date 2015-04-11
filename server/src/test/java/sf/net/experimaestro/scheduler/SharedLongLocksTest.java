package sf.net.experimaestro.scheduler;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;

import static sf.net.experimaestro.utils.Lazy.format;


/**
 * Test for SharedLongLocks
 */
public class SharedLongLocksTest {
    @Test(timeOut = 1000)
    public void shared_exclusive() throws InterruptedException {
        final IntList list = IntLists.synchronize(new IntArrayList());
        final SharedLongLocks locks = new  SharedLongLocks();

        final int lockThread1Ready = IntLocks.newLockID();
        final int lockThread1Locked = IntLocks.newLockID();
        final int lockThread2Ready = IntLocks.newLockID();
        final int LockThread3Ready = IntLocks.newLockID();

        CountDownLatch latch = new CountDownLatch(2);

        new Thread( () -> {
            IntLocks.removeLock(lockThread1Ready);
            try(EntityLock ignored = locks.sharedLock(1)) {
                IntLocks.waitLockID(LockThread3Ready);
                IntLocks.removeLock(lockThread1Locked);
                pause(500);
                list.add(1);
            }
            latch.countDown();
        }).start();

        new Thread( () -> {
            IntLocks.removeLock(lockThread2Ready);
            IntLocks.waitLockID(lockThread1Locked);
            try(EntityLock ignored = locks.sharedLock(1)) {
                IntLocks.waitLockID(LockThread3Ready);
                pause(500);
                list.add(1);
            }
            latch.countDown();
        }).start();


        new Thread(() -> {
            IntLocks.waitLockID(lockThread1Ready);
            IntLocks.waitLockID(lockThread2Ready);
            IntLocks.removeLock(LockThread3Ready);
            try (EntityLock ignored = locks.exclusiveLock(1)) {
                list.add(2);
            }
            latch.countDown();
        }).start();

        latch.await();
        assertOrdered(list);
    }


    @Test(timeOut = 1000, description = "Test two exclusive locks")
    public void exclusive() throws InterruptedException {
        final IntList list = IntLists.synchronize(new IntArrayList());
        final SharedLongLocks locks = new  SharedLongLocks();

        final int lockThread1Ready = IntLocks.newLockID();
        final int lockThread2Ready = IntLocks.newLockID();

        CountDownLatch latch = new CountDownLatch(2);

        new Thread(() -> {
            IntLocks.removeLock(lockThread1Ready);
            IntLocks.waitLockID(lockThread2Ready);
            try(EntityLock ignored = locks.exclusiveLock(1)) {
                list.add(2);
            }
            latch.countDown();
        }).start();

        new Thread( () -> {
            IntLocks.waitLockID(lockThread1Ready);
            try(EntityLock ignored = locks.exclusiveLock(1)) {
                IntLocks.removeLock(lockThread2Ready);
                pause(500);
                list.add(1);
            }
            latch.countDown();
        }).start();

        latch.await();
        assertOrdered(list);
    }


    private void pause(int timeout) {
        synchronized (this) {
            try {
                this.wait(timeout);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private void assertOrdered(IntList list) {
        for(int i = 1; i < list.size(); i++) {
            if (list.get(i - 1) > list.get(i)) {
                throw new AssertionError(format("List %s is not ordered", list));
            }
        }
    }


}
