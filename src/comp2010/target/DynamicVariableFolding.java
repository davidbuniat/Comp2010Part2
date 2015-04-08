package comp2010.target;

public class DynamicVariableFolding {
    public int methodOne() {
        int a = 42;
        int b = (a + 764) * 3;
        a = 22;
        return b + 1234 - a;
    }

    public boolean methodTwo() {
        int x = 12345;
        int y = 54321;
        System.out.println(x < y);
        y = 0;
        return x > y;
    }

    public int methodThree() {
        int i = 0;
        int j = i + 3;
        i = j + 4;
        j = i + 5;
        return i * j;
    }
    
    public int methodFour() {
        int i = 0;
        int j = i + 3;
        i = j + 4;
        i=0;
        j = i + 5;
        return i/2;
    }
    
    public int methodFive() {
        int f = 0;
        int i = f*12;
        int q = i + 3;
        i = q + 4;
        int a = i + 5 - q;
        return q * a;
    }
    
    public int methodSix() {
        int a = 0;
        int b = a + 3;
        int c = b + 5;
        int d = c + 7;
        b = d-3;
        c = c-5;
        a = a-3;

        return b+a+c;
    }
}