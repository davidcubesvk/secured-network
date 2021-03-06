package com.davidcubesvk.securedNetworkProxy.ipWhitelist;

import com.davidcubesvk.securedNetworkCore.log.Log;
import com.davidcubesvk.securedNetworkProxy.SecuredNetworkProxy;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Class covering the IP-whitelisting feature. Using this class, for example, an incoming connection can be checked, or
 * a set of whitelisted IPs can be retrieved.
 */
public class IPWhitelist {

    /**
     * The server IP placeholder.
     */
    private static final String IP_PLACEHOLDER = "{ip}";

    //Proxy IP
    private String proxyIP;

    //Whitelisted IPs
    private final Set<IPHolder> whitelisted = new HashSet<>();
    //Enabled
    private boolean enabled;

    //Disconnect message
    private TextComponent disconnectMessage;

    //The plugin instance
    private final SecuredNetworkProxy plugin;

    /**
     * Calls {@link #reload()} to load the internal data.
     *
     * @param plugin the plugin instance
     */
    public IPWhitelist(SecuredNetworkProxy plugin) {
        //Set
        this.plugin = plugin;
        //Reload
        reload();
    }

    /**
     * Checks the connecting player's VirtualHost, if the IP player used to connect is whitelisted (contains if-enabled
     * check).<br>
     * The <code>virtualHost</code> parameter should be an instance got from {@link PendingConnection#getVirtualHost()}
     * or similar method.
     *
     * @param virtualHost the player's virtual host
     * @return if the IP player used to connect is whitelisted
     */
    public IPCheckResult checkIP(InetSocketAddress virtualHost) {
        //If disabled
        if (!enabled)
            return new IPCheckResult(true);

        //The IP
        String ip = virtualHost.getHostString() + IPHolder.PORT_COLON + virtualHost.getPort();

        //Loop through whitelisted IPs
        for (IPHolder ipHolder : whitelisted) {
            //If do equal
            if (ipHolder.compare(ip))
                return new IPCheckResult(true);
        }

        //Not found, return the default message
        return new IPCheckResult(false, disconnectMessage);
    }

    /**
     * Reloads the internal data.
     */
    public void reload() {
        //Log
        plugin.getLog().log(Level.INFO, Log.Source.WHITELIST, "Loading internal variables...");

        //If enabled
        enabled = plugin.getConfiguration().getBoolean("ip-whitelist.enabled");
        //Do not continue if disabled
        if (!enabled)
            return;

        //The default message
        disconnectMessage = new TextComponent(ChatColor.translateAlternateColorCodes('&', plugin.getConfiguration().getString("disconnect.whitelist")));

        //Reload whitelisted IPs
        if (reloadIPs())
            //Get the proxy IP
            getProxyIP();
    }

    /**
     * Reloads whitelisted IPs.
     * @return if any of the IPs uses {@link #IP_PLACEHOLDER} and calling {@link #getProxyIP()} is needed
     */
    private boolean reloadIPs() {
        //Get whitelisted IPs
        List<String> whitelistedList = plugin.getConfiguration().getStringList("ip-whitelist.ips");
        //Clear the list
        whitelisted.clear();
        //Loop through every IP
        for (String ip : whitelistedList) {
            //Create a new holder
            IPHolder ipHolder = new IPHolder();
            //Set the IP
            if (ipHolder.setIp(ip))
                //Add
                whitelisted.add(ipHolder);
            else
                //Log
                plugin.getLog().logConsole(Level.SEVERE, Log.Source.WHITELIST, "IP \"" + ip + "\" is not specified correctly! Removing from the whitelist.");
        }

        //If IP placeholder is present
        return whitelistedList.toString().contains(IP_PLACEHOLDER);
    }

    /**
     * Gets and sets the IP of this server into an internal field.
     */
    private void getProxyIP() {
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            try {
                //Log the operation
                plugin.getLog().log(Level.INFO, Log.Source.WHITELIST, "Getting the IP of the server...");

                //Open stream and initialize reader
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL(plugin.getConfiguration().getString("ip-whitelist.ip-website")).openStream()));
                //Set the IP
                proxyIP = bufferedReader.readLine();

                //Log the IP
                plugin.getLog().log(Level.INFO, Log.Source.WHITELIST, "Public IP got successfully, hosting on " + proxyIP + "!");
                //Replace {ip} with the server's IP
                for (IPHolder ipHolder : whitelisted)
                    ipHolder.setIp(ipHolder.getIp().replace(IP_PLACEHOLDER, proxyIP));
            } catch (Exception ex) {
                //Log the error
                plugin.getLog().logConsoleWithoutThrowable(Level.SEVERE, Log.Source.WHITELIST, "An error occurred while getting the IP of the server for the {ip} placeholder! Is the server address correct?", ex);
            }
        });
    }

    /**
     * Returns the whitelisted IPs in a set.
     *
     * @return the whitelisted IPs
     */
    public Set<IPHolder> getWhitelisted() {
        return whitelisted;
    }

}