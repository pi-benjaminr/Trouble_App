package TroubleServer;



import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.lang.Math;

public class WorkerThread extends Thread {

	private DatagramPacket rxPacket;
	private DatagramSocket socket;

	public WorkerThread(DatagramPacket packet, DatagramSocket socket) {
		this.rxPacket = packet;
		this.socket = socket;
	}

	@Override
	public void run() {
		// convert the rxPacket's payload to a string
		String payload = new String(rxPacket.getData(), 0, rxPacket.getLength())
				.trim();
		System.out.print("RECEIVED: " + payload + "\n");

		// dispatch request handler functions based on the payload's prefix

		if (payload.startsWith("REGISTER")) {
			onRegisterRequested(payload);
			return;
		}
		
		if (payload.startsWith("CREATE")) {
			onCreateRequested(payload);
			return;
		}

		if (payload.startsWith("JOIN")) {
			onJoinRequested(payload);
			return;
		}
		
		if (payload.startsWith("LEAVE")) {
			onLeaveRequested(payload);
			return;
		}
		
		if (payload.startsWith("LAUNCH")) {
			onLaunchRequested(payload);
			return;
		}
		
		if (payload.startsWith("SEND")) {
			onSendRequested(payload);
			return;
		}
		
		if (payload.startsWith("POLL")) {
			onPollRequested(payload);
			return;
		}
		
		if (payload.startsWith("GETGAMESTATE")) {
			onGetGamestateRequested(payload);
			return;
		}
		
		if (payload.startsWith("ROLL")) {
			onRollRequested(payload);
			return;
		}
		
		if (payload.startsWith("MOVE")) {
			onMoveRequested(payload);
			return;
		}
		
		if(payload.startsWith("NEXT")) {
			onNextRequested(payload);
			return;
		}
		
		if(payload.startsWith("ENDGAME")) {
			onEndgameRequested(payload);
			return;
		}
		
		if (payload.startsWith("SHUTDOWN")) {
			onShutdownRequested(payload);
			return;
		}


		// if we got here, it must have been a bad request, so we tell the
		// client about it
		onBadRequest(payload);
	}

	// send a string, wrapped in a UDP packet, to the specified remote endpoint
	public void send(String payload, InetAddress address, int port)
			throws IOException {
		DatagramPacket txPacket = new DatagramPacket(payload.getBytes(),
				payload.length(), address, port);
		this.socket.send(txPacket);
	}

	/**
	 * REGISTER
	 * format:
	 * REGISTER username
	 * returns the user's new ID.
	 * 
	 * @param payload
	 */
	private void onRegisterRequested(String payload) {

		int number;
		synchronized (Server.nextId) {
			number = Server.nextId++;
		}
		
		Scanner scan = new Scanner(payload);
		scan.next();
		String username = scan.next();
		Server.userNames.put(number, username);

		try {
			send("" + number + "\n", this.rxPacket.getAddress(),
					this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * CREATE
	 * format:
	 * CREATE userid
	 * returns gamenum
	 * @param payload
	 */
	private void onCreateRequested(String payload) {
		Scanner scan = new Scanner(payload);
		scan.next();
		int id = scan.nextInt();
		String name = Server.userNames.get(id);
		int gamenum = Server.nextGame++;
		Server.games.put(gamenum, new Game(name, id));
		Server.inGame.put(id,gamenum);
		
		
		try {
			send("" + gamenum + "\n", this.rxPacket.getAddress(),
					this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * JOIN
	 * format:
	 * JOIN userid gamenum
	 * returns gamenum or -1
	 * @param payload
	 */
	private void onJoinRequested(String payload) {
		Scanner scan = new Scanner(payload);
		scan.next();
		int id = scan.nextInt();
		String name = Server.userNames.get(id);
		int gamenum = scan.nextInt();
		if (Server.games.containsKey(gamenum)) {
			String res = Server.games.get(gamenum).join(name, id);
			
			try {
				send("" + res + "\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (res.startsWith("S"))
				Server.inGame.put(id, gamenum);
		} else {
			try {
				send("" + "FAIL" + "\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * LEAVE
	 * format:
	 * LEAVE userid
	 * return 1 if successfull
	 * @param payload
	 */
	private void onLeaveRequested(String payload) {
		Scanner scan = new Scanner(payload);
		scan.next();
		int id = scan.nextInt();
		String name = Server.userNames.get(id);
		int gamenum = Server.inGame.get(id);
		Server.games.get(gamenum).leave(name, id);
		Server.inGame.remove(id);
		try {
			send("" + 1 + "\n", this.rxPacket.getAddress(),
					this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * LAUNCH
	 * format:
	 * LAUNCH userid
	 * return 1 if successful
	 * @param payload
	 */
	private void onLaunchRequested(String payload) {
		Scanner scan = new Scanner(payload);
		scan.next();
		int id = scan.nextInt();
		int gamenum = Server.inGame.get(id);
		Server.games.get(gamenum).launch();
		try {
			send("" + 1 + "\n", this.rxPacket.getAddress(),
					this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * SEND
	 * format:
	 * SEND userid message message message
	 * @param payload
	 */
	private void onSendRequested(String payload) {
		Scanner scan = new Scanner(payload);
		scan.next();
		int id = scan.nextInt();
		String message= scan.nextLine();
		String name = Server.userNames.get(id);
		int gamenum = Server.inGame.get(id);
		Server.games.get(gamenum).send(name, message);
	}
	
	/**
	 * POLL
	 * format:
	 * POLL userid
	 * returns each message in it's own packet, ending with an END message
	 * @param payload
	 */
	private void onPollRequested(String payload) {
		Scanner scan = new Scanner(payload);
		scan.next();
		int id = scan.nextInt();
		Integer gamenum = Server.inGame.get(id);
		if (gamenum == null) {
			try {
				send("" + "END" + "\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		List<String> msgs = Server.games.get(gamenum).poll(id);
		for (String msg : msgs) {
			try {
				send("" +"-" + msg + "\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			send("" + "END" + "\n", this.rxPacket.getAddress(),
					this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * GETGAMESTATE
	 * format:
	 * GETGAMESTATE userid
	 * @param payload
	 */
	private void onGetGamestateRequested(String payload) {
		Scanner scan = new Scanner(payload);
		scan.next();
		int id = scan.nextInt();
		if (Server.inGame.containsKey(id) && Server.games.containsKey(Server.inGame.get(id))) {
			int gamenum = Server.inGame.get(id);
			String res = Server.games.get(gamenum).toString();
			try {
				send("" + res + "\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				send("" + -1 + "\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * ROLL
	 * format:
	 * ROLL userid
	 * @param payload
	 */
	private void onRollRequested(String payload) {
		Scanner scan = new Scanner(payload);
		scan.next();
		int id = scan.nextInt();
		int gamenum = Server.inGame.get(id);
		int res = Server.games.get(gamenum).roll();
		try {
			send("" + res + "\n", this.rxPacket.getAddress(),
					this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * MOVE
	 * format:
	 * MOVE userid piece_owner piece_num destination
	 * @param payload
	 */
	private void onMoveRequested(String payload) {
		Scanner scan = new Scanner(payload);
		scan.next();
		int id = scan.nextInt();
		int owner = scan.nextInt();
		int piece = scan.nextInt();
		int dest = scan.nextInt();
		int gamenum = Server.inGame.get(id);
		Server.games.get(gamenum).move(owner, piece, dest);
		
	}
	
	/**
	 * NEXT
	 * format:
	 * NEXT userid
	 * @param payload
	 */
	private void onNextRequested(String payload) {
		Scanner scan = new Scanner(payload);
		scan.next();
		int id = scan.nextInt();
		int gamenum = Server.inGame.get(id);
		Server.games.get(gamenum).nextTurn();
	}
	
	/**
	 * ENDGAME
	 * format:
	 * ENDGAME userid
	 * @param payload
	 */
	private void onEndgameRequested(String payload) {
		Scanner scan = new Scanner(payload);
		scan.next();
		int id = scan.nextInt();
		if (Server.inGame.containsKey(id)) {
			int gamenum = Server.inGame.get(id);
			Server.inGame.remove(id);
			if (Server.games.containsKey(gamenum)) {
				Server.games.remove(gamenum);
			}
		}
	}
	
	
	

	/**
	 * SHUTDOWN format: SHUTDOWN closes the socket if the request comes from
	 * localhost. First waits for all other threads to finish.
	 * 
	 * @param payload
	 */
	private void onShutdownRequested(String payload) {
		// the string is the address that I found packets sent via netcat to be
		// coming from.
		if (this.rxPacket.getAddress().toString().equals("/0:0:0:0:0:0:0:1"))
			;
		{
			for (WorkerThread t : Server.threads) {
				try {
					if (t != Thread.currentThread())
						t.join();
				} catch (InterruptedException e) {
				}
			}
			socket.close();
		}
	}

	private void onBadRequest(String payload) {
		try {
			send("BAD REQUEST\n", this.rxPacket.getAddress(),
					this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
