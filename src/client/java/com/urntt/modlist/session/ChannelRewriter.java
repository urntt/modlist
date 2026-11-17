package com.urntt.modlist.session;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;

/**
 * Rewrites the channel list of a channel registration the client is about to send.
 */
public final class ChannelRewriter {
	private ChannelRewriter() {
	}

	/**
	 * Removes the channels of hidden namespaces from {@code channels} and appends the plan's fake channels.
	 *
	 * @param channels the channels the client really registers
	 * @param serverChannels the channels the server has announced so far
	 */
	public static Result rewrite(final Collection<Identifier> channels, final ReportPlan plan,
			final Collection<Identifier> serverChannels) {
		Set<Identifier> result = new LinkedHashSet<>();
		Set<Identifier> removed = new LinkedHashSet<>();
		for (Identifier channel : channels) {
			if (plan.isHidden(channel.getNamespace(), serverChannels)) {
				removed.add(channel);
			} else {
				result.add(channel);
			}
		}

		Set<Identifier> added = new LinkedHashSet<>();
		for (Identifier fake : plan.fakeChannels(serverChannels)) {
			if (result.add(fake)) {
				added.add(fake);
			}
		}
		return new Result(List.copyOf(result), Set.copyOf(removed), Set.copyOf(added));
	}

	/**
	 * @param channels the channels to register; when empty, nothing should be sent, as Fabric API itself never sends
	 *        an empty registration
	 * @param removed the real channels left out
	 * @param added the fake channels added
	 */
	public record Result(List<Identifier> channels, Set<Identifier> removed, Set<Identifier> added) {
	}
}
