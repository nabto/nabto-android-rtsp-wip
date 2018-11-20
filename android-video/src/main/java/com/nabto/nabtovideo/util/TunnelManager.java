package com.nabto.nabtovideo.util;

import android.util.Log;

import com.nabto.api.NabtoApi;
import com.nabto.api.NabtoStatus;
import com.nabto.api.Session;
import com.nabto.api.Tunnel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by ug on 31/10/2017.
 */

abstract class TunnelVisitor {
    public abstract void visit(Tunnel t);
}

class TunnelContainer {
    // enable looking up existing open tunnel for given device (many tunnels may exist per device)
    private HashMap<VideoDevice, LinkedList<Tunnel>> deviceToTunnel;

    // enable looking up device that has a given open tunnel (for cleanup)
    private HashMap<Tunnel, VideoDevice> tunnelToDevice;


    TunnelContainer() {
        deviceToTunnel = new HashMap<VideoDevice, LinkedList<Tunnel>>();
        tunnelToDevice = new HashMap<Tunnel, VideoDevice>();
    }

    public boolean hasTunnel(VideoDevice device) {
        return deviceToTunnel.containsKey(device);
    }

    public void add(VideoDevice device, Tunnel tunnel) {
        if (!this.deviceToTunnel.containsKey(device)) {
            this.deviceToTunnel.put(device, new LinkedList<Tunnel>());
        }
        this.deviceToTunnel.get(device).add(tunnel);
        if (tunnelToDevice.containsKey(tunnel)) {
            if (tunnelToDevice.get(tunnel) != device) {
                throw new RuntimeException("Assertion failed: tunnel associated with different devices");
            } else {
                // ignore
            }
        } else {
            this.tunnelToDevice.put(tunnel, device);
        }
    }

    public Tunnel popTunnelForDevice(VideoDevice device) {
        if (!this.deviceToTunnel.containsKey(device)) {
            return null;
        }
        Tunnel t = this.deviceToTunnel.get(device).pop();
        if (this.deviceToTunnel.get(device).size() == 0) {
            this.deviceToTunnel.remove(device);
            this.tunnelToDevice.remove(t);
        }
        return t;
    }

    public VideoDevice removeTunnel(Tunnel tunnel) {
        if (!tunnelToDevice.containsKey(tunnel)) {
            return null;
        }
        VideoDevice d = this.tunnelToDevice.get(tunnel);
        if (this.popTunnelForDevice(d) == null) {
            throw new RuntimeException("Assertion failed: maps not consistent");
        }
        return d;
    }

    public void traverse(TunnelVisitor visitor) {
        for (VideoDevice d : deviceToTunnel.keySet()) {
            for (Tunnel t : deviceToTunnel.get(d)) {
                visitor.visit(t);
            }
        }
    }

    public void clear() {
        this.deviceToTunnel.clear();
        this.tunnelToDevice.clear();
    }
}

public class TunnelManager {

    private NabtoApi nabtoApi;
    private Session session;
    private String email;
    private String password;

    private static TunnelManager instance;
    private TunnelContainer idleTunnels;
    private TunnelContainer activeTunnels;

    public static synchronized TunnelManager instance() {
        if (instance == null) {
            instance = new TunnelManager();
        }
        return instance;
    }

    public void initialize(NabtoApi nabtoApi, String email, String password) {
        this.nabtoApi = nabtoApi;
        this.session = nabtoApi.openSession(email, password);
        this.email = email;
        this.password = password;
        this.idleTunnels = new TunnelContainer();
        this.activeTunnels = new TunnelContainer();
    }

    class TunnelCloseVisitor extends TunnelVisitor {
        @Override
        public void visit(Tunnel t) {
            if (nabtoApi.tunnelClose(t) == NabtoStatus.OK) {
                Log.d(this.getClass().getName(), "Closed tunnel " + t);
            } else {
                Log.w(this.getClass().getName(), "Could not close tunnel " + t);
            }
        }
    }

    public void deinitialize() {
        idleTunnels.traverse(new TunnelCloseVisitor());
        activeTunnels.traverse(new TunnelCloseVisitor());
        idleTunnels.clear();
        activeTunnels.clear();
        if (session != null) {
            nabtoApi.closeSession(session);
            session = null;
        }
    }

    private synchronized void ensureOpenSession() {
        if (session == null) {
            session = this.nabtoApi.openSession(this.email, this.password);
        }
    }

    public synchronized Tunnel openTunnel(VideoDevice device) {
        ensureOpenSession();
        Tunnel t;
        if (this.idleTunnels.hasTunnel(device)) {
            Log.i(this.getClass().getName(), "Idle tunnel available for device " + device);
            t = this.idleTunnels.popTunnelForDevice(device);
        } else {
            Log.i(this.getClass().getName(), "Opening new tunnel for device " + device);
            t = nabtoApi.tunnelOpenTcp(0, device.name, device.host, device.port, this.session);
        }
        this.activeTunnels.add(device, t);
        return t;
    }

    public synchronized VideoDevice releaseTunnel(Tunnel tunnel) {
        VideoDevice d = this.activeTunnels.removeTunnel(tunnel);
        if (d != null) {
            this.idleTunnels.add(d, tunnel);
            Log.i(this.getClass().getName(), "Released tunnel " + tunnel + " for " + d);
            return d;
        } else {
            Log.d(this.getClass().getName(), "No active tunnel " + tunnel + " found");
            return null;
        }
    }

    public synchronized void closeTunnel(Tunnel tunnel) {
        this.releaseTunnel(tunnel);
        VideoDevice d = this.idleTunnels.removeTunnel(tunnel);
        if (d != null) {
            Log.i(this.getClass().getName(), "Removed idle tunnel " + tunnel + " for " + d);
        } else {
            Log.d(this.getClass().getName(), "No idle tunnel " + tunnel + " found");
        }
        nabtoApi.tunnelClose(tunnel);
    }

    public synchronized void populateConnectionCache(ArrayList<VideoDevice> devices) {
        ensureOpenSession();
        for (VideoDevice d: devices) {
            Log.d(this.getClass().getName(), "Adding device " + d + " to cache");
            Tunnel tunnel = nabtoApi.tunnelOpenTcp(0, d.name, d.host, d.port, this.session);
            idleTunnels.add(d, tunnel);
        }
    }

}
