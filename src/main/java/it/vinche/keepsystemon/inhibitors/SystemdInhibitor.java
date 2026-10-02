package it.vinche.keepsystemon.inhibitors;

import org.freedesktop.dbus.FileDescriptor;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.interfaces.DBusInterface;

// IDEA: it can be useful to separate sleep inhibition and shutdown inhibition
public class SystemdInhibitor implements Inhibitor {
    private FileDescriptor inhibitorFd;
    private DBusConnection bus;

    @DBusInterfaceName("org.freedesktop.login1.Manager")
    public interface Manager extends DBusInterface {
        FileDescriptor Inhibit(String what, String who, String why, String mode);
    }

    @Override
    public void inhibit(String reason) throws Exception {
        var newbus = DBusConnectionBuilder.forSystemBus().withShared(false).build();
        try {
            var manager = newbus.getRemoteObject(
                "org.freedesktop.login1",
                "/org/freedesktop/login1",
                Manager.class
            );

            var fd = manager.Inhibit(
                "sleep:idle:shutdown", "Minecraft Server", reason, "block"
            );
            if (isConnected()) {
                // close the old inhibitor before replacing
                // this can be used to change the reason
                unhibit();
            }
            inhibitorFd = fd;
            bus = newbus;
        } catch (Exception e) {
            newbus.close();
            throw e;
        }
    }

    @Override
    public void unhibit() throws Exception {
        if (isConnected()) {
            try {
                // closing the bus will also close the file descriptor
                bus.close();
            } finally {
                bus = null;
                inhibitorFd = null;
            }
        }
    }

    @Override
    public boolean isInhibited() {
        return isConnected() && inhibitorFd != null && inhibitorFd.getIntFileDescriptor() != -1;
    }

    private boolean isConnected() {
        return bus != null && bus.isConnected();
    }
}
