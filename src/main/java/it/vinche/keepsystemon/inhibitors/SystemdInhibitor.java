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
        if (instance == null || !instance.isConnected()) {
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
    public void inhibit(String reason) throws Exception {
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
            try {
                unhibit();
            } catch (Exception e) {
                FileDescriptorCast
                    .unsafeUsing(fd.getIntFileDescriptor())
                    .as(OutputStream.class)
                    .close();

                throw e;
            }
        }
        inhibitorFd = fd;
    }

    @Override
    public void unhibit() throws Exception {
        if (!isInhibited()) {
            return;
        }
        try {
            FileDescriptorCast
                .unsafeUsing(inhibitorFd.getIntFileDescriptor())
                .as(OutputStream.class)
                .close();
        } finally {
            inhibitorFd = null;
        }
    }

    @Override
    public boolean isInhibited() {
        return inhibitorFd != null && inhibitorFd.getIntFileDescriptor() != -1;
    }
    
    public void close() throws Exception {
        unhibit();
        BusInstance.getInstance().close();
    }
}
