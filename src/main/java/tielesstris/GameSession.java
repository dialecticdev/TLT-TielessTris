/**
 * TLT - TieLessTris
 * Autori: Malacarne, Morelli, Tognetti
 * Sezione: 5CI
 */

package tielesstris;

import java.util.*;

public class GameSession {
    private char[][] board = new char[3][3];
    private LinkedList<Move> moveHistory = new LinkedList<>();
    private Player playerX, playerO;
    private char currentPlayer = 'X';
    private boolean gameOver = false;
    private String winner = null;
    
    class Move {
        int row, col;
        char symbol;
        
        Move(int r, int c, char s) {
            row = r; 
            col = c; 
            symbol = s;
        }
    }
    
    public GameSession() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = '.';
            }
        }
        Server.log("Nuova GameSession creata, board inizializzata");
    }
    
    public synchronized String processMove(Player player, int row, int col) {
        String playerName = player.getUsername();
        char symbol = player.getSymbol();
        
        Server.log("ProcessMove chiamato per " + playerName + 
                             " (" + symbol + ") in (" + row + "," + col + ")");
        
        if (gameOver) {
        	Server.log("Partita già finita per " + playerName);
            return "ERROR Game already finished";
        }
        
        if (row < 0 || row >= 3 || col < 0 || col >= 3) {
        	Server.log("Coordinate non valide da " + playerName + ": (" + row + "," + col + ")");
            return "ERROR Invalid coordinates";
        }
        
        if (player.getSymbol() != currentPlayer) {
        	Server.log("Non è il turno di " + playerName + 
                                 " (turno attuale: " + currentPlayer + ")");
            return "ERROR Not your turn";
        }
        
        if (board[row][col] != '.') {
        	Server.log("Cella già occupata per " + playerName + 
                                 " in (" + row + "," + col + ")");
            return "ERROR Cell already occupied";
        }
        
        // Controlla se il giocatore ha già 3 simboli sulla griglia
        if (hasThreeSymbols(player.getSymbol())) {
        	Server.log(playerName + " ha già 3 simboli, rimuovo il più vecchio");
            
            // Trova e rimuovi la mossa più vecchia di questo giocatore
            Move oldestMove = findOldestMoveByPlayer(player.getSymbol());
            if (oldestMove != null) {
                // Rimuovi dalla griglia
                board[oldestMove.row][oldestMove.col] = '.';
                // Rimuovi dalla history
                moveHistory.remove(oldestMove);
                
                Server.log("Mossa rimossa: (" + oldestMove.row + "," + oldestMove.col + 
                                     ") di " + oldestMove.symbol);
                
                // Notifica rimozione
                notifyRemoval(oldestMove);
            } else {
            	Server.log("ATTENZIONE: Non trovata mossa da rimuovere per " + playerName);
            }
        }
        
        // Fai la nuova mossa
        board[row][col] = player.getSymbol();
        Move newMove = new Move(row, col, player.getSymbol());
        moveHistory.add(newMove);
        
        Server.log("Mossa registrata per " + playerName + 
                             " in (" + row + "," + col + "), history size: " + moveHistory.size());
        
        // Aggiorna la board per TUTTI i giocatori
        broadcastBoard();
        
        // Controlla vittoria
        if (checkWin(row, col, player.getSymbol())) {
            gameOver = true;
            winner = String.valueOf(player.getSymbol());
            Server.log("VITTORIA per " + playerName + " (" + symbol + ")");
            return "WINNER " + player.getSymbol();
        }
        
        // Cambia turno
        currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
        Server.log("Turno cambiato a: " + currentPlayer);
        
        // Aggiorna il turno per TUTTI i giocatori
        broadcastTurn();
        
        return "VALID_MOVE " + row + " " + col + " " + player.getSymbol();
    }
    
    // Controlla se un giocatore ha già 3 simboli sulla griglia
    private boolean hasThreeSymbols(char symbol) {
        int count = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == symbol) {
                    count++;
                    if (count >= 3) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    // Trova la mossa più vecchia di un giocatore specifico
    private Move findOldestMoveByPlayer(char symbol) {
        for (Move move : moveHistory) {
            if (move.symbol == symbol) {
                return move;
            }
        }
        return null;
    }
    
    private boolean checkWin(int row, int col, char symbol) {
        // Controlla riga
        boolean rowWin = true;
        for (int c = 0; c < 3; c++) {
            if (board[row][c] != symbol) {
                rowWin = false;
                break;
            }
        }
        
        // Controlla colonna
        boolean colWin = true;
        for (int r = 0; r < 3; r++) {
            if (board[r][col] != symbol) {
                colWin = false;
                break;
            }
        }
        
        // Controlla diagonale principale
        boolean diag1Win = true;
        if (row == col) {
            for (int i = 0; i < 3; i++) {
                if (board[i][i] != symbol) {
                    diag1Win = false;
                    break;
                }
            }
        } else {
            diag1Win = false;
        }
        
        // Controlla diagonale secondaria
        boolean diag2Win = true;
        if (row + col == 2) {
            for (int i = 0; i < 3; i++) {
                if (board[i][2 - i] != symbol) {
                    diag2Win = false;
                    break;
                }
            }
        } else {
            diag2Win = false;
        }
        
        boolean win = rowWin || colWin || diag1Win || diag2Win;
        if (win) {
        	Server.log("Vittoria rilevata per " + symbol + 
                                 " (rowWin=" + rowWin + ", colWin=" + colWin + 
                                 ", diag1Win=" + diag1Win + ", diag2Win=" + diag2Win + ")");
        }
        
        return win;
    }
    
    private void notifyRemoval(Move move) {
        if (playerX != null && playerX.isConnected()) {
            playerX.send("REMOVE " + move.row + " " + move.col);
            Server.log("Inviato REMOVE a " + playerX.getUsername() + 
                                 " per (" + move.row + "," + move.col + ")");
        }
        if (playerO != null && playerO.isConnected()) {
            playerO.send("REMOVE " + move.row + " " + move.col);
            Server.log("Inviato REMOVE a " + playerO.getUsername() + 
                                 " per (" + move.row + "," + move.col + ")");
        }
    }
    
    public void broadcastBoard() {
        String boardState = getBoardState();
        if (playerX != null && playerX.isConnected()) {
            playerX.send("BOARD " + boardState);
            Server.log("Inviato BOARD a " + playerX.getUsername() + ": " + boardState);
        }
        if (playerO != null && playerO.isConnected()) {
            playerO.send("BOARD " + boardState);
            Server.log("Inviato BOARD a " + playerO.getUsername() + ": " + boardState);
        }
    }
    
    public void broadcastTurn() {
        String turnMessage = "TURN " + currentPlayer;
        if (playerX != null && playerX.isConnected()) {
            playerX.send(turnMessage);
            Server.log("Inviato " + turnMessage + " a " + playerX.getUsername());
        }
        if (playerO != null && playerO.isConnected()) {
            playerO.send(turnMessage);
            Server.log("Inviato " + turnMessage + " a " + playerO.getUsername());
        }
    }
    
    public String getBoardState() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                sb.append(board[i][j]);
            }
        }
        return sb.toString();
    }
    
    // Metodo per ottenere statistiche del gioco (utile per debug)
    public String getGameStatus() {
        int countX = 0;
        int countO = 0;
        
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == 'X') countX++;
                if (board[i][j] == 'O') countO++;
            }
        }
        
        return "Turno: " + currentPlayer + 
               ", X: " + countX + " simboli" + 
               ", O: " + countO + " simboli" + 
               ", Mosse totali: " + moveHistory.size();
    }
    
    public void setPlayers(Player p1, Player p2) {
        this.playerX = p1;
        this.playerO = p2;
        
        // Assegna simboli
        p1.setSymbol('X');
        p2.setSymbol('O');
        
        // Invia messaggi di benvenuto
        p1.send("WELCOME X " + p2.getUsername());
        p2.send("WELCOME O " + p1.getUsername());
        
        // Invia stato iniziale della board
        String boardState = getBoardState();
        p1.send("BOARD " + boardState);
        p2.send("BOARD " + boardState);
        
        // CORREZIONE: Entrambi devono sapere che è il turno di X
        p1.send("TURN X");
        p2.send("TURN X");
        
        Server.log("Players assegnati: " + 
                             p1.getUsername() + "(X), " + p2.getUsername() + "(O)");
        Server.log("Inviato TURN X a entrambi i giocatori");
    }
    
    public void playerDisconnected(Player player) {
    	Server.log("Player disconnesso: " + player.getUsername());
        gameOver = true;
        Player opponent = (player == playerX) ? playerO : playerX;
        if (opponent != null && opponent.isConnected()) {
            opponent.send("OPPONENT_DISCONNECTED");
            opponent.send("GAME_OVER");
            Server.log("Notificato disconnessione a " + opponent.getUsername());
        }
    }
    
    public char getCurrentPlayer() {
        return currentPlayer;
    }
    
    public boolean isGameOver() {
        return gameOver;
    }
    
    public Player getPlayerX() {
        return playerX;
    }
    
    public Player getPlayerO() {
        return playerO;
    }
    
    public Player[] getPlayers() {
        return new Player[]{playerX, playerO};
    }
}