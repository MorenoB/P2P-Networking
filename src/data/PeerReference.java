package data;

/**
 *
 * @author Moreno
 */
public class PeerReference {

    private final int Id;
    private final String address;
    private final int portNumber;

    public PeerReference(int Id, String address, int portNumber) {
        this.Id = Id;
        this.address = address;
        this.portNumber = portNumber;
    }

    public String getAddress() {
        return address;
    }

    public int getId() {
        return Id;
    }

    public int getPortNumber() {
        return portNumber;
    }

}
