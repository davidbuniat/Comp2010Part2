package comp2010.target;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;

public class DynamicVariableFoldingTest
{
    DynamicVariableFolding dvf = new DynamicVariableFolding();
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    @Before
    public void setUpStreams()
    {
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void cleanUpStreams()
    {
        System.setOut(null);
    }

    @Test
    public void testMethodOne()
    {
        assertEquals(3630, dvf.methodOne());
    }

    @Test
    public void testMethodTwoOut()
    {
        dvf.methodTwo();
        assertEquals("true\n", outContent.toString());
    }

    @Test
    public void testMethodTwoReturn()
    {
        assertEquals(true, dvf.methodTwo());
    }

    @Test
    public void testMethodThree()
    {
        assertEquals(84, dvf.methodThree());
    }
    
    @Test
    public void testMethodFour(){
    	int i = 0;
        int j = i + 3;
        i = j + 4;
        i=0;
        j = i + 5;
        int ans =  i/2;
        assertEquals(ans, dvf.methodFour());
    }
    
    @Test
    public void testMethodFive(){
    	int f = 0;
        int i = f*12;
        int q = i + 3;
        i = q + 4;
        int a = i + 5 - q;
        int ans =  q * a;
        assertEquals(ans, dvf.methodFive());
    }
    
    @Test
    public void testMethodSix(){
    	 int a = 0;
         int b = a + 3;
         int c = b + 5;
         int d = c + 7;
         b = d-3;
         c = c-5;
         a = a-3;

         int ans = b+a+c;
        assertEquals(ans, dvf.methodSix());
    }
}
