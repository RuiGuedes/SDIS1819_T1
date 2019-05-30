package chord;

import java.util.List;

/**
 * Query class
 */
public class Query {

    /**
     * Return the neighbor nodes of an id
     * @param fileId Id to search
     * @return Array with predecessor, target and successor addresses
     */
    public static List<CustomInetAddress> findTargetAddress(long fileId) {
        final Node current = Chord.getNode();

        CustomInetAddress target = current.getAddress();
        CustomInetAddress predecessor = current.getPredecessor();
        CustomInetAddress successor = current.getSuccessor();

        if (predecessor == null && successor == null)   return List.of(target);

        if (Utilities.belongsToInterval(fileId,
                Utilities.hashCode(predecessor.getHostAddress(),predecessor.getPort()),
                Utilities.hashCode(target.getHostAddress(),target.getPort())))
            return nonNullsList(target, predecessor, successor);

        predecessor = current.findPredecessor(fileId);

        if(Utilities.sendRequest(predecessor,"ONLINE").equals("TRUE"))
            target = Utilities.addressRequest(predecessor, "YOUR_SUCCESSOR");

        if(target != null && Utilities.sendRequest(target,"ONLINE").equals("TRUE"))
            successor = Utilities.addressRequest(target, "YOUR_SUCCESSOR");

        return nonNullsList(target, predecessor, successor);
    }

    private static List<CustomInetAddress> nonNullsList(CustomInetAddress target, CustomInetAddress predecessor,
                                                        CustomInetAddress successor) {
        if (target != null && successor != null) {
            return List.of(target, predecessor, successor);
        }
        else if (target != null) {
            return List.of(target, predecessor);
        }
        else {
            return List.of(predecessor, successor);
        }
    }
}