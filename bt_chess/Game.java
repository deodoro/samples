package teste;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import java.util.Vector;
import java.util.Enumeration;

public class Game extends Canvas implements CommandListener {
    // commands
    static final int CMD_EXIT = 1;
    static final int CMD_START = 2;
    static final int CMD_OPTIONS = 3;
    static final int CMD_DRAW = 4;
    static final int CMD_QUIT = 5;
    static final int CMD_TAKEBACK = 6;
    static final int CMD_BACK = 7;
    static final int CMD_PROMOTION = 8;
    static final int CMD_ALLOW_TAKEBACK = 9;
    static final int CMD_DENY_TAKEBACK = 10;
    static final int CMD_ACCEPT_DRAW = 11;
    static final int CMD_REJECT_DRAW = 12;
    static final int CMD_ZLAST = 13; // must be ze last, of course
    
    // state variables
    static final int INITIALIZED = 0;
    static final int PICK_START = 1;
    static final int PICK_END = 2;
    static final int PICK_AUTO = 3;
    static final int IDLE = 4;
    
    static final int NO_CHECK = 0;
    static final int CHECK = 1;
    static final int CHECK_MATE = 2;
    
    static final int offsetX = 2;
    static final int offsetY = 1;
    static final int imageSize = 20;
    
    MIDlet midlet;
    Display dpy;
    Options options;
    Font font;
    ConnectionHandler connectionHandler;
    ProtocolHandler protocol;
    List promotionList;

    Command[] cmd;
    int gameState;
    
    int cellX;
    int cellY;
    Board board;
    int cellSize;
    Piece selected;
    
    /**
        Initializes the midlet
        MIDlet parent midlet
    */
    public Game(MIDlet midlet_) {
        Font font;

        // "global" variables
        midlet = midlet_;
        dpy = Display.getDisplay(midlet);

        font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);

        // set up commands
        cmd = new Command[CMD_ZLAST];

        cmd[CMD_EXIT] = new BoardCommand("Exit", Command.EXIT, 6, CMD_EXIT);
        cmd[CMD_OPTIONS] = new BoardCommand("Options", Command.SCREEN, 1, CMD_OPTIONS);
        cmd[CMD_START] = new BoardCommand("Start", Command.SCREEN, 2, CMD_START);
        cmd[CMD_DRAW] = new BoardCommand("Offer draw", Command.SCREEN, 1, CMD_DRAW);
        cmd[CMD_QUIT] = new BoardCommand("Resign", Command.SCREEN, 1, CMD_QUIT);
        cmd[CMD_TAKEBACK] = new BoardCommand("Takeback", Command.SCREEN, 2, CMD_TAKEBACK);
        cmd[CMD_BACK] = new BoardCommand("Back", Command.SCREEN, 2, CMD_BACK);
        cmd[CMD_PROMOTION] = new BoardCommand("Promote", Command.SCREEN, 2, CMD_PROMOTION);
        cmd[CMD_ALLOW_TAKEBACK] = new BoardCommand("Allow", Command.SCREEN, 3, CMD_ALLOW_TAKEBACK);
        cmd[CMD_DENY_TAKEBACK] = new BoardCommand("Deny", Command.SCREEN, 3, CMD_DENY_TAKEBACK);
        cmd[CMD_ACCEPT_DRAW] = new BoardCommand("Accept", Command.SCREEN, 3, CMD_ACCEPT_DRAW);
        cmd[CMD_REJECT_DRAW] = new BoardCommand("Reject", Command.SCREEN, 3, CMD_REJECT_DRAW);

        // set up the listener
        setCommandListener(this);

        // set up options screen
        options = new Options(dpy, this);
        connectionHandler = new ConnectionHandler(dpy, this);

        cellSize = (getWidth() - 4) / 8;
        board = new Board(Piece.WHITE);
        cellX = 4;
        cellY = 4;      
        selected = null;
        
        // set up initial state
/*      board = new Board(Piece.WHITE);
        setState(PICK_START);
        protocol = new ProtocolHandler();  */
        setState(INITIALIZED);
    }

    /**
        Debug output
        @param s debug message
    */
    void D(String s) {
        System.out.println(s);
    }

    /**
        Sets the game state
        @param ns New state
    */
    void setState(int ns) {
        gameState = ns;

        switch (gameState) {
        case IDLE:
        case INITIALIZED:
            removeCommand(cmd[CMD_DRAW]);
            removeCommand(cmd[CMD_QUIT]);
            removeCommand(cmd[CMD_TAKEBACK]);
            addCommand(cmd[CMD_START]);
            addCommand(cmd[CMD_EXIT]);
            break;
            
        case PICK_START:
        case PICK_END:
            addCommand(cmd[CMD_DRAW]);
            addCommand(cmd[CMD_QUIT]);
            addCommand(cmd[CMD_TAKEBACK]);
            removeCommand(cmd[CMD_START]);
            addCommand(cmd[CMD_EXIT]);
            break;

        case PICK_AUTO:
            removeCommand(cmd[CMD_DRAW]);
            removeCommand(cmd[CMD_QUIT]);
            removeCommand(cmd[CMD_START]);
            addCommand(cmd[CMD_TAKEBACK]);
            addCommand(cmd[CMD_EXIT]);
            selected = null;
            break;
        }
    }

    /**
        Handles the button commands
        @param c Command
        @param d Displayable object that fired the command
    */
    public void commandAction(Command c, Displayable d) {
        switch (((BoardCommand)c).tag) {
        case CMD_EXIT:
            midlet.notifyDestroyed();
            break;

        case CMD_OPTIONS:
            dpy.setCurrent(options);
            break;

        case CMD_DRAW:
            alertDraw();
            protocol.sendDrawRequest();
            break;
            
        case CMD_QUIT:
            protocol.sendCheckMate();
            looseGame();
            break;

        case CMD_TAKEBACK:
            protocol.sendRequestTakeback();
            alertTakeback();
            break;
            
        case CMD_START:     
            connectionHandler.connect();
            break;
            
        case CMD_BACK:
            dpy.setCurrent(this);
            break;
    
        case CMD_PROMOTION:
            int type = Piece.QUEEN;
            switch(promotionList.getSelectedIndex()) {
                case 0:
                    type = Piece.QUEEN;
                    break;
                case 1:
                    type = Piece.ROOK;
                    break;
                case 2:
                    type = Piece.BISHOP;
                    break;
                case 3:
                    type = Piece.KNIGHT;
                    break;
            }
            try {
                board.commitPromotion(selected, type);
                dpy.setCurrent(this);
                protocol.sendMove(board.getLastMove(), type);
                setState(PICK_AUTO);
                repaint();
            }
            catch (IllegalMoveException e) {
                D(e.toString());
            }
            promotionList = null;
            break;
        
        case CMD_ALLOW_TAKEBACK:
            board.undoLastMove();
            protocol.sendTakebackReply(true);
            dpy.setCurrent(this);
            setState(PICK_AUTO);
            repaint();
            break;
            
        case CMD_DENY_TAKEBACK:
            protocol.sendTakebackReply(false);
            dpy.setCurrent(this);
            break;
    
        case CMD_ACCEPT_DRAW:
            protocol.sendDrawReply(true);
            dpy.setCurrent(this);
            drawGame();
            break;
    
        case CMD_REJECT_DRAW:
            dpy.setCurrent(this);
            protocol.sendDrawReply(false);
            break;
        }
    }

    /**
        Draw the captured pieces
        @param g Graphics object
        @param y Screen line to draw
        @param e Piece enumeration
    */
    private void drawCaptures(Graphics g, int y, Enumeration e) {
        int i = 0;
        
        while (e.hasMoreElements()) {
            drawPiece(g, (Piece)e.nextElement(), i * cellSize + offsetX, y * cellSize + offsetY);
            if (++i == 8) {
                y++;
                i = 0;
            }
        }
    }
    
    /**
        Draws a piece on the screen
        @param g Graphics object
        @param p Piece to draw
        @param x Coordinate
        @param y Coordinate
    */
    private void drawPiece(Graphics g, Piece piece, int x, int y) {
        g.drawImage(ImageStore.getInstance().getImage(piece.getColor(), piece.getType()), x, y, Graphics.TOP | Graphics.LEFT);
    }
    
    /**
        Draws the piece on the board
        @param g Graphics object
        @param c Pieces color
    */
    private void drawPieces(Graphics g, int color) {        
        Piece piece;
        Cell cell;
        Enumeration pieces = board.getPieces(color);
        while (pieces.hasMoreElements()) {
            piece = (Piece)pieces.nextElement();
            cell = board.getCellViewCoords(piece.getCell());
            drawPiece(g, piece, offsetX + cell.getX() * cellSize + (cellSize - imageSize) / 2,  offsetY + cell.getY() * cellSize + (cellSize - imageSize) / 2);
        }
    }
    
    /**
        Draws the selected rectangle
        @param g Graphics object
        @param x Coordinate
        @param y Coordinate
    */
    private void drawSelected(Graphics g, int x, int y) {
        g.setColor(0xFF0000);
        g.drawRect(offsetX + x * cellSize, offsetY + y * cellSize, cellSize, cellSize);
    }
    
    /**
        Draws the highlighted rectangle
        @param g Graphics object
        @param x Coordinate
        @param y Coordinate
    */
    private void drawHighlight(Graphics g, Cell cell) {
        Cell c = board.getCellViewCoords(cell);
        g.setColor(0x00FF00);
        g.drawRect(offsetX + c.getX() * cellSize, offsetY + c.getY() * cellSize, cellSize, cellSize);
    }

    /**
        Paints the canvas
        @param g Graphics object
    */
    public void paint(Graphics g) {
        int color = Piece.WHITE;
        Enumeration pieces;
    
        g.setColor(0xFFFFFF);
        g.fillRect(0, 0, getWidth(), getHeight());

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (color == Piece.BLACK) 
                    g.setColor(0x006666);           
                else
                    g.setColor(0xCCFFFF);           
                g.fillRect(offsetX + i * cellSize,  offsetY + j * cellSize, cellSize, cellSize);
                color = 1 - color;
            }
            color = 1 - color;
        }
        
        drawPieces(g, Piece.WHITE);
        drawPieces(g, Piece.BLACK);

        if (selected != null)
            drawHighlight(g, selected.getCell());
            
        drawSelected(g, cellX, cellY);
    
        if ((gameState == INITIALIZED) || (gameState == IDLE)) {
            String str = "No connection";
            g.setColor(0xFFFFFF);
            g.fillRect(0, 3 * cellSize, getWidth(), cellSize);
            g.setColor(0xFF0000);
            g.drawString(str, (getWidth() - g.getFont().stringWidth(str)) / 2, 3 * cellSize, g.TOP | g.LEFT);
        }
    }

    /**
        Handles keypress events
        @param code Key code
    */
    public void keyPressed(int code) {
        if ((gameState == PICK_START) || (gameState == PICK_END)) {
            int game = getGameAction(code);

            switch (game) {
            case Canvas.UP:
                if (cellY > 0)
                    cellY--;
                break;

            case Canvas.DOWN:
                if (cellY < 7)
                    cellY++;
                break;

            case Canvas.LEFT:
                if (cellX > 0)
                    cellX--;
                break;

            case Canvas.RIGHT:
                if (cellX < 7)
                    cellX++;
                break;

            case Canvas.FIRE:
                switch(gameState) {
                case PICK_START:
                    Piece candidate = board.getPiece(getCoords(cellX, cellY));
                    if ((candidate != null) && (candidate.getColor() == board.getMyColor())) {
                        selected = candidate;
                        setState(PICK_END);
                    }
                    break;
                case PICK_END:
                    if (selected.inCell(getCoords(cellX, cellY))) {
                        selected = null;
                        setState(PICK_START);
                    }
                    else {
                        if (board.doMove(selected, getCoords(cellX, cellY))) {
                            if (selected.shouldPromote())
                                showPromotionForm();
                            else {
                                protocol.sendMove(board.getLastMove());
                                setState(PICK_AUTO);
                            }
                        }
                    }
                    break;
                }
                break;
                
            default:
                return;
            }

            repaint();
        }               
    }
    
    /**
        Protocol callback, moves a piece when commanded from the remote side
        @param move Movement in algebric coordinates
    */
    public void doMove(String move) {
        board.doMove(move);
        repaint();
        if (board.checkForChecks()) {
            if (board.detectMate(board.getMyColor()))
                looseGame();
            else
                alertCheck();
        }
        setState(PICK_START);
    }
        
    /**
        Protocol callback, moves a promoting pawn when commanded from the remote side
        @param move Movement in algebric coordinates
        @param type Pawn promotion type
    */
    public void doPromotionMove(String move, int type) {
        board.doMove(move, type);
        repaint();
        if (board.checkForChecks()) {
            if (board.detectMate(board.getMyColor()))
                looseGame();
            else
                alertCheck();
        }
        setState(PICK_START);
    }
    
    /**
        Protocol callback, performs a takeback or tells the player it was denied        
        @param anwer True if the takeback was accepted
    */
    public void replyTakeback(boolean answer) {
        if (answer) {
            board.undoLastMove();
            setState(PICK_START);
            repaint();
            alertTakebackSucceeded();
        }
        else 
            alertTakebackFailed();
    }
        
    /**
        Protocol callback, starts a game as server side
        @param color Player's piece color
        @param handler Remote communication protocol handler
    */
    public void startServerGame(int color, ProtocolHandler handler) {
        protocol = handler;
        protocol.setGame(this);
        protocol.sendStart(color == Piece.WHITE ? Piece.BLACK : Piece.WHITE);
        changeColor(color);
    }
    
    /**
        Protocol callback, starts a game as client side
        @param color Player's piece color
        @param handler Remote communication protocol handler
    */
    public void startClientGame(int color, ProtocolHandler handler) {
        protocol = handler;
        protocol.setGame(this);
        repaint();
    }
    
    /**
        Changes the game color (creates a new board and sets the pieces)
        @param color Player's piece color
    */
    public void changeColor(int color) {
        dpy.setCurrent(this);
        board = new Board(color);
        if (color == Piece.WHITE)
            setState(PICK_START);
        else
            setState(PICK_AUTO);
        repaint();
    }
    
    /**
        Protocol callback, cancels a game initialization
    */
    public void cancelGame() {
        setState(INITIALIZED);
        dpy.setCurrent(this);
        repaint();
    }
    
    /**
        Tells the player it's  a win
    */
    public void winGame() {
        Alert alert = new Alert("Check Mate");
        alert.setString("You win");
        alert.setTimeout(Alert.FOREVER);
        dpy.setCurrent(alert, this);
        setState(IDLE);
    }
    
    /**
        Tells the player he lost the game
    */
    private void looseGame() {
        Alert alert = new Alert("Check Mate");
        alert.setString("You loose");
        alert.setTimeout(Alert.FOREVER);
        dpy.setCurrent(alert, this);
        setState(IDLE);
    }
    
    /**
        Tells the player it's  a draw
    */
    private void drawGame() {
        Alert alert = new Alert("Draw");
        alert.setString("It's a draw");
        alert.setTimeout(Alert.FOREVER);
        dpy.setCurrent(alert, this);
        setState(IDLE);
    }
    
    /**
        Tells the player a takeback was requested to the remote side
    */
    private void alertTakeback() {
        Alert alert = new Alert("Takeback");
        alert.setString("A takeback request has been sent, please wait");
        alert.setTimeout(3000);
        dpy.setCurrent(alert, this);
    }
    
    /**
        Tells the player a draw was requested to the remote side
    */
    private void alertDraw() {
        Alert alert = new Alert("Draw offer");
        alert.setString("A draw request has been sent, please wait");
        alert.setTimeout(3000);
        dpy.setCurrent(alert, this);
    }
    
    /**
        Tells the player his king is in check
    */
    private void alertCheck() {
        Alert alert = new Alert("Check");
        alert.setString("Your king is in check");
        alert.setTimeout(2000);
        dpy.setCurrent(alert, this);
    }

    /**
        Tells the player a takeback was accepted by the remote side
    */
    private void alertTakebackSucceeded() {
        Alert alert = new Alert("Takeback");
        alert.setString("Your last move was undoed");
        alert.setTimeout(2000);
        dpy.setCurrent(alert, this);
    }

    /**
        Tells the player a takeback was denied by the remote side
    */
    private void alertTakebackFailed() {
        Alert alert = new Alert("Takeback");
        alert.setString("Your opponent denied your takeback request");
        alert.setTimeout(3000);
        dpy.setCurrent(alert, this);
    }

    /**
        Tells the player a draw was denied by the remote side
    */
    private void alertDrawFailed() {
        Alert alert = new Alert("Draw offer");
        alert.setString("Your opponent denied your draw request");
        alert.setTimeout(3000);
        dpy.setCurrent(alert, this);
    }
    
    /**
        Asks a player if he accepts the opposition's  takeback request
    */
    public void requestTakeback() {
        Alert alert = new Alert("Takeback");
        alert.setString("Your opponent is asking for a takeback, would you allow it ?");
        alert.addCommand(cmd[CMD_ALLOW_TAKEBACK]);
        alert.addCommand(cmd[CMD_DENY_TAKEBACK]);
        alert.setCommandListener(this);
        dpy.setCurrent(alert, this);
    }
    
    /**
        Asks a player for what piece he wants to promote his pawn to
    */
    private void showPromotionForm() {
        promotionList = new List("Pick a promotion", Choice.EXCLUSIVE);
        promotionList.append("Queen", ImageStore.getInstance().getImage(board.getMyColor(), Piece.QUEEN));
        promotionList.append("Rook", ImageStore.getInstance().getImage(board.getMyColor(), Piece.ROOK));
        promotionList.append("Bishop", ImageStore.getInstance().getImage(board.getMyColor(), Piece.BISHOP));
        promotionList.append("Knight", ImageStore.getInstance().getImage(board.getMyColor(), Piece.KNIGHT));
        promotionList.addCommand(cmd[CMD_PROMOTION]);
        promotionList.setCommandListener(this);
        dpy.setCurrent(promotionList);
    }
    
    /**
        Asks a player if he accepts a draw offer from the remote side
    */
    public void requestDraw() {
        Alert alert = new Alert("Draw offer");
        alert.setString("Your opponent is offering you a draw on this game, would you accept it ?");
        alert.addCommand(cmd[CMD_ACCEPT_DRAW]);
        alert.addCommand(cmd[CMD_REJECT_DRAW]);
        alert.setCommandListener(this);
        dpy.setCurrent(alert, this);
    }
    
    /**
        Draw reply callback, it ends the game or tells the user the draw request was denied
    */
    public void replyDraw(boolean value) {
        if (value) 
            drawGame();
        else
            alertDrawFailed();
    }
    
    /**
        Gets the normalized coordinates for a cell
    */
    private String getCoords(int x, int y) {
        if (board.getMyColor() == Piece.WHITE) 
            return Cell.AlgebricCoord(x, y);
        else
            return Cell.AlgebricCoord(7 - x, 7 - y);
    }
    
    /**
        Command object wrapper
    */
    class BoardCommand extends Command {
        int tag;

        BoardCommand(String label, int type, int pri, int tag_) {
            super(label, type, pri);
            tag = tag_;
        }
    }   

}        