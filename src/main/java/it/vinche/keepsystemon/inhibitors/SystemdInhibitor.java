package it.vinche.keepsystemon.inhibitors;

import java.io.OutputStream;

import org.freedesktop.dbus.FileDescriptor;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.newsclub.net.unix.FileDescriptorCast;

class BusInstance {
    private static DBusConnection instance;

    public static DBusConnection getInstance() throws Exception {
        if (instance == null) {
            instance = DBusConnectionBuilder.forSystemBus().build();
        }

        return instance;
    }
}

// IDEA: it can be useful to separate sleep inhibition and shutdown inhibition
public class SystemdInhibitor implements Inhibitor {
    private FileDescriptor inhibitorFd;

    @DBusInterfaceName("org.freedesktop.login1.Manager")
    public interface Manager extends DBusInterface {
        FileDescriptor Inhibit(String what, String who, String why, String mode);
    }

    @Override
    public boolean inhibit(String reason) {
        try {
            var manager = BusInstance.getInstance().getRemoteObject(
                "org.freedesktop.login1",
                "/org/freedesktop/login1",
                Manager.class
            );

            var fd = manager.Inhibit(
                "sleep:idle:shutdown", "Minecraft Server", reason, "block"
            );
            if (isInhibited()) {
                // close the old inhibitor before replacing
                // this can be used to change the reason
                unhibit();
            }
            inhibitorFd = fd;

        } catch (Exception e) {
            e.printStackTrace();
            if (isInhibited()) {
                unhibit();
            }
            inhibitorFd = null; // make sure this stays null
            return false;
        }

        return true;
    }

    @Override
    public boolean unhibit() {
        if (!isInhibited()) {
            return false;
        }
        try {
            FileDescriptorCast
                .unsafeUsing(inhibitorFd.getIntFileDescriptor())
                .as(OutputStream.class)
                .close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        inhibitorFd = null;
        return true;
    }

    @Override
    public boolean isInhibited() {
        return inhibitorFd != null && inhibitorFd.getIntFileDescriptor() != -1;
    }
    
}
