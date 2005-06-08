// Copyright (c) 2005  Scott Lamb <slamb@slamb.org>
//
// arch-tag: 9F734ECE-788F-4AF9-8662-FB2245C69A44

package net.spy.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import net.spy.db.DBSP;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

import net.spy.test.db.BooleanTest;

public class SPTTest extends MockObjectTestCase {
    private Mock connMock;
    private Connection conn;
    private Mock stMock;

    public void setUp() {
        connMock = new Mock(Connection.class);
        conn = (Connection) connMock.proxy();

        stMock = new Mock(PreparedStatement.class);
    }

    private void testCallSequence(boolean[] booleans) throws Exception {
        stMock.expects(atLeastOnce()).method("setQueryTimeout");
        stMock.expects(atLeastOnce()).method("setMaxRows");

        BooleanTest bt = new BooleanTest(conn);

        for (boolean aBoolean : booleans) {
            connMock.expects(once())
                    .method("prepareStatement")
                    .with(eq("select ? as a_boolean\n"))
                    .will(returnValue(stMock.proxy()));
            bt.setABoolean(aBoolean);
            Mock rsMock = new Mock(ResultSet.class);
            ResultSet rs = (ResultSet) rsMock.proxy();
            stMock.expects(once())
                  .method("setBoolean")
                  .with(eq(1), eq(aBoolean));
            stMock.expects(once())
                  .method("executeQuery")
                  .will(returnValue(rs));
            ResultSet actual = bt.executeQuery();
            assertEquals(rs, actual);
        }
        stMock.expects(once()).method("close");
        bt.close();
    }

    public void testSingleCalls() throws Exception {
        testCallSequence(new boolean[] { false });
        testCallSequence(new boolean[] { true });
    }

    public void testMultipleCalls() throws Exception {
        testCallSequence(new boolean[] { false, true });
    }
}
