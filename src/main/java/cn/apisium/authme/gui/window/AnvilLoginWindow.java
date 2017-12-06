package cn.apisium.authme.gui.window;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

import cn.apisium.authme.gui.variable.Variables;
import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.events.LoginEvent;

public class AnvilLoginWindow extends LoginWindow {

	public AnvilLoginWindow(ProtocolManager protocolManager, Plugin plugin) {
		super(protocolManager, plugin);
		protocolManager.addPacketListener(
				new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.WINDOW_CLICK) {
					@Override
					public void onPacketReceiving(PacketEvent event) {
						try {
							if (event.getPacketType() != PacketType.Play.Client.WINDOW_CLICK) {
								return;
							}
							PacketContainer packet = event.getPacket();
							StructureModifier<Integer> integers = packet.getIntegers();
							int windowID = integers.read(0);
							if (windowID != nameMap.get(event.getPlayer().getName())) {
								return;
							}
							int slot = integers.read(1);
							if (slot == 2 || slot == 0) {
								String password = playerInput.containsKey(event.getPlayer().getName())
										? playerInput.get(event.getPlayer().getName())
										: "";
								boolean registered = AuthMeApi.getInstance().isRegistered(event.getPlayer().getName());
								String start = AnvilLoginWindow.this.getInfoFor(event.getPlayer(),
										Variables.anvilInfo.get(0));
								password = password.startsWith(start) ? password.substring(start.length()) : password;
								if (!registered & !password.isEmpty()) {
									AuthMeApi.getInstance().registerPlayer(event.getPlayer().getName(), password);
								}
								Bukkit.getPluginCommand("login").execute(event.getPlayer(), "login",
										new String[] { password });
							}
							try {
								AnvilLoginWindow.this.openFor(event.getPlayer());
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						} catch (Throwable e) {
							if (Variables.debug)
								e.printStackTrace();
							// To avoid some strange issues
						}
					}
				});
		protocolManager.addPacketListener(
				new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.CUSTOM_PAYLOAD) {
					@Override
					public void onPacketReceiving(PacketEvent event) {
						try {
							if (event.getPacketType() != PacketType.Play.Client.CUSTOM_PAYLOAD) {
								return;
							}
							if (AuthMeApi.getInstance().isAuthenticated(event.getPlayer())) {
								return;
							}
							PacketContainer packet = event.getPacket();
							StructureModifier<String> strings = packet.getStrings();
							if (!strings.read(0).equals("MC|ItemName")) {
								return;
							}
							Object byteBuf = packet.getModifier().readSafely(1);
							try {
								Class<?> byteBufClass = byteBuf.getClass();
								boolean hasArray = (Boolean) byteBufClass.getDeclaredMethod("hasArray", new Class<?>[0])
										.invoke(byteBuf, new Object[0]);
								byte[] bytes;
								if (hasArray) {
									bytes = (byte[]) byteBufClass.getDeclaredMethod("array", new Class<?>[0])
											.invoke(byteBuf, new Object[0]);
								} else {
									int length = (Integer) byteBufClass
											.getDeclaredMethod("readableBytes", new Class<?>[0])
											.invoke(byteBuf, new Object[0]);
									bytes = new byte[length];
									int readerIndex = (Integer) byteBufClass
											.getDeclaredMethod("readerIndex", new Class<?>[0])
											.invoke(byteBuf, new Object[0]);
									byteBufClass
											.getDeclaredMethod("getBytes", new Class<?>[] { int.class, byte[].class })
											.invoke(byteBuf, new Object[] { readerIndex, bytes });
								}
								String contents;
								contents = new String(bytes, StandardCharsets.UTF_8).substring(1);
								playerInput.put(event.getPlayer().getName(), contents);

							} catch (IllegalAccessException e) {
							} catch (IllegalArgumentException e) {
							} catch (InvocationTargetException e) {
							} catch (NoSuchMethodException e) {
							} catch (SecurityException e) {
							}
						} catch (Throwable e) {
							if (Variables.debug)
								e.printStackTrace();
							// To avoid some strange issues
						}
					}
				});
	}

	private HashMap<String, String> playerInput = new HashMap<String, String>();
	private HashMap<String, Integer> nameMap = new HashMap<String, Integer>();
	private Random random = new Random();

	@SuppressWarnings("deprecation")
	@Override
	public void openFor(final Player player) throws InvocationTargetException {
		if (AuthMeApi.getInstance().isAuthenticated(player)) {
			return;
		}
		int key;
		if (!nameMap.containsKey(player.getName())) {
			key = random.nextInt(255);
			nameMap.put(player.getName(), key);
		} else {
			key = nameMap.get(player.getName());
		}
		final PacketContainer anvil = new PacketContainer(PacketType.Play.Server.OPEN_WINDOW);
		final PacketContainer slotAir = new PacketContainer(PacketType.Play.Server.SET_SLOT);
		final PacketContainer slotHead = new PacketContainer(PacketType.Play.Server.SET_SLOT);
		slotAir.getIntegers().write(0, -1);
		slotAir.getIntegers().write(1, -1);
		slotAir.getItemModifier().write(0, new ItemStack(Material.AIR));
		anvil.getIntegers().write(0, key);
		anvil.getStrings().write(0, "minecraft:anvil");
		anvil.getChatComponents().write(0, WrappedChatComponent.fromText("µÇÂ¼"));
		anvil.getIntegers().write(1, 0);
		slotHead.getIntegers().write(0, key);
		slotHead.getIntegers().write(1, 0);
		ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
		SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
		try {
			skullMeta.setOwner(player.getName());
			skullMeta.setOwningPlayer(player);
		} catch (Throwable e) {
		}
		List<String> lines = this.getInfoFor(player);
		skullMeta.setDisplayName(lines.get(0));
		skullMeta.setLore(lines.subList(1, lines.size()));
		item.setItemMeta(skullMeta);
		slotHead.getItemModifier().write(0, item);
		protocolManager.sendServerPacket(player, slotAir);
		protocolManager.sendServerPacket(player, anvil);
		Runnable sendSlot = new Runnable() {

			@Override
			public void run() {
				try {
					protocolManager.sendServerPacket(player, slotHead);
					if (!AuthMeApi.getInstance().isAuthenticated(player))
						Bukkit.getScheduler().runTaskLater(plugin, this, Variables.anvilLoginDelay);
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}

			}
		};
		Bukkit.getScheduler().runTaskLater(plugin, sendSlot, Variables.anvilLoginDelay);
	}

	@EventHandler
	public void onAuthed(LoginEvent event) {
		final PacketContainer closeWindow = new PacketContainer(PacketType.Play.Server.CLOSE_WINDOW);
		int key = nameMap.get(event.getPlayer().getName());
		closeWindow.getIntegers().write(0, key);
		try {
			protocolManager.sendServerPacket(event.getPlayer(), closeWindow);
		} catch (InvocationTargetException e) {
		}
	}

	@Override
	public List<String> getInfoFor(Player player) {
		List<String> info = new ArrayList<String>();
		for (String line : Variables.anvilInfo) {
			info.add(getInfoFor(player, line));
		}
		return info;
	}

}
