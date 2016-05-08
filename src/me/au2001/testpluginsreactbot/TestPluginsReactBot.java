package me.au2001.testpluginsreactbot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.JOptionPane;

import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.mc.protocol.data.game.MessageType;
import org.spacehq.mc.protocol.data.message.Message;
import org.spacehq.mc.protocol.packet.ingame.client.ClientChatPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerChatPacket;
import org.spacehq.packetlib.Client;
import org.spacehq.packetlib.event.session.ConnectedEvent;
import org.spacehq.packetlib.event.session.DisconnectedEvent;
import org.spacehq.packetlib.event.session.DisconnectingEvent;
import org.spacehq.packetlib.event.session.PacketReceivedEvent;
import org.spacehq.packetlib.event.session.PacketSentEvent;
import org.spacehq.packetlib.event.session.SessionListener;
import org.spacehq.packetlib.tcp.TcpSessionFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TestPluginsReactBot {

	private static String USER, PASS, HOST, FILE = "words.txt";
	private static int PORT = 25565;
	private static PrintWriter out;
	private static Client client;
	
	private static ArrayList<String> words = new ArrayList<String>();

	public static void main (String[] args) {
		if (args.length > 0) {
			Scanner in = new Scanner(System.in);
			while (USER == null || USER.isEmpty()) {
				System.out.print("Please enter your Minecraft username/email: ");
				USER = in.nextLine();
			}
			while (PASS == null || PASS.isEmpty()) {
				System.out.print("Please enter your Minecraft password: ");
				PASS = in.nextLine();
			}
			while (HOST == null || HOST.isEmpty()) {
				System.out.print("Please enter the IP address of the target server: ");
				HOST = in.nextLine();
			}
			System.out.print("Please enter the port of the target server (enter for default): ");
			String sprt = in.nextLine();
			try {
				if (sprt != null) PORT = Integer.parseInt(sprt);
				else System.out.println("No server port was specified (hit enter), using " + PORT + ".");
			} catch (NumberFormatException e) {
				System.out.println("The specified port isn't a valid number, using " + PORT + ".");
			}
			System.out.print("Please enter the path to the words file (enter for default): ");
			String sfle = in.nextLine();
			if (sfle != null && !sfle.isEmpty()) FILE = sfle;
			else System.out.println("The specified file isn't valid (empty), using " + FILE + ".");
			in.close();
		} else {
			USER = JOptionPane.showInputDialog(null, "Please enter your Minecraft username/email:", "TPReactionFarm", JOptionPane.QUESTION_MESSAGE);
			if (USER == null || USER.isEmpty()) {
				System.out.println("No Minecraft username/email was specified, quitting.");
				return;
			}
			PASS = JOptionPane.showInputDialog(null, "Please enter your Minecraft password:", "TPReactionFarm", JOptionPane.QUESTION_MESSAGE);
			if (PASS == null || PASS.isEmpty()) {
				System.out.println("No Minecraft password was specified, quitting.");
				return;
			}
			HOST = JOptionPane.showInputDialog(null, "Please enter the IP address of the target server:", "TPReactionFarm", JOptionPane.QUESTION_MESSAGE).substring(0);
			if (HOST == null || HOST.isEmpty()) {
				System.out.println("No server IP address was specified, quitting.");
				return;
			}
			try {
				PORT = Integer.parseInt(JOptionPane.showInputDialog(null, "Please enter the port of the target server (cancel for default):", "TPReactionFarm", JOptionPane.QUESTION_MESSAGE));
			} catch (NullPointerException e) {
				System.out.println("No server port was specified (clicked cancel button), quitting.");
				return;
			} catch (NumberFormatException e) {
				System.out.println("The specified port isn't a valid number, using " + PORT + ".");
			}
			String sfle = JOptionPane.showInputDialog(null, "Please enter the path to the words file (enter for default):", "TPReactionFarm", JOptionPane.QUESTION_MESSAGE);
			if (sfle == null) {
				System.out.println("No words file was specified (clicked cancel button), quitting.");
				return;
			} else if (!sfle.isEmpty()) FILE = sfle;
			else System.out.println("The specified file isn't valid (empty), using " + FILE + ".");
			
			String hdps = "";
			while (hdps.length() < PASS.length()) hdps += "*";
			
			String message = "Do you want to connect with the following informations?\n";
			message += "\nUsername/Email: " + USER;
			message += "\nPassword: " + hdps;
			message += "\nTarget Server: " + HOST + ":" + PORT;
			message += "\nWords File: " + FILE;
			if (JOptionPane.showConfirmDialog(null, message, "TPReactionFarm", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) return;
		}
		
		try {
			if (!new File(FILE).createNewFile()) {
				BufferedReader br = new BufferedReader(new FileReader(FILE));
			    for (String line; (line = br.readLine()) != null;) words.add(line);
			    br.close();
			    if (!words.isEmpty())
			    	System.out.println("Loaded " + words.size() + " words into memory.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(FILE, true)));
			for (String word : words) out.println(word);
			final MinecraftProtocol protocol = new MinecraftProtocol(USER, PASS, false, Proxy.NO_PROXY);
			client = new Client(HOST, PORT, protocol, new TcpSessionFactory(Proxy.NO_PROXY));
			client.getSession().addListener(new SessionListener() {
				public void packetReceived (PacketReceivedEvent event) {
					if (event.getPacket() instanceof ServerChatPacket) {
						if (((ServerChatPacket) event.getPacket()).getType().equals(MessageType.NOTIFICATION)) return;
						Message message = ((ServerChatPacket) event.getPacket()).getMessage();
						if (message.getFullText().startsWith(" The word was ")) {
							String word = message.getFullText().substring(14);
							if (!words.contains(word)) {
								words.add(word);
								System.out.println("Added word \"" + word + "\" to list (" + words.size() + " words in total).");
							} else {
								System.out.println("Failed to unscramble word \"" + word + "\".");
							}
						} else if (!message.getFullText().equals(" Scramble » ✖ Scramble Failed ✖")) {
							try {
								JsonObject obj = new JsonParser().parse(message.getText()).getAsJsonObject();
								if (obj.getAsJsonArray("extra").size() == 1) {
									JsonObject extra = obj.getAsJsonArray("extra").get(0).getAsJsonObject();
									JsonArray values = extra.getAsJsonObject("hoverEvent").getAsJsonArray("value");
									JsonObject value = values.get(values.size()-1).getAsJsonObject();
									String word = value.get("text").getAsString();
									String game = values.get(0).getAsJsonObject().get("text").getAsString();
									if (game.startsWith("Unscramble")) {
										System.out.println("Unscrambling \"" + word + "\"...");
										for (String test : words) {
											if (test.length() == word.length()) {
												String result = test;
												for (char c : word.toCharArray()) {
													if (test.contains("" + c)) {
														if (test.length() > 1) {
															int i = test.indexOf("" + c);
															test = test.substring(0, i) + test.substring(i+1);
														} else {
															event.getSession().send(new ClientChatPacket(result));
															break;
														}
													} else break;
												}
											}
										}
									} else if (game.startsWith("Type")) {
										if (!words.contains(word)) {
											words.add(word);
											System.out.println("Added word \"" + word + "\" to list (" + words.size() + " words in total).");
										}
										event.getSession().send(new ClientChatPacket(word));
									}
								} else {
									;
								}
							} catch (Exception e) {
								if (message.getFullText().contains("Scramble")) System.out.println(message.toJsonString());
								System.out.println(message.getFullText());
							}
						}
					}
				}

				public void connected (ConnectedEvent event) {
					System.out.println("Logged in on target server as " + protocol.getProfile().getName() + ".");
				}

				public void packetSent (PacketSentEvent event) {
					if (event.getPacket() instanceof ClientChatPacket) {
						String msg = ((ClientChatPacket) event.getPacket()).getMessage();
						System.out.println("Sent chat message \"" + msg + "\"...");
					}
				}

				public void disconnecting (DisconnectingEvent event) {}

				public void disconnected (DisconnectedEvent event) {
					System.out.println("Disconnected: " + event.getReason());
					out.close();
				}
			});
			client.getSession().setConnectTimeout(10);
			client.getSession().connect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
