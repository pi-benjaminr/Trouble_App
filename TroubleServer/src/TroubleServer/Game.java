package TroubleServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game {
	private List<String> playerNames = new ArrayList<String>();
	private List<Integer> playerIDs = new ArrayList<Integer>();
	private List<List<String>> mailboxes = new ArrayList<List<String>>();
	private int[][] pieces = new int[4][4];
	private boolean isStarted = false;
	private int currentTurn;
	private int die;
	
	
	
	public Game(String p1Name, int p1ID) {
		playerNames.add(p1Name);
		playerIDs.add(p1ID);
		mailboxes.add(new ArrayList<String>());
	}
	
	public String join(String pName, int pID) {
		if (playerNames.size() != 4) {
			playerNames.add(pName);
			playerIDs.add(pID);
			mailboxes.add(new ArrayList<String>());
			String res = "SUCCESS";
			for (int i = 0; i < size(); i++) {
				res += " " + playerNames.get(i);
			}
			return res;
		}
		return "FAIL";
	}
	
	public boolean leave(String pName, int pID) {
		if (playerIDs.contains(pID)) {
			int i = playerIDs.indexOf(pID);
			playerNames.remove(i);
			playerIDs.remove(i);
			mailboxes.remove(i);
			return true;
		}
		return false;
	}
	
	public int size() {
		return playerIDs.size();
	}
	
	public void launch() {
		int num = size();
		for (int i =0; i < num; i++) {
			for (int j = 0; j < 4; j++) {
				pieces[i][j] = -1;
			}
		}
		isStarted = true;
		die = -1;
	}
	
	public void send(String pName, String msg) {
		for (List<String> mailbox : mailboxes) {
			mailbox.add(pName+": "+msg);
		}
	}
	
	public List<String> poll(int pID) {
		int i = playerIDs.indexOf(pID);
		List<String> msgs = new ArrayList<String>(mailboxes.get(i));
		for (int j = 0; j < mailboxes.get(i).size(); j++) {
			mailboxes.set(i, new ArrayList<String>());
		}
		return msgs;
	}
	
	public String toString() {
		String res = "";
		res += size();
		if (isStarted) {
			res += " " + currentTurn;
			res += " " + die;
			for (int i = 0; i < size(); i++) {
				for (int j = 0; j < 4; j++) {
					res += " " + pieces[i][j];
				}
			}
		} else {
			res += " -1";
		}
		return res;
	}
	
	public int roll() {
		Random r = new Random();
		int num = r.nextInt(5) + 1;
		die = num;
		return num;
	}
	
	public void move(int pNum, int piece, int dest) {
		pieces[pNum][piece] = dest;
	}
	
	public int nextTurn() {
		int num = currentTurn;
		num = (num+1)%size();
		currentTurn = num;
		die = -1;
		return num;
		
	}

}
