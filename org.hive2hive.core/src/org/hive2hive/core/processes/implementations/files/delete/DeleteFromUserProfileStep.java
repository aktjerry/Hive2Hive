package org.hive2hive.core.processes.implementations.files.delete;

import org.hive2hive.core.exceptions.GetFailedException;
import org.hive2hive.core.exceptions.InvalidProcessStateException;
import org.hive2hive.core.exceptions.NoPeerConnectionException;
import org.hive2hive.core.exceptions.NoSessionException;
import org.hive2hive.core.exceptions.PutFailedException;
import org.hive2hive.core.log.H2HLogger;
import org.hive2hive.core.log.H2HLoggerFactory;
import org.hive2hive.core.model.IndexNode;
import org.hive2hive.core.model.UserProfile;
import org.hive2hive.core.network.NetworkManager;
import org.hive2hive.core.processes.framework.RollbackReason;
import org.hive2hive.core.processes.implementations.common.base.BaseGetProcessStep;
import org.hive2hive.core.processes.implementations.context.DeleteFileProcessContext;

public class DeleteFromUserProfileStep extends BaseGetProcessStep {

	private static final H2HLogger logger = H2HLoggerFactory.getLogger(DeleteFromUserProfileStep.class);

	private final DeleteFileProcessContext context;
	private final NetworkManager networkManager;

	private IndexNode fileNode;
	private IndexNode parentNode;

	public DeleteFromUserProfileStep(DeleteFileProcessContext context, NetworkManager networkManager)
			throws NoPeerConnectionException {
		super(networkManager.getDataManager());
		this.context = context;
		this.networkManager = networkManager;
	}

	@Override
	protected void doExecute() throws InvalidProcessStateException {
		if (context.consumeMetaDocument() == null) {
			cancel(new RollbackReason(this, "No meta document given."));
			return;
		}

		// get user profile
		UserProfile profile = null;
		try {
			profile = networkManager.getSession().getProfileManager().getUserProfile(getID(), true);
		} catch (GetFailedException | NoSessionException e) {
			cancel(new RollbackReason(this, "Could not get user profile."));
			return;
		}

		fileNode = profile.getFileById(context.consumeMetaDocument().getId());
		context.setDeletedNode(fileNode);

		// check preconditions
		if (!fileNode.getChildren().isEmpty()) {
			cancel(new RollbackReason(this, "Cannot delete a directory that is not empty."));
			return;
		}

		parentNode = fileNode.getParent();
		parentNode.removeChild(fileNode);
		context.setParentNode(parentNode);
		try {
			networkManager.getSession().getProfileManager().readyToPut(profile, getID());
		} catch (PutFailedException | NoSessionException e) {
			cancel(new RollbackReason(this, "Could not put user profile."));
		}
	}

	@Override
	protected void doRollback(RollbackReason reason) throws InvalidProcessStateException {

		if (fileNode != null && parentNode != null) {

			// get user profile
			UserProfile profile = null;
			try {
				profile = networkManager.getSession().getProfileManager().getUserProfile(getID(), true);
			} catch (GetFailedException | NoSessionException e) {
				logger.warn("Rollback failed: " + e.getMessage());
				return;
			}

			// TODO this is buggy! rather use list to check for containment instead of above if-statement
			// re-add file to user profile
			IndexNode parent = profile.getFileById(parentNode.getFileKey());
			parent.addChild(fileNode);
			fileNode.setParent(parent);

			try {
				networkManager.getSession().getProfileManager().readyToPut(profile, getID());
			} catch (PutFailedException | NoSessionException e) {
				logger.warn("Rollback failed: " + e.getMessage());
				return;
			}
		}
	}

}
