/**
 * Project 5
 * @YiyangZhou, zhou512, L06
 * @HongkaiHu, hu212, L11
 */

import java.io.*;
import java.util.*;
import java.net.*;

class Request {
	private String name;
	private String fromLocation;
	private String toLocation;
	private Socket socket;
	//private String iDontKnowWhatThisVariableDoes;

	public Request(String name, String fromLocation, String toLocation,
			Socket socket) {
		this.name = name;
		this.fromLocation = fromLocation;
		this.toLocation = toLocation;
		this.socket = socket;
	}

	public String getName() {
		return name;
	}

	public String getFromLocation() {
		return fromLocation;
	}

	public void setFromLocation(String from) {
		this.fromLocation = from;
	}

	public String getToLocation() {
		return toLocation;
	}

	public Socket getSocket() {
		return socket;
	}

	public String responseFormat(String toFormat) {
		String formatDone = String.format(toFormat, this.name,
				this.fromLocation, this.toLocation);
		return formatDone;
	}

}

public class SafeWalkServer extends ServerSocket implements Runnable {
	int port;
	ArrayList<Socket> socketList = new ArrayList<Socket>();
	private List<String> fromLocations = Arrays.asList("CL50", "EE", "LWSN",
			"PMU", "PUSH");
	private List<String> toLocations = Arrays.asList("CL50", "EE", "LWSN",
			"PMU", "PUSH", "*");
	ArrayList<String> validRequestFrom = new ArrayList<String>();
	ArrayList<String> validRequestTo = new ArrayList<String>();
	ArrayList<Request> holdingRequests = new ArrayList<Request>();

	/**
	 * Construct the server, and create a server socket, bound to the specified
	 * port.
	 * 
	 * @throws IOException
	 *             IO error when opening the socket.
	 */
	public SafeWalkServer(int port) throws IOException {
		super(port);
		setReuseAddress(true);
		this.port = port;
	}

	/**
	 * Construct the server, and create a server socket, bound to a port that is
	 * automatically allocated.
	 * 
	 * @throws IOException
	 *             IO error when opening the socket.
	 */
	public SafeWalkServer() throws IOException {
		super(0);
		System.out.println("Port not specified. Using free port "
				+ this.getPort() + ".");
	}

	public int getPort() {
		return this.getLocalPort();
	}

	public void run() {
		while (true) {
			try {
				Socket socket = this.accept();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));
				PrintWriter out = new PrintWriter(socket.getOutputStream(),
						true);
				String instance = reader.readLine();
				if (instance != null) {
					if (instance.charAt(0) == ':') {
						String[] commandInitializer = instance.split(",");
						if (instance.substring(1).equals("RESET")) {
							for (int i = socketList.size()-1; i >= 0; i--) {
								PrintWriter out2 = new PrintWriter(socketList
										.get(i).getOutputStream(), true);
								out2.println("ERROR: connection reset");
								socketList.get(i).close();
								socketList.remove(i);
							}
							out.println("RESPONSE: success");
							socket.close();

						} else if (instance.substring(1).equals("SHUTDOWN")) {
							for (int i = 0; i < socketList.size(); i++) {
								PrintWriter out2 = new PrintWriter(socketList
										.get(i).getOutputStream(), true);
								out2.println("ERROR: connection reset");
								socketList.get(i).close();
							}
							out.println("RESPONSE: success");
							out.close();
							reader.close();
							socket.close();
							this.close();
							// setReuseAddress(true);
							break;
						} else if (commandInitializer.length == 4 && commandInitializer[0]
								.equals(":PENDING_REQUESTS")) {
							if(commandInitializer[1].equals("#") && commandInitializer[2].equals("*") 
									&& commandInitializer[3].equals("*")){
								out.println("RESPONSE: # of pending requests = "
										+ holdingRequests.size());
								socket.close();
							}
							else if(commandInitializer[1].equals("#") && fromLocations.contains(commandInitializer[2])
									&& commandInitializer[3].equals("*")){
								out.println("RESPONSE: # of pending requests from "+ commandInitializer[2]+ " = "
									+ Collections.frequency(validRequestFrom, commandInitializer[2]));
								socket.close();
							}
							else if(commandInitializer[1].equals("#") && commandInitializer[2].equals("*")
									&& fromLocations.contains(commandInitializer[3])){
								out.println("RESPONSE: # of pending requests to "
										+ commandInitializer[3]+ " = "+ Collections.frequency(validRequestTo, commandInitializer[3]));
								socket.close();
							}
							
							else if ((commandInitializer[1].equals("*")
									&& commandInitializer[2].equals("*") && commandInitializer[3]
										.equals("*"))) {
								String toPrint = "";
								// list all pending requests.
								out.printf("[");
								for (int i = 0; i < holdingRequests.size(); i++) {
									if(i != holdingRequests.size()-1){
											toPrint = String.format(
											"[%s, %s, %s], ", holdingRequests.get(i).getName(),
											holdingRequests.get(i)
													.getFromLocation(),
											holdingRequests.get(i)
													.getToLocation());
									}
									else{
										toPrint = String.format("[%s, %s, %s]", holdingRequests.get(i).getName(),
											holdingRequests.get(i)
													.getFromLocation(),
											holdingRequests.get(i)
													.getToLocation());
									}
									out.printf(toPrint);
								}
								
								out.println("]");
								socket.close();
							}
							else{
								out.println("ERROR: invalid command");
								socket.close();
							}
						}

						else {
							out.println("ERROR: invalid command");
							socket.close();
						}
					}

					else {
						socketList.add(socket);
						Request validRequest = this.convertRequest(instance,
								socket);
						if (validRequest != null) {
							Request matchRequest = findMatch(validRequest);
							if (matchRequest != null) {
								PrintWriter print = new PrintWriter(
										validRequest.getSocket()
												.getOutputStream(), true);
								PrintWriter matchPrint = new PrintWriter(
										matchRequest.getSocket()
												.getOutputStream(), true);
								print.println("RESPONSE: "+ matchRequest.getName() + ","+ matchRequest.getFromLocation() + ","
										+ matchRequest.getToLocation());

								matchPrint.println("RESPONSE: "+ validRequest.getName() + ","+ validRequest.getFromLocation() + ","
										+ validRequest.getToLocation());
								holdingRequests.remove(matchRequest);
								validRequestFrom.remove(validRequest.getFromLocation());
								validRequestTo.remove(validRequest.getToLocation());
								validRequestFrom.remove(matchRequest.getFromLocation());
								validRequestTo.remove(matchRequest.getToLocation());
								socketList.remove(validRequest.getSocket());
								socketList.remove(matchRequest.getSocket());
								matchRequest.getSocket().close();
								validRequest.getSocket().close();
							} else {
								holdingRequests.add(validRequest);
								//out.println("woops");
							}
						} else {
							out.println("ERROR: invalid request");
						}
					}
				}

			} catch (IOException e) {
				/** dfdfdfa */
			}
		}
	}

	public static boolean isPortValid(String port) {
		int portNum;
		try {
			portNum = Integer.parseInt(port);
		} catch (NumberFormatException e) {
			return false;
		}
		if (portNum <= 1025 || portNum > 65535) {
			return false;
		}
		return true;
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			try {
				SafeWalkServer server = new SafeWalkServer();
				Thread t = new Thread(server);
				t.start();

			} catch (IOException e) { /* fdsfad */
			}
		} else {
			try {
				boolean validity = isPortValid(args[0]);
				if (validity) {
					SafeWalkServer server = new SafeWalkServer(
							Integer.parseInt(args[0]));
					Thread t = new Thread(server);
					t.start();
				} else {
					System.out.println("Invalid port");
				}
			} catch (IOException e) {
				System.out.println("Port already used");
			}
		}
	}

	public Request convertRequest(String input, Socket socket) {
		String[] tokens = input.split(",");
		if (tokens.length == 3) {
			if (fromLocations.contains(tokens[1])
					&& toLocations.contains(tokens[2])
					&& !tokens[1].equals(tokens[2])) {
				Request clientRequest = new Request(tokens[0], tokens[1],
						tokens[2], socket);
				validRequestFrom.add(tokens[1]);
				validRequestTo.add(tokens[2]);
				return clientRequest;
			}
		}
		return null;
	}

	public Request findMatch(Request clientRequest) throws IOException {
		Iterator<Request> holding = this.holdingRequests.iterator();
		while (holding.hasNext()) {
			Request potential = holding.next();
			if (clientRequest.getFromLocation().equals(
					potential.getFromLocation())) {
				if ((clientRequest.getToLocation().equals(potential.getToLocation()) && !clientRequest.getToLocation().equals("*"))
						|| ((clientRequest.getToLocation().equals("*")) && !potential.getToLocation().equals("*"))
						|| (potential.getToLocation().equals("*") && !clientRequest.getToLocation().equals("*"))) {
					return potential;
				}	
			}
		}
		return null;
	}
}
