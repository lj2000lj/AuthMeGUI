package cn.apisium.authme.gui.variable;

import java.util.Arrays;
import java.util.List;

public class Variables {
	public static boolean debug = false;
	public static String windowType = "anvil";
	public static int anvilLoginDelay = 5;
	public static int signLoginLine = 3;
	public static String scriptEngine = "js";
	public static String scriptPath = "";
	@NonConfig
	public static String script = "";
	public static String registeredStatueMessage = "登录";
	public static String unregisteredStatueMessage = "注册";
	public static String statueVariable = "%statue%";
	public static String playerVariable = "%player%";
	public static String playerCustomVariable = "%player_custom%";
	public static String playerDisplayVariable = "%player_display%";
	public static String playerIpVariable = "%player_ip%";
	public static String playerLevelVariable = "%player_level%";
	public static String serverIpVariable = "%server_ip%";
	public static List<String> anvilInfo = Arrays
			.asList(new String[] { "输入密码" + statueVariable + "§k", "完成后点击头像即可登录" });
	public static List<String> signInfo = Arrays
			.asList(new String[] { playerVariable, "请在下一行输入密码", "§k", "点击完成进行" + statueVariable });
}
