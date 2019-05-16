package cz.metacentrum.perun.core.impl;

import cz.metacentrum.perun.core.api.Group;
import cz.metacentrum.perun.core.api.PerunBeanProcessingPool;
import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;
import cz.metacentrum.perun.core.blImpl.GroupsManagerBlImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SynchronizationPool {
	private final PerunBeanProcessingPool<Group> poolOfGroupsToBeSynchronized = new PerunBeanProcessingPool<>();
	private final PerunBeanProcessingPool<Group> poolOfGroupsStructuresToBeSynchronized = new PerunBeanProcessingPool<>();
	//Access lock to create concurrent access by any operation to any pool of this class.
	private final Lock poolAccessLock = new ReentrantLock(true);
	//Semaphore which takes care about emptiness of list of waiting jobs (threads will wait for another job)
	//Counter in this semaphore counts number of waiting jobs in the pool (0 means no jobs are waiting to be processed)
	private final Semaphore notEmptyGroupsPoolSemaphore = new Semaphore(0, true);
	//Semaphore which takes care about emptiness of list of waiting jobs (threads will wait for another job)
	//Counter in this semaphore counts number of waiting jobs in the pool (0 means no jobs are waiting to be processed)
	private final Semaphore notEmptyGroupsStructuresPoolSemaphore = new Semaphore(0, true);

	public boolean putGroupStructureToPoolOfWaitingGroupsStructures(Group group, boolean asFirst) throws InternalErrorException {
		try {
			poolAccessLock.lock();
			if (poolOfGroupsStructuresToBeSynchronized.putJobIfAbsent(group, asFirst)) {
				notEmptyGroupsStructuresPoolSemaphore.release();
				return true;
			}
			return false;
		} finally {
			poolAccessLock.unlock();
		}
	}

	public boolean putGroupToPoolOfWaitingGroups(Group group, boolean asFirst) throws InternalErrorException {
		try {
			poolAccessLock.lock();
			if (poolOfGroupsToBeSynchronized.putJobIfAbsent(group, asFirst)) {
				notEmptyGroupsPoolSemaphore.release();
				return true;
			}
			return false;
		} finally {
			poolAccessLock.unlock();
		}

	}

	public Group takeGroup(PerunSessionImpl sess) throws InterruptedException, InternalErrorException {
		while(true) {
			//I can take only if there is not empty list of waiting jobs
			notEmptyGroupsPoolSemaphore.acquire();

			try {
				poolAccessLock.lock();
				for (Group group : poolOfGroupsToBeSynchronized.getWaitingJobs()) {
					boolean allowed = true;
					List<Group> groupStructureJobs = poolOfGroupsStructuresToBeSynchronized.getWaitingJobs();
					groupStructureJobs.addAll(poolOfGroupsStructuresToBeSynchronized.getRunningJobs());
					for (Group groupStructure : groupStructureJobs) {
						List<Group> allSubGroups = sess.getPerunBl().getGroupsManagerBl().getAllSubGroups(sess, groupStructure);
						if (allSubGroups.contains(group)) {
							allowed = false;
						}
					}
					if (allowed) {
						poolOfGroupsToBeSynchronized.putJobIfAbsent(group, true);
						return poolOfGroupsToBeSynchronized.takeJob();
					}
				}
				notEmptyGroupsPoolSemaphore.release();
			} finally {
				poolAccessLock.unlock();
			}
			Thread.sleep(10000);
		}
	}

	public Group takeGroupStructure(PerunSessionImpl sess) throws InterruptedException, InternalErrorException {
		while(true) {
			//I can take only if there is not empty list of waiting jobs
			notEmptyGroupsStructuresPoolSemaphore.acquire();

			try{
				poolAccessLock.lock();
				for (Group groupStructure : poolOfGroupsStructuresToBeSynchronized.getWaitingJobs()) {
					boolean allowed = true;
					List<Group> allSubGroups = sess.getPerunBl().getGroupsManagerBl().getAllSubGroups(sess, groupStructure);
					for (Group runningGroup : poolOfGroupsToBeSynchronized.getRunningJobs()) {
						if (allSubGroups.contains(runningGroup)) {
							allowed = false;
						}
					}
					if (allowed) {
						poolOfGroupsStructuresToBeSynchronized.putJobIfAbsent(groupStructure, true);
						return poolOfGroupsStructuresToBeSynchronized.takeJob();
					}
				}
				notEmptyGroupsStructuresPoolSemaphore.release();
			} finally {
				poolAccessLock.unlock();
			}
			Thread.sleep(10000);
		}
	}

	public boolean removeGroupStructure(Group group) {
		try {
			poolAccessLock.lock();
			return poolOfGroupsStructuresToBeSynchronized.removeJob(group);
		} finally {
			poolAccessLock.unlock();
		}
	}

	public boolean removeGroup(Group group) {
		try {
			poolAccessLock.lock();
			return poolOfGroupsToBeSynchronized.removeJob(group);
		} finally {
			poolAccessLock.unlock();
		}
	}

	public PerunBeanProcessingPool<Group> asPoolOfGroupsToBeSynchronized() {
		return poolOfGroupsToBeSynchronized;
	}

	public PerunBeanProcessingPool<Group> asPoolOfGroupsStructuresToBeSynchronized() {
		return poolOfGroupsStructuresToBeSynchronized;
	}
}
