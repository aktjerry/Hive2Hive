package org.hive2hive.core.model;

import java.security.PublicKey;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Holds meta data of a file in the DHT
 * 
 * @author Nico
 * 
 */
public class MetaFile extends MetaDocument {

	private static final long serialVersionUID = 1L;
	private final List<FileVersion> versions;

	public MetaFile(PublicKey id, String fileName, List<FileVersion> versions) {
		super(id, fileName);
		this.versions = versions;
	}

	public List<FileVersion> getVersions() {
		return versions;
	}

	public int getTotalSize() {
		if (versions == null) {
			return 0;
		} else {
			int sum = 0;
			for (FileVersion version : versions) {
				sum += version.getSize();
			}
			return sum;
		}
	}

	public FileVersion getNewestVersion() {
		if (versions == null || versions.isEmpty()) {
			return null;
		}

		Collections.sort(versions, new Comparator<FileVersion>() {
			@Override
			public int compare(FileVersion o1, FileVersion o2) {
				return new Integer(o1.getIndex()).compareTo(o2.getIndex());
			}
		});

		return versions.get(versions.size() - 1);
	}

	public FileVersion getVersionByIndex(int index) {
		if (versions == null || versions.isEmpty()) {
			return null;
		}

		for (FileVersion version : versions) {
			if (version.getIndex() == index)
				return version;
		}

		return null;
	}
}
