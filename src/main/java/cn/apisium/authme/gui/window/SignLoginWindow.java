package cn.apisium.authme.gui.window;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;

import cn.apisium.authme.gui.variable.Variables;
import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.events.LoginEvent;

public class SignLoginWindow extends LoginWindow {

	public SignLoginWindow(ProtocolManager protocolManager, Plugin plugin) {
		super(protocolManager, plugin);
		protocolManager.addPacketListener(
				new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.UPDATE_SIGN) {
					@Override
					public void onPacketReceiving(PacketEvent event) {
						if (event.getPacketType() != PacketType.Play.Client.UPDATE_SIGN) {
							return;
						}
						if (AuthMeApi.getInstance().isAuthenticated(event.getPlayer())) {
							return;
						}
						PacketContainer packet = event.getPacket();
						String[] strings = packet.getStringArrays().read(0);
						if (strings[Variables.signLoginLine - 1].isEmpty()) {
							return;
						}
						boolean registered = AuthMeApi.getInstance().isRegistered(event.getPlayer().getName());
						String password = strings[Variables.signLoginLine - 1].substring(SignLoginWindow.this
								.getInfoFor(event.getPlayer(), Variables.signInfo.get(Variables.signLoginLine - 1))
								.length());
						if (!registered & !password.isEmpty()) {
							AuthMeApi.getInstance().registerPlayer(event.getPlayer().getName(), password);
						}
						Bukkit.getPluginCommand("login").execute(event.getPlayer(), "login", new String[] { password });
					}
				});
	}

	@Override
	public void openFor(Player player) throws InvocationTargetException {
		if (AuthMeApi.getInstance().isAuthenticated(player)) {
			return;
		}
		PacketContainer fakeBlockChange = new PacketContainer(PacketType.Play.Server.BLOCK_CHANGE);
		PacketContainer updateEntity = new PacketContainer(PacketType.Play.Server.TILE_ENTITY_DATA);
		PacketContainer sign = new PacketContainer(PacketType.Play.Server.OPEN_SIGN_EDITOR);
		Location block = player.getLocation().getBlock().getLocation();
		BlockPosition position = new BlockPosition(block.getBlockX(), block.getBlockY() + 1, block.getBlockZ());
		fakeBlockChange.getBlockPositionModifier().write(0, position);
		fakeBlockChange.getBlockData().write(0, WrappedBlockData.createData(Material.SIGN_POST));
		protocolManager.sendServerPacket(player, fakeBlockChange);
		updateEntity.getBlockPositionModifier().write(0, position);
		updateEntity.getIntegers().write(0, 9);
		NbtCompound signNbt = (NbtCompound) updateEntity.getNbtModifier().read(0);
		signNbt = signNbt == null ? NbtFactory.ofCompound("") : signNbt;
		List<String> lines = this.getInfoFor(player);
		for (int i = 0; i < lines.size() || i < 4; i++) {
			signNbt.put("Text" + (i + 1), "{\"text\":\"" + lines.get(i) + "\"}");
		}
		updateEntity.getNbtModifier().write(0, signNbt);
		protocolManager.sendServerPacket(player, updateEntity);
		sign.getBlockPositionModifier().write(0, position);
		protocolManager.sendServerPacket(player, sign);
	}

	@EventHandler
	public void onAuthed(LoginEvent event) {
		final PacketContainer updateBlock = new PacketContainer(PacketType.Play.Server.BLOCK_CHANGE);
		PacketContainer updateEntity = new PacketContainer(PacketType.Play.Server.TILE_ENTITY_DATA);
		Player player = event.getPlayer();
		Location block = player.getLocation().getBlock().getLocation();
		BlockPosition position = new BlockPosition(block.getBlockX(), block.getBlockY() + 1, block.getBlockZ());
		updateBlock.getBlockPositionModifier().write(0, position);
		updateBlock.getBlockData().write(0, WrappedBlockData.createData(block.getBlock().getType()));
		try {
			protocolManager.sendServerPacket(event.getPlayer(), updateBlock);
		} catch (InvocationTargetException e) {
		}
		try {
			updateEntity.getBlockPositionModifier().write(0, position);
			updateEntity.getIntegers().write(0, 9);
			updateEntity.getNbtModifier().write(0, NbtFactory.readBlockState(block.getBlock()));
			protocolManager.sendServerPacket(player, updateEntity);
		} catch (Throwable e) {
		}
	}

	@Override
	public List<String> getInfoFor(Player player) {
		List<String> info = new ArrayList<String>();
		for (String line : Variables.signInfo) {
			info.add(getInfoFor(player, line));
		}
		return info;
	}

}
