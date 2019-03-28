package com.yl.heartratedetectZTShao;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals("Wrong",4, 2 + 2);
    }
    @Test
    public void multiply_isCorrect() throws Exception {
        assertEquals("Product wrong",9,3*3);
    }
    @Test(timeout=100) public void method(){}
}