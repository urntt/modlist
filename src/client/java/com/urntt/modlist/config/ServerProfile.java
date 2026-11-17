package com.urntt.modlist.config;

import com.urntt.modlist.ServerAddresses;
import org.jspecify.annotations.Nullable;

/**
 * A profile that applies to the servers whose address matches {@link #address()}.
 */
public final class ServerProfile {
	private String address = "";
	private Profile profile = new Profile();

	private transient @Nullable Runnable onChange;

	ServerProfile() {
	}

	ServerProfile(final String address, final Profile profile) {
		this.address = address;
		this.profile = profile;
	}

	public String address() {
		return this.address;
	}

	/** Sets the address; invalid addresses are ignored, so the profile never matches by accident. */
	public void setAddress(final String address) {
		String trimmed = address.trim();
		if (ServerAddresses.isValid(trimmed) && !trimmed.equals(this.address)) {
			this.address = trimmed;
			if (this.onChange != null) {
				this.onChange.run();
			}
		}
	}

	public Profile profile() {
		return this.profile;
	}

	/** Whether this profile applies to the server the player connected to at {@code connectedAddress}. */
	public boolean matches(final String connectedAddress) {
		return ServerAddresses.matches(this.address, connectedAddress);
	}

	void attach(final Runnable onChange) {
		this.onChange = onChange;
		this.profile.attach(onChange);
	}

	/** Whether the entry is usable after loading; entries with a missing or invalid address are dropped. */
	boolean normalize() {
		if (this.address == null || !ServerAddresses.isValid(this.address.trim())) {
			return false;
		}
		this.address = this.address.trim();
		if (this.profile == null) {
			this.profile = new Profile();
		}
		this.profile.normalize();
		return true;
	}
}
