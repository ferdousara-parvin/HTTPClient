package Helpers;

public enum PacketType {
    DATA(0),
    ACK(1),
    SYN(2),
    SYN_ACK(3),
    NAK(4),
    FIN(5);

    public int value;
    PacketType(int i) {
        value = i;
    }
}




