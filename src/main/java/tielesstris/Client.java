/**
 * TLT - TieLessTris
 * Autori: Malacarne, Morelli, Tognetti
 * Sezione: 5CI
 * 
 * 1) Server
 * - WELCOME <simbolo> <nAvversario>
 * - WAITING
 * - VALID_MOVE <colonna> <riga> <simbolo>
 * - BOARD [sim1, simb2, simb3, ...]
 * - WINNER <simboloWinner>
 * 
 * 2) Client
 * - CONNECT <username>
 * - MOVE <riga> <colonna> (0-2)
 * - BOARD?
 * - QUIT
 * - PING
 * 
 */

package tielesstris;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Scanner scanner;
    private char mySymbol;
    private char[][] board = new char[3][3];
    private volatile boolean gameActive = true;
    
    /**
     * Metodo per la connessione al server di un client
     * @param host
     * @param port
     * @param username
     * @throws IOException
     */
    public void connect(String host, int port, String username) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        scanner = new Scanner(System.in);
        
        // Inizializza board vuota
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = '.';
            }
        }
        
        // Invia username al server
        out.println("CONNECT " + username);
        System.out.println("Connessione stabilita al server " + host + ":" + port);
        
        // Thread per ricevere messaggi dal server
        Thread receiver = new Thread(new ServerReceiver());
        receiver.start();
        
        // Thread per inviare comandi
        Thread sender = new Thread(new UserInputHandler());
        sender.start();
        
        // Attendi la fine dei thread
        try {
            receiver.join();
            sender.join();
        } catch (InterruptedException e) {
            System.out.println("Client interrotto");
        }
        
        disconnect();
    }
    
    /**
     * Classe per la ricezione dal server dei messaggi di gioco
     */
    class ServerReceiver implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while (gameActive && (message = in.readLine()) != null) {
                    handleServerMessage(message);
                }
            } catch (IOException e) {
                if (gameActive) {
                    System.out.println("Disconnesso dal server: " + e.getMessage());
                }
            } finally {
                gameActive = false;
                scanner.close();
            }
        }
        
        private void handleServerMessage(String msg) {
            System.out.println("[Server] " + msg);
            
            String[] parts = msg.split(" ");
            if (parts.length == 0) return;
            
            String command = parts[0];
            
            switch(command) {
                case "WELCOME":
                    if (parts.length >= 3) {
                        mySymbol = parts[1].charAt(0);
                        System.out.println("\n=== BENVENUTO! ===");
                        System.out.println("Sei il giocatore: " + mySymbol);
                        System.out.println("Avversario: " + parts[2]);
                        System.out.println("===================\n");
                    }
                    break;
                    
                case "WAITING":
                    System.out.println("In attesa di un avversario...");
                    break;
                    
                case "BOARD":
                    if (parts.length >= 2) {
                        updateBoard(parts[1]);
                        printBoard();
                    }
                    break;
                    
                case "TURN":
                    if (parts.length >= 2) {
                        char turnPlayer = parts[1].charAt(0);
                        System.out.println("\n=== TURNO ATTIVO: Giocatore " + turnPlayer + " ===");
                        
                        if (turnPlayer == mySymbol) {
                            System.out.println(">>> È il TUO turno! <<<");
                            System.out.println("Inserisci: MOVE <riga> <colonna> (0-2)");
                            System.out.print(">>> ");
                        } else {
                            System.out.println("--- Turno dell'avversario, attendi... ---");
                        }
                    }
                    break;
                    
                case "VALID_MOVE":
                    System.out.println("Mossa valida effettuata");
                    break;
                    
                case "REMOVE":
                    if (parts.length >= 3) {
                        System.out.println("\n>>> ATTENZIONE: Mossa rimossa in (" + 
                                          parts[1] + "," + parts[2] + ") <<<");
                    }
                    break;
                    
                case "WINNER":
                    if (parts.length >= 2) {
                        System.out.println("\n=================================");
                        if (parts[1].charAt(0) == mySymbol) {
                            System.out.println("***  COMPLIMENTI! HAI VINTO!  ***");
                        } else {
                            System.out.println("***       HAI PERSO           ***");
                        }
                        System.out.println("=================================\n");
                        gameActive = false;
                    }
                    break;
                    
                case "GAME_OVER":
                    System.out.println("Partita terminata");
                    gameActive = false;
                    break;
                    
                case "OPPONENT_DISCONNECTED":
                    System.out.println("\n>>> L'avversario si è disconnesso. Partita terminata.");
                    gameActive = false;
                    break;
                    
                case "ERROR":
                    System.out.println("ERRORE: " + msg.substring(6));
                    break;
                    
                case "PONG":
                    // Risposta al ping, nessuna azione necessaria
                    break;
                    
                default:
                    System.out.println("Messaggio non riconosciuto dal server: " + msg);
            }
        }
    }
    
    /**
     * Metodo per la gestione dell'input dell'utente
     */
    class UserInputHandler implements Runnable {
        @Override
        public void run() {
            System.out.println("\nComandi disponibili:");
            System.out.println("- MOVE <riga> <colonna>  (es: MOVE 0 1)");
            System.out.println("- BOARD?                  (richiede stato board)");
            System.out.println("- QUIT                    (esce dal gioco)");
            System.out.println("- PING                    (test connessione)");
            System.out.println("===============================================\n");
            
            while (gameActive) {
                try {
                    if (System.in.available() > 0 || scanner.hasNextLine()) {
                        String input = scanner.nextLine().trim();
                        
                        if (input.equalsIgnoreCase("QUIT")) {
                            out.println("QUIT");
                            gameActive = false;
                            break;
                        } else if (input.equalsIgnoreCase("BOARD?")) {
                            out.println("BOARD?");
                        } else if (input.equalsIgnoreCase("PING")) {
                            out.println("PING");
                        } else if (input.startsWith("MOVE ")) {
                            if (gameActive) {
                                out.println(input);
                            } else {
                                System.out.println("Partita non attiva");
                            }
                        } else if (!input.isEmpty()) {
                            System.out.println("Comando non valido. Usa: MOVE <r> <c>, BOARD?, QUIT, PING");
                        }
                    } else {
                        Thread.sleep(100); // Evita CPU spinning
                    }
                } catch (IOException e) {
                    System.out.println("Errore input: " + e.getMessage());
                    gameActive = false;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    gameActive = false;
                }
            }
        }
    }
    
    /**
     * Metodo per l'aggiornamento del tabellone di gioco per ciascun client
     * @param boardStr
     */
    private void updateBoard(String boardStr) {
        try {
            if (boardStr.length() != 9) {
                System.out.println("ERRORE: Stringa board di lunghezza errata: " + boardStr.length());
                return;
            }
            
            int index = 0;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    board[i][j] = boardStr.charAt(index++);
                }
            }
        } catch (Exception e) {
            System.out.println("Errore nell'aggiornamento della board: " + e.getMessage());
            System.out.println("Stringa board ricevuta: " + boardStr);
        }
    }
    
    /**
     * Metodo per la stampa del tabellone di gioco
     */
    private void printBoard() {
        System.out.println("\n   TABELLONE DI GIOCO");
        System.out.println("   +---+---+---+");
        for (int i = 0; i < 3; i++) {
            System.out.print(" " + i + " | ");
            for (int j = 0; j < 3; j++) {
                char c = board[i][j];
                if (c == '.') {
                    System.out.print("  | ");
                } else {
                    System.out.print(c + " | ");
                }
            }
            System.out.println("\n   +---+---+---+");
        }
        System.out.println("     0   1   2");
        System.out.println("  Tu sei: " + mySymbol + "\n");
    }
    
    /**
     * Metodo per la disconnessione dalla partita
     */
    private void disconnect() {
        gameActive = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignora errori di chiusura
        }
        
        if (scanner != null) {
            scanner.close();
        }
        
        System.out.println("Disconnesso");
    }
    
    /**
     * Metodo main del client
     * @param args
     */
    public static void main(String[] args) {
    	Client client = new Client();
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== TIELESS TRIS CLIENT ===");
        System.out.print("Inserisci username: ");
        String username = scanner.nextLine();
        
        String host = "localhost";
        int port = 12345;
        
        if (args.length > 0) {
            host = args[0];
        }
        if (args.length > 1) {
            port = Integer.parseInt(args[1]);
        }
        
        try {
            client.connect(host, port, username);
        } catch (IOException e) {
            System.err.println("Impossibile connettersi al server " + host + ":" + port);
            System.err.println("Errore: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }
}