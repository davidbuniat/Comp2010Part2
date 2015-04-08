package comp2010.target;

public class ConstantVariableFolding
{
    public int methodOne(){
        int a = 42;
        int b = (a + 764) * 3;
        return b + 1234 - a;
    }

    public double methodTwo(){
        double i = 0;
        int j = 1;
        return i + j;
    }

    public boolean methodThree(){
        int x = 12345;
        int y = 54321;
        return x > y;
    }
    
    public float methodFour(){
        float x = 12344125F;
        float y = 2145F;
        return x/y;
    }
    
    public int methodFive(){
        int x = 2134;
        int z = (x + 12345)/2;
        int y = 54321%z;
        return y%5;
    }
    
    public double methodSix(){
        int asd = 12345;
        int div = 54321;
        return asd*div/Math.PI;
    }
}