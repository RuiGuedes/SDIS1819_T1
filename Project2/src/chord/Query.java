package chord;

public class Query {

    /**
     * Return the neighbor nodes of an id
     * @param current Current node
     * @param fileId Id to search
     * @return Array with predecessor, target and successor addresses
     */
    public static CustomInetAddress[] findTargetAddress(Node current, long fileId) {
        CustomInetAddress target = current.getAddress();
        CustomInetAddress predecessor = current.getPredecessor();
        CustomInetAddress successor = current.getSuccessor();

        if (Utilities.belongsToInterval(fileId,
                Utilities.hashCode(predecessor.getHostAddress(),predecessor.getPort()),
                Utilities.hashCode(target.getHostAddress(),target.getPort())))
            return new CustomInetAddress[] {predecessor, target, successor};

        predecessor = current.findPredecessor(fileId);

        if(Utilities.sendRequest(predecessor,"ONLINE").equals("TRUE"))
            target = Utilities.addressRequest(predecessor, "YOUR_SUCCESSOR");

        if(target != null && Utilities.sendRequest(target,"ONLINE").equals("TRUE"))
            successor = Utilities.addressRequest(target, "YOUR_SUCCESSOR");

        return new CustomInetAddress[] {predecessor, target, successor};
    }
}