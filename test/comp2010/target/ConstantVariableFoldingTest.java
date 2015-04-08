package comp2010.target;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by ntrolls on 04/03/15.
 */
public class ConstantVariableFoldingTest {

    ConstantVariableFolding cvf = new ConstantVariableFolding();

    @Test
    public void testMethodOne(){
        assertEquals(3610, cvf.methodOne());
    }

    @Test
    public void testMethodTwo(){
        assertEquals(1.0, cvf.methodTwo(), 0);
    }

    @Test
    public void testMethodThree(){
        assertEquals(false, cvf.methodThree());
    }
    

	@Test
    public void testMethodFour(){
    	float ans = 12344125F/2145F;
        assertEquals( ans, cvf.methodFour(), 1);
    }
    
    @Test
    public void testMethodFive(){
    	int x = 2134;
        int z = (x + 12345)/2;
        int y = 54321%z;
        int fin = y%5;
        		
        assertEquals(fin, cvf.methodFive());
    }
    
	@Test
    public void testMethodSix(){
    	int asd = 12345;
        int div = 54321;
        double ans =  asd*div/Math.PI*1;
        assertEquals(ans, cvf.methodSix(), 1);
    }
}

