package me.au2001.testpluginsreactbot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.swing.JOptionPane;

import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.mc.protocol.data.game.MessageType;
import org.spacehq.mc.protocol.data.message.Message;
import org.spacehq.mc.protocol.packet.ingame.client.ClientChatPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerChatPacket;
import org.spacehq.mc.protocol.packet.ingame.server.window.ServerSetSlotPacket;
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
import com.udojava.evalex.Expression;

public class TestPluginsReactBot implements SessionListener {

	private static String USER, PASS, HOST, FILE = "words.txt";
	private static int PORT = 25565;
	private static PrintWriter out;
	private static Client client;
	private static Scanner in;
	private static MinecraftProtocol protocol;
	private static boolean sell = true, waslast = false;
	private static List<String> welcoming = new ArrayList<String>(), words = new ArrayList<String>();

	public static void main (String[] args) {
		in = new Scanner(System.in);

		requestInfo(args.length > 0);

		// USER = "yourusername@example.com";
		// PASS = "YourPassword";
		// HOST = "testplugins.com";

		loadWords();

		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(FILE, true)));
			protocol = new MinecraftProtocol(USER, PASS, false, Proxy.NO_PROXY);
			client = new Client(HOST, PORT, protocol, new TcpSessionFactory(Proxy.NO_PROXY));
			client.getSession().addListener(new TestPluginsReactBot());
			client.getSession().setConnectTimeout(10);
			client.getSession().connect();

			while (client.getSession().isConnected())
				if (in.hasNextLine()) client.getSession().send(new ClientChatPacket(in.nextLine()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void loadWords () {
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
	}

	public static void requestInfo (boolean cmdline) {
		if (cmdline) {
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
		} else {
			USER = JOptionPane.showInputDialog(null, "Please enter your Minecraft username/email:", "TPReactionFarm", JOptionPane.QUESTION_MESSAGE);
			if (USER == null || USER.isEmpty()) {
				System.out.println("No Minecraft username/email was specified, quitting.");
				System.exit(0);
			}
			PASS = JOptionPane.showInputDialog(null, "Please enter your Minecraft password:", "TPReactionFarm", JOptionPane.QUESTION_MESSAGE);
			if (PASS == null || PASS.isEmpty()) {
				System.out.println("No Minecraft password was specified, quitting.");
				System.exit(0);
			}
			HOST = JOptionPane.showInputDialog(null, "Please enter the IP address of the target server:", "TPReactionFarm", JOptionPane.QUESTION_MESSAGE).substring(0);
			if (HOST == null || HOST.isEmpty()) {
				System.out.println("No server IP address was specified, quitting.");
				System.exit(0);
			}
			try {
				PORT = Integer.parseInt(JOptionPane.showInputDialog(null, "Please enter the port of the target server (enter for default):", "TPReactionFarm", JOptionPane.QUESTION_MESSAGE));
			} catch (NullPointerException e) {
				System.out.println("No server port was specified (clicked cancel button), quitting.");
				System.exit(0);
			} catch (NumberFormatException e) {
				System.out.println("The specified port isn't a valid number, using " + PORT + ".");
			}
			String sfle = JOptionPane.showInputDialog(null, "Please enter the path to the words file (enter for default):", "TPReactionFarm", JOptionPane.QUESTION_MESSAGE);
			if (sfle == null) {
				System.out.println("No words file was specified (clicked cancel button), quitting.");
				System.exit(0);
			} else if (!sfle.isEmpty()) FILE = sfle;
			else System.out.println("The specified file isn't valid (empty), using " + FILE + ".");

			String hdps = "";
			while (hdps.length() < PASS.length()) hdps += "*";

			String message = "Do you want to connect with the following informations?\n";
			message += "\nUsername/Email: " + USER;
			message += "\nPassword: " + hdps;
			message += "\nTarget Server: " + HOST + ":" + PORT;
			message += "\nWords File: " + FILE;
			if (JOptionPane.showConfirmDialog(null, message, "TPReactionFarm", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) System.exit(0);
		}
	}

	public static void autoRespond (String message) {
		while (message.startsWith(" ")) message = message.substring(1);
		if (message.startsWith("Reaction » ") || message.startsWith("Scramble » ")) return;
		if (message.contains("»")) {
			String username = message.split("»")[0].replaceAll("\\[.*?\\]", "").replace(" ", "");
			if (username.equalsIgnoreCase(protocol.getProfile().getName())) {
				waslast = true;
				return;
			}
			message = message.substring(message.indexOf("»")).toLowerCase();
			while (message.startsWith(" ")) message = message.substring(1);
			if (message.contains(protocol.getProfile().getName().toLowerCase()) || waslast) {
				if (message.matches(".*(hello|hi|hai|hey|hola|yo|bonjour).*"))
					client.getSession().send(new ClientChatPacket("Hello, " + username + "."));
				else if (message.matches(".*(how *(the *(hell|fuck))? *(are|r) *(you|u)*|(are|r) *(you|u) *a *(ro)?bot).*"))
					client.getSession().send(new ClientChatPacket("I am the meaning of life, the essence of reality, " + username + "."));
				else if (message.matches(".*(thanks|thx).*"))
					client.getSession().send(new ClientChatPacket("You're welcome, " + username + "!"));
				else if (message.matches(".*((wanna|want( *to)?)? *pvp).*"))
					client.getSession().send(new ClientChatPacket("It wouldn't be fair: a perfectly optimized bot versus an unbelievably slow Human..."));
				else if (message.matches(".*(what'?s *up|sup|how('?s| *is) *it *going?|what *(are|r) *(you|u) *doing?).*"))
					client.getSession().send(new ClientChatPacket("I'm busy calculating the question which has 42 as answer, " + username + "."));
				else if (message.matches(".*(url|link|github|download).*"))
					client.getSession().send(new ClientChatPacket(username + ", my bot is available on GitHub: git.io/vwjTZ"));
				else if (message.matches(".*(stop|shut *(the fuck)? *(up|down)|stfu|sleep|(fuck|f) *(you|u)?|go *away|get *(the *fuck)? *out).*"))
					client.getSession().send(new ClientChatPacket("Sorry " + username + ", I can only be stopped by " + protocol.getProfile().getName() + "."));
				else if (message.matches(".*(how *much *is|what *is|what'?s|how much'?s).*")) {
					String expr = message.split("(how *much *is *|what *is *|what'?s *|how much'?s *)")[1];
					client.getSession().send(new ClientChatPacket(expr + " = " + new Expression(expr).eval()));
				} else client.getSession().send(new ClientChatPacket(username + ", 16GB of RAM wasn't enough to understand your message."));
			}
			waslast = false;
		} else if (message.startsWith("[+]")) {
			String username = message.substring(3).replace(" ", "");
			if (username.equalsIgnoreCase(protocol.getProfile().getName())) {
				autoRespond(" [0] [A] SomeGuy  » how much is sqrt(4*3-2)");
				return;
			}
			if (!welcoming.remove(username)) client.getSession().send(new ClientChatPacket("Welcome back, " + username + "."));
			else client.getSession().send(new ClientChatPacket("Welcome on TestPlugins, " + username + "!"));
		} else if (message.matches("This is [a-zA-Z0-9_]{3,16} first time joining!")) {
			String username = message.replace("This is ", "").replace(" first time joining!", "");
			if (username.equalsIgnoreCase(protocol.getProfile().getName())) return;
			welcoming.add(username);
		}
	}

	public void packetReceived (PacketReceivedEvent event) {
		if (event.getPacket() instanceof ServerChatPacket) {
			if (((ServerChatPacket) event.getPacket()).getType().equals(MessageType.NOTIFICATION)) return;
			Message message = ((ServerChatPacket) event.getPacket()).getMessage();
			if (message.getFullText().startsWith(" The word was ")) {
				String word = message.getFullText().substring(14);
				if (!words.contains(word)) {
					words.add(word); out.println(word);
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
												System.out.println("Sent chat message \"" + result + "\"...");
												sell = true;
												break;
											}
										} else break;
									}
								}
							}
						} else if (game.startsWith("Type")) {
							if (!words.contains(word)) {
								words.add(word); out.println(word);
								System.out.println("Added word \"" + word + "\" to list (" + words.size() + " words in total).");
							}
							event.getSession().send(new ClientChatPacket(word));
							System.out.println("Sent chat message \"" + word + "\"...");
							sell = true;
						}
					} else {
						boolean next = false;
						for (int i = 0; i < obj.getAsJsonArray("extra").size(); i++) {
							String word = obj.getAsJsonArray("extra").get(i).getAsJsonObject().get("text").getAsString();
							if (next) {
								if (!words.contains(word)) {
									words.add(word); out.println(word);
									System.out.println("Added word \"" + word + "\" to list (" + words.size() + " words in total).");
								}
								break;
							} else if (word.equals("'")) next = true;
						}
					}
				} catch (Exception e) {
					System.out.println(message.getFullText().replaceAll("§[0-9a-fk-o]", ""));
					autoRespond(message.getFullText().replaceAll("§[0-9a-fk-o]", ""));
				}
			}
		} else if (event.getPacket() instanceof ServerSetSlotPacket) {
			ServerSetSlotPacket packet = event.getPacket();
			if (packet.getSlot() > -1 && packet.getItem() != null) {
				if (packet.getItem().getId() == 133 && sell) {
					event.getSession().send(new ClientChatPacket("/sellall"));
					sell = false;
				} else if (packet.getItem().getId() == 57)
					event.getSession().send(new ClientChatPacket("/sellall"));
			}
		}
	}

	public void connected (ConnectedEvent event) {
		System.out.println("Logged in on target server as " + protocol.getProfile().getName() + ".");
	}

	public void packetSent (PacketSentEvent event) {}

	public void disconnecting (DisconnectingEvent event) {}

	public void disconnected (DisconnectedEvent event) {
		System.out.println("Disconnected: " + event.getReason());
		out.close(); in.close();
		System.exit(0);
	}

}