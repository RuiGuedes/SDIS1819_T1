import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMI extends Remote {
    String backup(String filename, Integer replication_degree) throws IOException;
    String restore(String filename) throws RemoteException;
    String delete(String filename) throws RemoteException;
    String reclaim(Integer disk_space) throws RemoteException;
    String state() throws RemoteException;
}
