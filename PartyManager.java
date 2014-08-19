import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PartyManager implements CommandExecutor {
	private static final String[] HELP_MESSAGE = {
			ChatColor.YELLOW + "/<cmd> help" + ChatColor.WHITE + " - "
					+ ChatColor.AQUA + "Prints this help message.",
			ChatColor.YELLOW + "/<cmd> leave" + ChatColor.WHITE + " - "
					+ ChatColor.AQUA + "Leaves the current party.",
			ChatColor.YELLOW + "/<cmd> list" + ChatColor.WHITE + " - "
					+ ChatColor.AQUA + "Lists the members of your party.",
			ChatColor.YELLOW + "/<cmd> promote playername" + ChatColor.WHITE
					+ " - " + ChatColor.AQUA
					+ "Makes playername the leader of your party.",
			ChatColor.YELLOW + "/<cmd> remove playername" + ChatColor.WHITE
					+ " - " + ChatColor.AQUA
					+ "Removes playername from your party.",
			ChatColor.YELLOW + "/<cmd> disband" + ChatColor.WHITE + " - "
					+ ChatColor.AQUA + "Disbands your party.",
			ChatColor.YELLOW + "/<cmd> invite playername" + ChatColor.WHITE
					+ " - " + ChatColor.AQUA
					+ "Invites playername to your party.",
			ChatColor.YELLOW + "/<cmd> accept playername" + ChatColor.WHITE
					+ " - " + ChatColor.AQUA
					+ "Accepts an invite from playername." };
	public static final String PARTY_PREFIX = ChatColor.WHITE + "["
			+ ChatColor.YELLOW + "Party" + ChatColor.WHITE + "] "
			+ ChatColor.AQUA;
	public static final String PARTY_PREFIX_ERROR = PARTY_PREFIX
			+ ChatColor.RED;

	private final MainPlugin plugin;

	private Map<String, Party> partyMapping = new HashMap<String, Party>();

	public PartyManager(final MainPlugin mainPlugin) {
		this.plugin = mainPlugin;
	}

	public Party getPartyOf(String name) {
		return partyMapping.get(name.toLowerCase());
	}

	@SuppressWarnings("unused")
	private void cleanList(Iterator<String> members) {
		while (members.hasNext()) {
			Player pl = plugin.getServer().getPlayer(members.next());
			if (pl == null || !pl.isValid() || !pl.isOnline()) {
				members.remove();
			}
		}
	}

	private void sendMessage(String player, String message) {
		Player p = plugin.getServer().getPlayer(player);
		if (p != null && p.isValid() && p.isOnline()) {
			p.sendMessage(TextUtil.wrapText(message));
		}
	}

	private void playerLeaveParty(String s) {
		Party p = getPartyOf(s);
		if (p != null) {
			p.getMembers().remove(s);
			partyMapping.remove(s.toLowerCase());
			plugin.updateParty(s, null);
			if (p.getOwner().equalsIgnoreCase(s)) {
				disbandParty(p);
			} else {
				for (String name : p.getMembers()) {
					sendMessage(name, PARTY_PREFIX + s
							+ " has left your party.");
					plugin.updateParty(name, p);
				}
			}
		}
	}

	private void playerJoinParty(String name, Party p) {
		p.getMembers().add(name);
		partyMapping.put(name.toLowerCase(), p);
		sendMessage(name, PARTY_PREFIX + "You have joined " + p.getOwner()
				+ "'s party.");
		plugin.updateParty(name, p);
		for (String mem : p.getMembers()) {
			if (!mem.equalsIgnoreCase(name)) {
				sendMessage(mem, PARTY_PREFIX + name
						+ " has joined your party.");
			}
			plugin.updateParty(mem, p);
		}
	}

	private void disbandParty(Party p) {
		for (String s : p.getMembers()) {
			plugin.updateParty(s, null);
			partyMapping.remove(s.toLowerCase());
			sendMessage(s, PARTY_PREFIX + "Your party has been disbanded.");
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if (args.length > 0) {
			Party p = getPartyOf(sender.getName());
			if (args[0].equalsIgnoreCase("list")) {
				if (p != null) {
					sender.sendMessage(TextUtil.wrapText(PARTY_PREFIX
							+ p.getOwner() + "'s Party"));
					for (String s : p.getMembers()) {
						if (!s.equalsIgnoreCase(p.getOwner())) {
							sender.sendMessage(PARTY_PREFIX + s);
						}
					}
				} else {
					sender.sendMessage(PARTY_PREFIX_ERROR
							+ "You aren't in a party.");
				}
				return true;
			} else if (args[0].equalsIgnoreCase("leave")) {
				if (p != null) {
					playerLeaveParty(sender.getName());
				} else {
					sender.sendMessage(PARTY_PREFIX_ERROR
							+ "You aren't in a party.");
				}
				return true;
			} else if (args[0].equalsIgnoreCase("disband")) {
				if (p != null) {
					if (p.getOwner().equalsIgnoreCase(sender.getName())) {
						disbandParty(p);
					} else {
						sender.sendMessage(PARTY_PREFIX_ERROR + p.getOwner()
								+ " is the owner of your party.");
					}
				} else {
					sender.sendMessage(PARTY_PREFIX_ERROR
							+ "You aren't in a party.");
				}
				return true;
			} else if (args[0].equalsIgnoreCase("accept")) {
				if (p == null) {
					if (args.length >= 2) {
						// Look for an invite
						Party invited = getPartyOf(args[1]);
						if (invited != null
								&& invited.getInvites().contains(
										sender.getName())) {
							playerJoinParty(sender.getName(), invited);
							invited.getInvites().remove(sender.getName());
						} else {
							sender.sendMessage(TextUtil
									.wrapText(PARTY_PREFIX_ERROR
											+ "You haven't been invited to "
											+ args[1] + "'s party"));
						}
					} else {
						// Print out invites
						Set<String> invites = new HashSet<String>();
						for (Party party : partyMapping.values()) {
							if (party.getInvites().contains(sender.getName())) {
								invites.add(party.getOwner());
							}
						}
						if (invites.size() == 0) {
							sender.sendMessage(PARTY_PREFIX_ERROR
									+ "You haven't been invited to any parties.");
						} else {
							sender.sendMessage(TextUtil
									.wrapText(PARTY_PREFIX
											+ "You have been invited to the following parties."));
							for (String s : invites) {
								sender.sendMessage(PARTY_PREFIX + " - " + s
										+ "'s party");
							}
						}
					}
				} else {
					sender.sendMessage(PARTY_PREFIX_ERROR
							+ "You are already in a party.");
				}
				return true;
			} else if (args[0].equalsIgnoreCase("invite")) {
				if (p == null) {
					partyMapping.put(sender.getName(),
							new Party(sender.getName()));
					p = getPartyOf(sender.getName());
					sender.sendMessage(PARTY_PREFIX
							+ "You have formed a new party.");
					plugin.updateParty(sender.getName(), p);
				}
				if (p != null
						&& !p.getOwner().equalsIgnoreCase(sender.getName())) {
					sender.sendMessage(PARTY_PREFIX_ERROR
							+ "You aren't the owner of your current party.");
				} else if (p != null) {
					if (args.length >= 2) {
						// Invite a new player
						Player invited = plugin.getServer().getPlayer(args[1]);
						if (invited != null && invited.isValid()
								&& invited.isOnline()) {
							if (p.addInvite(invited.getName())) {
								sender.sendMessage(TextUtil
										.wrapText(PARTY_PREFIX
												+ "You have invited "
												+ invited.getName()
												+ " to your party."));
								invited.sendMessage(TextUtil
										.wrapText(PARTY_PREFIX
												+ "You have been invited to "
												+ sender.getName()
												+ "'s party.\nUse the command \"/"
												+ label
												+ " accept "
												+ sender.getName()
												+ "\" to accept.  You have 60 seconds."));
							} else {
								sender.sendMessage(PARTY_PREFIX
										+ "You have already invited this player.");
							}
						} else {
							sender.sendMessage(PARTY_PREFIX_ERROR + args[1]
									+ " isn't a valid player or isn't online.");
						}
					} else {
						// List invites
						sender.sendMessage(PARTY_PREFIX
								+ "You've invited the following players.");
						for (String s : p.getInvites()) {
							sender.sendMessage(PARTY_PREFIX + " - " + s);
						}
					}
				}
				return true;
			} else if (args[0].equalsIgnoreCase("remove")) {
				if (p == null) {
					sender.sendMessage(PARTY_PREFIX_ERROR
							+ "You aren't in a party.");
				} else if (p != null
						&& !p.getOwner().equalsIgnoreCase(sender.getName())) {
					sender.sendMessage(PARTY_PREFIX_ERROR
							+ "You aren't the owner of your current party.");
				} else if (p != null) {
					if (args.length >= 2) {
						// Promote a player
						if (args[1].equalsIgnoreCase(sender.getName())) {
							sender.sendMessage(PARTY_PREFIX_ERROR
									+ "You can't remove yourself from your party.");
						} else if (p.getMembers().remove(args[1])) {
							sender.sendMessage(PARTY_PREFIX
									+ "You have removed " + args[1]
									+ " from your party.");
							plugin.updateParty(args[1], null);
							for (String s : p.getMembers()) {
								plugin.updateParty(s, p);
							}
						} else {
							sender.sendMessage(PARTY_PREFIX_ERROR + args[1]
									+ " isn't in your party.");
						}
					} else {
						sender.sendMessage(PARTY_PREFIX_ERROR
								+ "You need to specify a player to promote.");
					}
				}
				return true;
			} else if (args[0].equalsIgnoreCase("promote")) {
				if (p == null) {
					sender.sendMessage(PARTY_PREFIX_ERROR
							+ "You aren't in a party.");
				} else if (p != null
						&& !p.getOwner().equalsIgnoreCase(sender.getName())) {
					sender.sendMessage(PARTY_PREFIX_ERROR
							+ "You aren't the owner of your current party.");
				} else if (p != null) {
					if (args.length >= 2) {
						// Promote a player
						if (p.getMembers().contains(args[1])) {
							Player invited = plugin.getServer().getPlayer(
									args[1]);
							if (invited != null && invited.isValid()
									&& invited.isOnline()) {
								promoteToLeader(p, invited.getName());
							} else {
								sender.sendMessage(PARTY_PREFIX_ERROR
										+ args[1]
										+ " isn't a valid player or isn't online.");
							}
						} else {
							sender.sendMessage(PARTY_PREFIX_ERROR + args[1]
									+ " isn't in your party.");
						}
					} else {
						sender.sendMessage(PARTY_PREFIX_ERROR
								+ "You need to specify a player to promote.");
					}
				}
			} else if (args[0].equalsIgnoreCase("disband")) {
				if (p == null) {
					sender.sendMessage(PARTY_PREFIX_ERROR
							+ "You aren't in a party.");
				} else if (p != null
						&& !p.getOwner().equalsIgnoreCase(sender.getName())) {
					sender.sendMessage(PARTY_PREFIX_ERROR
							+ "You aren't the owner of your current party.");
				} else if (p != null) {
					disbandParty(p);
				}
				return true;
			}
		}
		List<String> lines = new ArrayList<String>();
		for (int i = 0; i < HELP_MESSAGE.length; i++) {
			lines.addAll(Arrays.asList(TextUtil.wrapText(HELP_MESSAGE[i]
					.replace("<cmd>", label))));
		}
		sender.sendMessage(lines.toArray(new String[0]));
		return true;
	}

	private void promoteToLeader(Party p, String name) {
		String owner = p.getOwner();
		p.setOwner(name);
		p.getMembers().add(name);
		p.getMembers().add(owner);
		sendMessage(name, PARTY_PREFIX
				+ "You have been promoted to the owner of " + owner
				+ "'s party.");
		sendMessage(owner, "You have promoted " + name
				+ " to the owner of your party.");
		for (String s : p.getMembers()) {
			if (!s.equalsIgnoreCase(name) && !s.equalsIgnoreCase(owner)) {
				sendMessage(
						s,
						PARTY_PREFIX
								+ name
								+ " has been promoted to the leader of your current party.");
			}
			plugin.updateParty(s, p);
		}
	}
}