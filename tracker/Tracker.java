package salva.mario.tracker;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;


import salva.mario.client.Peer;
import salva.mario.common.*;

public class Tracker implements ITracker {

    private List<Peer> peerList = new ArrayList<>();

    public Tracker() throws RemoteException {
    }

    /**
     * Login
     * @param peer
     */
    @Override
    public void addPeer(Peer peer) {
        Peer listItem = null;

        // Remove peer with the same name
        for (Peer listPeer : peerList) {
            if (listPeer.getName().equals(peer.getName())) {
                listItem = listPeer;
            }
        }

        try {
            peerList.remove(listItem);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Add peer
        peerList.add(peer);
        System.out.println("Client registered: " + peer.getName());
        System.out.println("Current peers: " + peerList);

    }

    /**
     * Desloguearse
     */
    @Override
    public void deletePeer(Peer peer) {
        // Remove peer with the same name
        Peer listItem = null;

        for (Peer listPeer : peerList) {
            if (listPeer.getName().equals(peer.getName())) {
                listItem = listPeer;
            }
        }

        try {
            peerList.remove(listItem);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Client logged off: " + peer.getName());
        System.out.println("Current peers: " + peerList);
    }

    @Override
    public List<Peer> getPeerList() {
        return peerList;
    }

}
