package cn.apisium.authme.gui.window;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.ProtocolManager;

import cn.apisium.authme.gui.variable.Variables;
import fr.xephi.authme.api.v3.AuthMeApi;

public abstract class LoginWindow implements Listener {

	protected final ProtocolManager protocolManager;
	protected final Plugin plugin;

	public LoginWindow(ProtocolManager protocolManager, Plugin plugin) {
		this.protocolManager = protocolManager;
		this.plugin = plugin;
	}

	public abstract void openFor(Player player) throws InvocationTargetException;

	public abstract List<String> getInfoFor(Player player);

	public String getInfoFor(Player player, String string) {
		string = string
				.replaceAll(Variables.statueVariable,
						AuthMeApi.getInstance().isRegistered(player.getName()) ? Variables.registeredStatueMessage
								: Variables.unregisteredStatueMessage)
				.replaceAll(Variables.playerVariable, player.getName())
				.replaceAll(Variables.playerCustomVariable, player.getCustomName())
				.replaceAll(Variables.playerDisplayVariable, player.getDisplayName())
				.replaceAll(Variables.playerIpVariable, player.getAddress().getHostString())
				.replaceAll(Variables.serverIpVariable, Bukkit.getIp())
				.replaceAll(Variables.playerLevelVariable, player.getExpToLevel() + "");
		if (Variables.script.isEmpty()) {
			return string;
		}
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByName(Variables.scriptEngine);
		Bindings bindings = engine.createBindings();
		bindings.put("line", string);
		bindings.put("player", player);
		bindings.put("plugin", plugin);
		try {
			string = engine.eval(Variables.script, bindings).toString();
		} catch (ScriptException e) {
			e.printStackTrace();
		}
		return string;
	}
}
