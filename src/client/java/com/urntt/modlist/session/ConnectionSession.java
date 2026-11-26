package com.urntt.modlist.session;

import com.urntt.modlist.config.ProfileSettings;
import com.urntt.modlist.registry.RegistrySyncFilter;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.network.Connection;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * The state of one game connection to a server: the profile it uses, what the server has announced, and what the
 * client changed. Packets are handled on the network thread and the client thread, so the state is thread-safe.
 */
public final class ConnectionSession {
	private final Connection connection;
	private final @Nullable String address;
	private final ProfileSettings profile;
	private final boolean serverProfile;

	private final Set<Identifier> serverChannels = ConcurrentHashMap.newKeySet();
	private final Map<Integer, Identifier> loginQueries = new ConcurrentHashMap<>();
	private final Set<String> serverRegistryNamespaces = ConcurrentHashMap.newKeySet();
	private volatile @Nullable String serverBrand;

	private volatile @Nullable String realBrand;
	private volatile @Nullable String reportedBrand;
	private final Set<Identifier> hiddenChannels = ConcurrentHashMap.newKeySet();
	private final Set<Identifier> fakedChannels = ConcurrentHashMap.newKeySet();
	private final AtomicInteger droppedPayloads = new AtomicInteger();
	private final AtomicInteger refusedLoginQueries = new AtomicInteger();
	private volatile RegistrySyncFilter.@Nullable Result ignoredRegistryContent;
	private final AtomicBoolean noticesShown = new AtomicBoolean();

	ConnectionSession(final Connection connection, final @Nullable String address, final ProfileSettings profile,
			final boolean serverProfile) {
		this.connection = connection;
		this.address = address;
		this.profile = profile;
		this.serverProfile = serverProfile;
	}

	public Connection connection() {
		return this.connection;
	}

	/** The address the player connected to, or {@code null} if it is unknown. */
	public @Nullable String address() {
		return this.address;
	}

	public ProfileSettings profile() {
		return this.profile;
	}

	/** Whether a server profile matched the address, rather than the default profile. */
	public boolean usesServerProfile() {
		return this.serverProfile;
	}

	/** The report plan of the profile as it is now; the profile may be edited while connected. */
	public ReportPlan plan() {
		return ReportPlan.of(this.profile);
	}

	/** The channels the server has announced, including the channels of its login queries. */
	public Set<Identifier> serverChannels() {
		return Collections.unmodifiableSet(this.serverChannels);
	}

	void addServerChannels(final Iterable<Identifier> channels) {
		channels.forEach(this.serverChannels::add);
	}

	void removeServerChannels(final Iterable<Identifier> channels) {
		channels.forEach(this.serverChannels::remove);
	}

	void recordLoginQuery(final int transactionId, final Identifier channel) {
		this.loginQueries.put(transactionId, channel);
		this.serverChannels.add(channel);
	}

	@Nullable Identifier loginQueryChannel(final int transactionId) {
		return this.loginQueries.get(transactionId);
	}

	public @Nullable String serverBrand() {
		return this.serverBrand;
	}

	void setServerBrand(final String serverBrand) {
		this.serverBrand = serverBrand;
	}

	/** The namespaces of the entries in the server's registry sync, if it sent one. */
	public Set<String> serverRegistryNamespaces() {
		return Collections.unmodifiableSet(this.serverRegistryNamespaces);
	}

	public void addServerRegistryNamespaces(final Iterable<String> namespaces) {
		namespaces.forEach(this.serverRegistryNamespaces::add);
	}

	/** The brand the client really has, once it has been sent. */
	public @Nullable String realBrand() {
		return this.realBrand;
	}

	/** The brand the server received, once it has been sent. */
	public @Nullable String reportedBrand() {
		return this.reportedBrand;
	}

	void recordBrand(final String realBrand, final String reportedBrand) {
		this.realBrand = realBrand;
		this.reportedBrand = reportedBrand;
	}

	/** The real channels left out of the channel registrations. */
	public Set<Identifier> hiddenChannels() {
		return Collections.unmodifiableSet(this.hiddenChannels);
	}

	/** The fake channels added to the channel registrations. */
	public Set<Identifier> fakedChannels() {
		return Collections.unmodifiableSet(this.fakedChannels);
	}

	void recordRewrite(final ChannelRewriter.Result result) {
		this.hiddenChannels.addAll(result.removed());
		this.fakedChannels.addAll(result.added());
	}

	public int droppedPayloads() {
		return this.droppedPayloads.get();
	}

	void recordDroppedPayload() {
		this.droppedPayloads.incrementAndGet();
	}

	public int refusedLoginQueries() {
		return this.refusedLoginQueries.get();
	}

	void recordRefusedLoginQuery() {
		this.refusedLoginQueries.incrementAndGet();
	}

	/** What was dropped from the server's registry sync, or {@code null} if nothing was. */
	public RegistrySyncFilter.@Nullable Result ignoredRegistryContent() {
		return this.ignoredRegistryContent;
	}

	public void setIgnoredRegistryContent(final RegistrySyncFilter.Result ignoredRegistryContent) {
		this.ignoredRegistryContent = ignoredRegistryContent;
	}

	/** Returns {@code true} the first time it is called, so join notices are shown once per connection. */
	public boolean claimJoinNotices() {
		return this.noticesShown.compareAndSet(false, true);
	}
}
