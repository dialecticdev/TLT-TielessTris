/**
 * TLT - TieLessTris
 * Autori: Malacarne, Morelli, Tognetti
 * Sezione: 5CI
 */

package tielesstris;

import java.io.*;
import java.net.*;

public class Player {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private char symbol;
    private boolean connected;
    
    public Player(Socket socket, String username, PrintWriter out, BufferedReader in) {
        this.socket = socket;
        this.username = username;
        this.out = out;
        this.in = in;
        this.connected = true;
        Server.log("Player " + username + " creato, socket: " + 
                              socket.getInetAddress() + ":" + socket.getPort());
    }
    
    /**
     * Metodo per l'invio di un messaggio
     * @param message
     */
    public void send(String message) {
        if (out != null) {
            out.println(message);
            out.flush(); // Importante: forza l'invio immediato
            Server.log("Inviato a " + username + ": \"" + message + "\"");
        } else {
        	Server.log("ERRORE: out è null per " + username);
        }
    }
    
    /**
     * Metodo per la ricezione di un messaggio
     * @return
     * @throws IOException
     */
    public String receive() throws IOException {
        if (in != null) {
            String message = in.readLine();
            if (message != null) {
            	Server.log("Ricevuto da " + username + " in receive(): \"" + message + "\"");
            }
            return message;
        } else {
        	Server.log("ERRORE: in è null per " + username);
            return null;
        }
    }
    
    /**
     * Metodo per settare il simbolo del giocatore
     * @param symbol
     */
    public void setSymbol(char symbol) {
        this.symbol = symbol;
        Server.log(username + " assegnato simbolo: " + symbol);
    }
    
    /**
     * Metodo per ricavare il simbolo del giocatore
     * @return
     */
    public char getSymbol() {
        return symbol;
    }
    
    /**
     * Metodo per ricavare il nome utente del giocatore
     * @return
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Metodo per disconnettere il giocatore dalla partita
     */
    public void disconnect() {
        if (connected) {
            connected = false;
            Server.log(username + " disconnesso (flag impostato)");
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                    Server.log("Socket chiuso per " + username);
                }
            } catch (IOException e) {
            	Server.log("Errore chiusura socket per " + username + ": " + e.getMessage());
            }
        }
    }
    
    /** 
     * Metodo per ricavare la condizione di connessione al server
     * @return
     */
    public boolean isConnected() {
        if (!connected) {
            return false;
        }
        
        boolean socketConnected = false;
        try {
            socketConnected = socket != null && 
                             !socket.isClosed() && 
                             socket.isConnected() && 
                             !socket.isInputShutdown() && 
                             !socket.isOutputShutdown();
        } catch (Exception e) {
            socketConnected = false;
        }
        
        boolean result = connected && socketConnected;
        if (!result) {
        	Server.log("Connessione persa per " + username + 
                                 ": connected=" + connected + ", socketConnected=" + socketConnected);
        }
        return result;
    }
}