package de.duellplugin.models;

import java.util.*;

public class Party {

    /** Maximum party size (leader + members). */
    public static final int MAX_SIZE = 4;

    private final UUID leader;
    private final List<UUID> members;
    /** Map: invitee UUID → inviter (leader) UUID */
    private final Map<UUID, UUID> pendingInvites;

    public Party(UUID leader) {
        this.leader = leader;
        this.members = new ArrayList<>();
        this.members.add(leader);
        this.pendingInvites = new HashMap<>();
    }

    public UUID getLeader() { return leader; }

    public List<UUID> getMembers() { return Collections.unmodifiableList(members); }

    public boolean isMember(UUID uuid) { return members.contains(uuid); }

    public boolean isFull() { return members.size() >= MAX_SIZE; }

    public int size() { return members.size(); }

    public void addMember(UUID uuid) {
        if (!members.contains(uuid)) members.add(uuid);
    }

    public void removeMember(UUID uuid) { members.remove(uuid); }

    /** Invites a player (called by the leader). */
    public void invite(UUID invitee) {
        pendingInvites.put(invitee, leader);
    }

    /** Returns true if the player has a pending invite to this party. */
    public boolean hasInvite(UUID uuid) { return pendingInvites.containsKey(uuid); }

    /** Accepts the invite and adds the member. */
    public void acceptInvite(UUID uuid) {
        pendingInvites.remove(uuid);
        addMember(uuid);
    }

    /** Cancels a pending invite. */
    public void cancelInvite(UUID uuid) { pendingInvites.remove(uuid); }
}
