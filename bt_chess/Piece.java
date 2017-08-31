package teste;

import java.util.Vector;

public class Piece {

    public final static int WHITE = 0;
    public final static int BLACK = 1;  
    
    public final static int PAWN = 0;
    public final static int ROOK = 1;
    public final static int BISHOP = 2;
    public final static int KNIGHT = 3;
    public final static int QUEEN = 4;
    public final static int KING = 5;

    private int color, type;
    private Cell cell;
    
    public int getColor() {
        return color;
    }
    
    public int getType() {
        return type;
    }
    
    public Piece(int _type, int _color) {
        type = _type;
        color = _color;
    }
    
    public Cell getCell() {
        return cell;
    }
    
    public boolean inCell(String coords) {
        return cell.equals(coords);
    }
    
    protected boolean inCell(int x, int y) {
        return (cell.getX() == x) && (cell.getY() == y);
    }
    
    public boolean isMoveAllowed(String coord, boolean capturing) {
        return isMoveAllowed(Cell.NumericCoord(coord), capturing);
    }

    protected boolean isMoveAllowed(Cell end, boolean capturing) {
        int dx = Board.abs(end.dx(cell));
        int dy = Board.abs(end.dy(cell));
        switch(type) {
            case PAWN:
                return (((color == WHITE) && (end.dy(cell) < 0)) || ((color == BLACK) && (end.dy(cell) > 0))) &&        
                            (dy < 3) && 
                            (((capturing) && (dx == 1) && (dy == 1)) || 
                            ((!capturing) && (dx == 0) && ((dy == 1) ||
                                        (((color == WHITE) && (cell.getY() == 6)) || ((color == BLACK) && (cell.getY() == 1))))));
            case ROOK:
                return (dx == 0) || (dy == 0);
            case BISHOP:
                return (dx == dy);
            case KNIGHT:
                return ((dx == 2) && (dy == 1)) || ((dx == 1) && (dy == 2));
            case QUEEN:
                return (dx == 0) || (dy == 0) || (dx == dy);
            case KING:
                return ((dx < 2) && (dy < 2)) || ((dy == 0) && (dx == 2));
        }
        return false;
    }
    
    public boolean shouldPromote() {
        return (type == Piece.PAWN) && ((cell.getY() == 0) || (cell.getY() == 7));
    }
    
    public void promote(int new_type) throws IllegalMoveException {
        if (type == Piece.PAWN)
            type = new_type;
        else
            throw new IllegalMoveException("Invalid piece type");
    }
    
    public void demote() {
        type = Piece.PAWN;
    }
    
    protected void moveTo(Cell newCell) {
        cell = newCell;
    }
    
    public void moveTo(String coord) {
        cell = Cell.NumericCoord(coord);
    }
    
    public String toString() {
        StringBuffer b = new StringBuffer();
        switch(type) {
            case PAWN:
                b.append("P");
                break;
            case ROOK:
                b.append("R");
                break;
            case BISHOP:
                b.append("B");
                break;
            case KNIGHT:
                b.append("N");
                break;
            case QUEEN:
                b.append("Q");
                break;
            case KING:
                b.append("K");
                break;
        }
        if (color == WHITE) 
            b.append("W");
        else
            b.append("B");
        b.append("@");
        b.append(cell.toString());
        return b.toString();
    }
    
    public Vector getPossibleMoves() {
        Vector result = new Vector();
        Cell new_cell;
        
        switch(type) {
            case KING:
                for (int i = -1; i < 2; i++) 
                    for (int j = -1; j < 2; j++) 
                        if ((i != 0) || (j != 0)) {
                            new_cell = cell.adjacent(i, j);
                            if (new_cell != null)
                                result.addElement(new_cell);
                        }
                break;
        }
        return result;
    }
    
}        