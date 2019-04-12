package cz.metacentrum.perun.core.impl;

import cz.metacentrum.perun.core.api.Group;
import cz.metacentrum.perun.core.api.PerunBeanProcessingPool;
import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SynchronizationPool {
	private final PerunBeanProcessingPool<Group> poolOfGroupsToBeSynchronized = new PerunBeanProcessingPool<>();
	private final PerunBeanProcessingPool<Group> poolOfGroupsStructuresToBeSynchronized = new PerunBeanProcessingPool<>();
	//Access lock to create concurrent access by any operation to any pool of this class.
	private final Lock poolAccessLock = new ReentrantLock(true);

	public boolean putGroupStructureToPoolOfWaitingGroupsStructures(Group group, boolean asFirst) throws InternalErrorException {
		return poolOfGroupsStructuresToBeSynchronized.putJobIfAbsent(group, asFirst);
	}

	public boolean putGroupToPoolOfWaitingGroups(PerunSessionImpl sess, Group group, boolean asFirst) throws InternalErrorException {
		try {
			poolAccessLock.lock();
			for (Group groupStructure : poolOfGroupsStructuresToBeSynchronized.getWaitingJobs()) {
				List<Group> allSubGroups = sess.getPerunBl().getGroupsManagerBl().getAllSubGroups(sess, groupStructure);
				if (!allSubGroups.contains(group)) {
					return poolOfGroupsToBeSynchronized.putJobIfAbsent(group, asFirst);
				}
			}
		} finally {
			poolAccessLock.unlock();
		}
		return false;
	}

	public Group takeGroup(PerunSessionImpl sess) throws InterruptedException, InternalErrorException {
		try {
			poolAccessLock.lock();
			for (Group group : poolOfGroupsToBeSynchronized.getRunningJobs()) {
				boolean allowed = true;
				for (Group groupStructure : poolOfGroupsStructuresToBeSynchronized.getWaitingJobs()) {
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
			throw new InterruptedException("Cannot synchronize because I said so");
		} finally {
			poolAccessLock.unlock();
		}
	}

	public Group takeGroupStructure(PerunSessionImpl sess) throws InterruptedException, InternalErrorException {
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
			throw new InterruptedException("Cannot synchronize because I said so");
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
