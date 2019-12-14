package tracker;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import common.Utils;


public class MainTracker {

    public static void main(String[] args) {
        Utils.setCodeBase(ITracker.class);

        // Entrada al jar del trackker
        // Inicializa un registry de rmi y instancia un tracker
        Tracker server = null;
        try {
            server = new Tracker();
            ITracker remote = (ITracker) UnicastRemoteObject.exportObject(server, 2789);

            Registry registry = LocateRegistry.createRegistry(2789);
//			Registry registry = LocateRegistry.getRegistry(2789);
            registry.rebind("tracker", remote);

            System.out.println("Tracker listo, presione ENTER para terminar");
            System.in.read();
            System.out.println("La sesion ha finalizado.   :( ");

            registry.unbind("tracker");
            UnicastRemoteObject.unexportObject(server, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
