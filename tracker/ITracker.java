package tracker;

import client.Peer;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;


public interface ITracker extends Remote {

    /*
     * La interfaz ITracker nos permite establecer la conexión entre peer y tracker,
     * contiene los métodos del objeto Tracker.
     * Esto permitirá al peer interaccionar con el tracker,
     * pudiendo acceder a información de otros peers
     *
     */
    public void addPeer(Peer peer) throws RemoteException;

    public void deletePeer(Peer peer) throws RemoteException;

    public List<Peer> getPeerList() throws RemoteException;

}
