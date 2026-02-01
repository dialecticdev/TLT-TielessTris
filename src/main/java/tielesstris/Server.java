/**
 * TLT - TieLessTris
 * Autori: Malacarne, Morelli, Tognetti
 * Sezione: 5CI
 */

package tielesstris;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

import tielesstris.utils.GameRunner;

public class Server {
    private ServerSocket serverSocket;
    public List<GameSession> activeGames = new ArrayList<>();
    private volatile Player waitingPlayer = null;
    
    // Logger
    public static void log(String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        String timestamp = sdf.format(new Date());
        String threadName = Thread.currentThread().getName();
        System.out.println("[" + timestamp + "][" + threadName + "] " + message);
    }
    
    /**
     * Metodo per l'avvio del server
     * @param port
     * @throws IOException
     */
    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        log("Server avviato sulla porta " + port);
        
        while (true) {
            Socket clientSocket = serverSocket.accept();
            String clientInfo = clientSocket.getInetAddress() + ":" + clientSocket.getPort();
            log("Nuova connessione da: " + clientInfo);
            
            Thread clientThread = new Thread(new ClientHandler(clientSocket));
            clientThread.start();
        }
    }
    
    /**
     * Classe per la gestione del singolo client tramite un Thread apposito.
     */
    class ClientHandler implements Runnable {
        private Socket socket;
        
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            String clientInfo = socket.getInetAddress() + ":" + socket.getPort();
            log("Thread ClientHandler avviato per: " + clientInfo);
            
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                
                // Leggi messaggio di connessione
                String connectMsg = in.readLine();
                if (connectMsg == null) {
                    log("Client " + clientInfo + " disconnesso senza inviare messaggio");
                    socket.close();
                    return;
                }
                
                log("Ricevuto da " + clientInfo + ": \"" + connectMsg + "\"");
                
                if (!connectMsg.startsWith("CONNECT ")) {
                    String errorMsg = "ERROR Invalid connection message";
                    out.println(errorMsg);
                    log("Inviato a " + clientInfo + ": \"" + errorMsg + "\"");
                    socket.close();
                    return;
                }
                
                String username = connectMsg.substring(8).trim();
                if (username.isEmpty()) {
                    username = "Guest_" + (System.currentTimeMillis() % 10000);
                }
                
                Player player = new Player(socket, username, out, in);
                log("Player creato: " + username + " (" + clientInfo + ")");
                
                synchronized (Server.this) {
                    if (waitingPlayer == null) {
                        // Primo giocatore in attesa
                        waitingPlayer = player;
                        String waitingMsg = "WAITING For opponent...";
                        player.send(waitingMsg);
                        log(username + " messo in lista d'attesa");
                    } else {
                        // Secondo giocatore: inizia partita
                        Player player1 = waitingPlayer;
                        Player player2 = player;
                        waitingPlayer = null;
                        
                        GameSession game = new GameSession();
                        game.setPlayers(player1, player2);
                        activeGames.add(game);
                        
                        Thread gameThread = new Thread(new GameRunner(game));
                        gameThread.start();
                        
                        log("Partita iniziata: " + player1.getUsername() + " vs " + player2.getUsername());
                    }
                }
                
            } catch (IOException e) {
                log("Errore nel ClientHandler per " + clientInfo + ": " + e.getMessage());
                try {
                    socket.close();
                } catch (IOException ex) {
                    log("Errore nella chiusura del socket: " + ex.getMessage());
                }
            }
        }
    }
   
    
    
    /**
     * Metodo main, gestisce l'avvio del server
     * @param args
     */
    public static void main(String[] args) {
    	System.out.println("      _____                    _____        _____          \n"
    			+ "     /\\    \\                  /\\    \\      /\\    \\         \n"
    			+ "    /::\\    \\                /::\\____\\    /::\\    \\        \n"
    			+ "    \\:::\\    \\              /:::/    /    \\:::\\    \\       \n"
    			+ "     \\:::\\    \\            /:::/    /      \\:::\\    \\      \n"
    			+ "      \\:::\\    \\          /:::/    /        \\:::\\    \\     \n"
    			+ "       \\:::\\    \\        /:::/    /          \\:::\\    \\    \n"
    			+ "       /::::\\    \\      /:::/    /           /::::\\    \\   \n"
    			+ "      /::::::\\    \\    /:::/    /           /::::::\\    \\  \n"
    			+ "     /:::/\\:::\\    \\  /:::/    /           /:::/\\:::\\    \\ \n"
    			+ "    /:::/  \\:::\\____\\/:::/____/           /:::/  \\:::\\____\\\n"
    			+ "   /:::/    \\::/    /\\:::\\    \\          /:::/    \\::/    /\n"
    			+ "  /:::/    / \\/____/  \\:::\\    \\        /:::/    / \\/____/ \n"
    			+ " /:::/    /            \\:::\\    \\      /:::/    /          \n"
    			+ "/:::/    /              \\:::\\    \\    /:::/    /           \n"
    			+ "\\::/    /                \\:::\\    \\   \\::/    /            \n"
    			+ " \\/____/                  \\:::\\    \\   \\/____/             \n"
    			+ "                           \\:::\\    \\                      \n"
    			+ "                            \\:::\\____\\                     \n"
    			+ "                             \\::/    /                     \n"
    			+ "                              \\/____/                      \n"
    			+ "\n\t TieLessTris \t\n"
    			+ "\t Autori: Malacarne, Morelli, Tognetti \t\n"
    			+ "\t Sezione: 5CI \t\n");
        Server server = new Server();
        try {
            log("Avvio server TielessTris...");
            int port = 12345;
            if (args.length > 0) {
                port = Integer.parseInt(args[0]);
            }
            server.start(port);
        } catch (IOException e) {
            log("Errore avvio server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}