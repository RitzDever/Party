
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Party {
	private static final long INVITE_EXPIRY = 1000 * 60;
	private String owner;
	private Set<String> allMembers = new HashSet<String>();
	private Map<String, Long> invites = new HashMap<String, Long>();

	public Party(String string) {
		allMembers.add(string);
		owner = string;
	}

	public Set<String> getMembers() {
		return allMembers;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public boolean addInvite(String name) {
		boolean contained = invites.containsKey(name);
		invites.put(name, System.currentTimeMillis());
		return !contained;
	}

	public boolean removeInvite(String name) {
		return invites.remove(name) != null;
	}

	public Set<String> getInvites() {
		// Clean invite set
		Set<Entry<String, Long>> entries = invites.entrySet();
		for (Entry<String, Long> entry : entries) {
			if (entry.getValue() + INVITE_EXPIRY < System.currentTimeMillis()) {
				invites.remove(entry.getKey());
			}
		}
		return invites.keySet();
	}
}
