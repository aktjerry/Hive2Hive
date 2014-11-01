package org.hive2hive.core.processes.files.add;

import java.nio.file.Path;
import java.security.PublicKey;

import net.tomp2p.peers.PeerAddress;

import org.hive2hive.core.H2HSession;
import org.hive2hive.core.events.framework.interfaces.IFileEventGenerator;
import org.hive2hive.core.events.implementations.FileAddEvent;
import org.hive2hive.core.exceptions.GetFailedException;
import org.hive2hive.core.exceptions.NoSessionException;
import org.hive2hive.core.file.FileUtil;
import org.hive2hive.core.model.Index;
import org.hive2hive.core.model.versioned.UserProfile;
import org.hive2hive.core.network.data.UserProfileManager;
import org.hive2hive.core.network.messages.direct.BaseDirectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddNotificationMessage extends BaseDirectMessage implements IFileEventGenerator{

	private static final long serialVersionUID = -695268345354561544L;

	private static final Logger logger = LoggerFactory.getLogger(AddNotificationMessage.class);

	private final PublicKey fileKey;

	public AddNotificationMessage(PeerAddress targetAddress, PublicKey fileKey) {
		super(targetAddress);
		this.fileKey = fileKey;
	}

	@Override
	public void run() {
		logger.debug("Add file notification message received.");

		H2HSession session;
		try {
			session = networkManager.getSession();
		} catch (NoSessionException e) {
			logger.error("No user seems to be logged in.");
			return;
		}

		UserProfileManager profileManager = session.getProfileManager();

		UserProfile userProfile;
		try {
			userProfile = profileManager.getUserProfile(getMessageID(), false);
		} catch (GetFailedException e) {
			logger.error("Couldn't load user profile.", e);
			return;
		}

		Index addedFile = userProfile.getFileById(fileKey);
		if (addedFile == null) {
			logger.error("Got notified about a file we don't know.");
			return;
		}

		// trigger event
		Path addedFilePath = FileUtil.getPath(session.getRoot(), addedFile);
		getEventBus().publish(new FileAddEvent(addedFilePath, addedFile.isFile()));
	}

}
