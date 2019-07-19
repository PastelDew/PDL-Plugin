package pdlab.minecraft.teamservice.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class Team {
	private static final String RANK_LEADER = "ÆÀÀå";
	private static final String RANK_NEWBIE = "ÆÀ¿ø";
	private UUID playerLeader = null;
	private HashMap<UUID, String> players;
	private ArrayList<String> rank;
	
	public Team() {
		this(null);
	}
	
	public Team(UUID leader) {
		playerLeader = leader;
		players = new HashMap<>();
		rank = new ArrayList<>();
		rank.add(RANK_LEADER);
		rank.add(RANK_NEWBIE);
	}
	
	public void addPlayer(UUID uuid) {
		if(playerLeader == null)
			playerLeader = uuid;
		players.put(uuid, RANK_LEADER);
	}
	
	public void removePlayer(UUID uuid) {
		players.remove(uuid);
		if(players.size() == 0)
			playerLeader = null;
	}
	
	public UUID getLeader() {
		return playerLeader;
	}
	
	public void setLeader(UUID uuid) {
		playerLeader = uuid;
	}
	
	public void addRank(String rankName) {
		
	}
}
