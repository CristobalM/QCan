package cl.uchile.dcc.qcan;


import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;


public class TestSuiteOne {

    File fromResource(String resourcePath){
		return new File(Objects.requireNonNull(getClass().getClassLoader().getResource(resourcePath)).getFile());
	}

	@Test
    public void labelTest() {
        try {
            QueryParserTest qp = new QueryParserTest(fromResource("testFiles/test01.txt"));
            assertTrue(qp.equalQueries(0, 1));
            qp = new QueryParserTest(fromResource("testFiles/test02.txt"));
            assertTrue(qp.equalQueries(0, 1));
            assertTrue(qp.equalQueries(1, 2));
            qp = new QueryParserTest(fromResource("testFiles/test03.txt"));
            assertTrue(qp.equalQueries(0, 1));
            qp = new QueryParserTest(fromResource("testFiles/test04.txt"));
            assertTrue(qp.equalQueries(0, 1));
            qp = new QueryParserTest(fromResource("testFiles/test08.txt"));
            assertTrue(qp.equalQueries(0, 1));
            assertTrue(qp.equalQueries(1, 2));
            assertTrue(qp.equalQueries(3, 4));
            assertTrue(qp.equalQueries(5, 6));
            assertTrue(qp.equalQueries(6, 7));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void unionTest() {
        try {
            QueryParserTest qp = new QueryParserTest(fromResource("testFiles/test06.txt"));
            assertTrue(qp.equalQueries(0, 1));
            assertTrue(qp.equalQueries(1, 2));
            assertTrue(qp.equalQueries(3, 4));
            assertTrue(qp.equalQueries(4, 5));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void branchRelabelTest() {
        try {
            QueryParserTest qp = new QueryParserTest(fromResource("testFiles/test16.txt"));
            assertTrue(qp.equalQueries(0, 1));
            assertTrue(qp.equalQueries(1, 2));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void branchRelabelTest2() {
        try {
            QueryParserTest qp = new QueryParserTest(fromResource("testFiles/test14.txt"));
            assertTrue(qp.equalQueries(0, 1));
            assertTrue(qp.equalQueries(2, 3));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void UCQMinimisationTest() {
        try {
            QueryParserTest qp = new QueryParserTest(fromResource("testFiles/test20.txt"));
            assertTrue(qp.equalQueries(0, 1));
            assertTrue(qp.equalQueries(1, 2));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void namedGraphTest() {
        try {
            QueryParserTest qp = new QueryParserTest(fromResource("testFiles/test13.txt"));
            assertTrue(qp.equalQueries(0, 1));
            assertTrue(qp.equalQueries(2, 3));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void filterTest() {
        try {
            QueryParserTest qp = new QueryParserTest(fromResource("testFiles/test15.txt"));
            assertTrue(qp.equalQueries(0, 1));
            assertFalse(qp.equalQueries(1, 2));
            QueryParserTest qp1 = new QueryParserTest(fromResource("testFiles/filterTest1"));
            assertTrue(qp1.equalQueries(0, 1));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void optionalTest() {
        try {
            QueryParserTest qp = new QueryParserTest(fromResource("testFiles/test11.txt"));
            assertTrue(qp.equalQueries(0, 1));
            //assertTrue(qp.equalQueries(2, 3));
            assertTrue(qp.equalQueries(4, 5));
            assertTrue(qp.equalQueries(6, 7));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void propertyPathTest() {
        QueryParserTest qp;
        try {
            qp = new QueryParserTest(fromResource("testFiles/propertyPathsTest1.txt"));
            assertTrue(qp.equalQueries(0, 1));
            assertTrue(qp.equalQueries(1, 2));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void generalTest() {
        try {
            QueryParserTest qp = new QueryParserTest(fromResource("testFiles/test17.txt"));
            //qp = new QueryParserTest(new File("testFiles/test24.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
