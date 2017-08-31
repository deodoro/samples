package teste;

import java.util.Vector;
import java.util.Enumeration;

public class Board {
    
    int myColor;    
    private boolean [][] mayCastle;
    
    private static final int QUEEN_SIDE = 0;
    private static final int KING_SIDE = 1;
    
    private Vector [] pieces;
    private Vector [] captures;
    private Vector moves;
    private boolean [][] bitmap;
    
    /**
        Creates an empty board
    */
    public Board() {
        initBoard();
    }   
    
    /**
        Creates a board, sets the pieces and flags myColor as the parameter
        @param _myColor This player's pieces
    */
    public Board(int _myColor) {
        int piece_types[] = {Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING, Piece.BISHOP, Piece.KNIGHT, Piece.ROOK};
        initBoard();
        myColor = _myColor;
        for (int i = 0; i < piece_types.length; i++) {
            putPiece(i, 0, piece_types[i], Piece.BLACK);
            putPiece(i, 1, Piece.PAWN, Piece.BLACK);
            putPiece(i, 6, Piece.PAWN, Piece.WHITE);
            putPiece(i, 7, piece_types[i], Piece.WHITE);
        }  
/*      This is a good game to test, it has enpassant, check, checkmate, promotion and castle possibilities with a few pieces

        putPiece("B7", Piece.PAWN, Piece.BLACK);
        putPiece("C5", Piece.PAWN, Piece.WHITE);
        putPiece("E8", Piece.KING, Piece.BLACK);
        putPiece("H7", Piece.QUEEN, Piece.WHITE);
        putPiece("E1", Piece.KING, Piece.WHITE);
        putPiece("A1", Piece.ROOK, Piece.WHITE);
        putPiece("H1", Piece.ROOK, Piece.WHITE);   */
    }
    
    /**
        Initializes the board internals
    */
    private void initBoard() {
        mayCastle = new boolean[2][2];
        mayCastle[Piece.WHITE][QUEEN_SIDE] = true;
        mayCastle[Piece.WHITE][KING_SIDE] = true;
        mayCastle[Piece.BLACK][QUEEN_SIDE] = true;
        mayCastle[Piece.BLACK][KING_SIDE] = true;
        
        moves = new Vector();
        pieces = new Vector[2];
        pieces[Piece.WHITE] = new Vector();
        pieces[Piece.BLACK] = new Vector();
        captures = new Vector[2];
        captures[Piece.WHITE] = new Vector();
        captures[Piece.BLACK] = new Vector();
        myColor = Piece.WHITE;
        bitmap = new boolean[8][8];
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++)
                bitmap[i][j] = false;
    }

    /**
        Puts a piece on the board
        @param x X coordinate
        @param y Y coordinate
        @param type Piece type
        @param color Piece color
    */
    private void putPiece(int x, int y, int type, int color) {
        Piece piece = new Piece(type, color);
        movePiece(piece, new Cell(x, y));
        pieces[piece.getColor()].addElement(piece);
    }
    
    /**
        Puts a piece on the board
        @param piece Piece to put
        @param cell Where to put it
    */
    private void movePiece(Piece piece, Cell cell) {
        if (piece.getCell() != null) 
            bitmap[piece.getCell().getX()][piece.getCell().getY()] = false;
        piece.moveTo(cell);
        if (cell != null) 
            bitmap[cell.getX()][cell.getY()] = true;
    }

    /**
        Puts a piece on the board
        @param coord Algebric coordinates of the piece
        @param type Piece type
        @param color Piece color
    */
    public void putPiece(String coord, int type, int color) {
        Cell cell = Cell.NumericCoord(coord);
        putPiece(cell.getX(), cell.getY() , type, color);
    }
    
    /**
        Gets the piece in a square
        @param x Square coordinate
        @param y Square coordinate
        @return The piece object in the square or null, if there is none
    */
    private Piece getPiece(int x, int y) {
        for (int i = 0; i < 2; i++) {
            Enumeration e = pieces[i].elements();
            while (e.hasMoreElements()) {
                Piece piece = (Piece)e.nextElement();
                if (piece.getCell().equals(x, y))
                    return piece;
            }
        }
        return null;
    }
    
    /**
        Gets the piece in a square
        @param cell Square coordinates
        @return The piece object in the square or null, if there is none
    */
    private Piece getPiece(Cell cell) {
        return getPiece(cell.x, cell.y);
    }

    /**
        Gets the piece in a square
        @param coord Algebric coordinates
        @return The piece object in the square or null, if there is none
    */
    public Piece getPiece(String coord) {
        return getPiece(getCell(coord));
    }
    
    /**
        Gets the pieces for a color
        @param color Piece color
        @return Enumeration for the pieces
    */
    public Enumeration getPieces(int color) {
        return pieces[color].elements();
    }
    
    /**
        Gets the cell object for a square
        @param x Square coordinate
        @param y Square coordinate
        @return A cell contaning the corrdinates
    */
    private Cell getCell(int x, int y) {    
        return new Cell(x,y);       
    }
    
    /**
        Gets the cell object for a square
        @param coord Square coordinates
        @return A cell contaning the corrdinates
    */
    private Cell getCell(String coord) {
        return Cell.NumericCoord(coord);
    }
    
    /**
        Gets the "view" cell object for a square
        @param coord Square coordinates
        @return The square coordinates are reverted if the player's pieces are black. So that the game midlet can invisibly 
                 draw the pieces on the screen and keep the player's in the bottom.
    */
    public Cell getCellViewCoords(Cell cell) {
        if (myColor == Piece.WHITE)
            return new Cell(cell.getX(), cell.getY());
        else
            return new Cell(7 - cell.getX(), 7 - cell.getY());
    }

    /**
        Gets the player's pieces' color
    */
    public int getMyColor() {
        return myColor;
    }
    
    /**
        Gets the player's captures
        @return A vector containing the pieces the player captured
    */
    public Vector getMyCaptures() {
        return captures[myColor];
    }
    
    /**
        Gets the player's captured pieces
        @return A vector containing the pieces the opposition has captured
    */
    public Vector getOppositeCaptures() {
        return captures[1 - myColor];
    }
    
    /**
        Detects if a route contains any pieces
        @param start Route start cell
        @param end Route end cell 
        @return True if there are no pieces in the path. The start and end cells are not included in the search.
    */
    private boolean routeIsClear(Cell start, Cell end) {
        int dx = end.dx(start);
        int dy = end.dy(start);
        int x = start.getX();
        int y = start.getY();
        int incX = 0, incY = 0;
        
        if (dx != 0) 
            incX = dx / abs(dx);
        if (dy != 0) 
            incY = dy / abs(dy);
        
        x += incX;
        y += incY;
        while ((x != end.getX()) || (y != end.getY()) && Cell.inBounds(x) && Cell.inBounds(y)) {
            if (bitmap[x][y])
                return false;
            x += incX;
            y += incY;
        }
        return true;
    }

    /**
        Gets the cell objects for the squares in the route between start and end
        @param start Route start cell
        @param end Route end cell
        @return A vector containg the squares' cell objects. Includes the end square, not the start.
    */
    private Vector getRoute(Cell start, Cell end) {
        Vector result = new Vector();
        int dx = end.dx(start);
        int dy = end.dy(start);
        int x = start.getX();
        int y = start.getY();
        int incX = 0, incY = 0;
        
        if (dx != 0) 
            incX = dx / abs(dx);
        if (dy != 0) 
            incY = dy / abs(dy);
        
        x += incX;
        y += incY;
        while ((x != end.getX()) || (y != end.getY()) && Cell.inBounds(x) && Cell.inBounds(y)) {
            result.addElement(new Cell(x, y));
            x += incX;
            y += incY;
        }
        result.addElement(end);
        return result;
    }   

    /**
        Checks if it is possible to castle
        @param piece The king trying to castle
        @param cellEnd End square for the king's movement
        @return True if it is possible to castle to that square (there's no piece in between, the pieces haven't moved yet, there's no check in the jump cell)
    */
    private boolean tryCastle(Piece piece, Cell cellEnd) {
        int side = piece.getCell().dx(cellEnd) > 0 ? QUEEN_SIDE : KING_SIDE;
        Cell rookCell = side == QUEEN_SIDE ? getCell(0, piece.getCell().getY()) : getCell(7, piece.getCell().getY());
        Cell jumpCell = side == QUEEN_SIDE ? piece.getCell().adjacent(-1,0) : piece.getCell().adjacent(1,0);
        
        return ((mayCastle[piece.getColor()][side]) && 
                (routeIsClear(piece.getCell(), rookCell)) &&
                (!tryAttack(jumpCell, piece.getColor())));
    }

    /**
        Updates the casteling flag. If the user moved a king or a rook, it sets that side as can't castle
        @param piece Piece which moved
        @param cellStart Start square for the piece's movement
    */
    private void updateCastleFlag(Piece piece, Cell cellStart) {
        switch(piece.getType()) {
            case Piece.KING:
                mayCastle[piece.getColor()][QUEEN_SIDE] = false;
                mayCastle[piece.getColor()][KING_SIDE] = false;
                break;
            case Piece.ROOK:
                if (cellStart.getX() == 0)
                    mayCastle[piece.getColor()][QUEEN_SIDE] = false;
                else {
                    if (cellStart.getX() == 7) 
                        mayCastle[piece.getColor()][KING_SIDE] = false;
                }
        }
    }
    
    /**
        Updates the necessary objects for a piece movement
        @param piece Piece to move
        @param cell End square for the piece
        @param capture Capturing piece
        @param cellPassant If it is a enpassant movement, stores the captured pawn's square
        @return The movement record
    */
    private MoveRecord performMove(Piece piece, Cell cell, Piece capture, Cell cellPassant) {
        Cell save = piece.getCell();
        boolean isCastle = (piece.getType() == Piece.KING) && (abs(cell.dx(piece.getCell())) == 2);
        MoveRecord record;
        
        record = new MoveRecord(save, cell, capture, cellPassant, mayCastle);

        movePiece(piece, cell);
        updateCastleFlag(piece, cell);
        // Atualizar o roque
        if (isCastle) {
            if (save.dx(cell) < 0)
                movePiece(getPiece(7, save.getY()), save.adjacent(1, 0));
            else
                movePiece(getPiece(0, save.getY()), save.adjacent(-1,0));
        }
        if (capture != null) {
            pieces[capture.getColor()].removeElement(capture);
            captures[1 - capture.getColor()].addElement(capture);
        }
        return record;
    }
        
    /**
        Moves a piece. It checks the validity of the movement prior to performing it. This method doesn't update the internal objects, it
        calls performMove to do that
        @param piece Piece to move
        @param cellEnd End square coordinates
        @return true if the move was valid and performed
    */
    public boolean doMove(Piece piece, Cell cellEnd) {
        boolean isPassant = isPassantMove(piece, cellEnd);
        Piece capture;
        Cell cellPassant = null;
        MoveRecord record;
        
        if (isPassant) {
            cellPassant = getCell(cellEnd.getX(), piece.getCell().getY());
            capture = getPiece(cellPassant);
        }
        else 
            capture = getPiece(cellEnd);

        if (((capture == null) || (capture.getColor() != piece.getColor())) &&
            piece.isMoveAllowed(cellEnd, capture != null)) {
            switch (piece.getType()) {
            case Piece.KNIGHT:
                // NÃ£o verifica o cavalo
                break;
            case Piece.KING:
                // Rei nÃ£o pode entrar em cheque
                if ((abs(cellEnd.dx(piece.getCell())) == 2) && (!tryCastle(piece, cellEnd))) 
                    return false;
                break;
            default:
                if (!routeIsClear(piece.getCell(), cellEnd))
                    return false;
                // Outras peÃ§as nÃ£o pulam as peÃ§as inimigas
                break;
            }
            
            record = performMove(piece, cellEnd, capture, cellPassant);
            
            if (checkForChecks(piece.getColor())) {
                undoMove(record);
                return false;
            }
            else {
                moves.addElement(record);
                return true;
            }
        }
        else
            return false;
    }
    
    /**
        Moves a piece. Overloaded version for algebric coordinates as the second parameter
        @param piece Piece to move
        @param coords Movement end square
        @return true if the move was valid and performed
    */
    public boolean doMove(Piece piece, String coords) {
        return doMove(piece, Cell.NumericCoord(coords));
    }
    
    /**
        Moves a piece. Overloaded version for algebric coordinates 
        @param coords Movement coordinates
        @return true if the move was valid and performed
    */
    public boolean doMove(String move) {
        return doMove(getPiece(move.substring(0,2)), move.substring(2));
    }
    
    /**
        Moves and promotes a pawn.
        @param move Movement coordinates
        @param type Pawn promotion type
        @return true if the move was valid and performed
    */
    public boolean doMove(String move, int type) {
        Piece piece = getPiece(move.substring(0,2));        
        if (piece.getType() == Piece.PAWN) {
            try {
                if (doMove(piece, move.substring(2))) {
                    piece.promote(type);
                    return true;
                }
            }
            catch (Exception e) {
            }
        }
        return false;
    }
    
    /**
        Promotes a pawn after the move was done (necessary for the Midlet interface, as promotion can only be detected after
        the pawn has moved, thus keeping simple the checking methods)
        @param piece Pawn to promote
        @param type Pawn promotion type
    */
    public void commitPromotion(Piece piece, int type) throws IllegalMoveException {
        MoveRecord record = (MoveRecord)moves.lastElement();
        if (record.getEndCell().equals(piece.getCell())) {
            piece.promote(type);
            record.updatePromotion(piece);
        }
        else
            throw new IllegalMoveException("Cells don't match");
    }
    
    /**
        Undoes a move record
        @param record Move record to undo
    */
    private void undoMove(MoveRecord record) {
        movePiece(getPiece(record.getEndCell()), record.getStartCell());
        if (record.getPromoted() != null)
            getPiece(record.getStartCell()).demote();
        if (record.getCaptured() != null) {
            captures[1 - record.getCaptured().getColor()].removeElement(record.getCaptured());
            pieces[record.getCaptured().getColor()].addElement(record.getCaptured());
            if (record.getPassantCell() == null) 
                movePiece(record.getCaptured(), record.getEndCell());
            else
                movePiece(record.getCaptured(), record.getPassantCell());
        }
        mayCastle = record.getCastleFlags();
    }
    
    /**
        Undoes the last move
    */
    public void undoLastMove() {
        if (moves.size() > 0) {
            undoMove((MoveRecord)moves.lastElement());
            moves.removeElementAt(moves.size() - 1);
        }
    }

    /**
        Retrieves the coordinates for the last movement
        @param Algebric coordinates for the last move
    */
    public String getLastMove() {
        if (moves.size() > 0) 
            return "" + (MoveRecord)moves.lastElement();
        else
            return "";
    }
    
    /**
        Detects if a square is attackable by any piece on the opposite side
        @param cell Square to check
        @param color Color for the side to check
        @return True if the square is attacked by any of the opposition pieces
    */
    private boolean tryAttack(Cell cell, int color) {
        Enumeration e = pieces[1 - color].elements();
        Piece p;
        int dx, dy;
        
        while (e.hasMoreElements()) {
            p = (Piece)e.nextElement();
            if (p.isMoveAllowed(cell, true)) {
                switch (p.getType()) {
                case Piece.KNIGHT:
                case Piece.KING:
                    return true;
                case Piece.PAWN:
                    // Se o peÃ£o estÃ¡ na diagonal, ele estÃ¡ atacando
                    if (abs(p.getCell().dx(cell)) == abs(p.getCell().dx(cell)))
                        return true;
                    break;
                default:
                    // Se a rota estÃ¡ vÃ¡lida e limpa, a peÃ§a estÃ¡ atacando
                    if (routeIsClear(p.getCell(), cell)) 
                        return true;
                    break;
                }
            }
        }
        return false;
    } 

    /**
        Cheks for checked kings
        @param color The side to check
        @return True if the king for "color" is in check
    */
    public boolean checkForChecks(int color) {  
        Piece king = findKing(color);
        if (king != null)
            return tryAttack(king.getCell(), color);
        else
            return false;
    }
    
    /**
        Finds a player's king
        @param color The king's color to find
        @return The piece object for the "color"'s king
    */
    private Piece findKing(int color) {
        Enumeration e = pieces[color].elements();
        Piece p;
        while (e.hasMoreElements()) {
            p = (Piece)e.nextElement();
            if (p.getType() == Piece.KING)
                return p;
        }
        return null;
    }
    
    /**
        Overloaded helper version for checkForChecks, implicetly considers the player's side
        @return True if the player is under check
    */
    public boolean checkForChecks() {
        return checkForChecks(myColor);
    }
    
    /**
        Modulus
        @param x integer to retrieve the modulo
        @return Modulo of x
    */
    protected static int abs(int x) {
        return x >= 0 ? x : 0 - x;
    }
    
    /**
        Detects an en passant move
        @param piece Moving piece
        @param cellEnd Movement end square
        @return true if the player is trying an en passant movement
    */
    private boolean isPassantMove(Piece piece, Cell cellEnd) {
        if ((piece.getType() == Piece.PAWN) &&
            (abs(cellEnd.dx(piece.getCell())) == 1) &&
            (abs(cellEnd.dx(piece.getCell())) == 1) &&
            ((cellEnd.getY() == 5) || (cellEnd.getY() == 2))) {
            MoveRecord record = (MoveRecord)moves.lastElement();            
            return (record != null) &&
                   (getPiece(record.getEndCell()).getType() == Piece.PAWN) &&
                   (abs(record.getStartCell().getY() - record.getEndCell().getY()) == 2) &&
                   (record.getEndCell().getX() == cellEnd.getX());
        }
        return false;
    }

    /**
        Detects the piece that is threating a king
        @param king King in check
        @return The attacking piece or null if there is more than one
    */
    private Piece getThreat(Piece king) {   
        Enumeration e = pieces[1 - king.getColor()].elements();
        Piece p;
        Piece threat = null;
        int dx, dy;
        int count = 0;
        
        while (e.hasMoreElements() && count < 2) {
            p = (Piece)e.nextElement();
            if (p.isMoveAllowed(king.getCell(), true)) {
                switch (p.getType()) {
                case Piece.KNIGHT:
                    threat = p;
                    count++;
                    break;
                case Piece.PAWN:
                    // Se o peÃ£o estÃ¡ na diagonal, ele estÃ¡ atacando
                    if (abs(p.getCell().dx(king.getCell())) == abs(p.getCell().dx(king.getCell()))) {
                        threat = p;
                        count++;
                    }
                    break;
                default:
                    // Se a rota estÃ¡ vÃ¡lida e limpa, a peÃ§a estÃ¡ atacando
                    if (routeIsClear(p.getCell(), king.getCell())) {
                        threat = p;
                        count++;
                    }
                    break;
                }
            }
        }
        if (count == 1)
            return threat;
        else
            return null;
    }
    
    /**
        Detects if a king is allowed to make a move
        @param king The checked king
        @return True if the king may move to a square safely
    */
    private boolean kingMayMove(Piece king) {
        Cell cell;
        Piece capture;
        MoveRecord record;
        boolean result;
        Enumeration possible = king.getPossibleMoves().elements();
        while (possible.hasMoreElements()) {
            cell = (Cell)possible.nextElement();
            capture = getPiece(cell);
            if ((capture == null) || (capture.getColor() != king.getColor())) {
                record = performMove(king, cell, capture, null);
                result = tryAttack(cell, king.getColor());              
                undoMove(record);
                if (!result) 
                    return true;
            }
        }
        return false;
    }
    
    /**
        Detects a checkmate
        @param color Color to check
        @return True if it is a checkmate (doesn't check for checks in advance)
    */
    public boolean detectMate(int color) {
        Piece king = findKing(color);
        Piece threat;

        if (king == null) 
            return false;
        // o rei pode mover-se
        if (kingMayMove(king)) 
            return false;
        // se mais de uma peÃ§a ameaÃ§a, nÃ£o tem mais saÃ­da
        threat = getThreat(king);
        if (threat != null) {
            Enumeration e = pieces[color].elements();
            Enumeration f;
            Piece p;
            Vector route = getRoute(king.getCell(), threat.getCell());
            // a peÃ§a ameaÃ§adora pode ser capturada
            while (e.hasMoreElements()) {
                p = (Piece)e.nextElement();
                if (p != king) {
                    f = route.elements();
                    while (f.hasMoreElements()) {
                        if (doMove(p, ((Cell)f.nextElement()).toString())) {
                            undoLastMove();
                            return false;
                        }
                    }
                }
            }
            // ou o caminho pode ser bloqueado
            // desde que nÃ£o deixe o rei em cheque tambÃ©m           
        }
        return true;
    }

    /**
        Detects a checkmate. Overloaded helper version
        @return True if it is a checkmate upon this player(doesn't check for checks in advance)
    */
    public boolean detectMate() {
        return detectMate(myColor);
    }

}