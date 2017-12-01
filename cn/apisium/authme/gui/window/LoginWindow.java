package cn.apisium.authme.gui.window;

import java.lang.reflect.InvocationTargetException;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.ProtocolManager;

public abstract class LoginWindow {

	protected final ProtocolManager protocolManager;
	protected final Plugin plugin;

	public LoginWindow(ProtocolManager protocolManager, Plugin plugin) {
		this.protocolManager = protocolManager;
		this.plugin = plugin;
	}

	public abstract void openFor(Player player) throws InvocationTargetException;
}
