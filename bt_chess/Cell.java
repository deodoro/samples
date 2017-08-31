package teste;

public class Cell {
    
    int x, y;
    boolean selected;
    
    public Cell(int _x, int _y) {
        x = _x;
        y = _y;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public boolean equals(Cell other) {
        return (x == other.x) && (y == other.y);
    }
    
    public boolean equals(String coord) {
        Cell other = NumericCoord(coord);
        return (x == other.x) && (y == other.y);
    }
    
    public boolean equals(int _x, int _y) {
        return (x == _x) && (y == _y);
    }
    
    public String toString() {
        return AlgebricCoord(x, y);
    }

    public static String AlgebricCoord(int x, int y) {
        StringBuffer s = new StringBuffer();
        
        s.append(String.valueOf((char)('A' + x)));
        s.append(7 - y + 1);
        return s.toString();
    }

    public static Cell NumericCoord(String coord) {
        return new Cell(coord.charAt(0) - 'A', 8 - ((int)coord.charAt(1) - '0'));
    }
    
    protected static boolean inBounds(int v) {
        return (v >= 0) && (v < 8);
    }
    
    public int dx(Cell other) {
        return x - other.x;
    }

    public int dy(Cell other) {
        return y - other.y;
    }
    
    public Cell adjacent(int dx, int dy) {
        if (inBounds(x + dx) && inBounds(y + dy))
            return new Cell(x + dx, y + dy);
        else
            return null;
    }
    
}        