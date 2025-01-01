package com.gato.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GatocontrollerClient implements ClientModInitializer {
	private final MinecraftClient client = MinecraftClient.getInstance();
	private static final Logger LOGGER = LoggerFactory.getLogger("GatoController");
	private WebSocketClient wsClient;
	private final Gson gson = new Gson();
	private ScheduledExecutorService scheduler;
	private static final String WEBSOCKET_URL = "ws://localhost:8000/ws/minecraft";
	private boolean isConnectedToServer = false;

	@Override
	public void onInitializeClient() {
		LOGGER.info("GatoController Started!");

		// Register connection events more safely
		ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> {
			CompletableFuture.runAsync(() -> {
				try {
					isConnectedToServer = true;
					if (wsClient == null || !wsClient.isOpen()) {
						connectToWebSocket();
					} else {
						sendInitialConnection();
					}
					startStatusUpdates();
				} catch (Exception e) {
					LOGGER.error("Error in JOIN event handler", e);
				}
			});
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> {
			CompletableFuture.runAsync(() -> {
				try {
					isConnectedToServer = false;
					stopStatusUpdates();
					//close the websocket connection
					wsClient.close();
				} catch (Exception e) {
					LOGGER.error("Error in DISCONNECT event handler", e);
				}
			});
		});
	}

	private void connectToWebSocket() {
		try {
			wsClient = new WebSocketClient(new URI(WEBSOCKET_URL)) {
				@Override
				public void onOpen(ServerHandshake handshakedata) {
					LOGGER.info("Connected to control center");
					CompletableFuture.runAsync(() -> {
						if (isConnectedToServer) {
							sendInitialConnection();
						}
					});
				}

				@Override
				public void onMessage(String message) {
					handleWebSocketMessage(message);
				}

				@Override
				public void onClose(int code, String reason, boolean remote) {
					LOGGER.info("Disconnected from control center: " + reason);
					scheduleReconnect();
				}

				@Override
				public void onError(Exception ex) {
					LOGGER.error("WebSocket error: " + ex.getMessage());
				}
			};
			wsClient.connect();
		} catch (Exception e) {
			LOGGER.error("Failed to create WebSocket connection: " + e.getMessage());
		}
	}

	private void sendInitialConnection() {
		if (!isConnectedToServer || client.getSession() == null) return;

		JsonObject connectionMessage = new JsonObject();
		connectionMessage.addProperty("type", "connect");
		connectionMessage.addProperty("name", client.getSession().getUsername());
		connectionMessage.addProperty("status", "online");
		wsClient.send(gson.toJson(connectionMessage));
	}

	private void handleWebSocketMessage(String message) {
		try {
			JsonObject json = gson.fromJson(message, JsonObject.class);
			if (json.has("type")) {
				switch (json.get("type").getAsString()) {
					case "command":
						handleCommand(json.get("command").getAsString());
						break;
					case "ping":
						handlePing();
						break;
					case "chat":
						if (json.get("message").getAsString().startsWith("/")) {
							sendCommand(json.get("message").getAsString().substring(1));
						} else {
							sendMessage(json.get("message").getAsString());
						}
						break;
					case "travel":
						sendMessage("#goto " + json.get("x").getAsString() + " " + json.get("y").getAsString() + " " + json.get("z").getAsString());
						break;
					default:
						LOGGER.warn("Unknown message type: " + json.get("type").getAsString());
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error processing message: " + e.getMessage());
		}
	}

	private void handleCommand(String command) {
		CompletableFuture.runAsync(() -> {
			try {
				String[] args = command.split(" ");
				switch (args[0].toLowerCase()) {
					case "stop":
						sendMessage("#stop");
						break;
					case "follow":
						if (args.length > 1) {
							sendMessage("#follow player " + args[1]);
						}
						break;
					case "goto":
						if (args.length > 1) {
							sendMessage("#goto " + String.join(" ",
									Arrays.copyOfRange(args, 1, args.length)));
						}
						break;
					case "chat":
						if (args.length > 1) {
							sendMessage(String.join(" ",
									Arrays.copyOfRange(args, 1, args.length)));
						}
						break;
					case "home":
						sendCommand("team home");
						break;
					default:
						LOGGER.warn("Unknown command: " + command);
				}
			} catch (Exception e) {
				LOGGER.error("Error executing command: " + command, e);
			}
		}, client::execute);
	}

	private void handlePing() {
		JsonObject pong = new JsonObject();
		pong.addProperty("type", "pong");
		wsClient.send(gson.toJson(pong));
	}

	private void startStatusUpdates() {
		stopStatusUpdates(); // Stop any existing scheduler
		scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(() -> {
			try {
				if (isConnectedToServer && client.player != null && wsClient != null && wsClient.isOpen()) {
					JsonObject status = new JsonObject();
					status.addProperty("type", "status_update");

					JsonObject statusData = new JsonObject();
					statusData.addProperty("x", client.player.getX());
					statusData.addProperty("y", client.player.getY());
					statusData.addProperty("z", client.player.getZ());
					statusData.addProperty("health", client.player.getHealth());
					statusData.addProperty("hunger", client.player.getHungerManager().getFoodLevel());
					statusData.addProperty("dimension",
							client.player.getWorld().getRegistryKey().getValue().toString());

					status.add("status", statusData);
					wsClient.send(gson.toJson(status));
				}
			} catch (Exception e) {
				LOGGER.error("Error sending status update", e);
			}
		}, 0, 1, TimeUnit.SECONDS);
	}

	private void stopStatusUpdates() {
		if (scheduler != null && !scheduler.isShutdown()) {
			scheduler.shutdown();
			try {
				scheduler.awaitTermination(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				LOGGER.error("Error shutting down scheduler", e);
			}
		}
	}

	private void scheduleReconnect() {
		CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
			try {
				connectToWebSocket();
			} catch (Exception e) {
				LOGGER.error("Reconnection failed", e);
				scheduleReconnect();
			}
		});
	}

	private void sendMessage(String message) {
		if (client.getNetworkHandler() != null) {
			client.getNetworkHandler().sendChatMessage(message);
			LOGGER.info("Sent message: " + message);
		}
	}

	private void sendCommand(String message) {
		if (client.getNetworkHandler() != null) {
			client.getNetworkHandler().sendChatCommand(message);
			LOGGER.info("Sent command: " + message);
		}
	}

	public void cleanup() {
		try {
			if (wsClient != null) {
				wsClient.close();
			}
			stopStatusUpdates();
		} catch (Exception e) {
			LOGGER.error("Error during cleanup", e);
		}
	}
}