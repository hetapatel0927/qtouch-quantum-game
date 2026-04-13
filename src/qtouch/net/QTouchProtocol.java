package qtouch.net;

/**
 * Defines all protocol constants used for communication
 * between QTouch Client and Server.
 */
public class QTouchProtocol {

    /** Separator used between protocol fields */
    public static final String SEP = "#";

    /** Protocol: client disconnects */
    public static final String P0_DISCONNECT = "P0";

    /** Protocol: client sends game configuration to server */
    public static final String P1_SEND_GAME = "P1";

    /** Protocol: client requests latest saved game from server */
    public static final String P2_GET_GAME = "P2";

    /** Protocol: client attempts login */
    public static final String P3_LOGIN = "P3";

    /** Protocol: client requests new account creation */
    public static final String P4_SIGNUP = "P4";

    /** Protocol: client saves game configuration */
    public static final String P5_SAVE = "P5";

    /** Protocol: client loads last saved game */
    public static final String P6_LOAD = "P6";
}
