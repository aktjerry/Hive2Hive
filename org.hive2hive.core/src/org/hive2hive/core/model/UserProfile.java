package org.hive2hive.core.model;

import java.io.File;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PublicKey;

import org.hive2hive.core.H2HConstants;
import org.hive2hive.core.TimeToLiveStore;
import org.hive2hive.core.file.FileManager;
import org.hive2hive.core.network.data.NetworkContent;
import org.hive2hive.core.security.EncryptionUtil;

/**
 * File which contains all keys and meta information about the files of the owner.
 * 
 * @author Nico, Seppi
 * 
 */
public class UserProfile extends NetworkContent {

	private static final long serialVersionUID = 1L;

	private final String userId;
	private final KeyPair encryptionKeys;
	private final IndexNode root;

	public UserProfile(String userId) {
		this(userId, EncryptionUtil.generateRSAKeyPair(H2HConstants.KEYLENGTH_USER_KEYS), EncryptionUtil
				.generateProtectionKey());
	}

	/**
	 * Constructor for the user profile.
	 * 
	 * @param userId
	 *            the user id of the owner of the user profile
	 * @param encryptionKeys
	 *            the encryption keys for encrypting the user profile
	 * @param protectionKeys
	 *            the default keys for content protection
	 */
	public UserProfile(String userId, KeyPair encryptionKeys, KeyPair protectionKeys) {
		if (userId == null)
			throw new IllegalArgumentException("User id can't be null.");
		if (encryptionKeys == null)
			throw new IllegalArgumentException("Encryption keys can't be null.");
		if (protectionKeys == null)
			throw new IllegalArgumentException("Protection keys can't be null.");
		this.userId = userId;
		this.encryptionKeys = encryptionKeys;
		root = new IndexNode(encryptionKeys, protectionKeys);
	}

	public String getUserId() {
		return userId;
	}

	public KeyPair getEncryptionKeys() {
		return encryptionKeys;
	}

	public KeyPair getProtectionKeys() {
		return root.getProtectionKeys();
	}

	public IndexNode getRoot() {
		return root;
	}

	@Override
	public int getTimeToLive() {
		return TimeToLiveStore.getInstance().getUserProfile();
	}

	public IndexNode getFileById(PublicKey fileId) {
		return findById(root, fileId);
	}

	private IndexNode findById(IndexNode current, PublicKey fileId) {
		if (current.getFileKey().equals(fileId)) {
			return current;
		}

		IndexNode found = null;
		for (IndexNode child : current.getChildren()) {
			found = findById(child, fileId);
			if (found != null) {
				return found;
			}
		}
		return found;
	}

	public IndexNode getFileByPath(File file, FileManager fileManager) {
		Path relativePath = fileManager.getRoot().relativize(file.toPath());
		return getFileByPath(relativePath);
	}

	public IndexNode getFileByPath(File file, File root) {
		Path relativePath = root.toPath().relativize(file.toPath());
		return getFileByPath(relativePath);
	}

	public IndexNode getFileByPath(Path relativePath) {
		String[] split = relativePath.toString().split(FileManager.getFileSep());
		IndexNode current = root;
		for (int i = 0; i < split.length; i++) {
			if (split[i].isEmpty()) {
				continue;
			}
			IndexNode child = current.getChildByName(split[i]);
			if (child == null) {
				return null;
			} else {
				current = child;
			}
		}

		return current;
	}
}
